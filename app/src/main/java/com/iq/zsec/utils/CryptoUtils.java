package com.iq.zsec.utils;

import android.content.Context;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-CBC encryption engine for Z_SEC.
 *
 * File format (encrypted):
 *   [4 bytes MAGIC "ZSEC"] [16 bytes IV] [AES-256 ciphertext]
 *
 * The MAGIC prefix lets us reliably detect whether a file is
 * encrypted or legacy-plain, so we can fall back gracefully.
 */
public class CryptoUtils {

    // Magic bytes — "ZSEC" in ASCII hex
    private static final byte[] MAGIC = {
        (byte)0x5A, (byte)0x53, (byte)0x45, (byte)0x43
    };

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final int    IV_LEN    = 16;
    private static final int    MAGIC_LEN = 4;

    private CryptoUtils() {}

    // ── Key derivation ───────────────────────────────────────────

    /**
     * Derives a 256-bit AES key from any string via SHA-256.
     */
    public static SecretKeySpec deriveKey(String source) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = d.digest(source.getBytes("UTF-8"));
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Device-bound key derived from ANDROID_ID.
     * Stable for the device lifetime; changes only on factory reset
     * (at which point all data is already gone anyway).
     */
    public static SecretKeySpec deviceKey(Context ctx) throws Exception {
        String id = Settings.Secure.getString(
            ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (id == null || id.isEmpty()) id = "zsec_fallback_device";
        return deriveKey("ZSEC_DEVICE_BIND_v1_" + id);
    }

    // ── Low-level encrypt / decrypt ──────────────────────────────

    /**
     * Encrypts raw bytes → [MAGIC][IV][ciphertext]
     */
    public static byte[] encrypt(byte[] data, SecretKeySpec key)
	throws Exception {
        byte[] iv = new byte[IV_LEN];
        new SecureRandom().nextBytes(iv);
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] cipher = c.doFinal(data);

        byte[] out = new byte[MAGIC_LEN + IV_LEN + cipher.length];
        System.arraycopy(MAGIC,  0, out, 0,                  MAGIC_LEN);
        System.arraycopy(iv,     0, out, MAGIC_LEN,          IV_LEN);
        System.arraycopy(cipher, 0, out, MAGIC_LEN + IV_LEN, cipher.length);
        return out;
    }

    /**
     * Decrypts [MAGIC][IV][ciphertext] → raw bytes.
     * Returns null if magic bytes are missing (file is not encrypted).
     */
    public static byte[] decrypt(byte[] data, SecretKeySpec key)
	throws Exception {
        if (!hasMagic(data)) return null; // not an encrypted file

        byte[] iv     = new byte[IV_LEN];
        int    offset = MAGIC_LEN + IV_LEN;
        byte[] cipher = new byte[data.length - offset];

        System.arraycopy(data, MAGIC_LEN, iv,     0, IV_LEN);
        System.arraycopy(data, offset,    cipher, 0, cipher.length);

        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return c.doFinal(cipher);
    }

    // ── File-level operations (use device key internally) ────────

    /**
     * Encrypts a file IN-PLACE using the device-bound key.
     * Skips files that are already encrypted.
     */
    public static boolean encryptFile(File src, Context ctx) {
        FileInputStream  fis  = null;
        FileOutputStream fos  = null;
        File             temp = new File(src.getParent(), src.getName() + ".tmp");
        try {
            fis = new FileInputStream(src);
            byte[] plain = new byte[(int) src.length()];
            fis.read(plain);
            fis.close();
            fis = null;

            // Already encrypted? Skip
            if (hasMagic(plain)) return true;

            byte[] enc = encrypt(plain, deviceKey(ctx));

            fos = new FileOutputStream(temp);
            fos.write(enc);
            fos.flush();
            fos.close();
            fos = null;

            src.delete();
            return temp.renameTo(src);

        } catch (Exception e) {
            e.printStackTrace();
            if (temp.exists()) temp.delete();
            return false;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Decrypts src → dest using device key.
     * If src is NOT encrypted, falls back to a raw copy.
     */
    public static boolean decryptFileToPath(File src, File dest, Context ctx) {
        FileInputStream  fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            byte[] raw = new byte[(int) src.length()];
            fis.read(raw);
            fis.close();
            fis = null;

            byte[] out;
            if (hasMagic(raw)) {
                out = decrypt(raw, deviceKey(ctx));
                if (out == null) out = raw; // fallback
            } else {
                out = raw; // legacy unencrypted file
            }

            fos = new FileOutputStream(dest);
            fos.write(out);
            fos.flush();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            if (dest.exists()) dest.delete();
            return false;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Decrypts src to a byte[] in memory using device key.
     * Returns raw bytes if file is not encrypted (legacy support).
     */
    public static byte[] decryptToBytes(File src, Context ctx) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(src);
            byte[] raw = new byte[(int) src.length()];
            fis.read(raw);

            if (hasMagic(raw)) {
                byte[] plain = decrypt(raw, deviceKey(ctx));
                return (plain != null) ? plain : raw;
            }
            return raw; // legacy unencrypted

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fis != null) try { fis.close(); } catch (IOException ignored) {}
        }
    }

    // ── Helper ───────────────────────────────────────────────────

    private static boolean hasMagic(byte[] data) {
        if (data == null || data.length < MAGIC_LEN + IV_LEN + 1) return false;
        for (int i = 0; i < MAGIC_LEN; i++) {
            if (data[i] != MAGIC[i]) return false;
        }
        return true;
    }
}
