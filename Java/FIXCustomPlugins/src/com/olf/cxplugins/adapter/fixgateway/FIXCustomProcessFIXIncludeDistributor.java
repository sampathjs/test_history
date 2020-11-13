package com.olf.cxplugins.adapter.fixgateway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-04-16	V1.0	jwaechter	- Initial Version
 */


/**
 * Class used to distribute requests to process incoming FIX messages to the
 * matching FIX message processing class usually depending on the sender.
 * @author jwaechter
 * @version 1.0
 */
public class FIXCustomProcessFIXIncludeDistributor implements
IFIXCustomProcessFIXInclude {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";

	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FIXGateway";

	/**
	 * The variable used containing the list of processors.
	 */
	public static final String PROCESSORS = "Processors";

	private List<String> processorNames;

	private List<GuardedCustomFixProcessFIXInclude> processors;

	public FIXCustomProcessFIXIncludeDistributor () {
		init();
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetFixTableName
	   Description:      This function will be used to get the name of the column if the default column (first column) is not 
	                     desirable one. (it usually occurred when you have multiple columns of argt passed in)

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 

	   Return Values:    If fail, null will be returned
	                     If success, column name will be returned.

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/
	@Override
	public String ProcessFixInc_GetFixTableName(Table argTbl) throws OException
	{
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetFixTableName(argTbl);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetFixTableName");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetFutIntrumentTranf
	   Description:      This function should perform data gather from incoming FIX table,
	                     create tradefield table and add values to it.  Return tradefield table 
	                     of future instrument in tranf structure.

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    If fail, tradeFieldTbls with empty row will be returned and xstring should be populated with error 
	                     If success, return Table containing tranf table(s) filled with trade values  

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/
	@Override
	public Table ProcessFixInc_GetFutIntrumentTranf(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetFutIntrumentTranf(argTbl, message_name, incomingFixTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetFutIntrumentTranf");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetReconcilationRequest
	   Description:      This function will get the reconcilation request.   

	   Parameters:       argTbl (Table) - argument table contains 
	                     returnTable - table required passing back to oc engine
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    if success - 1 should be returned
	                     if fail, error code should be returned and xstring should be populated with error 

	   Release    : Original 6/1/2012
	-------------------------------------------------------------------------------*/
	@Override
	public int ProcessFixInc_GetReconcilationRequest(Table argTbl, Table returnTable, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetReconcilationRequest(argTbl, returnTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetReconcilationRequest");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetToolset
	   Description:      This function should return toolset

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    If fail, 0<= will be returned and xstring should be populated with error 
	                     If success, return toolset id (int)

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/
	@Override
	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetToolset(argTbl, message_name, incomingFixTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetToolset");
	}

	/*-------------------------------------------------------------------------------
       Name:             ProcessFixInc_GetTranStatus
       Description:      This function should return Transaction Status

       Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
                         message_name - incoming fix message name with version number
                         incomingFixTable - table contains incoming FIX message data
                         xstring (XString ) - Should populated with error when fail  

       Return Values:    If fail, null will be returned and xstring should be populated with error 
                         If success, return transaction status (TRAN_STATUS_ENUM)  

       Release    : Original 6/1/2007
       -------------------------------------------------------------------------------*/
	@Override
	public TRAN_STATUS_ENUM ProcessFixInc_GetTranStatus(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{	   
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetTranStatus(argTbl, message_name, incomingFixTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetTranStatus");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetTransactionTranf
	   Description:      This function should perform data gather from incoming FIX table,
	                     create tradefield table and add values to it.  Return tradefield table 
	                     of transaction in tranf structure.

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     TOOLSET_ENUM toolset - toolset
	                     TRAN_STATUS_ENUM tranStatus - transaction status
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    If fail, tradeFieldTbls with empty row will be returned and xstring should be populated with error 
	                     If success, return Table containing tranf table(s) filled with trade values  

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/

	@Override
	public Table ProcessFixInc_GetTransactionTranf(Table argTbl, String message_name, Table incomingFixTable, TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetTransactionTranf(argTbl, message_name, incomingFixTable, toolset, tranStatus, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetTransactionTranf");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetUserTable
	   Description:      This function should return user table for associated incoming FIX message. 

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    If fail, Util.NULL_TABLE will be returned and xstring should be populated with error 
	                     If success, return user table

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/
	@Override
	public Table ProcessFixInc_GetUserTable(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_GetUserTable(argTbl, message_name, incomingFixTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_GetUserTable");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_PostAction
	   Description:      This function will perform any post processing action.  Post processing such as replying 
	                     FIX message to sender can be performed in here.  

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     tran - transaction returned from processTran 
	                     returnTable - table required passing back to oc engine
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    if success - 1 should be returned
	                     if fail, error code should be returned and xstring should be populated with error 

	   Release    : Original 6/1/2007
	   -------------------------------------------------------------------------------*/
	public int ProcessFixInc_PostAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, Table returnTable, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_PostAction(argTbl, message_name, incomingFixTable, tran, returnTable, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_PostAction");
	}

	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_PreAction
	   Description:      This function will perform any pre-processing action.

	   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     tran - transaction returned from processTran 
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    if success - 1 should be returned
	                     if fail, error code should be returned and xstring should be populated with error 

	   Release    : Original 5/23/2016
	   -------------------------------------------------------------------------------*/
	@Override
	public int ProcessFixInc_PreAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, XString xstring) throws OException {
		for (GuardedCustomFixProcessFIXInclude processor : processors) {
			if (processor.canProcess(argTbl)) {
				return processor.ProcessFixInc_PreAction(argTbl, message_name, incomingFixTable, tran, xstring);
			}
		}
		// nothing applicable
		throw new RuntimeException ("Could not find suitable processor among the list of defined processors '" + processorNames.toString() 
		+ "' for incoming FIX Message at method ProcessFixInc_PreAction");
	}

	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() {
		try {
			constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
			try {
				Logging.init(FIXCustomProcessFIXIncludeDistributor.class, CONTEXT, SUBCONTEXT);
			} catch (Exception e) {
				throw new RuntimeException("Error initialising logging. " + e.getMessage());
			}
			try {
				processorNames = Arrays.asList(constRep.getStringValue(PROCESSORS, 
						"com.olf.cxplugins.adapter.fixgateway.FIXCustomProcessFIXIncludeBBG" 
								+ ",com.olf.cxplugins.adapter.fixgateway.FIXCustomProcessFIXIncludeJPMPrecMetalFwd"
								+ ",com.olf.cxplugins.adapter.fixgateway.FIXCustomProcessFIXIncludeJPMPrecMetalSpot"
								+ ",com.olf.cxplugins.adapter.fixgateway.FIXCustomProcessFIXIncludeJPMPrecMetalSwap"
								+ ",com.olf.cxplugins.adapter.fixgateway.FIXCustomProcessFIXIncludeJPMBaseMetalSwap"
						)
						.split(","));
			} catch (Exception ex) {
				Logging.error("Exception while retrieving list of FIX message processors:\n " + ex.toString());
				for (StackTraceElement ste : ex.getStackTrace()) {
					Logging.error(ste.toString());
				}
				throw new RuntimeException ("Error retrieving list of FIX Processors");
			}
			processors = new ArrayList<>(processorNames.size());
			for (String processorName : processorNames) {				
				try { 
					Class c = Class.forName(processorName);
					Object processor = c.newInstance(); 
					if (!(processor instanceof GuardedCustomFixProcessFIXInclude)) {
						throw new RuntimeException ("FIX Message Processor '" + processorName + "' is not a an instance of GuardedCustomFixProcessFIXInclude");
					}
					processors.add((GuardedCustomFixProcessFIXInclude)processor);
				} catch (InstantiationException ex) {
					String errorMessage = "Error while instantiating an instance of FIX message processor '" + processorName + "':\n" + ex.toString();
					Logging.error(errorMessage);
					for (StackTraceElement ste : ex.getStackTrace()) {
						Logging.error(ste.toString());
					}
					throw new RuntimeException (errorMessage);
				} catch (ClassNotFoundException ex) {
					String errorMessage = "Error could not find class of processor '" + processorName + "':\n" + ex.toString();
					Logging.error(errorMessage);
					for (StackTraceElement ste : ex.getStackTrace()) {
						Logging.error(ste.toString());
					}
					throw new RuntimeException (errorMessage);
				} catch (IllegalAccessException ex) {
					String errorMessage = "Error could not access class of processor '" + processorName + "':\n" + ex.toString();
					Logging.error(errorMessage);
					for (StackTraceElement ste : ex.getStackTrace()) {
						Logging.error(ste.toString());
					}
					throw new RuntimeException (errorMessage);
				}
			}			
		} catch (OException ex) {
			Logging.error("Error while initializing class '" + getClass().getSimpleName() + "':\n" + ex.toString());
			for (StackTraceElement ste : ex.getStackTrace()) {
				Logging.error(ste.toString());
			}
		} catch (Throwable t) {
			Logging.error("Exception while processing: ", t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		} finally {
			Logging.close();
		}
	}

}
