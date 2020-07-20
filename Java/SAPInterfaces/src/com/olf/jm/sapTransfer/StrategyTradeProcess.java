package com.olf.jm.sapTransfer;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.connex.AbstractGearAssembly;
import com.olf.embedded.connex.EnumRequestData;
import com.olf.embedded.connex.EnumRequestStatus;
import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.RequestData;
import com.olf.embedded.connex.RequestException;
import com.olf.embedded.connex.TradeBuilder;
import com.olf.jm.logging.Logging;
import com.olf.jm.sapTransfer.strategy.StrategyBooker;
import com.openlink.util.constrepository.ConstRepository;


/**
 * The Class StrategyTradeProcess. Main gear used in the booking of strategy deals.
 */
@ScriptCategory({ EnumScriptCategory.Connex })
public class StrategyTradeProcess extends AbstractGearAssembly {

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "StrategyTradeProcess";
	
	
	/* (non-Javadoc)
	 * @see com.olf.embedded.connex.AbstractGearAssembly#
	 * process(com.olf.embedded.application.Context, com.olf.embedded.connex.Request, com.olf.embedded.connex.RequestData)
	 */
	@Override
	public final void process(final Context context, final Request request,
			final RequestData requestData) {
		
		try {
			init();
		} catch (Exception e) {
			throw new RequestException(EnumRequestStatus.FailureInvalidArg, e.getMessage());
		}
		
		TradeBuilder tradeBuilder = (TradeBuilder) requestData;
		
		
		try (StrategyBooker booker = new StrategyBooker(context, tradeBuilder)) {
			booker.validate();
			
			booker.book();
			
			request.setResultTable(booker.getResponseMessage(), EnumRequestData.TradeBuilder);
			
		} catch (Exception e) {
			throw new RequestException(EnumRequestStatus.FailureInvalidArg, e.getMessage());
		}finally{
			Logging.close();
		}
		
	}
	

	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		ConstRepository constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Error";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {
			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			
		} catch (Exception e) {
			throw new Exception("Error initialising logging. " + e.getMessage());
		}

	}	
}
