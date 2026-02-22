package com.iq.zsec;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;

import com.iq.zsec.db.DatabaseHelper;
import com.iq.zsec.db.FileRecord;
import com.iq.zsec.utils.FileUtils;
import com.iq.zsec.utils.SessionManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

public class ZSecDocumentsProvider extends DocumentsProvider {

    public static final String  AUTHORITY    = "com.iq.zsec.documents";
    private static final String ROOT_ID      = "zsec_root";
    private static final String ROOT_DOC_ID  = "root";

    private static final String[] DEFAULT_ROOT_PROJECTION = {
        Root.COLUMN_ROOT_ID,
        Root.COLUMN_ICON,
        Root.COLUMN_TITLE,
        Root.COLUMN_SUMMARY,
        Root.COLUMN_FLAGS,
        Root.COLUMN_DOCUMENT_ID,
        Root.COLUMN_MIME_TYPES,
        Root.COLUMN_AVAILABLE_BYTES
    };

    private static final String[] DEFAULT_DOC_PROJECTION = {
        Document.COLUMN_DOCUMENT_ID,
        Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_SIZE,
        Document.COLUMN_MIME_TYPE,
        Document.COLUMN_LAST_MODIFIED,
        Document.COLUMN_FLAGS
    };

    @Override
    public boolean onCreate() {
        DatabaseHelper.getInstance(getContext());
        FileUtils.getProtectedDir(getContext());
        return true;
    }

    // ────────────────────────────────────────────────────────────
    // queryRoots
    // ────────────────────────────────────────────────────────────
    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        String[] cols = (projection != null) ? projection : DEFAULT_ROOT_PROJECTION;
        MatrixCursor result = new MatrixCursor(cols);
        MatrixCursor.RowBuilder row = result.newRow();

        boolean locked = !SessionManager.getInstance().isAuthenticated();

        row.add(Root.COLUMN_ROOT_ID,      ROOT_ID);
        row.add(Root.COLUMN_ICON,         R.mipmap.ic_launcher);
        row.add(Root.COLUMN_TITLE,        "Z_SEC Vault");
        row.add(Root.COLUMN_SUMMARY,      locked
				? "Locked — open Z_SEC app first"
				: "Unlocked");
        row.add(Root.COLUMN_FLAGS,
                Root.FLAG_SUPPORTS_CREATE |
                Root.FLAG_LOCAL_ONLY      |
                Root.FLAG_SUPPORTS_IS_CHILD);
        row.add(Root.COLUMN_DOCUMENT_ID,  ROOT_DOC_ID);
        row.add(Root.COLUMN_MIME_TYPES,   "*/*");
        row.add(Root.COLUMN_AVAILABLE_BYTES,
                getContext().getFilesDir().getFreeSpace());

        return result;
    }

    // ────────────────────────────────────────────────────────────
    // queryDocument
    // ────────────────────────────────────────────────────────────
    @Override
    public Cursor queryDocument(String documentId, String[] projection)
	throws FileNotFoundException {
        checkAuth();
        String[] cols = (projection != null) ? projection : DEFAULT_DOC_PROJECTION;
        MatrixCursor result = new MatrixCursor(cols);

        if (ROOT_DOC_ID.equals(documentId)) {
            addRootDocRow(result);
        } else {
            FileRecord r = DatabaseHelper.getInstance(getContext())
				.getByStoredName(documentId);
            if (r == null) {
                throw new FileNotFoundException("Not found: " + documentId);
            }
            addFileRow(result, r);
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────
    // queryChildDocuments
    // ────────────────────────────────────────────────────────────
    @Override
    public Cursor queryChildDocuments(String parentDocumentId,
									  String[] projection, String sortOrder) throws FileNotFoundException {
        checkAuth();
        String[] cols = (projection != null) ? projection : DEFAULT_DOC_PROJECTION;
        MatrixCursor result = new MatrixCursor(cols);

        List<FileRecord> files =
            DatabaseHelper.getInstance(getContext()).getAllFiles();
        for (int i = 0; i < files.size(); i++) {
            addFileRow(result, files.get(i));
        }
        return result;
    }

    // ────────────────────────────────────────────────────────────
    // openDocument
    // ────────────────────────────────────────────────────────────
    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode,
											 CancellationSignal signal) throws FileNotFoundException {
        checkAuth();
        SessionManager.getInstance().refresh();

        File file = FileUtils.getProtectedFile(getContext(), documentId);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + documentId);
        }
        return ParcelFileDescriptor.open(
            file, ParcelFileDescriptor.parseMode(mode));
    }

    // ────────────────────────────────────────────────────────────
    // createDocument
    // ────────────────────────────────────────────────────────────
    @Override
    public String createDocument(String parentDocId, String mimeType,
								 String displayName) throws FileNotFoundException {
        checkAuth();

        String storedName = FileUtils.generateStoredName(displayName);
        File newFile = FileUtils.getProtectedFile(getContext(), storedName);

        try {
            if (!newFile.createNewFile()) {
                throw new FileNotFoundException(
                    "Could not create: " + displayName);
            }
        } catch (java.io.IOException e) {
            throw new FileNotFoundException(
                "Create failed: " + e.getMessage());
        }

        FileRecord record =
            new FileRecord(displayName, storedName, mimeType, 0L);
        DatabaseHelper.getInstance(getContext()).insertFile(record);

        Uri notifyUri = DocumentsContract.buildChildDocumentsUri(
            AUTHORITY, parentDocId);
        getContext().getContentResolver().notifyChange(notifyUri, null);

        return storedName;
    }

    // ────────────────────────────────────────────────────────────
    // deleteDocument
    // ────────────────────────────────────────────────────────────
    @Override
    public void deleteDocument(String documentId) throws FileNotFoundException {
        checkAuth();

        File file = FileUtils.getProtectedFile(getContext(), documentId);
        if (file.exists() && !file.delete()) {
            throw new FileNotFoundException(
                "Could not delete: " + documentId);
        }
        DatabaseHelper.getInstance(getContext()).deleteFile(documentId);
    }

    @Override
    public boolean isChildDocument(String parentDocId, String documentId) {
        return ROOT_DOC_ID.equals(parentDocId)
            && !ROOT_DOC_ID.equals(documentId);
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────

    /**
     * ✅ NO AuthenticationRequiredException — AIDE incompatible.
     *
     * Strategy:
     *   If vault is locked → silently launch AuthActivity so user
     *   can unlock, then throw FileNotFoundException to deny access.
     *   After the user unlocks inside the app, they retry the picker.
     */
    private void checkAuth() throws FileNotFoundException {
        if (SessionManager.getInstance().isAuthenticated()) {
            return;
        }

        // Launch AuthActivity so user can unlock the vault
        Intent authIntent = new Intent(getContext(), AuthActivity.class);
        authIntent.putExtra(AuthActivity.EXTRA_SAF_MODE, true);
        authIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TOP);
        getContext().startActivity(authIntent);

        // Deny access until vault is unlocked
        throw new FileNotFoundException(
            "Z_SEC vault is locked. Please unlock via the Z_SEC app.");
    }

    private void addRootDocRow(MatrixCursor result) {
        result.newRow()
            .add(Document.COLUMN_DOCUMENT_ID,   ROOT_DOC_ID)
            .add(Document.COLUMN_DISPLAY_NAME,  "Z_SEC Vault")
            .add(Document.COLUMN_MIME_TYPE,     Document.MIME_TYPE_DIR)
            .add(Document.COLUMN_SIZE,          0L)
            .add(Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
            .add(Document.COLUMN_FLAGS,         Document.FLAG_DIR_SUPPORTS_CREATE);
    }

    private void addFileRow(MatrixCursor result, FileRecord r) {
        result.newRow()
            .add(Document.COLUMN_DOCUMENT_ID,   r.getStoredName())
            .add(Document.COLUMN_DISPLAY_NAME,  r.getFileName())
            .add(Document.COLUMN_SIZE,          r.getFileSize())
            .add(Document.COLUMN_MIME_TYPE,     r.getMimeType())
            .add(Document.COLUMN_LAST_MODIFIED, r.getDateAdded())
            .add(Document.COLUMN_FLAGS,
                 Document.FLAG_SUPPORTS_DELETE |
                 Document.FLAG_SUPPORTS_WRITE);
    }
}
