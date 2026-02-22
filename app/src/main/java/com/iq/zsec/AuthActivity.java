package com.iq.zsec;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.iq.zsec.utils.SessionManager;
import com.iq.zsec.utils.VaultKeyManager;

public class AuthActivity extends Activity {

    public static final String EXTRA_SAF_MODE = "saf_mode";

    private static final int  MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MS   = 30000L;

    private boolean safMode        = false;
    private int     failedAttempts = 0;
    private boolean lockedOut      = false;

    private EditText etPassword;
    private Button   btnUnlock;
    private TextView tvError, tvAttemptsLeft, tvLockout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(
            new ColorDrawable(Color.parseColor("#0D1117")));
        setContentView(R.layout.activity_auth);

        safMode        = getIntent().getBooleanExtra(EXTRA_SAF_MODE, false);
        etPassword     = (EditText) findViewById(R.id.et_password);
        btnUnlock      = (Button)   findViewById(R.id.btn_unlock);
        tvError        = (TextView) findViewById(R.id.tv_error);
        tvAttemptsLeft = (TextView) findViewById(R.id.tv_attempts);
        tvLockout      = (TextView) findViewById(R.id.tv_lockout);

        btnUnlock.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { attemptUnlock(); }
			});
    }

    private void attemptUnlock() {
        if (lockedOut) return;
        String input = etPassword.getText().toString();
        clearErrors();

        if (TextUtils.isEmpty(input)) { showError("Enter your master password."); return; }

        // ✅ Verify against AMP/vault.key
        if (!VaultKeyManager.isSetup(this)) {
            showError("No vault found. Open Z_SEC to set it up.");
            return;
        }

        if (VaultKeyManager.verifyPassword(this, input)) {
            SessionManager.getInstance().setAuthenticated();
            onSuccess();
        } else {
            etPassword.setText("");
            failedAttempts++;
            int remaining = MAX_ATTEMPTS - failedAttempts;
            if (failedAttempts >= MAX_ATTEMPTS) {
                triggerLockout();
            } else {
                showError("Incorrect password.");
                tvAttemptsLeft.setText(remaining + " attempt(s) remaining");
                tvAttemptsLeft.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onSuccess() {
        if (safMode) {
            setResult(RESULT_OK);
            finish();
        } else {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }
    }

    private void triggerLockout() {
        lockedOut = true;
        btnUnlock.setEnabled(false);
        etPassword.setEnabled(false);
        tvError.setVisibility(View.GONE);
        tvAttemptsLeft.setVisibility(View.GONE);
        tvLockout.setVisibility(View.VISIBLE);

        new CountDownTimer(LOCKOUT_MS, 1000L) {
            @Override public void onTick(long ms) {
                tvLockout.setText("Too many attempts. Retry in " + (ms / 1000) + "s");
            }
            @Override public void onFinish() {
                lockedOut = false; failedAttempts = 0;
                btnUnlock.setEnabled(true);
                etPassword.setEnabled(true);
                tvLockout.setVisibility(View.GONE);
            }
        }.start();
    }

    private void showError(String msg)  { tvError.setText(msg); tvError.setVisibility(View.VISIBLE); }
    private void clearErrors()          { tvError.setVisibility(View.GONE); tvAttemptsLeft.setVisibility(View.GONE); }

    @Override
    public void onBackPressed() {
        if (safMode) { setResult(RESULT_CANCELED); finish(); }
        // Block back in normal mode — user must authenticate
    }
	}
