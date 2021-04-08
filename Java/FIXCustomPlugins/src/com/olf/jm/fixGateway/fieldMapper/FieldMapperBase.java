package com.olf.jm.fixGateway.fieldMapper;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 * 2020-05-14 - V0.2 - jwaechter - FIX Tag is now a string.
 */


/**
 * The Class FieldMapperBase. Base implementation of a field mapper class.
 */
public abstract class FieldMapperBase implements FieldMapper {

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldName()
	 */
	@Override
	abstract public TRANF_FIELD getTranFieldName();
	
	/**
	 * Gets the fix tag  name.
	 *
	 * @return the tag  name
	 */
	abstract public String getTagFieldName();

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getSide()
	 */
	@Override
	public int getSide() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#getTranFieldValue(com.olf.openjvs.Table)
	 */
	@Override
	public String getTranFieldValue(Table message) throws FieldMapperException {
		String tagValue = null;
		
		try {
			if(message == null || message.getNumRows() != 1) {
				String errorMessage = "Invalid message table, table is null or wrong number of rows.";
				Logging.error(errorMessage);
				throw new FieldMapperException(errorMessage);				
			}
		} catch (OException e1) {
			String errorMessage = "Error validating the message table. " + e1.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);	
		}
		
		try {
			tagValue = message.getString(getTagFieldName(), 1);
			Logging.info("Mapping field field " + getTagFieldName() + " value " + tagValue + " to Endur field " + getTranFieldName().toString());
		} catch (OException e) {
			String errorMessage = "Error reading field " + getTagFieldName() + ". " + e.getMessage();
			Logging.error(errorMessage);
			throw new FieldMapperException(errorMessage);
		}
		
		return tagValue;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#isInfoField()
	 */
	@Override
	public boolean isInfoField() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.fixGateway.fieldMapper.FieldMapper#infoFieldName()
	 */
	@Override
	public String infoFieldName() throws FieldMapperException {
		throw new FieldMapperException("Not an info field.");
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Mapping tag " + getTagFieldName() + " to " + getTranFieldName().toString();
	}
	
	/**
	 * The the seq num 2 the field is applicable for.
	 * @return
	 */
	public int getSeqNum2() {
		return 0;
	}
	
	/**
	 * The the seq num 3 the field is applicable for.
	 * @return
	 */
	public int getSeqNum3() {
		return 0;
		
	}
	
	/**
	 * The the seq num 4 the field is applicable for.
	 * @return
	 */
	public int getSeqNum4() {
		return 0;
		
	}
	
	/**
	 * The the seq num 5 the field is applicable for.
	 * @return
	 */
	public int getSeqNum5() {
		return 0;		
	}
	
}
