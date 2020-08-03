package com.olf.jm.coverage;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.connex.AbstractGearAssembly;
import com.olf.embedded.connex.EnumRequestStatus;
import com.olf.embedded.connex.Request;
import com.olf.embedded.connex.RequestData;
import com.olf.embedded.connex.RequestException;
import com.olf.embedded.connex.RequestOutput;
import com.olf.jm.SapInterface.messageProcessor.IMessageProcessor;
import com.olf.jm.coverage.messageProcessor.CoverageProcessor;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;



/**
 * The Class CoverageRequest.
 * 
 * Defines the pre / post gear methods used on the coverage interface. 
 * 
 * Validates the message structure / content before converting it into the standard tradebuilder format.
 * 
 */
@ScriptCategory({ EnumScriptCategory.Connex })
public class CoverageRequest extends AbstractGearAssembly {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;
	
	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";
	
	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "Coverage";
	
	
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() throws Exception {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

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
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.olf.embedded.connex.AbstractGearAssembly#postProcess(com.olf.embedded
	 * .application.Context, com.olf.embedded.connex.Request,
	 * com.olf.embedded.connex.RequestData,
	 * com.olf.embedded.connex.RequestOutput)
	 */
	@Override
	public final void postProcess(final Context context, final Request request,
			final RequestData requestData, final RequestOutput requestOutput) {

		try {
			init();
			IMessageProcessor processor = new CoverageProcessor(context, constRep);
			processor.processResponseMessage(request, requestOutput);
		} catch (Exception e) {

			Logging.error("Error processing request. " + e.getMessage());
		}finally{
			Logging.close();
		}
	}

	/* (non-Javadoc)
	 * @see com.olf.embedded.connex.AbstractGearAssembly#preProcess(com.olf.embedded.application.Context, 
	 * com.olf.embedded.connex.Request, com.olf.embedded.connex.RequestData)
	 */
	@Override
	public final void preProcess(final Context context, final Request request,
			final RequestData requestData) {

		try {					
			init();
			
			IMessageProcessor processor = new CoverageProcessor(context, constRep);
			processor.processRequestMessage(request, requestData);
		} catch (Exception e) {
			Logging.error("Error processing request. " + e.getMessage());
			
			throw new RequestException(EnumRequestStatus.FailureInvalidArg, e.getMessage());
		}finally{
			Logging.close();
		}
		
	}
}
