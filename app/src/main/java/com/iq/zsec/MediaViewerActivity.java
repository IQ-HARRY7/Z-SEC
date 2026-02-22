package com.iq.zsec;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.content.FileProvider;

import com.iq.zsec.db.DatabaseHelper;
import com.iq.zsec.db.FileRecord;
import com.iq.zsec.utils.CryptoUtils;
import com.iq.zsec.utils.FileUtils;

import java.io.File;
import java.util.List;

public class MediaViewerActivity extends Activity {

    public static final String EXTRA_STORED_NAME = "stored_name";

    // ── State ────────────────────────────────────────────────────
    private FileRecord       record;
    private File             tempShareFile = null;
    private File             tempVideoFile = null;

    // ── Zoom state ───────────────────────────────────────────────
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor   = 1.0f;
    private float translateX    = 0f;
    private float translateY    = 0f;
    private float lastTouchX    = 0f;
    private float lastTouchY    = 0f;
    private boolean isDragging  = false;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 6.0f;

    // ── Views ────────────────────────────────────────────────────
    private ImageView    imageView;
    private ScrollView   scrollText;
    private TextView     tvText;
    private LinearLayout cardUnsupported;
    private TextView     tvUnsupportedMsg;
    private LinearLayout videoContainer;
    private VideoView    videoView;
    private LinearLayout videoControls;
    private ImageButton  btnPlayPause;
    private TextView     tvVideoName;

    // ── All files in vault (for next/prev) ───────────────────────
    private List<FileRecord> allFiles;
    private int              currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(
            new ColorDrawable(Color.parseColor("#0D1117")));
        setContentView(R.layout.activity_media_viewer);

        String storedName = getIntent().getStringExtra(EXTRA_STORED_NAME);
        if (storedName == null) { finish(); return; }

        // Load all vault files for next/prev navigation
        allFiles = DatabaseHelper.getInstance(this).getAllFiles();
        for (int i = 0; i < allFiles.size(); i++) {
            if (allFiles.get(i).getStoredName().equals(storedName)) {
                currentIndex = i;
                break;
            }
        }
        record = allFiles.get(currentIndex);

        bindViews();
        setupZoom();
        setupButtons();
        loadContent();
    }

    // ── Bind all views ───────────────────────────────────────────

    private void bindViews() {
        imageView        = (ImageView)    findViewById(R.id.image_view);
        scrollText       = (ScrollView)   findViewById(R.id.scroll_text);
        tvText           = (TextView)     findViewById(R.id.tv_text);
        cardUnsupported  = (LinearLayout) findViewById(R.id.card_unsupported);
        tvUnsupportedMsg = (TextView)     findViewById(R.id.tv_unsupported_msg);
        videoContainer   = (LinearLayout) findViewById(R.id.video_container);
        videoView        = (VideoView)    findViewById(R.id.video_view);
        videoControls    = (LinearLayout) findViewById(R.id.video_controls);
        btnPlayPause     = (ImageButton)  findViewById(R.id.btn_play_pause);
        tvVideoName      = (TextView)     findViewById(R.id.tv_video_name);
    }

    // ── Zoom / pan setup ─────────────────────────────────────────

    private void setupZoom() {
        scaleDetector = new ScaleGestureDetector(this,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scaleFactor *= detector.getScaleFactor();
                    scaleFactor  = Math.max(MIN_ZOOM,
											Math.min(scaleFactor, MAX_ZOOM));

                    // Reset pan when zooming back to 1x
                    if (scaleFactor == MIN_ZOOM) {
                        translateX = 0f;
                        translateY = 0f;
                    }
                    applyImageTransform();
                    return true;
                }
            });

        imageView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					scaleDetector.onTouchEvent(event);

					// Pan only when zoomed in
					if (scaleFactor > MIN_ZOOM) {
						switch (event.getActionMasked()) {
							case MotionEvent.ACTION_DOWN:
								lastTouchX = event.getX();
								lastTouchY = event.getY();
								isDragging = true;
								break;

							case MotionEvent.ACTION_MOVE:
								if (isDragging && !scaleDetector.isInProgress()) {
									translateX += event.getX() - lastTouchX;
									translateY += event.getY() - lastTouchY;
									lastTouchX  = event.getX();
									lastTouchY  = event.getY();
									applyImageTransform();
								}
								break;

							case MotionEvent.ACTION_UP:
							case MotionEvent.ACTION_CANCEL:
								isDragging = false;
								break;
						}
					}
					return true;
				}
			});
    }

    private void applyImageTransform() {
        Matrix matrix = new Matrix();
        matrix.setScale(scaleFactor, scaleFactor,
						imageView.getWidth()  / 2f,
						imageView.getHeight() / 2f);
        matrix.postTranslate(translateX, translateY);
        imageView.setImageMatrix(matrix);
    }

    // ── Buttons ──────────────────────────────────────────────────

    private void setupButtons() {
        ((TextView) findViewById(R.id.tv_viewer_title)).setText(record.getFileName());

        // Back
        ((ImageButton) findViewById(R.id.btn_viewer_back))
            .setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });

        // Share
        ((ImageButton) findViewById(R.id.btn_viewer_share))
            .setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { shareCurrentFile(); }
            });

        // Previous file
        ((ImageButton) findViewById(R.id.btn_prev_file))
            .setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { navigateFile(-1); }
            });

        // Next file
        ((ImageButton) findViewById(R.id.btn_next_file))
            .setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { navigateFile(1); }
            });

        // Zoom reset (double-tap hint button)
        ((TextView) findViewById(R.id.btn_zoom_reset))
            .setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    scaleFactor = 1.0f;
                    translateX  = 0f;
                    translateY  = 0f;
                    applyImageTransform();
                }
            });

        // Play / Pause
        btnPlayPause.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { togglePlayPause(); }
			});
    }

    // ── Navigate prev / next ─────────────────────────────────────

    private void navigateFile(int direction) {
        int next = currentIndex + direction;
        if (next < 0 || next >= allFiles.size()) {
            Toast.makeText(this,
						   direction > 0 ? "Last file." : "First file.",
						   Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any playing video
        if (videoView.isPlaying()) videoView.stopPlayback();
        cleanupTempFiles();

        currentIndex = next;
        record       = allFiles.get(currentIndex);

        // Reset zoom
        scaleFactor = 1.0f;
        translateX  = 0f;
        translateY  = 0f;

        ((TextView) findViewById(R.id.tv_viewer_title)).setText(record.getFileName());
        loadContent();
    }

    // ── Load content ─────────────────────────────────────────────

    private void loadContent() {
        // Hide all panels first
        imageView.setVisibility(View.GONE);
        scrollText.setVisibility(View.GONE);
        cardUnsupported.setVisibility(View.GONE);
        videoContainer.setVisibility(View.GONE);
        findViewById(R.id.bar_zoom).setVisibility(View.GONE);
        videoControls.setVisibility(View.GONE);

        File src = FileUtils.getProtectedFile(this, record.getStoredName());
        if (!src.exists()) {
            showUnsupported("File not found on device."); return;
        }

        String mime = record.getMimeType().toLowerCase();
        String name = record.getFileName().toLowerCase();

        if (isVideo(mime, name))     showVideo(src);
        else if (isImage(mime, name)) showImage(src);
        else if (isText(mime, name))  showText(src);
        else showUnsupported(
				"No preview for this file type.\nTap Share to open in another app.");
    }

    // ── Image viewer ─────────────────────────────────────────────

    private void showImage(File src) {
        byte[] data = CryptoUtils.decryptToBytes(src, this);
        if (data == null) { showUnsupported("Could not read file."); return; }

        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
        if (bmp == null) { showUnsupported("Could not decode image."); return; }

        // ✅ MATRIX scale type required for zoom
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        imageView.setImageBitmap(bmp);
        imageView.setVisibility(View.VISIBLE);

        // Show zoom reset bar
        findViewById(R.id.bar_zoom).setVisibility(View.VISIBLE);
    }

    // ── Video player ─────────────────────────────────────────────

    private void showVideo(final File src) {
        // Decrypt to temp file — VideoView needs a real URI
        File shareDir = new File(getCacheDir(), "shared");
        if (!shareDir.exists()) shareDir.mkdirs();
        tempVideoFile = new File(shareDir, record.getFileName());

        boolean ok = CryptoUtils.decryptFileToPath(src, tempVideoFile, this);
        if (!ok) ok = FileUtils.copyFileToFile(src, tempVideoFile);

        if (!ok) {
            showUnsupported("Could not prepare video for playback.");
            return;
        }

        Uri videoUri = Uri.fromFile(tempVideoFile);

        // ✅ Native MediaController — gives scrubber, volume, fullscreen
        MediaController mc = new MediaController(this);
        mc.setAnchorView(videoView);
        videoView.setMediaController(mc);
        videoView.setVideoURI(videoUri);

        tvVideoName.setText(record.getFileName());
        videoContainer.setVisibility(View.VISIBLE);
        videoControls.setVisibility(View.VISIBLE);

        videoView.setOnPreparedListener(
            new android.media.MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(android.media.MediaPlayer mp) {
                    mp.setLooping(false);
                    videoView.start();
                    updatePlayPauseIcon();
                }
            });

        videoView.setOnCompletionListener(
            new android.media.MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(android.media.MediaPlayer mp) {
                    btnPlayPause.setImageResource(
                        android.R.drawable.ic_media_play);
                }
            });

        videoView.requestFocus();
    }

    private void togglePlayPause() {
        if (videoView.isPlaying()) {
            videoView.pause();
        } else {
            videoView.start();
        }
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        btnPlayPause.setImageResource(
            videoView.isPlaying()
			? android.R.drawable.ic_media_pause
			: android.R.drawable.ic_media_play);
    }

    // ── Text viewer ──────────────────────────────────────────────

    private void showText(File src) {
        byte[] data = CryptoUtils.decryptToBytes(src, this);
        if (data == null) { showUnsupported("Could not read file."); return; }
        try {
            tvText.setText(new String(data, "UTF-8"));
            scrollText.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            showUnsupported("Text decode error.");
        }
    }

    // ── Unsupported ──────────────────────────────────────────────

    private void showUnsupported(String msg) {
        tvUnsupportedMsg.setText(msg);
        cardUnsupported.setVisibility(View.VISIBLE);
    }

    // ── Share ────────────────────────────────────────────────────

    private void shareCurrentFile() {
        File src = FileUtils.getProtectedFile(this, record.getStoredName());
        if (!src.exists()) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        File shareDir = new File(getCacheDir(), "shared");
        if (!shareDir.exists()) shareDir.mkdirs();
        tempShareFile = new File(shareDir, record.getFileName());

        boolean ok = CryptoUtils.decryptFileToPath(src, tempShareFile, this);
        if (!ok) ok = FileUtils.copyFileToFile(src, tempShareFile);
        if (!ok) {
            Toast.makeText(this, "Could not prepare file.", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(
            this, "com.iq.zsec.fileprovider", tempShareFile);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(record.getMimeType());
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(
						  intent, "Share \"" + record.getFileName() + "\""));
    }

    // ── Cleanup ──────────────────────────────────────────────────

    private void cleanupTempFiles() {
        if (tempShareFile != null && tempShareFile.exists()) tempShareFile.delete();
        if (tempVideoFile != null && tempVideoFile.exists()) tempVideoFile.delete();
    }
// Add inside MediaViewerActivity — called by XML android:onClick
	public void rewindVideo(View v) {
		if (videoView != null) {
			int pos = videoView.getCurrentPosition() - 10000;
			videoView.seekTo(Math.max(pos, 0));
		}
	}

	public void forwardVideo(View v) {
		if (videoView != null) {
			int pos = videoView.getCurrentPosition() + 10000;
			videoView.seekTo(Math.min(pos, videoView.getDuration()));
		}
	}
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null && videoView.isPlaying()) videoView.stopPlayback();
        cleanupTempFiles();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            updatePlayPauseIcon();
        }
    }

    // ── Type detection ───────────────────────────────────────────

    private boolean isImage(String mime, String name) {
        return mime.startsWith("image/")
            || name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".png") || name.endsWith(".bmp")
            || name.endsWith(".webp") || name.endsWith(".gif");
    }

    private boolean isVideo(String mime, String name) {
        return mime.startsWith("video/")
            || name.endsWith(".mp4") || name.endsWith(".mkv")
            || name.endsWith(".avi") || name.endsWith(".mov")
            || name.endsWith(".3gp") || name.endsWith(".webm")
            || name.endsWith(".flv") || name.endsWith(".ts");
    }

    private boolean isText(String mime, String name) {
        if (mime.startsWith("text/")) return true;
        return name.endsWith(".txt")  || name.endsWith(".md")
            || name.endsWith(".json") || name.endsWith(".xml")
            || name.endsWith(".java") || name.endsWith(".py")
            || name.endsWith(".html") || name.endsWith(".css")
            || name.endsWith(".js")   || name.endsWith(".log")
            || name.endsWith(".csv")  || name.endsWith(".sh")
            || name.endsWith(".ini")  || name.endsWith(".cfg");
    }
}
