package 	com.matthey.pmm.toms.transport;

import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.jetbrains.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Contains a row in the Counter Party <-> TOMS Product <-> Loco <-> Form <-> Account Name
 * mapping
 * @author jwaechter
 * @version 1.0
 */
@Immutable
@JsonSerialize(as = ImmutableCounterPartyTickerRuleTo.class)
@JsonDeserialize(as = ImmutableCounterPartyTickerRuleTo.class)
@JsonRootName (value = "counterparty_ticker_rule")
@ApiModel(value = "Counter Party Ticker Rule", description = "Input validation rule that maps a counter party to the allowed tickers, metal locations, metal forms and Endur side accounts")
public abstract class CounterPartyTickerRuleTo {  
	@ApiModelProperty(value = "The ID of the counterparty (business unit) of this validation rule. The counterparty can both be internal or external.",
			allowEmptyValue = false,
			required = true)
    public abstract long idCounterParty();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the counter party having the idCounterParty as it should be displayed. Parties are their individual entity type. ",
		allowEmptyValue = true,
		required = false)
    public abstract String counterPartyDisplayString();
    
	@ApiModelProperty(value = "The ID of one ticker that is allowed in combination with the counterparty. Tickers are instances of Reference of ReferenceType ID #30 (Ticker)",
			allowEmptyValue = false,
			required = true)
    public abstract long idTicker();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the ticker having the idTicker as it should be displayed",
		allowEmptyValue = true,
		required = false)
    public abstract String tickerDisplayName();
    
	@ApiModelProperty(value = "The ID of the location that is allowed in combination with the counterparty and ticker. The IDs are instances of Reference having ReferenceType ID #23 (Location)",
			allowEmptyValue = false,
			required = true)
    public abstract long idMetalLocation();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the metal location having the idMetalLocation as it should be displayed",
		allowEmptyValue = true,
		required = false)
    public abstract String metalLocationDisplayString();

	@ApiModelProperty(value = "The ID of the metal from that is allowed in combination with the counterparty and ticker. The IDs are instances of Reference having ReferenceType ID #22 (Form)",
			allowEmptyValue = false,
			required = true)
    public abstract long idMetalForm();

    @Auxiliary
    @Nullable
    @JsonInclude(value = Include.NON_NULL, content = Include.NON_NULL)
	@ApiModelProperty(value = "The name of the metal location having the idMetalLocation as it should be displayed",
		allowEmptyValue = true,
		required = false)
    public abstract String metalFormDisplayString();

    /**
     * The name of the Endur account belonging to the combination of counterparty, ticker, metal location and metal form.
     * @return
     */
    public abstract String accountName();
}
