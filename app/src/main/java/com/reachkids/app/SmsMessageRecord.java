package com.reachkids.app;

/** A locally persisted SMS; this app does not persist contacts. */
public final class SmsMessageRecord {
    public final long id;
    public final String address;
    public final String body;
    public final long timestamp;
    public final boolean outgoing;
    public final String status;
    public final boolean read;

    public SmsMessageRecord(long id, String address, String body, long timestamp,
            boolean outgoing, String status, boolean read) {
        this.id = id;
        this.address = address;
        this.body = body;
        this.timestamp = timestamp;
        this.outgoing = outgoing;
        this.status = status;
        this.read = read;
    }
}
