package com.matthey.openlink.pnl;


public class PNL_EntryDataUniqueID 
{
	public int m_dealNum;
	public int m_dealLeg;
	public int m_dealPdc;
	public int m_dealReset;
	
	public PNL_EntryDataUniqueID(int dealNum, int dealLeg, int dealPdc, int dealReset)
	{
		m_dealNum = dealNum;
		m_dealLeg = dealLeg;
		m_dealPdc = dealPdc;
		m_dealReset = dealReset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_dealLeg;
		result = prime * result + m_dealNum;
		result = prime * result + m_dealPdc;
		result = prime * result + m_dealReset;
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
		PNL_EntryDataUniqueID other = (PNL_EntryDataUniqueID) obj;
		if (m_dealLeg != other.m_dealLeg)
			return false;
		if (m_dealNum != other.m_dealNum)
			return false;
		if (m_dealPdc != other.m_dealPdc)
			return false;
		if (m_dealReset != other.m_dealReset)
			return false;
		return true;
	}
	
	
}
