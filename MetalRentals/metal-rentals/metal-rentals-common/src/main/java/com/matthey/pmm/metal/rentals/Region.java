package com.matthey.pmm.metal.rentals;

public enum Region {
    CN, NonCN;

    public static Region fromInternalBU(String internalBU) {
        return internalBU.equals("JM PMM CN") ? Region.CN : Region.NonCN;
    }
}
