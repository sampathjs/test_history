package com.matthey.openlink.pnl;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class Interest_PNL_Aggregator 
{
	Table m_data = null;
	
	public void initialise() throws OException
	{
	}
	
	public void addDealsToProcess(Table data) throws OException
	{
		if (m_data == null)
		{
			m_data = data.cloneTable();
		}		
		
		data.copyRowAddAll(m_data);
	}
	
	public Table getData() throws OException
	{
		return (m_data != null) ? m_data : new Table("");
	}
}
