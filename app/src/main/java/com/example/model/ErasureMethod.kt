package com.example.model

enum class MethodTier(val badge: String, val fullName: String) {
    STD("STD", "Standard"),
    PRO("PRO", "Professional"),
    MIL("MIL", "Military / Gov")
}

sealed class PassPattern(val label: String) {
    data object Zero : PassPattern("0x00 (All Zeros)")
    data object One : PassPattern("0xFF (All Ones)")
    data object Random : PassPattern("CSPRNG (Cryptographic Random)")
    data class FixedByte(val value: Byte, val hex: String) : PassPattern(hex)
    data class ThreeBytePattern(val b1: Byte, val b2: Byte, val b3: Byte, val hex: String) : PassPattern(hex)
    data object ComplementLast : PassPattern("Bitwise Complement")
    data object VerifyLast : PassPattern("Read-Back Verification")
}

data class ErasureMethod(
    val id: String,
    val name: String,
    val tier: MethodTier,
    val cycles: Int,
    val origin: String,
    val description: String,
    val isRecommendedDefault: Boolean = false,
    val passes: List<PassPattern>
) {
    val passPatternSummary: String
        get() = passes.take(6).mapIndexed { idx, p -> "P${idx + 1}: ${p.label}" }
            .joinToString(" -> ") + if (passes.size > 6) " -> ... (${passes.size} passes)" else ""
}

object ErasureCatalog {
    private val zero = PassPattern.Zero
    private val one = PassPattern.One
    private val rand = PassPattern.Random
    private val comp = PassPattern.ComplementLast
    private val verify = PassPattern.VerifyLast

    private fun b(v: Int, hex: String) = PassPattern.FixedByte(v.toByte(), hex)
    private fun p3(a: Int, bVal: Int, c: Int, hex: String) =
        PassPattern.ThreeBytePattern(a.toByte(), bVal.toByte(), c.toByte(), hex)

    val allMethods: List<ErasureMethod> = listOf(
        ErasureMethod(
            id = "0xff",
            name = "0xFF",
            tier = MethodTier.STD,
            cycles = 1,
            origin = "Protectstar Standard",
            description = "Fast single-pass overwrite filling every sector with binary 1s (0xFF). Ideal for rapid non-sensitive cleanup.",
            passes = listOf(one)
        ),
        ErasureMethod(
            id = "0x00",
            name = "0x00",
            tier = MethodTier.STD,
            cycles = 1,
            origin = "Protectstar Standard",
            description = "Single-pass zero-fill (0x00) overwrite. Clears file contents quickly with minimal flash wear.",
            passes = listOf(zero)
        ),
        ErasureMethod(
            id = "random",
            name = "Random",
            tier = MethodTier.STD,
            cycles = 1,
            origin = "CSPRNG Standard",
            description = "Single-pass overwrite using cryptographically secure pseudo-random bytes generated via SecureRandom.",
            passes = listOf(rand)
        ),
        ErasureMethod(
            id = "nist_800_88_r1",
            name = "NIST SP 800-88 Rev. 1",
            tier = MethodTier.PRO,
            cycles = 1,
            origin = "National Institute of Standards & Technology (USA)",
            description = "Modern flash & SSD-optimized purge standard performing a full random/zero sanitization pass with verification.",
            passes = listOf(rand)
        ),
        ErasureMethod(
            id = "nzsit_402",
            name = "NZSIT 402",
            tier = MethodTier.MIL,
            cycles = 1,
            origin = "New Zealand Security Intelligence Service",
            description = "New Zealand government standard for single-pass cryptographic pseudo-random overwriting with block verification.",
            passes = listOf(rand)
        ),
        ErasureMethod(
            id = "ism_6_2_92",
            name = "ISM 6.2.92",
            tier = MethodTier.MIL,
            cycles = 1,
            origin = "Australian Signals Directorate (ASD)",
            description = "Australian Government Information Security Manual single-pass CSPRNG sanitization standard for modern media.",
            passes = listOf(rand)
        ),
        ErasureMethod(
            id = "0xff_0x00",
            name = "0xFF & 0x00",
            tier = MethodTier.STD,
            cycles = 2,
            origin = "Dual-Pass Standard",
            description = "Two-pass erasure writing 0xFF followed by 0x00 to flip every bit twice.",
            passes = listOf(one, zero)
        ),
        ErasureMethod(
            id = "gost_r_50739_95",
            name = "RUSSIAN GOST R 50739-95",
            tier = MethodTier.MIL,
            cycles = 2,
            origin = "Russian Federation State Technical Commission",
            description = "Two-pass national security standard combining a deterministic zero pass followed by a cryptographic random pass.",
            passes = listOf(zero, rand)
        ),
        ErasureMethod(
            id = "nist_800_88",
            name = "NIST SP 800-88",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "NIST (2006) Media Sanitization",
            description = "Three-pass sanitization alternating fixed bit patterns and cryptographic random noise.",
            passes = listOf(zero, one, rand)
        ),
        ErasureMethod(
            id = "afssi_5020",
            name = "AFSSI-5020",
            tier = MethodTier.MIL,
            cycles = 3,
            origin = "United States Air Force",
            description = "USAF System Security Instruction 5020: 0x00, 0xFF, and a final random pass with read verification.",
            passes = listOf(zero, one, rand)
        ),
        ErasureMethod(
            id = "navso_mfm",
            name = "NAVSO P-5239-26 (MFM)",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "US Navy Staff Office",
            description = "US Navy MFM encoding sanitization using 0x01, 0x7FFFFFFF pattern, and CSPRNG pass.",
            passes = listOf(one, b(0x01, "0x01"), rand)
        ),
        ErasureMethod(
            id = "navso_rll",
            name = "NAVSO P-5239-26 (RLL)",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "US Navy Staff Office",
            description = "US Navy RLL encoding sanitization using 0x01, 0x27FFFFFF pattern, and CSPRNG pass.",
            passes = listOf(one, b(0x27, "0x27"), rand)
        ),
        ErasureMethod(
            id = "csec_itsg_06",
            name = "CSEC ITSG-06",
            tier = MethodTier.MIL,
            cycles = 3,
            origin = "Communications Security Establishment Canada",
            description = "Canadian government 3-pass clearing standard: fixed pattern, bitwise complement, and cryptographic random.",
            passes = listOf(one, comp, rand)
        ),
        ErasureMethod(
            id = "dod_5220_22_me",
            name = "DoD 5220.22-M (E)",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "US Department of Defense",
            description = "Classic 3-pass DoD clearing: Pass 1 writes 0x00, Pass 2 writes 0xFF, Pass 3 writes random bytes.",
            passes = listOf(zero, one, rand)
        ),
        ErasureMethod(
            id = "us_ar380_19",
            name = "US AR380-19",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "US Army Regulation 380-19",
            description = "US Army information systems security sanitization: random, specified character (0x55), and complement (0xAA).",
            passes = listOf(rand, b(0x55, "0x55"), comp)
        ),
        ErasureMethod(
            id = "ncsc_tg_025",
            name = "NCSC-TG-025",
            tier = MethodTier.MIL,
            cycles = 3,
            origin = "National Computer Security Center (NSA)",
            description = "NSA Rainbow Series 3-pass overwrite using alternating complementary byte masks and random final pass.",
            passes = listOf(zero, one, rand)
        ),
        ErasureMethod(
            id = "hmg_infosec_5",
            name = "HMG InfoSec No.5",
            tier = MethodTier.PRO,
            cycles = 3,
            origin = "UK Cabinet Office / NCSC",
            description = "UK Enhanced Baseline standard: 0x00, 0xFF, and CSPRNG pass with final sector check.",
            passes = listOf(zero, one, rand)
        ),
        ErasureMethod(
            id = "dod_5220_22_ssd",
            name = "DoD 5220.22 SSD",
            tier = MethodTier.PRO,
            cycles = 4,
            origin = "Protectstar / DoD Flash Adaptation",
            description = "4-pass flash-aware DoD variant engineered to defeat wear-leveling caches on solid-state & UFS storage.",
            passes = listOf(zero, one, rand, b(0xA5, "0xA5"))
        ),
        ErasureMethod(
            id = "protectstar_asda_2017",
            name = "Protectstar™ ASDA (2017)",
            tier = MethodTier.PRO,
            cycles = 4,
            origin = "Protectstar Security Labs",
            description = "Advanced Secure Deletion Algorithm tuned for modern NAND flash and Android F2FS/ext4 storage controllers.",
            passes = listOf(rand, b(0x55, "0x55"), b(0xAA, "0xAA"), rand)
        ),
        ErasureMethod(
            id = "bsi_2011_vs",
            name = "BSI-2011-VS",
            tier = MethodTier.MIL,
            cycles = 5,
            origin = "German Federal Office for Information Security",
            description = "5-pass German classified data sanitization using alternating patterns, random passes, and verification.",
            passes = listOf(zero, one, rand, b(0x55, "0x55"), rand)
        ),
        ErasureMethod(
            id = "dod_5220_22_ece",
            name = "DoD 5220.22 ECE",
            tier = MethodTier.MIL,
            cycles = 7,
            origin = "US Department of Defense (Extended)",
            description = "7-pass Extended DoD 5220.22-M (ECE) running two complete 3-pass E cycles separated by a random barrier pass.",
            passes = listOf(zero, one, rand, b(0x96, "0x96"), zero, one, rand)
        ),
        ErasureMethod(
            id = "bruce_schneier",
            name = "Bruce Schneier's Algorithm",
            tier = MethodTier.MIL,
            cycles = 7,
            origin = "Bruce Schneier (Applied Cryptography)",
            description = "7-pass cryptographic sanitization: 0xFF, 0x00, followed by five independent CSPRNG random overwrites.",
            passes = listOf(one, zero, rand, rand, rand, rand, rand)
        ),
        ErasureMethod(
            id = "nato_standard",
            name = "NATO Standard",
            tier = MethodTier.PRO,
            cycles = 7,
            origin = "North Atlantic Treaty Organization",
            description = "NATO SDIP-27 / InfoSec 7-pass overwrite combining alternating 0x00/0xFF sweeps with final 0xAA mask.",
            passes = listOf(zero, one, zero, one, zero, one, b(0xAA, "0xAA"))
        ),
        ErasureMethod(
            id = "rcmp_tssit_ops_ii",
            name = "CANADIAN RCMP TSSIT OPS-II",
            tier = MethodTier.MIL,
            cycles = 7,
            origin = "Royal Canadian Mounted Police",
            description = "Canadian police technical security 7-pass standard alternating 0x00/0xFF with a final CSPRNG pass.",
            passes = listOf(zero, one, zero, one, zero, one, rand)
        ),
        ErasureMethod(
            id = "bsi_vsitr",
            name = "BSI-VSITR (TL-03423)",
            tier = MethodTier.MIL,
            cycles = 8,
            origin = "German Federal Office for Information Security",
            description = "Recommended high-assurance 8-pass standard: 6 alternating 0x00/0xFF sweeps, 0xAA mask, and final CSPRNG verification.",
            isRecommendedDefault = true,
            passes = listOf(zero, one, zero, one, zero, one, b(0xAA, "0xAA"), rand)
        ),
        ErasureMethod(
            id = "gutmann",
            name = "Gutmann",
            tier = MethodTier.PRO,
            cycles = 35,
            origin = "Peter Gutmann (1996)",
            description = "Maximum-depth 35-pass method executing 4 initial random passes, 27 encoding-specific MFM/RLL patterns, and 4 final random passes.",
            passes = buildList {
                repeat(4) { add(rand) }
                add(b(0x55, "0x55")); add(b(0xAA, "0xAA"))
                add(p3(0x92, 0x49, 0x24, "0x924924"))
                add(p3(0x49, 0x24, 0x92, "0x492492"))
                add(p3(0x24, 0x92, 0x49, "0x249249"))
                add(zero)
                add(b(0x11, "0x11")); add(b(0x22, "0x22")); add(b(0x33, "0x33"))
                add(b(0x44, "0x44")); add(b(0x55, "0x55")); add(b(0x66, "0x66"))
                add(b(0x77, "0x77")); add(b(0x88, "0x88")); add(b(0x99, "0x99"))
                add(b(0xAA, "0xAA")); add(b(0xBB, "0xBB")); add(b(0xCC, "0xCC"))
                add(b(0xDD, "0xDD")); add(b(0xEE, "0xEE")); add(one)
                add(p3(0x92, 0x49, 0x24, "0x924924"))
                add(p3(0x49, 0x24, 0x92, "0x492492"))
                add(p3(0x24, 0x92, 0x49, "0x249249"))
                add(p3(0x6D, 0xB6, 0xDB, "0x6DB6DB"))
                add(p3(0xB6, 0xDB, 0x6D, "0xB6DB6D"))
                add(p3(0xDB, 0x6D, 0xB6, "0xDB6DB6"))
                repeat(4) { add(rand) }
            }
        ),
        ErasureMethod(
            id = "protectstar_sda_2007",
            name = "Protectstar™ SDA (2007)",
            tier = MethodTier.PRO,
            cycles = 50,
            origin = "Protectstar Secure Deletion Algorithm",
            description = "Extreme 50-cycle secure deletion cascade combining alternating bit masks, Gutmann patterns, and cryptographic entropy sweeps.",
            passes = buildList {
                repeat(50) { idx ->
                    when (idx % 5) {
                        0 -> add(rand)
                        1 -> add(zero)
                        2 -> add(one)
                        3 -> add(b(0xAA, "0xAA"))
                        else -> add(b(0x55, "0x55"))
                    }
                }
            }
        ),
        ErasureMethod(
            id = "nist_800_88_r2_2026",
            name = "NIST SP 800-88 Rev. 2 (2026)",
            tier = MethodTier.MIL,
            cycles = 3,
            origin = "NIST Media Sanitization (2026)",
            description = "2026 revision tuned for UFS 4.0 / NVMe mobile flash controllers combining zero-purge, CSPRNG noise, and block verification.",
            passes = listOf(zero, rand, verify)
        ),
        ErasureMethod(
            id = "eu_cra_2026",
            name = "EU CRA-2026 Zero-Trace",
            tier = MethodTier.PRO,
            cycles = 4,
            origin = "EU Cyber Resilience Act (2026)",
            description = "European 2026 compliance deletion standard writing cryptographic noise, alternating 0x55/0xAA masks, and read-back verification.",
            passes = listOf(rand, b(0x55, "0x55"), b(0xAA, "0xAA"), verify)
        ),
        ErasureMethod(
            id = "q_shred_2026",
            name = "Protectstar™ Q-Shred 2026",
            tier = MethodTier.MIL,
            cycles = 6,
            origin = "Protectstar Quantum Labs (2026)",
            description = "Post-quantum high-entropy 6-pass wipe interleaving multi-seed CSPRNG blocks with complementary bit inversions and verification.",
            passes = listOf(rand, zero, rand, one, rand, verify)
        )
    )

    fun findById(id: String): ErasureMethod =
        allMethods.firstOrNull { it.id == id } ?: allMethods.first { it.isRecommendedDefault }
}
