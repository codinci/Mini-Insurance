package com.example.miniinsurance.quote;

public enum QuoteStatus {

    /** Priced and purchasable until expiresAt. */
    QUOTED,

    /** Failed a decline rule; never purchasable. */
    DECLINED,

    /** Converted into a policy; single use. */
    PURCHASED,

    /**
     * Derived, never stored: a QUOTED quote whose validity window has passed.
     * Appears in responses and filters only.
     */
    EXPIRED
}
