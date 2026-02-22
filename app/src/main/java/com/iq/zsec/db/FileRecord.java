package com.iq.zsec.db;

/**
 * Represents a single protected file's metadata entry in the local DB.
 *
 * fileName   — original display name shown to user
 * storedName — UUID-based filename used on disk (never the original name)
 * mimeType   — MIME type for SAF document projection
 * fileSize   — in bytes
 * dateAdded  — epoch millis
 */
public class FileRecord {

    private long   id;
    private String fileName;
    private String storedName;
    private String mimeType;
    private long   fileSize;
    private long   dateAdded;

    public FileRecord() {}

    public FileRecord(String fileName, String storedName, String mimeType, long fileSize) {
        this.fileName   = fileName;
        this.storedName = storedName;
        this.mimeType   = mimeType;
        this.fileSize   = fileSize;
        this.dateAdded  = System.currentTimeMillis();
    }

    // ── Getters ──────────────────────────────────────────────────
    public long   getId()          { return id; }
    public String getFileName()    { return fileName; }
    public String getStoredName()  { return storedName; }
    public String getMimeType()    { return mimeType; }
    public long   getFileSize()    { return fileSize; }
    public long   getDateAdded()   { return dateAdded; }

    // ── Setters ──────────────────────────────────────────────────
    public void setId(long id)                { this.id = id; }
    public void setFileName(String n)         { this.fileName = n; }
    public void setStoredName(String n)       { this.storedName = n; }
    public void setMimeType(String m)         { this.mimeType = m; }
    public void setFileSize(long s)           { this.fileSize = s; }
    public void setDateAdded(long d)          { this.dateAdded = d; }

    /** documentId used by the SAF DocumentsProvider = storedName */
    public String getDocumentId() { return storedName; }
}
