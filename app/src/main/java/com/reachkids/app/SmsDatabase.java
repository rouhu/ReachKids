package com.reachkids.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Local SMS-only UI store. This deliberately does not mirror system Contacts or MMS content. */
public final class SmsDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "reach_kids_sms.db";
    private static final int DATABASE_VERSION = 2;
    private static final String TABLE_MESSAGES = "messages";

    public SmsDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE_MESSAGES + " ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "address TEXT NOT NULL, body TEXT NOT NULL, timestamp INTEGER NOT NULL, "
                + "outgoing INTEGER NOT NULL, status TEXT NOT NULL, read INTEGER NOT NULL)");
        database.execSQL("CREATE INDEX messages_address_timestamp ON " + TABLE_MESSAGES
                + " (address, timestamp DESC)");
        database.execSQL("CREATE UNIQUE INDEX messages_unique_incoming ON " + TABLE_MESSAGES
                + " (address, body, timestamp, outgoing)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Keep the first of any exact incoming duplicates created by both SMS_DELIVER and SMS_RECEIVED.
            database.execSQL("DELETE FROM " + TABLE_MESSAGES + " WHERE outgoing=0 AND _id NOT IN ("
                    + "SELECT MIN(_id) FROM " + TABLE_MESSAGES
                    + " WHERE outgoing=0 GROUP BY address, body, timestamp)");
            database.execSQL("CREATE UNIQUE INDEX messages_unique_incoming ON " + TABLE_MESSAGES
                    + " (address, body, timestamp, outgoing)");
        }
    }

    public long insertIncoming(String address, String body, long timestamp) {
        return insertIncomingIgnoringDuplicate(address, body, timestamp);
    }

    public long insertOutgoingPending(String address, String body, long timestamp) {
        return insert(address, body, timestamp, true, "sending", true);
    }

    private long insert(String address, String body, long timestamp, boolean outgoing,
            String status, boolean read) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("body", body);
        values.put("timestamp", timestamp);
        values.put("outgoing", outgoing ? 1 : 0);
        values.put("status", status);
        values.put("read", read ? 1 : 0);
        return getWritableDatabase().insertOrThrow(TABLE_MESSAGES, null, values);
    }

    private long insertIncomingIgnoringDuplicate(String address, String body, long timestamp) {
        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("body", body);
        values.put("timestamp", timestamp);
        values.put("outgoing", 0);
        values.put("status", "received");
        values.put("read", 0);
        return getWritableDatabase().insertWithOnConflict(TABLE_MESSAGES, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void updateStatus(long messageId, String status) {
        ContentValues values = new ContentValues();
        values.put("status", status);
        getWritableDatabase().update(TABLE_MESSAGES, values, "_id=?",
                new String[]{String.valueOf(messageId)});
    }

    public List<SmsThread> loadThreads() {
        List<SmsThread> threads = new ArrayList<>();
        String query = "SELECT m.address, m.body, m.timestamp, m.outgoing, "
                + "(SELECT COUNT(*) FROM " + TABLE_MESSAGES + " u WHERE u.address=m.address "
                + "AND u.outgoing=0 AND u.read=0) AS unread "
                + "FROM " + TABLE_MESSAGES + " m INNER JOIN (SELECT address, MAX(timestamp) last_timestamp "
                + "FROM " + TABLE_MESSAGES + " GROUP BY address) latest "
                + "ON m.address=latest.address AND m.timestamp=latest.last_timestamp "
                + "ORDER BY m.timestamp DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(query, null)) {
            while (cursor.moveToNext()) {
                threads.add(new SmsThread(cursor.getString(0), cursor.getString(1),
                        cursor.getLong(2), cursor.getInt(3) != 0, cursor.getInt(4)));
            }
        }
        return threads;
    }

    public List<SmsMessageRecord> loadConversation(String address) {
        List<SmsMessageRecord> messages = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_MESSAGES,
                new String[]{"_id", "address", "body", "timestamp", "outgoing", "status", "read"},
                "address=?", new String[]{address}, null, null, "timestamp ASC, _id ASC")) {
            while (cursor.moveToNext()) {
                messages.add(new SmsMessageRecord(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getLong(3), cursor.getInt(4) != 0,
                        cursor.getString(5), cursor.getInt(6) != 0));
            }
        }
        return messages;
    }

    public void markConversationRead(String address) {
        ContentValues values = new ContentValues();
        values.put("read", 1);
        getWritableDatabase().update(TABLE_MESSAGES, values,
                "address=? AND outgoing=0", new String[]{address});
    }
}
