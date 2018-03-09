package com.olf.jm.sapTransfer.messageMapper;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.Context;
import com.olf.jm.SapInterface.businessObjects.ISapEndurTrade;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapPartyData;
import com.olf.jm.SapInterface.businessObjects.dataFactories.ISapTemplateData;
import com.olf.jm.SapInterface.messageMapper.IFieldMapperFactory;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.IFieldMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.template.ITemplateNameMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.tran.IntBUMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.tran.IntLEMapper;
import com.olf.jm.SapInterface.messageMapper.fieldMapper.tran.IntPortfolioMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.AccountBuninessUnitMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.AccountMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.ChargesInUsdMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.ChargesMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.EndDateMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.EnumToFrom;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.ForceVatMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.FormMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.InstrumentTypeMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.LocoMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.QtyMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.ReferenceMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.MetalTransferRequestNumberMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.SettleDateMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.StartDateMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.ToolSetMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.TradeStatusMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.TransactionDateMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.UnitMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.template.TransferTemplateMapper;
import com.olf.jm.sapTransfer.messageMapper.fieldMapper.tran.MetalMapper;
import com.olf.openrisk.table.Table;

/**
 * A factory for creating CoverageFieldMapper objects. Building the mappers needed to 
 * populate the required fields in the output message based on the instrument type.
 */
public class TransferFieldMapperFactory implements IFieldMapperFactory {

	/** The context the script is running in. */
	private Context context;
	
	/** The sap party data loaded from the db. */
	private ISapPartyData sapPartyData;
	
	/** The sap template data loaded from the db. */
	private ISapTemplateData sapTemplateData;
	
	/** The source date (input message). */
	private Table source;
	
	/** The existing trade if one exists. */
	private ISapEndurTrade existingTrade;
	
	/**
	 * Instantiates a new coverage field mapper factory.
	 *
	 * @param currentContext the  context in which the script is running.
	 * @param currentSource the input message to be mapped.
	 * @param currentSapPartyData the  sap party data.
	 * @param currentSapTemplateData the  sap template data.
	 * @param trade the coverage trade if one exists in the db.
	 */
	public TransferFieldMapperFactory(final Context currentContext, final Table currentSource, 
			final ISapPartyData currentSapPartyData, final ISapTemplateData currentSapTemplateData, final ISapEndurTrade trade) {
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

		templateMapper = new TransferTemplateMapper();

		return templateMapper;
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.IFieldMapperFactory#buildFieldMappers(java.lang.String)
	 */
	@Override
	public final List<IFieldMapper> buildFieldMappers(final String instrumentType) {

		List<IFieldMapper> fieldMappers = new ArrayList<IFieldMapper>();
		
		fieldMappers.add(new MetalTransferRequestNumberMapper(context, source));
		fieldMappers.add(new MetalMapper(context, source));
		fieldMappers.add(new QtyMapper(context, source));
		fieldMappers.add(new UnitMapper(context, source));
		
		fieldMappers.add(new TransactionDateMapper(context, source));
		fieldMappers.add(new StartDateMapper(context, source));
		fieldMappers.add(new EndDateMapper(context, source));
		
		fieldMappers.add(new SettleDateMapper(context, source));
		
		fieldMappers.add(new ChargesMapper(context));
		fieldMappers.add(new ChargesInUsdMapper(context));
		
		fieldMappers.add(new ForceVatMapper(context));
		
		fieldMappers.add(new ToolSetMapper(context));
		fieldMappers.add(new InstrumentTypeMapper(context));
		
		fieldMappers.add(new ReferenceMapper(context));
				
		fieldMappers.add(new TradeStatusMapper(context, source, sapPartyData));
		
		fieldMappers.add(new LocoMapper(context, sapPartyData, EnumToFrom.TO));
		fieldMappers.add(new LocoMapper(context, sapPartyData, EnumToFrom.FROM));
		
		fieldMappers.add(new FormMapper(context, sapPartyData, EnumToFrom.TO));
		fieldMappers.add(new FormMapper(context, sapPartyData, EnumToFrom.FROM));	
		
		fieldMappers.add(new AccountMapper(context, sapPartyData, EnumToFrom.TO));
		fieldMappers.add(new AccountMapper(context, sapPartyData, EnumToFrom.FROM));
		
		fieldMappers.add(new AccountBuninessUnitMapper(context, sapPartyData, EnumToFrom.TO));
		fieldMappers.add(new AccountBuninessUnitMapper(context, sapPartyData, EnumToFrom.FROM));
		
		fieldMappers.add(new IntBUMapper(context, sapPartyData));
		fieldMappers.add(new IntLEMapper(context, sapPartyData));
		fieldMappers.add(new IntPortfolioMapper(context, sapPartyData));		
		
		return fieldMappers;
	}
	



}
