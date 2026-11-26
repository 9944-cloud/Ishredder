package com.example.engine

import com.example.model.UsdtNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class UsdtVerificationResult {
    data class Verified(val blockNumber: String, val contractMatched: Boolean) : UsdtVerificationResult()
    data class Failed(val reason: String) : UsdtVerificationResult()
}

class UsdtBlockchainVerifier {

    fun isValidTxHash(txHashInput: String, network: UsdtNetwork): Boolean {
        val txHash = txHashInput.trim()
        return if (network.isTron) {
            txHash.matches(Regex("^[0-9a-fA-F]{64}$"))
        } else {
            txHash.matches(Regex("^0x[0-9a-fA-F]{64}$"))
        }
    }

    fun isValidMerchantAddress(addressInput: String, network: UsdtNetwork): Boolean {
        val addr = addressInput.trim()
        return if (network.isTron) {
            addr.matches(Regex("^T[1-9A-HJ-NP-Za-km-z]{33}$"))
        } else {
            addr.matches(Regex("^0x[0-9a-fA-F]{40}$"))
        }
    }

    suspend fun verifyTransaction(
        txHashInput: String,
        network: UsdtNetwork,
        merchantAddressInput: String = ""
    ): UsdtVerificationResult = withContext(Dispatchers.IO) {
        val txHash = txHashInput.trim()
        val merchantAddress = merchantAddressInput.trim()
        if (merchantAddress.isNotEmpty() && !isValidMerchantAddress(merchantAddress, network)) {
            return@withContext UsdtVerificationResult.Failed(
                if (network.isTron) {
                    "Invalid TRON receiving address (must start with 'T', 34 Base58 chars)."
                } else {
                    "Invalid ${network.shortCode} receiving address (must be 0x + 40 hex chars)."
                }
            )
        }
        if (network.isTron) {
            if (!isValidTxHash(txHash, network)) {
                return@withContext UsdtVerificationResult.Failed(
                    "Invalid TRON TxID. Expected 64 hexadecimal characters."
                )
            }
            verifyTronUsdt(txHash, network)
        } else {
            if (!isValidTxHash(txHash, network)) {
                return@withContext UsdtVerificationResult.Failed(
                    "Invalid ${network.shortCode} TxHash. Expected 0x followed by 64 hex characters."
                )
            }
            verifyEvmUsdt(txHash, network, merchantAddress)
        }
    }

    private fun verifyTronUsdt(txHash: String, network: UsdtNetwork): UsdtVerificationResult {
        return try {
            val payload = JSONObject().put("value", txHash).toString()
            val responseText = postJson(network.rpcUrl, payload)
            if (responseText.isBlank() || responseText == "{}") {
                return UsdtVerificationResult.Failed("Transaction not found on TRON mainnet or still pending.")
            }
            val json = JSONObject(responseText)
            val blockNumber = json.optLong("blockNumber", 0L)
            val receipt = json.optJSONObject("receipt")
            val result = receipt?.optString("result", "") ?: ""
            val isSuccess = result == "SUCCESS" || (blockNumber > 0L && result.isEmpty())
            if (!isSuccess || blockNumber <= 0L) {
                return UsdtVerificationResult.Failed("Transaction failed or not yet confirmed on TRON.")
            }
            val logs = json.optJSONArray("log")
            var usdtContractMatched = false
            if (logs != null) {
                for (i in 0 until logs.length()) {
                    val entry = logs.optJSONObject(i) ?: continue
                    val addrHex = entry.optString("address", "").lowercase()
                    // Official TRON USDT hex is 41a614f803b6fd780986a42c78ec9c7f77e6ded13c
                    if (addrHex == "41a614f803b6fd780986a42c78ec9c7f77e6ded13c" ||
                        addrHex == "a614f803b6fd780986a42c78ec9c7f77e6ded13c"
                    ) {
                        usdtContractMatched = true
                        break
                    }
                }
            }
            if (!usdtContractMatched) {
                return UsdtVerificationResult.Failed(
                    "Confirmed TRON transaction does not match the official Tether USDT (TRC-20) contract."
                )
            }
            UsdtVerificationResult.Verified(
                blockNumber = "#$blockNumber",
                contractMatched = true
            )
        } catch (e: Exception) {
            UsdtVerificationResult.Failed("TRON network error: ${e.message ?: "Unable to verify"}")
        }
    }

    private fun verifyEvmUsdt(
        txHash: String,
        network: UsdtNetwork,
        merchantAddress: String
    ): UsdtVerificationResult {
        return try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "eth_getTransactionReceipt")
                put("params", JSONArray().put(txHash))
            }.toString()

            val responseText = postJson(network.rpcUrl, body)
            val json = JSONObject(responseText)
            if (json.has("error") && !json.isNull("error")) {
                val msg = json.optJSONObject("error")?.optString("message") ?: "RPC error"
                return UsdtVerificationResult.Failed("${network.shortCode} RPC error: $msg")
            }
            val receipt = json.optJSONObject("result")
                ?: return UsdtVerificationResult.Failed("Transaction receipt not found on ${network.shortCode}.")

            val status = receipt.optString("status", "")
            if (status != "0x1") {
                return UsdtVerificationResult.Failed("Transaction did not succeed on ${network.shortCode} (status=$status).")
            }

            val blockHex = receipt.optString("blockNumber", "0x0")
            val blockNum = blockHex.removePrefix("0x").toLongOrNull(16) ?: 0L
            val targetContract = network.contractAddress.lowercase()
            val transferSig = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef"
            val expectedToTopic = if (merchantAddress.isNotEmpty()) {
                "0x" + merchantAddress.removePrefix("0x").lowercase().padStart(64, '0')
            } else {
                null
            }
            val logs = receipt.optJSONArray("logs")
            var usdtMatched = false
            var recipientMatched = expectedToTopic == null
            if (logs != null) {
                for (i in 0 until logs.length()) {
                    val logObj = logs.optJSONObject(i) ?: continue
                    if (logObj.optString("address", "").lowercase() == targetContract) {
                        usdtMatched = true
                        val topics = logObj.optJSONArray("topics")
                        if (expectedToTopic != null &&
                            topics != null &&
                            topics.length() >= 3 &&
                            topics.optString(0, "").lowercase() == transferSig &&
                            topics.optString(2, "").lowercase() == expectedToTopic
                        ) {
                            recipientMatched = true
                            break
                        }
                    }
                }
            }
            if (!usdtMatched) {
                return UsdtVerificationResult.Failed(
                    "Confirmed ${network.shortCode} transaction does not transfer official USDT (${network.contractAddress})."
                )
            }
            if (!recipientMatched) {
                return UsdtVerificationResult.Failed(
                    "USDT transfer recipient does not match configured receiving address."
                )
            }
            UsdtVerificationResult.Verified(
                blockNumber = "#$blockNum",
                contractMatched = true
            )
        } catch (e: Exception) {
            UsdtVerificationResult.Failed("${network.shortCode} network error: ${e.message ?: "Unable to verify"}")
        }
    }

    private fun postJson(endpoint: String, jsonBody: String): String {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(jsonBody)
            writer.flush()
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(stream ?: return "", Charsets.UTF_8)).use { it.readText() }
        conn.disconnect()
        return text
    }
}
