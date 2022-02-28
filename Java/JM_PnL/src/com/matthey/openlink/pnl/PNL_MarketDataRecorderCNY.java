package com.matthey.openlink.pnl;
import com.matthey.openlink.generic.ConstRepoRetrieval;
import com.matthey.openlink.jde_extract.IJdeDataManager;
import com.matthey.openlink.jde_extract.JDE_Data_ManagerCNY;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Ref;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-02-18   V1.1    agrawa01 - memory leaks & formatting changes
 */

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_OPS_SERVICE)
public class PNL_MarketDataRecorderCNY extends PNLMarketDataRecorderBase {

	@Override
	public IJdeDataManager getDataManager() {
		return new JDE_Data_ManagerCNY();
	}

	@Override
	public IPnlUserTableHandler getUserTableHandler() {
		return new PNL_UserTableHandlerCNY();
	}

	@Override
	public String getInterestCurveName() {
		return  ConstRepoRetrieval.getShiborIndexName();
	}

	@Override
	public void calculateFXDate(PNL_MarketDataEntry dataEntry, int liborIndex, int fxFixingDate) throws OException {
		int currencyIndex = Ref.getValue(SHM_USR_TABLES_ENUM.INDEX_TABLE, "FX_USD.CNY");
		if (dataEntry.m_indexID > 0) {					  
			dataEntry.m_spotRate = MTL_Position_Utilities.getSpotGptRate(dataEntry.m_indexID);
			dataEntry.m_forwardRate = MTL_Position_Utilities.getRateForDate(dataEntry.m_indexID, fxFixingDate);
			dataEntry.m_usdDF = MTL_Position_Utilities.getRateForDate(liborIndex, fxFixingDate);   
		   
			// If this leg's currency is quoted as "Ccy per USD", convert to "USD per Ccy"
			if (!MTL_Position_Utilities.getConvention(dataEntry.m_metalCcy)) {
				if (dataEntry.m_spotRate > 0)
					dataEntry.m_spotRate = 1 / dataEntry.m_spotRate;
				   
				if (dataEntry.m_forwardRate > 0)
					dataEntry.m_forwardRate = 1 / dataEntry.m_forwardRate;       
			}
			double spotRate = MTL_Position_Utilities.getSpotGptRate(currencyIndex);
			dataEntry.m_spotRate = dataEntry.m_spotRate  * spotRate;
				
			double forwardRate = MTL_Position_Utilities.getRateForDate(currencyIndex, fxFixingDate);
			dataEntry.m_forwardRate = dataEntry.m_forwardRate  * forwardRate;

		} else {
			// assume this is the USD leg
			double spotRate = MTL_Position_Utilities.getSpotGptRate(currencyIndex);
			dataEntry.m_spotRate = dataEntry.m_spotRate  * spotRate;
			
			double forwardRate = MTL_Position_Utilities.getRateForDate(currencyIndex, fxFixingDate);
			dataEntry.m_forwardRate = dataEntry.m_forwardRate  * forwardRate;    					
		}
		
		if (getCnInterestFlag() == 0) { 
			// No interest in calculation
			dataEntry.m_forwardRate = dataEntry.m_spotRate;
			dataEntry.m_usdDF = 1;
		}
	}
	
	private int getCnInterestFlag() throws OException {
		ConstRepository constRep;
		int value;
		
		try {
			constRep = new ConstRepository("MiddleOffice", "PnL");
			value = constRep.getIntValue("CNInterest");
		} catch (ConstantTypeException | ConstantNameException | OException e) {
			String errorMessage = "Error loading the CN Interest flag from the db. " + e.getMessage();
			Logging.error(errorMessage);
			throw new OException(errorMessage);
		}
		return value;
	}

}
