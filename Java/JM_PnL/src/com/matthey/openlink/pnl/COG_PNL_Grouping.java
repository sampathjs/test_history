package com.matthey.openlink.pnl;

public class COG_PNL_Grouping 
{
	int m_bunit;
	int m_metalCcyGroup;
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_bunit;
		result = prime * result + m_metalCcyGroup;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		COG_PNL_Grouping other = (COG_PNL_Grouping) obj;
		if (m_bunit != other.m_bunit)
			return false;
		if (m_metalCcyGroup != other.m_metalCcyGroup)
			return false;
		return true;
	}	
}
