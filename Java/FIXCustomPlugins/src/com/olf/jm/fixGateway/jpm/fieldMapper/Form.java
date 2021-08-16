package com.olf.jm.fixGateway.jpm.fieldMapper;

import com.olf.jm.fixGateway.fieldMapper.FieldMapperBase;
import com.olf.jm.fixGateway.fieldMapper.FieldMapperException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2020-05-15 - V0.1 - jwaechter - Initial Version
 * 2020-08-13 - V0.2 - jwaechter - Added logic for pure currency deals.
 * 
 */


/**
 *  Class responsible for mapping the Form. Currently WIP as the specification regarding 
 *  this field is not final.
 */
public class Form extends FieldMapperBase {

	
	private static final String INGOT = "Ingot";
	private static final String SPONGE = "Sponge";
	private static final String NONE = "None";
	

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTranFieldName()
	 */
	@Override
	public TRANF_FIELD getTranFieldName() {
		return TRANF_FIELD.TRANF_TRAN_INFO;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapperBase#getTagFieldName()
	 */
	@Override
	public String getTagFieldName() {
		return null; // complex logic
	}
	
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		InternalBunit internalBunit = new InternalBunit();
		String intBU = internalBunit.getTranFieldValue(message);
		Ticker ticker = new Ticker ();
		boolean pureCurrencyDeal = ticker.isPureCurrencyDeal(message);
		if (pureCurrencyDeal) {
			return NONE;
		}
		switch (intBU) {
		case "JM PMM UK":
			return SPONGE;
		case "JM PMM US":
			return SPONGE;
		case "JM PMM HK":
			return INGOT;
		case "JM PMM CN":
			return SPONGE;
		default:
			throw new FieldMapperException ("Error retrieval value for Tran Info field 'Form': Unknown Int BU '" + intBU + "'");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#isInfoField()
	 */
	@Override
	public boolean isInfoField() {
		return true;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#infoFieldName()
	 */
	@Override
	public String infoFieldName() throws FieldMapperException {
		return "Form";
	}
}
