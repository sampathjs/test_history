package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.jm.fixGateway.fieldMapper.fields.EnumInstrumentTags;
import com.olf.jm.fixGateway.fieldUtils.FixMessageHelper;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-18 - V0.1 - jwaechter - Initial Version
 */

/**
 * Derives the internal port folio for ComSwap MetalSwap deals of the JPM interface based
 * on the incoming FIX message.  
 * @author jwaechter
 */
public class BaseMetalInternalPortfolio extends FieldMapperBase {
	@Override
	public String getTranFieldValue(Table message)
			throws FieldMapperException {
		String symbol = FixMessageHelper.getInstrumentField(EnumInstrumentTags.SYMBOL, message);
		String startingLetter = symbol.substring(0, 1).toUpperCase();
		String endString = symbol.substring(1).toLowerCase();
		String metalPart =  startingLetter + endString;
		
		InternalBunit precMetalSpotInternalBunit = new InternalBunit();
		String bunit = precMetalSpotInternalBunit.getTranFieldValue(message);
		String regionPart = bunit.substring (7);
		
		return regionPart + " " + metalPart;
	}

	@Override
	public TRANF_FIELD getTranFieldName() {
		// TODO Auto-generated method stub
		return TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO;
	}

	@Override
	public String getTagFieldName() {
		return null; // complex logics
	}


}
