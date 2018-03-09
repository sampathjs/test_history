package com.olf.jm.coverage.messageMapper;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.IFieldMapperFactory;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.tran.IntBUMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.tran.IntPortfolioMapper;
import com.olf.jm.coverage.businessObjects.ICoverageTrade;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.MetalCurrencyMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.IndexMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.MaturityDateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.NotionalMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.StartDateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.leg.VolumeUnitMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.resetDef.RefSourceMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.template.CoverageTemplateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.AutoSIShortlistTranMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.BuySellMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.CashFlowTypeMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.CurrencyPairMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.DealTrackingNumberMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.ExtBUMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.FXBaseCurrencyUnitMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.FXDateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.FXDealtAmountMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.FXSpotRateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.FormMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.IsCoverageMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.LocoMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.PriceUnitMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.SAPCounterpartyMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.SAPUserMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.SIPhysTranMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.SapOrderIdMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.TradeDateMapper;
import com.olf.jm.coverage.messageMapper.fieldMapper.tran.TradeStatusMapper;
import com.olf.openrisk.table.Table;
import com.openlink.util.logging.PluginLog;

/**
 * A factory for creating CoverageFieldMapper objects. Building the mappers needed to 
 * populate the required fields in the output message based on the instrument type.
 */
public class CoverageFieldMapperFactory implements IFieldMapperFactory {

	/** The context the script is running in. */
	private Context context;
	
	/** The sap party data loaded from the db. */
	private ISapPartyData sapPartyData;
	
	/** The sap template data loaded from the dp. */
	private ISapTemplateData sapTemplateData;
	
	/** The source date (input message). */
	private Table source;
	
	/** The existing trade if one exists. */
	private ICoverageTrade existingTrade;
	
	/**
	 * Instantiates a new coverage field mapper factory.
	 *
	 * @param currentContext the  context in which the script is running.
	 * @param currentSource the input message to be mapped.
	 * @param currentSapPartyData the  sap party data.
	 * @param currentSapTemplateData the  sap template data.
	 * @param trade the coverage trade if one exists in the db.
	 */
	public CoverageFieldMapperFactory(final Context currentContext, final Table currentSource, 
			final ISapPartyData currentSapPartyData, final ISapTemplateData currentSapTemplateData, final ICoverageTrade trade) {
		context = currentContext;
		
		sapPartyData = currentSapPartyData;
		
		sapTemplateData = currentSapTemplateData;
		
		source = currentSource;
		
		existingTrade = trade;
		
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.IFieldMapperFactory#buildTemplateMapper(java.lang.String)
	 */
	@Override
	public final ITemplateNameMapper buildTemplateMapper(final String instrumentType) {
		ITemplateNameMapper templateMapper = null;
		
		switch (instrumentType) {
		case "FX":
		case "METAL-SWAP":
			templateMapper = new CoverageTemplateMapper(source, sapTemplateData);
			break;

		default:
			String errorMessage = "Instrument type " + instrumentType + " is not supported.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		return templateMapper;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.IFieldMapperFactory#buildFieldMappers(java.lang.String)
	 */
	@Override
	public final List<IFieldMapper> buildFieldMappers(final String instrumentType) {

		List<IFieldMapper> fieldMappers = new ArrayList<IFieldMapper>();
		
		switch (instrumentType) {
		case "FX":
			setFxFieldMappers(fieldMappers);
			break;
			
		case "METAL-SWAP":
			setMtlSwapFieldMappers(fieldMappers);
			break;

		default:
			String errorMessage = "Instrument type " + instrumentType + " is not supported.";
			PluginLog.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}
		
		return fieldMappers;
	}
	
	/**
	 * Populate the input parameter with the field mappers needed to construct a 
	 * metal swap trade.
	 *
	 * @param fieldMappers list to populate with the field mappers.
	 */
	private void setMtlSwapFieldMappers(final List<IFieldMapper> fieldMappers) {
		fieldMappers.add(new SapOrderIdMapper(context, source));
		fieldMappers.add(new IsCoverageMapper(context, source));
		fieldMappers.add(new ExtBUMapper(context, source, sapPartyData));
		fieldMappers.add(new SIPhysTranMapper(context, source, sapPartyData));
		fieldMappers.add(new AutoSIShortlistTranMapper(context, source, sapPartyData));
		fieldMappers.add(new IntBUMapper(context, sapPartyData));
		fieldMappers.add(new IntPortfolioMapper(context, sapPartyData));
		fieldMappers.add(new TradeDateMapper(context, source));		
		fieldMappers.add(new BuySellMapper(context, source));
		
		fieldMappers.add(new TradeStatusMapper(context, existingTrade));

		fieldMappers.add(new SAPCounterpartyMapper(context, source));
		fieldMappers.add(new SAPUserMapper(context, source));
		
		// leg level fields
		fieldMappers.add(new StartDateMapper(context, source, 0));
		fieldMappers.add(new StartDateMapper(context, source, 1));
		
		fieldMappers.add(new MaturityDateMapper(context, source, 0));
		fieldMappers.add(new MaturityDateMapper(context, source, 1));		

		fieldMappers.add(new NotionalMapper(context, source, 0));
		fieldMappers.add(new NotionalMapper(context, source, 1));
		
		fieldMappers.add(new VolumeUnitMapper(context, source, 1));
		fieldMappers.add(new VolumeUnitMapper(context, source, 0));
		
		fieldMappers.add(new PriceUnitMapper(context, source, 1));
		fieldMappers.add(new PriceUnitMapper(context, source, 0));		
				
		fieldMappers.add(new IndexMapper(context, sapTemplateData, 1));
		
		fieldMappers.add(new RefSourceMapper(context, sapTemplateData, 1));
		
		fieldMappers.add(new MetalCurrencyMapper(context, source, 0));	
		
		fieldMappers.add(new DealTrackingNumberMapper(context, existingTrade));
		
		fieldMappers.add(new LocoMapper(context, source, sapPartyData));
		fieldMappers.add(new FormMapper(context, source, sapPartyData));
	}
	
	/**
	 * Populate the input parameter with the field mappers needed to construct a 
	 * fx trade.
	 *
	 * @param fieldMappers list to populate with the field mappers.
	 */
	private void setFxFieldMappers(final List<IFieldMapper> fieldMappers) {
		fieldMappers.add(new SapOrderIdMapper(context, source));
		fieldMappers.add(new ExtBUMapper(context, source, sapPartyData));	
		fieldMappers.add(new SIPhysTranMapper(context, source, sapPartyData));	
		fieldMappers.add(new AutoSIShortlistTranMapper(context, source, sapPartyData));
		fieldMappers.add(new CurrencyPairMapper(context, existingTrade, sapTemplateData));		
		fieldMappers.add(new IntBUMapper(context, sapPartyData));	
		fieldMappers.add(new IntPortfolioMapper(context, sapPartyData));	
		
		fieldMappers.add(new SAPCounterpartyMapper(context, source));
		fieldMappers.add(new SAPUserMapper(context, source));
		
		fieldMappers.add(new FXBaseCurrencyUnitMapper(context, source));		
		
		fieldMappers.add(new FXDateMapper(context, source));		
		fieldMappers.add(new FXDealtAmountMapper(context, source));		
		fieldMappers.add(new CashFlowTypeMapper(context, existingTrade, sapTemplateData));		
		fieldMappers.add(new FXSpotRateMapper(context, sapTemplateData, existingTrade));		
		fieldMappers.add(new TradeDateMapper(context, source));		
		fieldMappers.add(new BuySellMapper(context, source));
		fieldMappers.add(new TradeStatusMapper(context, existingTrade));
		fieldMappers.add(new IsCoverageMapper(context, source));
		
		fieldMappers.add(new DealTrackingNumberMapper(context, existingTrade));
		
		fieldMappers.add(new LocoMapper(context, source, sapPartyData));
		fieldMappers.add(new FormMapper(context, source, sapPartyData));
	}



}
