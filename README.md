# iShredder Excellent — Full Version Stable (`2026.1.0-stable`)

Secure Android data erasure & privacy suite supporting **Android 5.0 (API 21) through Android 17 (API 36)**.

## Features
- **30 Certified Erasure Algorithms**: Including *Protectstar Q-Shred 2026*, *NIST SP 800-88 Rev. 2 (2026)*, *EU CRA-2026 Zero-Trace*, *BSI-VSITR*, *Gutmann (35 passes)*, and *Protectstar SDA (50 passes)*.
- **Full Real Theme UI & True OLED (`#000000`)**: Switch live across **OLED Obsidian**, **iOS 27**, **Android 17**, **HyperOS 4**, **ColorOS 17**, and **M3 Expressive**, plus global Pure OLED pixel-off mode and custom accent spectrum overrides.
- **Excellent 2026 Suite**: Privacy Radar (0–100 score), EXIF & GPS Photo Privacy Scrubber, `JobScheduler` Auto-Shred Guard, and Hardware Flash & CSPRNG Speed Lab.
- **Factory Reset**: Pre-reset multi-pass sanitization of free space, mounted SAF folders, cache, clipboard, and contacts with direct Android Reset Settings launch, plus in-app factory reset.
- **USDT-Only In-App Purchases**: On-chain verification for TRC-20, ERC-20, BEP-20, and Polygon ($1.49 – $39.99).

## Automated GitHub Releases (APK / AAB)
This repository includes `.github/workflows/release-apk.yml`, which automatically builds, runs unit tests, computes SHA-256 checksums, and publishes APK releases under **GitHub Releases** on every push to `main`/`master` or `v*` tag.

### Optional: Production Keystore Signing Secrets
To publish production upload-key-signed Release `.apk` and `.aab` files in GitHub Releases, add these repository secrets under **Settings → Secrets and variables → Actions**:
- `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` keystore (`base64 -w 0 my-upload-key.jks`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: `upload` key alias password

Without those secrets configured, the workflow verifies `:app:compileReleaseKotlin`, runs unit tests, and publishes an installable APK signed with the included debug keystore.
