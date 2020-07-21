package com.jm.eod.comstor;

import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openjvs.SystemUtil;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.EnumQueryType;
import com.olf.openrisk.io.Queries;
import com.olf.openrisk.io.Query;
import com.olf.openrisk.io.QueryResult;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumLegFieldId;
import com.olf.openrisk.trading.EnumResetDefinitionFieldId;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.embedded.application.ScriptCategory;

import java.util.ArrayList;
import java.util.List;

import com.olf.embedded.application.EnumScriptCategory;


/*
 * History:
 * 2020-06-14 	V1.0 	jwaechter	- Initial Version
 */

/**
 * This plugin is supposed to be the main plugin of a task.
 * The task is executed the saved query defined by Constants Repository 
 * "EOD\CommStorDateFix\QueryName" (defaulted to {@value #SAVED_QUERY_NAME_DEFAULT})
 * that is assumed to contain a list of COMM-STOR deals.
 * 
 * The maturity date of the COMM-STOR deals on the deal leg is being picked up and saved
 * to the 'Reset Shift' and 'RFI Shift' fields on the legs other than the deal leg.  
 * 
 * @author jwaechter
 * @version 1.0
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class ComStorDateFix extends AbstractGenericScript {
	private final static String SAVED_QUERY_NAME_CR = "QueryName";
	private final static String SAVED_QUERY_NAME_DEFAULT = "comm-stor-resetdates";
	private final static String CONTEXT = "EOD";
	private final static String SUBCONTEXT = "CommStorDateFix";
	private ConstRepository constRepo; 
	private String savedQueryName;
	
    public Table execute(final Session session, final ConstTable table) {
        try {
        	initLogging ();
        	List<Integer> tranNumsToProces = loadTranNums(session);
        	for (int tranNum : tranNumsToProces) {
        		processComStor (session, tranNum);
        	}
        } finally {
        	Logging.close();
        }
    	return null;
    }

	private void processComStor(Session session, int tranNum) {
		Logging.info("Processing ComStor tran # " + tranNum);
		try (Transaction tran = session.getTradingFactory().retrieveTransactionById(tranNum);
			 Leg dealLeg = tran.getLeg(0)) {
 			 String matDate = dealLeg.getDisplayString(EnumLegFieldId.MaturityDate);
			 Logging.info("Retrieved maturity date '" + matDate + "' from deal leg" );
 			 for (int legNo=tran.getLegCount()-1; legNo >= 1; legNo--) {
 				try (Leg locLeg = tran.getLeg(legNo)) {
 					 Logging.info("Processing leg '" + legNo + "'" );
 					 locLeg.getResetDefinition().setValue(EnumResetDefinitionFieldId.Shift, matDate);
 					 locLeg.getResetDefinition().setValue(EnumResetDefinitionFieldId.RefIndexShift, matDate);
 					 Logging.info("Processing leg '" + legNo + "': Shift and Reference Index Shift are set to maturity date" );
 				}
 			 }
			 Logging.info("Reprocessing transaction #" + tranNum + " to " + tran.getTransactionStatus() );
 			 tran.process(tran.getTransactionStatus());
		}		
		Logging.info("Finished processing ComStor tran # " + tranNum);		
	}

	public List<Integer> loadTranNums(final Session session) {
		List<Integer> tranNumsToProces = new ArrayList<>();
		Logging.info("Retrieving tran nums from saved query '" + savedQueryName + "'");
		if (!session.getIOFactory().queryExists(savedQueryName)) {
			Logging.error("Could not find saved query '" + savedQueryName + "'");
			return null;
		}
		
		try (Query query = session.getTradingFactory().getQuery(savedQueryName);
			 QueryResult qr = query.execute()) {
			 for (int tranNum : qr.getObjectIdsAsInt()) {
				tranNumsToProces.add(tranNum);
			 }
		}
		Logging.info("Processing the dates on the following tran nums: " +tranNumsToProces.toString());
		return tranNumsToProces;
	}
    
	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception on initialisation errors or the logger or constant repository.
	 */
	protected void initLogging() {
		try {
			this.constRepo = new ConstRepository(CONTEXT, SUBCONTEXT);			
			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
			savedQueryName = constRepo.getStringValue(SAVED_QUERY_NAME_CR, SAVED_QUERY_NAME_DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging: " + e.getMessage());
		}
	}
}
