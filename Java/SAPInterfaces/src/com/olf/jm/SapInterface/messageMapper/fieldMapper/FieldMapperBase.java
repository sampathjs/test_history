package com.olf.jm.SapInterface.messageMapper.fieldMapper;

import com.olf.embedded.application.Context;
import com.olf.embedded.connex.ConnexFactory;
import com.olf.embedded.connex.EnumTransportType;
import com.olf.embedded.connex.TradeBuilder;
import com.olf.jm.SapInterface.messageMapper.MessageMapperException;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTradingObject;
import com.olf.openrisk.trading.EnumTransactionFieldId;


/**
 * The Class FieldMapperBase. Abstract base class for field mappers providing default functionality 
 * to deriver mapping classes.
 */
public abstract class FieldMapperBase implements IFieldMapper {
	
	/** The context the mapper is running in. */
	protected Context context;
	
	/** The trade builder template object used to retrieve field names. */
	private TradeBuilder tradeBuilderTemplate;
	
	/**
	 * Instantiates a new field mapper base.
	 *
	 * @param currentContext the current context
	 */
	protected FieldMapperBase(final Context currentContext) {
		
		context = currentContext;
		
		ConnexFactory cf = context.getConnexFactory();
	
		tradeBuilderTemplate = cf.createDefaultTradeBuilder(EnumTransportType.Request, true);
		
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.IFieldMapper#getConnexFieldName()
	 */
	@Override
	public final String getConnexFieldName() throws MessageMapperException {
		String columnName = "";
		if (isInfoField()) {
			columnName = getInfoFieldName();
		} else {
			switch (getTradingObject()) {
			case Transaction:
				columnName = tradeBuilderTemplate.getFieldName(
						getTradingObject(), getTransactionFieldId().getValue());
				break;
			case Leg:
				columnName = tradeBuilderTemplate.getFieldName(
						getTradingObject(), getLegFieldId().getValue());
				break;
			case ResetDefinition:
				columnName = tradeBuilderTemplate.getFieldName(
						getTradingObject(), getResetDefinitionFieldId().getValue());
				break;
			default:
				throw new MessageMapperException(
						"Unsupported transaction type "
								+ getTradingObject().toString());
			}
		}
		return columnName;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.IFieldMapper#getValue()
	 */
	@Override
	public abstract String getValue();

	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.IFieldMapper#getSide()
	 */
	@Override
	public final String getSide() {
		Integer side = new Integer(getLegId());
		return side.toString();
	}
	
	
	/* (non-Javadoc)
	 * @see com.olf.jm.coverage.messageMapper.fieldMapper.IFieldMapper#isApplicable()
	 */
	@Override
	public boolean isApplicable() {
		return true;
	}
	
	/**
	 * Gets the trading object.
	 *
	 * @return the trading object
	 */
	protected abstract EnumTradingObject getTradingObject();
	
	
	/**
	 * Gets the transaction field id. Only applicable if the field is 
	 * part of the transaction object.
	 *
	 * @return the transaction field id
	 */
	protected EnumTransactionFieldId getTransactionFieldId() {
		throw new RuntimeException("Not a transaction level field.");
	}
	
	/**
	 * Gets the leg field id. Only applicable if the field is part
	 * of the leg object.
	 *
	 * @return the leg field id
	 */
	protected EnumLegFieldId getLegFieldId() {
		throw new RuntimeException("Not a leg level field.");
	}
	
	
	/**
	 * Gets the reset definition field id. Only applicable if the field is
	 * part of the reset definition object.
	 *
	 * @return the reset definition field id
	 */
	protected EnumResetDefinitionFieldId getResetDefinitionFieldId() {
		throw new RuntimeException("Not a leg level field.");
	}
	
	/**
	 * Gets the leg id field is applicable to.
	 *
	 * @return the leg id
	 */
	protected int getLegId() {
		return 0;
	}
	
	/**
	 * Checks if is leg level.
	 *
	 * @return true, if is leg level
	 */
	protected boolean isLegLevel() {
		return getLegId() == 0 ? true : false;
	}
	
	/**
	 * Flag to indicate if the field is an tran info field.
	 *
	 * @return true, if is info field
	 */
	protected boolean isInfoField() {
		return false;
	}
	
	/**
	 * Gets the info field name.
	 *
	 * @return the info field name
	 */
	protected String getInfoFieldName() {
		throw new RuntimeException("Not a info field.");
	}
	
	

}
