package com.matthey.openlink.stamping;

/**
 * The Enum LedgerStatus.
 */
public enum LedgerStatus {
    PENDING_SENT("Pending Sent"),
    SENT("Sent"),
    PENDING_CANCELLED("Pending Cancelled"),
    CANCELLED_SENT("Cancelled Sent"),
    NOT_SENT("NOT Sent"),
    IGNORE("Ignore");

    /**
     * The enumeration value.
     */
    private final String value;

    LedgerStatus(final String value) {
        this.value = value;
    }

    /**
     * @return the enumeration value as a String.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the instance of Enum from the <code>String<code> value.
     * 
     * @param value the enumeration value as String
     * @return the Enum <br>
     *         <code>null<code> if no matching Enum is found
     */
    public static LedgerStatus fromString(final String value) {
        LedgerStatus ledgerStatus = null;
        for (LedgerStatus status : LedgerStatus.values()) {
            if (value.equals(status.getValue())) {
                ledgerStatus = status;
                break;
            }
        }
        return ledgerStatus;
    }
}