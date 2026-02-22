package com.iq.zsec;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iq.zsec.db.DatabaseHelper;
import com.iq.zsec.db.FileRecord;
import com.iq.zsec.utils.FileUtils;
import com.iq.zsec.utils.SessionManager;
import com.iq.zsec.utils.VaultKeyManager;

import java.io.File;
import java.util.List;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((View) findViewById(R.id.btn_back)).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });

        // Vault stats
        List<FileRecord> files = DatabaseHelper.getInstance(this).getAllFiles();
        long totalBytes = 0;
        for (int i = 0; i < files.size(); i++) totalBytes += files.get(i).getFileSize();
        ((TextView) findViewById(R.id.tv_stats)).setText(
            files.size() + " file(s)  •  " + FileUtils.formatSize(totalBytes) + " used");

        // Key file path info
        String ampPath = VaultKeyManager.getAmpDir(this).getAbsolutePath()
            + "/vault.key (AES-256)";
        ((TextView) findViewById(R.id.tv_key_path)).setText(ampPath);

        // Change Password
        findViewById(R.id.row_change_password).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { showChangePasswordDialog(); }
            });

        // Vault Path
        findViewById(R.id.row_vault_path).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) {
                    String path = FileUtils.getProtectedDir(
                        SettingsActivity.this).getAbsolutePath();
                    new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Vault Storage Path")
                        .setMessage(path)
                        .setPositiveButton("OK", null).show();
                }
            });

        // Lock
        findViewById(R.id.row_lock).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) {
                    SessionManager.getInstance().invalidate();
                    Intent i = new Intent(SettingsActivity.this, AuthActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                }
            });

        // Wipe
        findViewById(R.id.row_wipe).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { showWipeDialog(); }
            });
    }

    private void showChangePasswordDialog() {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setHint("New password (min 6 chars)");

        new AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(et)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    String p = et.getText().toString().trim();
                    if (p.length() < 6) {
                        Toast.makeText(SettingsActivity.this,
									   "Too short.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // ✅ Updates AMP/vault.key
                    boolean ok = VaultKeyManager.savePassword(SettingsActivity.this, p);
                    Toast.makeText(SettingsActivity.this,
								   ok ? "Password updated." : "Failed to update.",
								   Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void showWipeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Wipe Vault")
            .setMessage("Permanently delete ALL files and reset the app?\nThis CANNOT be undone.")
            .setPositiveButton("WIPE EVERYTHING", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) { wipeVault(); }
            })
            .setNegativeButton("Cancel", null).show();
    }

    private void wipeVault() {
        // Delete all encrypted files
        File dir = FileUtils.getProtectedDir(this);
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) files[i].delete();
        }

        // Wipe DB records
        DatabaseHelper db = DatabaseHelper.getInstance(this);
        List<FileRecord> records = db.getAllFiles();
        for (int i = 0; i < records.size(); i++) {
            db.deleteFile(records.get(i).getStoredName());
        }

        // Delete AMP/vault.key
        VaultKeyManager.deleteKeyFile(this);

        // Clear prefs
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE).edit().clear().apply();

        SessionManager.getInstance().invalidate();
        Intent i = new Intent(this, SetupActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
