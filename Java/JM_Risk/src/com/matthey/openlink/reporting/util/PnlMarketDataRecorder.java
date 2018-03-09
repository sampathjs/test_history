package com.matthey.openlink.reporting.util;

import java.util.Vector;

import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.trading.AbstractTradeProcessListener;
import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.EnumDebugLevel;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.market.EnumElementType;
import com.olf.openrisk.market.EnumGptCategory;
import com.olf.openrisk.market.EnumGptField;
import com.olf.openrisk.market.EnumIndexFieldId;
import com.olf.openrisk.market.GridPoint;
import com.olf.openrisk.market.GridPoints;
import com.olf.openrisk.market.Index;
import com.olf.openrisk.market.PriceLookup;
import com.olf.openrisk.staticdata.Address;
import com.olf.openrisk.staticdata.ConstField;
import com.olf.openrisk.staticdata.Country;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.ReferenceChoice;
import com.olf.openrisk.staticdata.StaticDataFactory;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumBuySell;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumToolset;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.EnumTranfField;
import com.olf.openrisk.trading.EnumTransactionFieldId;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Fields;
import com.olf.openrisk.trading.TradingFactory;
import com.olf.openrisk.trading.Transaction;

@ScriptCategory({ EnumScriptCategory.OpsSvcTrade })
public class PnlMarketDataRecorder extends AbstractTradeProcessListener
{
	private Session _session = null;
	
	/**
	 * OpService entry for handling qualifying Transactions
	 */
	@Override
	public void postProcess(Session session, DealInfo<EnumTranStatus> deals, boolean succeeded, Table clientData) 
	{		
		try {
			this._session = session;
			TradingFactory tf = session.getTradingFactory();
			PostProcessingInfo<EnumTranStatus>[] postprocessingitems = deals.getPostProcessingInfo();
			
			Vector<PnlMarketDataEntry> dataEntries = new Vector<PnlMarketDataEntry>();
			
			for (PostProcessingInfo<?> postprocessinginfo : postprocessingitems) 
			{
				int dealNum = postprocessinginfo.getDealTrackingId();
				int tranNum = postprocessinginfo.getTransactionId();
				Transaction trn = tf.retrieveTransactionById(tranNum);
				EnumToolset toolset = trn.getToolset();
				
				if (toolset == EnumToolset.Fx)
				{
					dataEntries.addAll(processFXDeal(trn));
				}
				
			}
		} 
		catch (Exception err) 
		{
			
		}
	}
	
	private static final String s_trnSettleDateField = "Settle Date";
	
	private static final String s_legCurrencyField = "Currency";
		
	
	Vector<PnlMarketDataEntry> processFXDeal(Transaction trn)
	{
		Vector<PnlMarketDataEntry> dataEntries = new Vector<PnlMarketDataEntry>();
		
		
		
		
		int ccyLegZero = trn.getLeg(0).getConstFields().getField(s_legCurrencyField).getValueAsInt();
		int ccyLegOne = trn.getLeg(1).getConstFields().getField(s_legCurrencyField).getValueAsInt();
		
		int settleDate = trn.getConstFields().getField(s_trnSettleDateField).getValueAsInt();
		
		PnlMarketDataEntry entryLegZero = new PnlMarketDataEntry();
		PnlMarketDataEntry entryLegOne = new PnlMarketDataEntry();
		
		entryLegZero.m_uniqueID = new PnlMarketDataUniqueID(trn.getDealTrackingId(), 0, 0, 0);
		entryLegZero.m_fixingDate = settleDate;
		entryLegZero.m_metalCcy = ccyLegZero;
		entryLegZero.m_indexID = PnLUtils.getIndexForCcy(_session, ccyLegZero);
				
		Index fxIndex = _session.getMarket().getIndex(_session.getStaticDataFactory().getName(EnumReferenceTable.Index, entryLegZero.m_indexID));	
		
		
		_session.getDebug().viewTable(fxIndex.getOutputTable());
		
		// entryLegZero.m_spotRate = 
		
		entryLegOne.m_uniqueID = new PnlMarketDataUniqueID(trn.getDealTrackingId(), 1, 0, 0);
		entryLegOne.m_fixingDate = settleDate;
		entryLegOne.m_metalCcy = ccyLegOne;
		entryLegZero.m_indexID = PnLUtils.getIndexForCcy(_session, ccyLegOne);
		
		return dataEntries;
	}	
}
