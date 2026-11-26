package com.example.model

enum class UsdtNetwork(
    val displayName: String,
    val shortCode: String,
    val contractAddress: String,
    val rpcUrl: String,
    val isTron: Boolean
) {
    TRC20(
        displayName = "TRON (TRC-20)",
        shortCode = "TRC-20",
        contractAddress = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",
        rpcUrl = "https://api.trongrid.io/wallet/gettransactioninfobyid",
        isTron = true
    ),
    ERC20(
        displayName = "Ethereum (ERC-20)",
        shortCode = "ERC-20",
        contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7",
        rpcUrl = "https://ethereum-rpc.publicnode.com",
        isTron = false
    ),
    BEP20(
        displayName = "BNB Smart Chain (BEP-20)",
        shortCode = "BEP-20",
        contractAddress = "0x55d398326f99059fF775485246999027B3197955",
        rpcUrl = "https://bsc-rpc.publicnode.com",
        isTron = false
    ),
    POLYGON(
        displayName = "Polygon PoS (USDT)",
        shortCode = "Polygon",
        contractAddress = "0xc2132D05D31c914a87C6611C10748AEb04B58e8F",
        rpcUrl = "https://polygon-rpc.com",
        isTron = false
    )
}

data class UsdtIapItem(
    val id: String,
    val title: String,
    val badge: String,
    val priceUsd: Double,
    val usdtAmount: String,
    val description: String,
    val highlights: List<String>
)

object UsdtIapCatalog {
    val items: List<UsdtIapItem> = listOf(
        UsdtIapItem(
            id = "item_report_pack",
            title = "Forensic Audit Pack (10x)",
            badge = "ITEM",
            priceUsd = 1.49,
            usdtAmount = "1.49",
            description = "Signed SHA-256 certificate export bundle for 10 erasure runs.",
            highlights = listOf("1.49 USDT one-time item", "SHA-256 verification hash", "Plain-text & compliance logs")
        ),
        UsdtIapItem(
            id = "item_pro_1m",
            title = "iShredder Pro (1 Month)",
            badge = "PRO",
            priceUsd = 4.99,
            usdtAmount = "4.99",
            description = "Professional standards including NIST SP 800-88, DoD 5220.22-M & HMG InfoSec.",
            highlights = listOf("4.99 USDT (30 days)", "14 PRO erasure methods", "Unlimited File Explorer shredding")
        ),
        UsdtIapItem(
            id = "item_mil_1m",
            title = "iShredder Military (1 Month)",
            badge = "MIL",
            priceUsd = 9.99,
            usdtAmount = "9.99",
            description = "Government & military algorithms including BSI-VSITR, NATO & DoD ECE.",
            highlights = listOf("9.99 USDT (30 days)", "All 27 STD/PRO/MIL algorithms", "Free Space & Contacts sanitization")
        ),
        UsdtIapItem(
            id = "item_excellent_1y",
            title = "iShredder Excellent (1 Year)",
            badge = "ANNUAL",
            priceUsd = 19.99,
            usdtAmount = "19.99",
            description = "Full annual license across Android 5.0 through Android 17 devices.",
            highlights = listOf("19.99 USDT / year", "All 27 algorithms up to 50 passes", "Unlimited Erasure Reports")
        ),
        UsdtIapItem(
            id = "item_pro_lifetime",
            title = "iShredder Excellent Pro (Lifetime)",
            badge = "LIFETIME",
            priceUsd = 29.99,
            usdtAmount = "29.99",
            description = "Permanent one-time license for all military & Protectstar™ ASDA algorithms.",
            highlights = listOf("29.99 USDT one-time", "Never expires • No recurring billing", "All 27 erasure standards")
        ),
        UsdtIapItem(
            id = "item_enterprise_lifetime",
            title = "Forensic Enterprise (Lifetime)",
            badge = "MAX",
            priceUsd = 39.99,
            usdtAmount = "39.99",
            description = "Complete forensic suite with Gutmann 35-pass, SDA 50-pass & unlimited exports.",
            highlights = listOf("39.99 USDT one-time", "Maximum 50-pass SDA protection", "Full audit trail & PIN security")
        )
    )
}
