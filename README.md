<div align="center">

```
███████╗    ███████╗███████╗ ██████╗
╚══███╔╝    ██╔════╝██╔════╝██╔════╝
  ███╔╝     ███████╗█████╗  ██║
 ███╔╝      ╚════██║██╔══╝  ██║
███████╗    ███████║███████╗╚██████╗
╚══════╝    ╚══════╝╚══════╝ ╚═════╝
```

**Z_SEC — Zero Exposure. Zero Compromise.**

[![Release](https://img.shields.io/github/v/release/IQ-HARRY7/IQ-HARRY7?color=2ECC71&label=release&style=flat-square)](https://github.com/IQ-HARRY7/IQ-HARRY7/releases)
[![License](https://img.shields.io/badge/license-open--source-2ECC71?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%2026%2B-2ECC71?style=flat-square&logo=android)](https://android.com)
[![Built With](https://img.shields.io/badge/built%20with-AIDE%20%26%20Claude-2ECC71?style=flat-square)](https://github.com/IQ-HARRY7/IQ-HARRY7)
[![Security](https://img.shields.io/badge/encryption-AES--256--CBC-2ECC71?style=flat-square)](#security-architecture)

<br/>

> *"Security is here, ALWAYS"* — IQ_HARRY & Claude & AIDE 🙌❤️

</div>

---

## What is Z_SEC?

**Z_SEC** is a military-grade, open-source file vault for Android. It operates as a native **Storage Access Framework (SAF) DocumentsProvider** — meaning it integrates directly into Android's own file ecosystem, not on top of it. Every file you import is encrypted with **AES-256-CBC** and stored in a private directory that no other app, file manager, or system tool can reach.

This is not a folder lock. This is not a gallery hider. Z_SEC is a **cryptographic vault** woven into the operating system itself.

---

## Features

### 🔐 Vault & Authentication
- Master password hashed with **salted SHA-256** — never stored in plain text
- Password stored in `AMP/vault.key` — **AES-256 encrypted, device-bound**
- Device-bound key derived from `ANDROID_ID` — vault is tied to your hardware
- **5-attempt brute-force lockout** with a 30-second cooldown timer
- **10-minute idle session timeout** — auto-locks when you walk away
- Constant-time password comparison to prevent timing side-channel attacks

### 🗄️ Encrypted Storage
- All files encrypted **in-place** with AES-256-CBC immediately on import
- Every file receives a **unique random IV** — identical files produce different ciphertext
- Files stored under **UUID-based names** — original filenames never revealed on disk
- Protected directory: `/data/data/com.iq.zsec/files/protected/`
- Vault key directory: `/data/data/com.iq.zsec/AMP/vault.key`
- Magic prefix (`ZSEC`) on all encrypted files for reliable detection

### 🔗 SAF DocumentsProvider Integration
- Appears natively in the **Android system file picker**
- When selected from any external app's `ACTION_OPEN_DOCUMENT` intent — Z_SEC demands authentication
- Files are delivered only via **controlled `content://` URIs** — raw filesystem paths are never exposed
- Supports `createDocument`, `deleteDocument`, `openDocument`, and `queryChildDocuments`

### 👁️ In-App Media Viewer
- **Images** — pinch-to-zoom (up to 6×), pan, and reset
- **Videos** — native playback with play/pause, ±10s rewind/forward, and scrubber via `MediaController`
- **Text & Code** — monospaced, selectable, scrollable viewer
- **Previous / Next** navigation across all vault files
- Files are decrypted to memory only — nothing written to disk for viewing

### 📤 Secure Sharing
- Long-press any file to enter **multi-select mode**
- Select multiple files → Share all at once via `ACTION_SEND_MULTIPLE`
- Files are temporarily decrypted to `getCacheDir()/shared/` only for the duration of the share intent
- Served via **FileProvider** — raw paths are never exposed to the receiving app
- Temp files are **deleted immediately** after sharing

### ⚙️ Settings & Control
- Change master password at any time
- View vault storage path and key file location
- Real-time vault stats — file count & total size
- **Wipe vault** — nuclear option that deletes all files, database records, and the key file
- Lock vault instantly from anywhere in the app

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      Z_SEC VAULT                        │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Password ──► SHA-256(salt + password + salt)           │
│                    │                                    │
│                    ▼                                    │
│  AMP/vault.key ◄── AES-256-CBC(SALT:HASH)               │
│  (device-bound key = SHA-256(ANDROID_ID))               │
│                                                         │
│  Import flow:                                           │
│  File ──► copy ──► AES-256-CBC encrypt ──► UUID.ext     │
│           (unique IV per file, ZSEC magic prefix)       │
│                                                         │
│  View flow:                                             │
│  UUID.ext ──► decrypt ──► byte[] in memory              │
│              (never touches disk)                       │
│                                                         │
│  Share flow:                                            │
│  UUID.ext ──► decrypt ──► getCacheDir()/shared/         │
│           ──► FileProvider URI ──► external app         │
│           ──► temp file deleted on return               │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Min SDK | Android 8.0 (API 26) |
| Target SDK | Android 10 (API 29) |
| Architecture | Manual MVVM — SQLiteOpenHelper + Singleton pattern |
| Encryption | AES-256-CBC (javax.crypto) |
| Key Derivation | SHA-256 (java.security) |
| Storage | SAF DocumentsProvider + internal files dir |
| UI | Native Android (no AppCompat dependency) |
| Build | AIDE (Android IDE on-device) |
| Dependencies | RecyclerView 1.1.0, CardView 1.0.0 |

> Built entirely on an Android phone using **AIDE** — no laptop, no desktop, no compromises.

---

## Project Structure

```
com.iq.zsec/
├── MainActivity.java          — Vault home screen, file list, FAB import
├── SetupActivity.java         — First-time vault creation
├── AuthActivity.java          — Password authentication + lockout logic
├── IntroActivity.java         — 6-page onboarding introduction
├── SettingsActivity.java      — Password change, stats, wipe vault
├── MediaViewerActivity.java   — Image/video/text viewer with zoom
├── ZSecDocumentsProvider.java — SAF DocumentsProvider engine
├── adapter/
│   └── FileAdapter.java       — RecyclerView with multi-select + long-press
├── db/
│   ├── DatabaseHelper.java    — SQLite file metadata store
│   └── FileRecord.java        — File metadata model
└── utils/
    ├── CryptoUtils.java       — AES-256-CBC engine (encrypt, decrypt, file ops)
    ├── VaultKeyManager.java   — AMP/vault.key read/write (device-bound)
    ├── SecurityUtils.java     — Salted SHA-256, constant-time compare
    ├── FileUtils.java         — File I/O, MIME detection, UUID naming
    └── SessionManager.java    — In-memory session with 10-min timeout
```

---

## Getting Started

### Requirements
- Android device running **Android 8.0 (API 26) or higher**
- AIDE installed (to build from source) — [aide.android.com](https://aide.android.com)

### Build from Source
```
1. Clone this repository
2. Open AIDE → Open Project → select the cloned folder
3. Build & Run
```

### First Launch
1. Create your **master password** (minimum 6 characters)
2. Read through the **6-page introduction**
3. Tap **+** to import and encrypt your first file
4. Your vault is now active 🔐

---

## Open Source

Z_SEC is **free and open-source software**, and it will always remain so.

The vault operates on the principle that security through obscurity is not security at all. Every line of code is readable, auditable, and improvable by the community.

If you'd like to contribute, report a bug, suggest a feature, or simply star the repo to show support:

**→ [github.com/IQ-HARRY7/IQ-HARRY7](https://github.com/IQ-HARRY7/IQ-HARRY7)**

---

## Roadmap

- [ ] Biometric authentication (fingerprint unlock)
- [ ] Encrypted cloud backup support
- [ ] Audio file playback in MediaViewer
- [ ] PDF viewer
- [ ] File renaming within the vault
- [ ] Dark/light theme toggle
- [ ] Duress password (opens a decoy vault)

---

## Credits

<div align="center">

Built with ❤️ by

**IQ_HARRY** — Vision, design & testing on real hardware

**Claude (Anthropic)** — Architecture, code generation & debugging

**AIDE** — The Android IDE that made on-device development possible

---

*Security is here, ALWAYS 🙌❤️*

*The journey begins. 🚀*

</div>
