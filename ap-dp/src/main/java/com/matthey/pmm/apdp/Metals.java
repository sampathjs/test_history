package com.matthey.pmm.apdp;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class Metals {
    public static final BiMap<String, String> METAL_NAMES = ImmutableBiMap.of("Platinum",
                                                                              "XPT",
                                                                              "Palladium",
                                                                              "XPD",
                                                                              "Rhodium",
                                                                              "XRH");
}
