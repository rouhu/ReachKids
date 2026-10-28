package com.reachkids.app;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.List;

/** Reads and changes the phone's Contacts provider without copying contacts into app storage. */
public final class ContactsRepository {
    private final ContentResolver resolver;

    public ContactsRepository(Context context) {
        resolver = context.getApplicationContext().getContentResolver();
    }

    public List<SystemContact> loadPhoneContacts() {
        List<SystemContact> contacts = new ArrayList<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) {
                return contacts;
            }
            while (cursor.moveToNext()) {
                contacts.add(new SystemContact(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getString(3),
                        cursor.getString(4)));
            }
        }
        return contacts;
    }

    /** Re-reads a selected phone row immediately before a call is started. */
    public SystemContact loadPhoneContact(long phoneDataId) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String selection = ContactsContract.CommonDataKinds.Phone._ID + "=?";
        String[] selectionArguments = {String.valueOf(phoneDataId)};
        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArguments,
                null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            return new SystemContact(
                    cursor.getLong(0),
                    cursor.getLong(1),
                    cursor.getLong(2),
                    cursor.getString(3),
                    cursor.getString(4));
        }
    }

    public SystemContact findContactByPhoneNumber(String phoneNumber) {
        for (SystemContact contact : loadPhoneContacts()) {
            if (SmsAddressValidator.addressesMatch(contact.phoneNumber, phoneNumber)) {
                return contact;
            }
        }
        return null;
    }

    public void addPhoneContact(String name, String phoneNumber) {
        ContentValues rawContactValues = new ContentValues();
        long rawContactId = ContentUris.parseId(resolver.insert(
                ContactsContract.RawContacts.CONTENT_URI, rawContactValues));

        try {
            insertData(rawContactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
            insertData(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        } catch (RuntimeException exception) {
            resolver.delete(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId),
                    null, null);
            throw exception;
        }
    }

    private void insertData(long rawContactId, String mimeType, String valueColumn, String value) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, mimeType);
        values.put(valueColumn, value);
        if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
            values.put(ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        }
        resolver.insert(ContactsContract.Data.CONTENT_URI, values);
    }

    public void updatePhoneContact(SystemContact contact, String name, String phoneNumber) {
        ContentValues phoneValues = new ContentValues();
        phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        resolver.update(ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, contact.phoneDataId),
                phoneValues, null, null);

        ContentValues nameValues = new ContentValues();
        nameValues.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        String nameSelection = ContactsContract.Data.RAW_CONTACT_ID + "=? AND "
                + ContactsContract.Data.MIMETYPE + "=?";
        String[] nameSelectionArgs = {
                String.valueOf(contact.rawContactId),
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
        };
        int changedRows = resolver.update(ContactsContract.Data.CONTENT_URI, nameValues,
                nameSelection, nameSelectionArgs);
        if (changedRows == 0) {
            insertData(contact.rawContactId,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        }
    }

    public void deleteRawContact(SystemContact contact) {
        resolver.delete(ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI,
                contact.rawContactId), null, null);
    }
}
