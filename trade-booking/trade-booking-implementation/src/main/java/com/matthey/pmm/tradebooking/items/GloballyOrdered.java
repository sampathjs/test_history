package com.matthey.pmm.tradebooking.items;

public interface GloballyOrdered<T> {

    int order();

    T order(int order);
}
