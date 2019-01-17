package com.matthey.openlink.pnl;
import com.matthey.openlink.jde_extract.IJdeDataManager;
import com.matthey.openlink.jde_extract.JDE_Data_Manager;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;


@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class PNL_MarketDataRecorder extends PNL_MarketDataRecorderBase {

	@Override
	public IJdeDataManager getDataManager() {
		return new JDE_Data_Manager();
	}

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandler();
	}

	@Override
	public String getInterestCurveName() {
		return  "LIBOR.USD";
	}

	@Override
	public void calculateFXDate(PNL_MarketDataEntry dataEntry, int liborIndex,
				int fxFixingDate) throws OException {
		if(dataEntry.m_indexID > 0)
		{					  
			dataEntry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(dataEntry.m_indexID);
			dataEntry.m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntry.m_indexID, fxFixingDate);
			dataEntry.m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fxFixingDate);   
		   
			// If this leg's currency is quoted as "Ccy per USD", convert to "USD per Ccy"
			if (!MTL_Position_Utilities.getConvention(dataEntry.m_metalCcy))
			{
				if (dataEntry.m_spotRate > 0)
					dataEntry.m_spotRate = 1 / dataEntry.m_spotRate;
				   
				if (dataEntry.m_forwardRate > 0)
					dataEntry.m_forwardRate = 1 / dataEntry.m_forwardRate;            		
			}
		}
		
	}

	

}
