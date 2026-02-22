package com.iq.zsec.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileUtils {

    private static final String PROTECTED_DIR = "protected";

    private FileUtils() {}

    public static File getProtectedDir(Context ctx) {
        File dir = new File(ctx.getFilesDir(), PROTECTED_DIR);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static File getProtectedFile(Context ctx, String storedName) {
        return new File(getProtectedDir(ctx), storedName);
    }

    public static String generateStoredName(String originalName) {
        String ext  = getExtension(originalName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return ext.isEmpty() ? uuid : (uuid + "." + ext);
    }

    public static String getExtension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1)
			? name.substring(dot + 1).toLowerCase()
			: "";
    }

    public static String getMimeType(String fileName) {
        String ext  = getExtension(fileName);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        return (mime != null) ? mime : "application/octet-stream";
    }

    /** Copies a content URI to a File. Used during import. */
    public static boolean copyUriToFile(Context ctx, Uri src, File dest) {
        InputStream  in  = null;
        OutputStream out = null;
        try {
            in  = ctx.getContentResolver().openInputStream(src);
            out = new FileOutputStream(dest);
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            if (dest.exists()) dest.delete();
            return false;
        } finally {
            if (in  != null) try { in.close();  } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    /** ✅ NEW — Copies one File to another File (for share fallback). */
    public static boolean copyFileToFile(File src, File dest) {
        FileInputStream  in  = null;
        FileOutputStream out = null;
        try {
            in  = new FileInputStream(src);
            out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            out.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            if (dest.exists()) dest.delete();
            return false;
        } finally {
            if (in  != null) try { in.close();  } catch (IOException ignored) {}
            if (out != null) try { out.close(); } catch (IOException ignored) {}
        }
    }

    public static String getFileName(Context ctx, Uri uri) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } finally {
            if (c != null) c.close();
        }
        String last = uri.getLastPathSegment();
        return (last != null) ? last : "unknown_file";
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024L)              return bytes + " B";
        if (bytes < 1024L * 1024L)      return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L)
			return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
