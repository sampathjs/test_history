/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import java.io.Serializable;

public class PrunerEntityKey implements Serializable
{
	private static final long serialVersionUID = 1L;

	private int m_primaryEntityNum;
	
	private int m_secondaryEntityNum;
	
	private int m_entityVersion;
	
	private int m_serviceId;
	
	public PrunerEntityKey( int primaryEntityNum, int secondaryEntityNum, int entityVersion, int serviceId )
	{
		m_primaryEntityNum = primaryEntityNum;
		m_secondaryEntityNum = secondaryEntityNum;
		m_entityVersion = entityVersion;
		m_serviceId = serviceId;
	}			
	
	public int GetPrimaryEntityNum()
	{
		return ( m_primaryEntityNum );
	}
	
	public int GetSecondaryEntityNum()
	{
		return ( m_secondaryEntityNum );				
	}
	
	public int GetEntityVersion()
	{
		return ( m_entityVersion );
	}
	
	public int GetServiceId()
	{
		return ( m_serviceId );
	}			
}
