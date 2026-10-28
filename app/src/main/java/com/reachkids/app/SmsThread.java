package com.reachkids.app;

/** Latest-message summary for a local SMS conversation. */
public final class SmsThread {
    public final String address;
    public final String preview;
    public final long timestamp;
    public final boolean outgoing;
    public final int unreadCount;

    public SmsThread(String address, String preview, long timestamp, boolean outgoing, int unreadCount) {
        this.address = address;
        this.preview = preview;
        this.timestamp = timestamp;
        this.outgoing = outgoing;
        this.unreadCount = unreadCount;
    }
}
