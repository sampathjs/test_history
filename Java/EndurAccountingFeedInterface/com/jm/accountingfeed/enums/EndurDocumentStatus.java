package com.jm.accountingfeed.enums;

import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

/**
 * Custom document status types in Endur
 * 
 * See stldoc_document_status table
 */
public enum EndurDocumentStatus 
{
	SENT_TO_COUNTERPARTY("2 Sent to CP"), 
    SEL_FOR_PAYMENT("3a Sel for Payment"),
    UNDO_PAYMENT("3 Undo/Hold Payment"),
    RECEIVE_PAYMENT("3 Receive Payment"),
    IGNORE_FOR_PAYMENT("3 Ignore for Payment"), 
    CANCELLED("Cancelled");
    
	private int id;
	private String fullName;
	
	private EndurDocumentStatus(String name) throws ExceptionInInitializerError 
	{
		try 
		{
			id = Ref.getValue(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, name);
			
			if (id < 0) 
			{
				throw new Exception();
			}
			
			fullName = name;
		} 
		catch (Exception e) 
		{
			throw new ExceptionInInitializerError("Invalid name specified" + name);
		}
	}
	
	public int id() 
	{
		return id;
	}
	
	public String fullName() 
	{
		return fullName;
	}
	
	public static EndurDocumentStatus fromInt(int val) throws Exception 
	{
		for (EndurDocumentStatus e : EndurDocumentStatus.values()) 
		{
			if (e.id == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid id: " + val);
	}
	
	public static EndurDocumentStatus fromString(String val) throws Exception 
	{
		for (EndurDocumentStatus e : EndurDocumentStatus.values()) 
		{
			if (e.fullName == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid name: " + val); 
	}
}