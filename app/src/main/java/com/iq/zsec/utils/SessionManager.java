package com.iq.zsec.utils;

/**
 * In-memory session state shared across Activities and the DocumentsProvider.
 * Both run in the same process, so this singleton works reliably.
 *
 * Session auto-expires after SESSION_TIMEOUT_MS of inactivity.
 */
public class SessionManager {

    private static volatile SessionManager instance;

    /** 10-minute idle timeout */
    private static final long SESSION_TIMEOUT_MS = 10 * 60 * 1000L;

    private boolean authenticated = false;
    private long lastActivity = 0L;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    /** Call after successful password verification. */
    public synchronized void setAuthenticated() {
        this.authenticated = true;
        this.lastActivity = System.currentTimeMillis();
    }

    /** Returns true only if authenticated AND session has not timed out. */
    public synchronized boolean isAuthenticated() {
        if (!authenticated) return false;
        boolean expired = (System.currentTimeMillis() - lastActivity) > SESSION_TIMEOUT_MS;
        if (expired) {
            invalidate();
            return false;
        }
        return true;
    }

    /** Resets the idle timer on user activity. */
    public synchronized void refresh() {
        if (authenticated) {
            lastActivity = System.currentTimeMillis();
        }
    }

    /** Locks the vault. Session must be re-established via AuthActivity. */
    public synchronized void invalidate() {
        authenticated = false;
        lastActivity = 0L;
    }
}
