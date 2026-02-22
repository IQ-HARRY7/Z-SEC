package com.iq.zsec;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntroActivity extends Activity {

    private int currentPage = 0;

    private static final String[] TITLES = {
        "Welcome to Z_SEC",
        "Your Private Vault",
        "How It Works",
        "SAF Integration",
        "You Are Ready",
        "About Z_SEC"          // ✅ Page 6
    };

    private static final String[] BODIES = {
        "Z_SEC is a secure file vault built directly into Android's "
        + "storage system. Your files stay hidden from every other app "
        + "on your device — protected by your master password.",

        "All files you import are stored in a private directory that "
        + "only Z_SEC can access:\n\n"
        + "/data/data/com.iq.zsec/files/protected/\n\n"
        + "No file manager or third-party app can browse this location.",

        "Use the + button on the home screen to import any file into "
        + "your vault. Z_SEC copies it into protected storage, assigns "
        + "it a random internal name and encrypts it with AES-256 — "
        + "your original filename is preserved only inside the app.",

        "Z_SEC is registered as a Storage Access Framework (SAF) "
        + "provider. When another app opens a file picker, you can "
        + "choose Z_SEC. It will ask for your password before "
        + "revealing anything — files are shared only via secure "
        + "content:// links, never raw file paths.",

        "Your vault is locked by default every time you close the app. "
        + "You have 5 password attempts before a 30-second lockout.\n\n"
        + "Tap GET STARTED to open your vault.",

        // ✅ Page 6 — About
        "Made with ❤️ by\n\nIQ_HARRY  &  Claude  &  AIDE\n\n"
        + "Security is here, ALWAYS 🙌❤️\n\n"
        + "Z_SEC is open-source and will always remain open-source.\n"
        + "If you'd like to help, visit us on GitHub.\n\n"
        + "The journey begins. 🚀"
    };

    private static final String[] ICONS = {
        "🔐", "🗄️", "📥", "🔗", "✅", "🌟"
    };

    // ✅ Only the last page has a clickable GitHub link
    private static final String GITHUB_URL =
	"https://github.com/IQ-HARRY7/IQ-HARRY7";

    private TextView tvIcon, tvTitle, tvBody, tvStep, tvGithub;
    private Button   btnNext;
    private View     dot1, dot2, dot3, dot4, dot5, dot6;
    private View[]   dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        tvIcon   = (TextView) findViewById(R.id.tv_icon);
        tvTitle  = (TextView) findViewById(R.id.tv_title);
        tvBody   = (TextView) findViewById(R.id.tv_body);
        tvStep   = (TextView) findViewById(R.id.tv_step);
        tvGithub = (TextView) findViewById(R.id.tv_github);
        btnNext  = (Button)   findViewById(R.id.btn_next);

        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dot5 = findViewById(R.id.dot5);
        dot6 = findViewById(R.id.dot6);
        dots = new View[]{dot1, dot2, dot3, dot4, dot5, dot6};

        // ✅ GitHub link tap → open in browser
        tvGithub.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent browser = new Intent(
						Intent.ACTION_VIEW, Uri.parse(GITHUB_URL));
					startActivity(browser);
				}
			});

        renderPage();

        btnNext.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentPage < TITLES.length - 1) {
						currentPage++;
						renderPage();
					} else {
						finishIntro();
					}
				}
			});
    }

    private void renderPage() {
        tvIcon.setText(ICONS[currentPage]);
        tvTitle.setText(TITLES[currentPage]);
        tvBody.setText(BODIES[currentPage]);
        tvStep.setText((currentPage + 1) + " / " + TITLES.length);

        boolean isLast = (currentPage == TITLES.length - 1);
        btnNext.setText(isLast ? "GET STARTED  🔐" : "NEXT  →");

        // ✅ Show GitHub button ONLY on last page
        tvGithub.setVisibility(isLast ? View.VISIBLE : View.GONE);

        // Update dots
        int active   = 0xFF2ECC71;
        int inactive = 0xFF2A2A2A;
        for (int i = 0; i < dots.length; i++) {
            dots[i].setBackgroundColor(i == currentPage ? active : inactive);
        }
    }

    private void finishIntro() {
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(SetupActivity.KEY_INTRO_DONE, true)
            .apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            currentPage--;
            renderPage();
        } else {
            super.onBackPressed();
        }
    }
}
