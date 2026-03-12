# ⚔️ Z_SEC — Secure File Vault

> **"Your files. Your fortress. No exceptions."**

![Version](https://img.shields.io/badge/Version-1.0.0-red?style=for-the-badge)
![Platform](https://img.shields.io/badge/Platform-Android-black?style=for-the-badge&logo=android)
![Security](https://img.shields.io/badge/Security-Military_Grade-darkred?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active-green?style=for-the-badge)
![License](https://img.shields.io/badge/License-Proprietary-grey?style=for-the-badge)

---

## 🔐 What is Z_SEC?

**Z_SEC** is a military-grade secure file management vault for Android.  
Built for users who demand absolute control over their private files —  
documents, images, videos, and more — locked behind layers of encryption  
and access control that no unauthorized party can penetrate.

Z_SEC does not just hide your files. It **fortifies** them.

---

## 🎖️ Features

### 🔒 Core Vault
- **AES-256 Encryption** — Industry-standard encryption on all stored files
- **Zero-Knowledge Architecture** — Z_SEC never sees your data. Ever.
- **Secure File Import/Export** — Move files in and out without exposure
- **Auto-Destruct Mode** — Files self-wipe after X failed unlock attempts

### 🛡️ Access Control
- **PIN / Password Protection** — Multi-layer authentication
- **Biometric Lock** — Fingerprint & Face ID support
- **Stealth Mode** — App disguises itself on the home screen
- **Decoy Vault** — Fake vault shown under duress password

### 📁 File Management
- **Full File Manager** — Browse, organize, rename, delete securely
- **Secure Folder System** — Create encrypted nested folders
- **Media Preview** — View images/videos without ever decrypting to storage
- **Recycle Bin** — Secure recovery before permanent deletion

### 🌐 Network & Backup
- **Offline First** — 100% functional with zero internet required
- **Encrypted Cloud Backup** — Optional encrypted backup support
- **Breach Detection** — Alerts on suspicious access attempts

---

## ⚙️ Installation

### Requirements
```
Android Version  :  8.0 (Oreo) or above
Storage          :  Minimum 50MB free
Permissions      :  Storage, Biometric (optional)
```

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/IQ-HARRY7/Z_SEC.git
```

**2. Open in Android Studio**
```
File → Open → Select Z_SEC folder
```

**3. Build the project**
```
Build → Make Project  (Ctrl + F9)
```

**4. Run on device or emulator**
```
Run → Run 'app'  (Shift + F10)
```

---

## 💻 Usage / Code Examples

### Initialize the Vault
```java
package com.zsec.vault;

import com.zsec.security.VaultManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Z_SEC Vault
        VaultManager vault = new VaultManager(this);
        vault.initialize();
    }
}
```

### Lock a File
```java
// Import a file into the secure vault
File targetFile = new File("/storage/emulated/0/secret.pdf");
vault.lockFile(targetFile);
// File is now AES-256 encrypted inside Z_SEC
```

### Unlock & Access a File
```java
// Authenticate and retrieve file
vault.authenticate(userPin, (isVerified) -> {
    if (isVerified) {
        vault.unlockFile("secret.pdf");
    } else {
        vault.triggerAlert(); // Log unauthorized attempt
    }
});
```

### Check Vault Integrity
```java
public boolean isVaultSafe() {
    File vaultDirectory = new File("/data/data/com.zsec.vault/");
    return vaultDirectory.exists() && vault.isIntact();
}
```

---

## 🗂️ Project Structure

```
Z_SEC/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zsec/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── VaultManager.java
│   │   │   │   │   ├── Encryption.java
│   │   │   │   │   └── AuthManager.java
│   │   │   │   └── filemanager/
│   │   │   │       ├── FileHandler.java
│   │   │   │       └── FolderManager.java
│   │   │   └── res/
│   │   │       ├── layout/
│   │   │       └── values/
├── README.md
└── build.gradle
```

---

## 🔐 Security Architecture

```
[ User Input ]
      ↓
[ Authentication Layer ]  ←  PIN / Biometric / Password
      ↓
[ Encryption Engine ]     ←  AES-256
      ↓
[ Secure Vault Storage ]  ←  Isolated from system
      ↓
[ Integrity Monitor ]     ←  Breach detection & alerts
```

---

## ⚠️ Disclaimer

Z_SEC is built for **legitimate personal privacy and security use only.**  
The developers hold no responsibility for misuse of this software.  
Always comply with your local laws and regulations.

---

## 👤 Author

**CREATED BY- IQ-HARRY7 (Balaram sahu)**

**Z_SEC** — Developed with precision and security in mind.  
Built for those who take their privacy seriously. 🎖️

---

> *"A fortress is only as strong as the one who built it."*

