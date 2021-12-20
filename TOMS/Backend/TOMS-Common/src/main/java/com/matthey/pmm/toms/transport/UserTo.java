package com.matthey.pmm.toms.transport;

import java.util.List;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Party entity having an ID, a name and a type.
 * Maintained by Endur.
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableUserTo.class)
@JsonDeserialize(as = ImmutableUserTo.class)
@JsonRootName (value = "user")
public abstract class UserTo {
	/**
	 * Endur side ID.
	 * @return
	 */
    public abstract long id();
   
    @Nullable
    @Auxiliary
    public abstract String email();

    @Nullable
    @Auxiliary
    public abstract String firstName();

    @Nullable
    @Auxiliary
    public abstract String lastName();

    public abstract long idLifecycleStatus();
    
    @Auxiliary
    public abstract long roleId();
    
    @Auxiliary
    public abstract List<Long> tradeableCounterPartyIds();
    
    @Auxiliary
    public abstract List<Long> tradeableInternalPartyIds();
    
    @Auxiliary
    public abstract List<Long> tradeablePortfolioIds();
    
    @Auxiliary
    @Nullable
    public abstract Long idDefaultInternalBu();

    @Auxiliary
    @Nullable
    public abstract Long idDefaultInternalPortfolio();
}