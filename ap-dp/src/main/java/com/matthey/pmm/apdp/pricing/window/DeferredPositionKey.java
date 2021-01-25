package com.matthey.pmm.apdp.pricing.window;

import org.immutables.value.Value;

@Value.Immutable
public interface DeferredPositionKey {
    
    @Value.Parameter
    String externalBU();
    
    @Value.Parameter
    String metal();
}
