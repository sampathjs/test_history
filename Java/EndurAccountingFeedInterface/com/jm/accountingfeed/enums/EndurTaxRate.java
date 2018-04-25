package com.jm.accountingfeed.enums;

import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;

/**
 * Custom tax rate types in Endur
 * 
 * See tax_rate table
 */
public enum EndurTaxRate 
{
	REVERSE_CHARGE_GOLD_POSITIVE("Reverse Charge Gold +"), 
    REVERSE_CHARGE_GOLD_NEGATIVE("Reverse Charge Gold -"); 
    
	private int id;
	private String fullName;
	
	private EndurTaxRate(String name) throws ExceptionInInitializerError 
	{
		try 
		{
			id = Ref.getValue(SHM_USR_TABLES_ENUM.TAX_RATE_TABLE, name);
			
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
	
	public static EndurTaxRate fromInt(int val) throws Exception 
	{
		for (EndurTaxRate e : EndurTaxRate.values()) 
		{
			if (e.id == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid id: " + val);
	}
	
	public static EndurTaxRate fromString(String val) throws Exception 
	{
		for (EndurTaxRate e : EndurTaxRate.values()) 
		{
			if (e.fullName == val) 
			{
				return e;
			}
		}
		
		throw new Exception("Invalid name: " + val); 
	}
}