package com.olf.jm.fixGateway.fieldMapper;

import com.olf.openjvs.Table;
import com.olf.openjvs.enums.TRANF_FIELD;

/*
 * History:
 * 2017-10-10 - V0.1 - scurran - Initial Version
 */


/**
 * The Interface FieldMapper. Represents an object that can convert a field in the fix message into a field in a 
 * trade builder message.
 */
public interface FieldMapper {

	/**
	 * Gets the transaction field name to populate.
	 *
	 * @return the tran field name
	 */
	TRANF_FIELD getTranFieldName();
	
	/**
	 * Gets the side field is applicable for.
	 *
	 * @return the side
	 */
	int getSide();
	
	/**
	 * Gets the transaction field value.
	 *
	 * @param message the message to extract field value from
	 * @return the transaction field value
	 * @throws FieldMapperException 
	 */
	String getTranFieldValue(Table message) throws FieldMapperException;
	
	/**
	 * Is the field being populated an info field.
	 *
	 * @return true, if is info field
	 */
	boolean isInfoField();
	
	/**
	 * If the field being populated what is the name of the info field.
	 *
	 * @return the info field name
	 * @throws FieldMapperException 
	 */
	String infoFieldName() throws FieldMapperException;
	
	/**
	 * The the seq num 2 the field is applicable for.
	 * @return
	 */
	int getSeqNum2();
	
	/**
	 * The the seq num 3 the field is applicable for.
	 * @return
	 */
	int getSeqNum3();
	
	/**
	 * The the seq num 4 the field is applicable for.
	 * @return
	 */
	int getSeqNum4();
	
	/**
	 * The the seq num 5 the field is applicable for.
	 * @return
	 */
	int getSeqNum5();	
}
