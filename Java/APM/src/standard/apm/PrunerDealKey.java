/* Released with version 09-Jan-2014_V14_0_6 of APM */

package standard.apm;

import java.io.Serializable;

public class PrunerDealKey implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int m_dealNum;
	
	private int m_tranNum;
	
	private int m_version;
	
	private int m_serviceId;
	
	public PrunerDealKey( int dealNum, int tranNum, int version, int serviceId )
	{
		m_dealNum = dealNum;
		m_tranNum = tranNum;
		m_version = version;
		m_serviceId = serviceId;
	}			
	
	public int GetDealNum()
	{
		return ( m_dealNum );
	}
	
	public int GetTranNum()
	{
		return ( m_tranNum );				
	}
	
	public int GetVersion()
	{
		return ( m_version );
	}
	
	public int GetServiceId()
	{
		return ( m_serviceId );
	}			
}
