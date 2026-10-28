package com.reachkids.app;

/** A phone-number row owned by the Android system Contacts provider. */
public final class SystemContact {
    public final long rawContactId;
    public final long phoneDataId;
    public final long contactId;
    public final String displayName;
    public final String phoneNumber;

    public SystemContact(long rawContactId, long phoneDataId, long contactId,
            String displayName, String phoneNumber) {
        this.rawContactId = rawContactId;
        this.phoneDataId = phoneDataId;
        this.contactId = contactId;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
    }
}
