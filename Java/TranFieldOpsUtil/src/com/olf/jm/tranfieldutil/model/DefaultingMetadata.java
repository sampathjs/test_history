package com.olf.jm.tranfieldutil.model;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-03	V1.1	jwaechter	- added guards for changes to FX Period / FX Far Period changes
 *                                  - Added guards for buy/sell changes on Swaps
 * 2016-02-19	V1.2	jwaechter	- fixed identificators on DEALT_RATE_DEFAULT_DEALT_RATE_LOC_SWAP2,
 *                                    DEALT_RATE_DEFAULT_DEALT_RATE_SWAP2 and DEALT_RATE_DEFAULT_DEALT_RATE_QUAL_SWAP2
 * 2016-02-25	V1.3	jwaechter	- added rules to default metal swap dates on legs
 */

/**
 * Contains the defaulting metadata in context of trade price and metal swaps. 
 * In the follwing enums certain fields called triggers are specified and another 
 * tran field is specified to be filled with the value of the trigger in case
 * a transaction matching instrument type and additional filter criteria is 
 * being processed.
 * @author jwaechter
 * @version 1.3
 */
public enum DefaultingMetadata implements TranFieldMetadata {
	METAL_SWAP_LEG0_MATURITY_BY_LEG0_START_DATE(INS_TYPE_ENUM.metal_swap, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_START_DATE, "", 0, 0, 0, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_MAT_DATE, "", 0, 0, 0, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_PYMT_PERIOD, "", 0, 0, 0, -1, -1),
					Operator.EQUALS, "1d")),
	METAL_SWAP_LEG1_MATURITY_BY_LEG0_START_DATE(INS_TYPE_ENUM.metal_swap, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_START_DATE, "", 0, 0, 0, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_MAT_DATE, "", 1, 0, 0, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_PYMT_PERIOD, "", 0, 0, 0, -1, -1),
					Operator.EQUALS, "1d")),
	METAL_SWAP_LEG1_START_DATE_BY_LEG0_START_DATE(INS_TYPE_ENUM.metal_swap, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_START_DATE, "", 0, 0, 0, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_MAT_DATE, "", 1, 0, 0, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_PYMT_PERIOD, "", 0, 0, 0, -1, -1),
					Operator.EQUALS, "1d")),
	TRADE_UNIT_DEFAULT_BASE_LOC_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT, "", 1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Location Swap")),
	TRADE_UNIT_DEFAULT_TERM_LOC_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT, "", 1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Location Swap")),
	DEALT_RATE_DEFAULT_DEALT_RATE_LOC_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Location Swap")),
	DEALT_RATE_DEFAULT_DEALT_RATE_LOC_SWAP2(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Location Swap")),
	TRADE_UNIT_DEFAULT_BASE_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_BASE_CCY_UNIT, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_BASE_UNIT, "", 1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	TRADE_UNIT_DEFAULT_TERM_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_TERM_CCY_UNIT, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_TERM_UNIT, "", 1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	DEALT_RATE_DEFAULT_DEALT_RATE_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	DEALT_RATE_DEFAULT_DEALT_RATE_SWAP2(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	BUY_SELL_DEALT_RATE_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_BUY_SELL, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	BUY_SELL_FAR_DEALT_RATE_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_BUY_SELL, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),
	BUY_SELL_FAR_PERIODSWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_BUY_SELL, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_PERIOD, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Swap")),

	DEALT_RATE_DEFAULT_DEALT_RATE_QUAL_SWAP(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Quality Swap")),
	DEALT_RATE_DEFAULT_DEALT_RATE_QUAL_SWAP2(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, "", 0, -1, -1, -1, -1),
			new AdditionalFilterCriterium(new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""), 
					Operator.EQUALS, "Quality Swap")),
	;

	private final INS_TYPE_ENUM triggeringInsType;
	
	private final TranFieldIdentificator trigger;
	
	private final TranFieldIdentificator guarded;
	
	private final List<AdditionalFilterCriterium> additionalFilterCriteria; 
	
	
	@SafeVarargs
	private DefaultingMetadata (
			final INS_TYPE_ENUM triggeringInsType,
			final TranFieldIdentificator trigger, 
			final TranFieldIdentificator guarded,
			AdditionalFilterCriterium ... additionalFilterCriteria) {
		this.triggeringInsType = triggeringInsType;
		this.trigger = trigger;
		this.guarded = guarded;
		this.additionalFilterCriteria = new ArrayList<>();
		if (additionalFilterCriteria == null) {
			return;
		}
		for (AdditionalFilterCriterium afc : additionalFilterCriteria) {
			this.additionalFilterCriteria.add(afc);
		}		
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.trading_units.model.TranFieldMetadata#getTriggeringInsType()
	 */
	@Override
	public INS_TYPE_ENUM getTriggeringInsType() {
		return triggeringInsType;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.trading_units.model.TranFieldMetadata#getTrigger()
	 */
	@Override
	public TranFieldIdentificator getTrigger() {
		return trigger;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.trading_units.model.TranFieldMetadata#getGuarded()
	 */
	@Override
	public TranFieldIdentificator getGuarded() {
		return guarded;
	}


	/* (non-Javadoc)
	 * @see com.olf.jm.trading_units.model.TranFieldMetadata#getAdditionalFilterCriteria()
	 */
	@Override
	public List<AdditionalFilterCriterium> getAdditionalFilterCriteria() {
		return additionalFilterCriteria;
	}
}
