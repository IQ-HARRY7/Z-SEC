package com.iq.zsec;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iq.zsec.utils.FileUtils;
import com.iq.zsec.utils.SessionManager;
import com.iq.zsec.utils.VaultKeyManager;

public class SetupActivity extends Activity {

    // SharedPreferences keys — non-security booleans only
    public static final String PREFS_NAME     = "zsec_prefs";
    public static final String KEY_INTRO_DONE = "intro_complete";

    private EditText etPassword, etConfirm;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        etPassword = (EditText) findViewById(R.id.et_password);
        etConfirm  = (EditText) findViewById(R.id.et_confirm_password);
        tvError    = (TextView) findViewById(R.id.tv_error);

        FileUtils.getProtectedDir(this);
        VaultKeyManager.getAmpDir(this); // ensure AMP/ exists

        ((Button) findViewById(R.id.btn_create)).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { attemptSetup(); }
            });
    }

    private void attemptSetup() {
        String pass    = etPassword.getText().toString().trim();
        String confirm = etConfirm.getText().toString().trim();
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(pass))  { showError("Password cannot be empty."); return; }
        if (pass.length() < 6)        { showError("Minimum 6 characters."); return; }
        if (!pass.equals(confirm))    {
            showError("Passwords do not match.");
            etConfirm.setText("");
            return;
        }

        // ✅ Save to AMP/vault.key (AES-256 encrypted, device-bound)
        boolean saved = VaultKeyManager.savePassword(this, pass);
        if (!saved) {
            showError("Failed to create vault. Try again.");
            return;
        }

        SessionManager.getInstance().setAuthenticated();
        Toast.makeText(this, "Vault created!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, IntroActivity.class));
        finish();
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }
}
