package com.iq.zsec;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iq.zsec.adapter.FileAdapter;
import com.iq.zsec.db.DatabaseHelper;
import com.iq.zsec.db.FileRecord;
import com.iq.zsec.utils.CryptoUtils;
import com.iq.zsec.utils.FileUtils;
import com.iq.zsec.utils.SessionManager;
import com.iq.zsec.utils.VaultKeyManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
implements FileAdapter.FileActionListener {

    private static final int REQ_PICK_FILE  = 1001;
    private static final int REQ_PERMISSION = 1002;

    private RecyclerView   recyclerView;
    private FileAdapter    adapter;
    private TextView       tvEmpty;
    private LinearLayout   barSelection;
    private TextView       tvSelectionCount;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(
            new ColorDrawable(Color.parseColor("#0D1117")));

        // ── Guards ────────────────────────────────────────────────
        if (!VaultKeyManager.isSetup(this)) {
            goTo(SetupActivity.class); return;
        }
        if (!SessionManager.getInstance().isAuthenticated()) {
            goTo(AuthActivity.class); return;
        }

        buildUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!SessionManager.getInstance().isAuthenticated()) {
            goTo(AuthActivity.class); return;
        }
        SessionManager.getInstance().refresh();
        if (recyclerView != null) refreshList();
    }

    private void buildUI() {
        setContentView(R.layout.activity_main);

        db              = DatabaseHelper.getInstance(this);
        recyclerView    = (RecyclerView)  findViewById(R.id.recycler_view);
        tvEmpty         = (TextView)      findViewById(R.id.tv_empty);
        barSelection    = (LinearLayout)  findViewById(R.id.bar_selection);
        tvSelectionCount= (TextView)      findViewById(R.id.tv_selection_count);

        adapter = new FileAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(
            new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(adapter);

        ((ImageButton) findViewById(R.id.btn_settings)).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            });

        ((ImageButton) findViewById(R.id.btn_lock)).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { lockVault(); }
            });

        // ✅ FAB "+"
        findViewById(R.id.fab_add).setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) { checkAndPickFile(); }
			});

        // ✅ Cancel selection
        findViewById(R.id.btn_cancel_selection).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) {
                    adapter.clearSelection();
                    barSelection.setVisibility(View.GONE);
                }
            });

        // ✅ Share selected
        findViewById(R.id.btn_share_selected).setOnClickListener(
            new View.OnClickListener() {
                @Override public void onClick(View v) { shareSelected(); }
            });

        refreshList();
    }

    // ── FileAdapter.FileActionListener ───────────────────────────

    @Override
    public void onViewFile(FileRecord record) {
        Intent i = new Intent(this, MediaViewerActivity.class);
        i.putExtra(MediaViewerActivity.EXTRA_STORED_NAME, record.getStoredName());
        startActivity(i);
    }

    @Override
    public void onDeleteFile(final FileRecord record) {
        new AlertDialog.Builder(this)
            .setTitle("Delete File")
            .setMessage("Permanently delete \"" + record.getFileName() + "\"?")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    File f = FileUtils.getProtectedFile(
                        MainActivity.this, record.getStoredName());
                    if (f.exists()) f.delete();
                    db.deleteFile(record.getStoredName());
                    refreshList();
                    Toast.makeText(MainActivity.this,
								   "Deleted.", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null).show();
    }

    @Override
    public void onSelectionChanged(final List<FileRecord> selected) {
        runOnUiThread(new Runnable() {
				@Override public void run() {
					if (selected.isEmpty()) {
						barSelection.setVisibility(View.GONE);
					} else {
						barSelection.setVisibility(View.VISIBLE);
						tvSelectionCount.setText(selected.size() + " selected");
					}
				}
			});
    }

    // ── Share selected files ─────────────────────────────────────

    private void shareSelected() {
        // Collect selected records from adapter
        // We pass through onSelectionChanged so we rebuild from DB
        // Simpler: re-query from DB matching selection IDs
        // Actually the adapter already has the list — let's
        // just re-iterate and collect from the DB snapshot.

        List<FileRecord> all = db.getAllFiles();
        final List<FileRecord> toShare = new ArrayList<FileRecord>();
        // We'll share all files currently selected by triggering share from
        // the selection bar — adapter manages selection state.
        // Trigger via a fresh snapshot from adapter callback.

        // Since we can't call back into adapter directly here,
        // we'll iterate all files and check the cache dir
        // (The selection bar is only visible when there ARE selected items)
        // → Let's add a getSelected() method to adapter.

        // For now, collect files whose temp decrypt exists or all
        // that the user might have selected. We use a workaround:
        // rebuild selection from the list visible to onSelectionChanged.

        // ✅ CLEAN SOLUTION: call a public method on adapter
        // (add getSelectedRecords() to FileAdapter)
        shareFilesInBackground(adapter.getSelectedRecords());
    }

    private void shareFilesInBackground(final List<FileRecord> records) {
        if (records.isEmpty()) {
            Toast.makeText(this, "Nothing selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(new Runnable() {
				@Override public void run() {
					File shareDir = new File(getCacheDir(), "shared");
					if (!shareDir.exists()) shareDir.mkdirs();

					final ArrayList<Uri> uris = new ArrayList<Uri>();

					for (int i = 0; i < records.size(); i++) {
						FileRecord r   = records.get(i);
						File       src = FileUtils.getProtectedFile(
							MainActivity.this, r.getStoredName());
						if (!src.exists()) continue;

						File dest = new File(shareDir, r.getFileName());
						boolean ok = CryptoUtils.decryptFileToPath(
							src, dest, MainActivity.this);
						if (!ok) ok = FileUtils.copyFileToFile(src, dest);
						if (!ok) continue;

						Uri uri = FileProvider.getUriForFile(
							MainActivity.this,
							"com.iq.zsec.fileprovider", dest);
						uris.add(uri);
					}

					runOnUiThread(new Runnable() {
							@Override public void run() {
								if (uris.isEmpty()) {
									Toast.makeText(MainActivity.this,
												   "Could not prepare files.",
												   Toast.LENGTH_SHORT).show();
									return;
								}

								Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
								intent.setType("*/*");
								intent.putParcelableArrayListExtra(
									Intent.EXTRA_STREAM, uris);
								intent.addFlags(
									Intent.FLAG_GRANT_READ_URI_PERMISSION);
								startActivity(Intent.createChooser(
												  intent, "Share " + uris.size() + " file(s)"));

								adapter.clearSelection();
								barSelection.setVisibility(View.GONE);
							}
						});
				}
			}).start();
    }

    // ── Import ───────────────────────────────────────────────────

    private void checkAndPickFile() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
			!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQ_PERMISSION);
        } else {
            launchPicker();
        }
    }

    private void launchPicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(
            Intent.createChooser(i, "Select file to secure"), REQ_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PICK_FILE && res == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) importFile(uri);
        }
    }

    private void importFile(final Uri uri) {
        new Thread(new Runnable() {
				@Override public void run() {
					final String origName   = FileUtils.getFileName(MainActivity.this, uri);
					final String storedName = FileUtils.generateStoredName(origName);
					final String mimeType   = FileUtils.getMimeType(origName);
					final File   dest       = FileUtils.getProtectedFile(
						MainActivity.this, storedName);

					// Step 1: copy raw file
					boolean copied = FileUtils.copyUriToFile(
						MainActivity.this, uri, dest);
					if (!copied) {
						runOnUiThread(new Runnable() {
								@Override public void run() {
									Toast.makeText(MainActivity.this,
												   "Import failed.", Toast.LENGTH_SHORT).show();
								}
							});
						return;
					}

					// ✅ Step 2: encrypt in-place with AES-256 device key
					boolean encrypted = CryptoUtils.encryptFile(dest, MainActivity.this);
					if (!encrypted) {
						dest.delete();
						runOnUiThread(new Runnable() {
								@Override public void run() {
									Toast.makeText(MainActivity.this,
												   "Encryption failed.", Toast.LENGTH_SHORT).show();
								}
							});
						return;
					}

					// Step 3: record metadata
					FileRecord record = new FileRecord(
						origName, storedName, mimeType, dest.length());
					db.insertFile(record);

					getContentResolver().notifyChange(
						android.provider.DocumentsContract.buildChildDocumentsUri(
							ZSecDocumentsProvider.AUTHORITY, "root"), null);

					runOnUiThread(new Runnable() {
							@Override public void run() {
								refreshList();
								Toast.makeText(MainActivity.this,
											   "\"" + origName + "\" encrypted & secured.",
											   Toast.LENGTH_SHORT).show();
							}
						});
				}
			}).start();
    }

    // ── Helpers ──────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int code,
										   String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == REQ_PERMISSION && results.length > 0
			&& results[0] == PackageManager.PERMISSION_GRANTED) {
            launchPicker();
        } else {
            Toast.makeText(this,
						   "Permission required to import.", Toast.LENGTH_SHORT).show();
        }
    }

    private void lockVault() {
        SessionManager.getInstance().invalidate();
        goTo(AuthActivity.class);
    }

    private void goTo(Class<?> cls) {
        Intent i = new Intent(this, cls);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void refreshList() {
        List<FileRecord> files = db.getAllFiles();
        adapter.setFiles(files);
        tvEmpty.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
