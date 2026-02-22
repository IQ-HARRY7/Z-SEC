package com.iq.zsec.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Manages the vault master key stored at:
 *   /data/data/com.iq.zsec/AMP/vault.key
 *
 * The vault.key file is AES-256 encrypted using the device-bound key.
 * Content inside (decrypted): "SALT:HASH" (UTF-8).
 *
 * ⚠️ Android "Clear Data" wipes ALL of /data/data/package/ by OS design.
 * This is a fundamental Android security boundary — no app can bypass it.
 * Storing in AMP/ provides security against root file extraction,
 * not against Clear Data (which is intentional user action).
 */
public class VaultKeyManager {

    private static final String AMP_DIR  = "AMP";
    private static final String KEY_FILE = "vault.key";
    private static final String SEP      = ":";

    private VaultKeyManager() {}

    /** Returns /data/data/com.iq.zsec/AMP/ */
    public static File getAmpDir(Context ctx) {
        // getDataDir() = /data/data/com.iq.zsec/ (API 24+, our minSdk=26)
        File dir = new File(ctx.getDataDir(), AMP_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File getKeyFile(Context ctx) {
        return new File(getAmpDir(ctx), KEY_FILE);
    }

    /** True if vault.key exists — used as "is setup complete" check */
    public static boolean isSetup(Context ctx) {
        return getKeyFile(ctx).exists();
    }

    /**
     * Hashes the password and writes encrypted vault.key.
     * Called during first-time setup and password change.
     */
    public static boolean savePassword(Context ctx, String password) {
        FileOutputStream fos = null;
        try {
            String salt    = SecurityUtils.generateSalt();
            String hash    = SecurityUtils.hashPassword(password, salt);
            String content = salt + SEP + hash;

            byte[] encrypted = CryptoUtils.encrypt(
                content.getBytes("UTF-8"),
                CryptoUtils.deviceKey(ctx));

            fos = new FileOutputStream(getKeyFile(ctx));
            fos.write(encrypted);
            fos.flush();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Returns true if the given password matches the stored hash.
     */
    public static boolean verifyPassword(Context ctx, String password) {
        FileInputStream fis = null;
        try {
            File keyFile = getKeyFile(ctx);
            if (!keyFile.exists()) return false;

            fis = new FileInputStream(keyFile);
            byte[] enc = new byte[(int) keyFile.length()];
            fis.read(enc);
            fis.close();
            fis = null;

            byte[] dec = CryptoUtils.decrypt(enc, CryptoUtils.deviceKey(ctx));
            if (dec == null) return false;

            String content = new String(dec, "UTF-8");
            int    sep     = content.indexOf(SEP);
            if (sep < 0) return false;

            String salt = content.substring(0, sep);
            String hash = content.substring(sep + 1);
            return SecurityUtils.verifyPassword(password, salt, hash);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
    }

    /** Deletes vault.key — called during vault wipe */
    public static void deleteKeyFile(Context ctx) {
        File f = getKeyFile(ctx);
        if (f.exists()) f.delete();
    }
}
