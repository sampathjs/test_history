package com.olf.jm.tranfieldutil.model;

import java.util.ArrayList;
import java.util.List;

import com.olf.openjvs.enums.INS_TYPE_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2015-MM-DD	V1.0	jwaechter	- Initial Version
 * 2016-02-03	V1.1	jwaechter	- added guards for changes to FX Period / FX Far Period changes
 */
/**
 * 
 * @author jwaechter
 * @version 1.1
 */
public enum OverriddenDefaultMetadata implements TranFieldMetadata {
	TRADE_PRICE_GUARD(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, 0, 0, 0, 0),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_AUX_TRAN_INFO, "Trade Price", 1, 0, 0, -1, -1)
			),
	FX_DEALT_RATE_BY_DX_DATE(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DATE, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, -1, -1, -1, -1),
				Operator.NOTEQUALS, "")
			),
	FAX_FAR_DEALT_RATE_BY_FX_FAR_DATE(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DATE, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_AUX_TRAN_INFO, "Trade Price", 1),
							Operator.NOTEQUALS, "")
			),
	FX_FAR_DEALT_RATE_BY_FX_FAR_SPOT_PRICE(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_SPOT_RATE, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_AUX_TRAN_INFO, "Trade Price", 1),
					Operator.NOTEQUALS, "")
			),
	FX_DEALT_RATE_BY_FX_SPOT_PRICE(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_SPOT_RATE, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, -1, -1, -1, -1),
					Operator.NOTEQUALS, "")
			),
	FX_DEALT_RATE_BY_FX_PERIOD(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_PERIOD, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, -1, -1, -1, -1),
					Operator.NOTEQUALS, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""),
					Operator.EQUALS, "Swap")
			),
	FX_FAR_DEALT_RATE_BY_FX_FAR_PERIOD(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_PERIOD, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, -1, -1, -1, -1),
					Operator.NOTEQUALS, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""),
					Operator.EQUALS, "Swap")
			),	
	FX_FAR_DEALT_RATE_BY_FX_PERIOD(INS_TYPE_ENUM.fx_instrument, 
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_PERIOD, ""),
			new TranFieldIdentificator (TRANF_FIELD.TRANF_FX_FAR_DEALT_RATE, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_TRAN_INFO, "Trade Price", 0, -1, -1, -1, -1),
					Operator.NOTEQUALS, ""),
			new AdditionalFilterCriterium (new TranFieldIdentificator (TRANF_FIELD.TRANF_CFLOW_TYPE, ""),
					Operator.EQUALS, "Swap")
			)	
	;

	private final INS_TYPE_ENUM triggeringInsType;
	
	private final TranFieldIdentificator trigger;
	
	private final TranFieldIdentificator guarded;
	
	private final List<AdditionalFilterCriterium> additionalFilterCriteria; 
	
	
	@SafeVarargs
	private OverriddenDefaultMetadata (
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
