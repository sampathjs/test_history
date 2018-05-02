package com.olf.recon.enums;

import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

public enum EndurCashflowType 
{
	VAT("VAT"), 
    MANUAL_VAT("Manual VAT"); 

	private int id;
	private String fullName;
	
	private EndurCashflowType(String name) throws ExceptionInInitializerError 
	{
		try 
		{
			id = Ref.getValue(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, name);
			
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
	
	public static EndurCashflowType fromInt(int val) throws Exception 
	{
		for (EndurCashflowType e : EndurCashflowType.values()) 
		{
			if (e.id == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid id: " + val);
	}
	
	public static EndurCashflowType fromString(String val) throws Exception 
	{
		for (EndurCashflowType e : EndurCashflowType.values()) 
		{
			if (e.fullName == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid name: " + val); 
	}
}