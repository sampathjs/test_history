package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.messageAcceptor.MessageAcceptorException;
import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessor;
import com.olf.jm.fixGateway.messageProcessor.TradeBookMessageProcessor;
import com.olf.openjvs.ConnexUtility;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OC_ARGUMENT_TAGS_ENUM;
import com.olf.openjvs.enums.OC_REQUEST_STATUS_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


/*
      Name       : FIXCustomProcessFIXIncludeBBG.java

      Description: The script provides a framework for the user to process incoming FIX message. 

                  User should reference this script as part of INCLUDE script to invoke
                  any built-in functions listed here.

      Release    : Original 6/1/2007

      Due to the addition of the JPM Interface there is now more than one class implementing the
      IFIXCustomProcessFIXInclude interface within the FIXCustomPlugins package. 

      This class contains the source code for processing a Bloomberg Tradebook FIX message. 
      Renamed from FIXCustomProcessFIXInclude.java

      2020-04-20 	jwaechter	- Now implementing GuardedCustomFixProcessFIXInclude
                                - Added canProcess method()
      2020-10-15	jwaechter	- Migrated to JMLogging

      TODO: Ensure original file FIXCustomProcessFIXInclude.java is deleted on PROD.

 */

public class FIXCustomProcessFIXIncludeBBG implements GuardedCustomFixProcessFIXInclude {
	/**
	 * The constants repository variable name of the variable containing the expected
	 * value of the sender comp ID. The default value if no such value is defined 
	 * is defined in {@link #DEFAULT_EXPECTED_SENDER_COMP_ID}
	 */
	private static final String BBG_SENDER_COMP_ID = "BBGSenderCompId";

	/**
	 * The default value used in case the variable {@link #BBG_SENDER_COMP_ID} is not defined
	 * in the ConstantsRepository.
	 */
	private static final String DEFAULT_EXPECTED_SENDER_COMP_ID = "BLPTB";

	/**
	 * The column name within the fix data table containing the Sender Comp ID.
	 */
	private static final String SENDER_COMP_ID_COL_NAME = "SenderCompID";

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";

	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FIXGateway";


	protected FIXSTDHelperInclude m_FIXSTDHelperInclude;

	protected String requiredSenderCompId;

	public FIXCustomProcessFIXIncludeBBG() {
		m_FIXSTDHelperInclude = new FIXSTDHelperInclude();
	}

	@Override
	public boolean canProcess(Table argTbl) throws OException {
		try {
			init();
			if (argTbl.getNumRows() != 1) {
				Logging.warn("No rows found in argument table");
				return false;
			}
			Table executionReportTable = null;
			for (int col = argTbl.getNumCols(); col >= 1; col--) {
				String colName = argTbl.getColName(col);
				if (colName.toLowerCase().contains("executionreport")) {
					if (argTbl.getColType(col) == COL_TYPE_ENUM.COL_TABLE.toInt()) {
						executionReportTable = argTbl.getTable(col, 1);
						break;
					} else {
						Logging.warn("Found column '" + colName + "' within the argument table, but it is not of type Table");
					}
				}
			}
			if (executionReportTable == null) {
				Logging.warn("No suitable execution report table found. Skipping processing of message");
				return false;
			} 
			if (executionReportTable.getNumRows() != 1) {
				Logging.warn("Suitable execution report table found but it does not have exactly one row. Skipping processing of message");			
				return false;
			}
			if (executionReportTable.getColNum(SENDER_COMP_ID_COL_NAME) > 0) {
				if (executionReportTable.getColType(SENDER_COMP_ID_COL_NAME) == COL_TYPE_ENUM.COL_STRING.toInt()) {
					String senderCompId = executionReportTable.getString(SENDER_COMP_ID_COL_NAME, 1);
					if (senderCompId.equalsIgnoreCase(requiredSenderCompId)) {
						Logging.info(this.getClass().getSimpleName() + " is processing incoming request");
						return true;
					} else {
						Logging.info(this.getClass().getSimpleName()
								+ " is not processing incoming request due to mismatching sender comp id ('" 
								+ requiredSenderCompId + "' expected, found '" + senderCompId + "')");
					}
				} else {
					Logging.warn("Found execution report table and it does contain column '" + SENDER_COMP_ID_COL_NAME + "' but this column is not of type String");
				}
			} else {
				Logging.warn("Found execution report table but it does not contain column '" + SENDER_COMP_ID_COL_NAME + "'");
			}
			return false;
		} catch (Throwable t) {
			Logging.error("Exception while processing: ", t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally {
			Logging.close();
		}
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
	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		return TOOLSET_ENUM.COM_FUT_TOOLSET;	   
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
	public String ProcessFixInc_GetFixTableName(Table argTbl) throws OException
	{
		String tableName = null;

		return tableName;
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
	public TRAN_STATUS_ENUM ProcessFixInc_GetTranStatus(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{	   
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED;   
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
	public Table ProcessFixInc_GetFutIntrumentTranf(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		try {
			init();
			Table holders = m_FIXSTDHelperInclude.createHolderTables(); 

			/* TODO - Add custom logic here */ 
			// General Declaration
			/* Sample codes */
			String ticker, msgtype, templatereference = null;  
			/* END Sample codes */ 


			//Step 1 - Data gathering from incoming FIX Table
			/* Sample codes to retrieve value from icomingFixTable */ 
			//msgtype = incomingFixTable.getString( "MsgType", 1); 
			//ticker = incomingFixTable.getString( "Symbol", 1);  			
			/* END Sample codes to retrieve value from icomingFixTable */

			//Step 2 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table() to create TradeField table.
			Table tradeFieldTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table();

			//Step 3 - Call m_FIXSTDHelperInclude.HelperInc_Add_TradeField() to add data to TradeField table
			/* Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */
			//m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, ConnexUtility.getTradeBuilderFieldName( TRANF_FIELD.TRANF_TRAN_STATUS ), "0", "", "", "New");			//New order
			//m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, ConnexUtility.getTradeBuilderFieldName( TRANF_FIELD.TRANF_TICKER ), "0", "", "", ticker); 
			/* END Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */

			//Step 4 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table() to wrap TradeField table into TradeBuilder table 
			Table tradeBuilderTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFieldTbl, templatereference);			

			//Step 5 - wrap into holders and return 
			m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilderTbl);	 	   
			/* END TODO */

			return holders;      	   		   
		} catch (Throwable t) {
			Logging.error("Exception while processing: ", t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally {
			Logging.close();
		}
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
	public Table ProcessFixInc_GetTransactionTranf(Table argTbl, String message_name, Table incomingFixTable, TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring) throws OException   {
		String templatereference = null;  

		Table holders = null;
		try {
			init();
			MessageProcessor messageProcessor = new TradeBookMessageProcessor();
			holders = m_FIXSTDHelperInclude.createHolderTables(); 
			Logging.info("Checking if message is accepted.");
			if(!messageProcessor.acceptMessage(incomingFixTable)) {
				Logging.info("Skipping message, not valid for processing.");
				return holders;
			}
			Logging.info("Mmessage has been accepted.");

			Table tradeFields = messageProcessor.processMessage(incomingFixTable);

			Table tradeBuilder = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFields, templatereference);			


			m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilder);	

			if (constRep.getStringValue("logLevel","INFO").equalsIgnoreCase("DEBUG")) {
				Logging.debug("Trade builder: " + tradeBuilder.tableToXMLString() );	    	
			}  
		} catch (MessageAcceptorException | MessageMapperException e) {
			if(holders != null) {
				holders.destroy();
			}
			holders = null;
			String errorMessage = "Error building the tradebuilder message. " + e.getLocalizedMessage();
			Logging.error(errorMessage);
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			}
			Str.xstringAppend(xstring, errorMessage);
		} catch (Throwable t) {
			Logging.error("Exception while processing: ", t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally {
			Logging.close();
		}

		return holders;   	
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
	public Table ProcessFixInc_GetUserTable(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		Table userTbl = Util.NULL_TABLE;

		/* TODO - Add custom logic here */ 
		/* END TODO */

		return userTbl;    
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
	public int ProcessFixInc_PreAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, XString xstring) throws OException
	{
		int retVal = 1;

		/* TODO - Add custom logic here */ 
		/* END TODO */

		return retVal;              
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
	public int ProcessFixInc_PostAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, Table returnTable, XString xstring) throws OException
	{
		int retVal = 1;

		/* TODO - Add custom logic here */ 
		/* END TODO */

		return retVal;              
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
	public int ProcessFixInc_GetReconcilationRequest(Table argTbl, Table returnTable, XString xstring) throws OException 
	{
		try {
			init();
			int retVal = 1;

			String version;
			String senderid, sendersubid, senderlocationid;
			String targetid, targetsubid, targetlocationid;  

			String reference;
			String prevfixmessage;

			String analysis_arg_tag = ConnexUtility.getArgumentColName(OC_ARGUMENT_TAGS_ENUM.OC_ANALYSIS_ARGUMENTS_TAG.toInt());

			Table argumentTables = Util.NULL_TABLE;
			Table messageTbl, processTbl, dataTbl, targetTbl, documentTbl = Util.NULL_TABLE;
			XString err_xstring; 

			if (argTbl.getColNum( analysis_arg_tag) <= 0)
				throw new OException ("ProcessFixInc_GetReconcilationRequest - Invalid arugment table.");

			argumentTables = argTbl.getTable( analysis_arg_tag, 1);

			version = get_argument_from_argt(argumentTables, "version", null);
			senderid = get_argument_from_argt(argumentTables, "senderid", null);
			sendersubid = get_argument_from_argt(argumentTables, "sendersubid", null);
			senderlocationid = get_argument_from_argt(argumentTables, "senderlocationid", null);

			targetid = get_argument_from_argt(argumentTables, "targetid", null);
			targetsubid = get_argument_from_argt(argumentTables, "targetsubid", null);
			targetlocationid = get_argument_from_argt(argumentTables, "targetlocationid", null);

			prevfixmessage = get_argument_from_argt(argumentTables, "prevfixmessage", null);
			reference = "reconciliation";			//Must be reconciliation

			dataTbl = m_FIXSTDHelperInclude.HelperInc_Create_Data_Table(); 

			m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "7", "1", COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//BeginSeqNo
			m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "16", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//EndSeqNo	

			targetTbl = m_FIXSTDHelperInclude.HelperInc_Create_Target_Table(); 
			m_FIXSTDHelperInclude.HelperInc_Add_Target (targetTbl, version, targetid, targetsubid, targetlocationid);

			processTbl = m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table();
			m_FIXSTDHelperInclude.HelperInc_Add_Message (processTbl, "", "O", "2", "", reference, 									//ResendRequest
					senderid, sendersubid, senderlocationid, 
					documentTbl, targetTbl, dataTbl); 

			err_xstring = Str.xstringNew(); 
			messageTbl = m_FIXSTDHelperInclude.HelperInc_BuildMessage(processTbl, err_xstring);
			if (messageTbl == null)
			{
				Str.xstringAppend(xstring, "ProcessFixInc_GetReconcilationRequest: m_FIXSTDHelperInclude.HelperInc_BuildMessage fail to run " + Str.xstringGetString(err_xstring));
				Str.xstringDestroy(err_xstring);
				return OC_REQUEST_STATUS_ENUM.OC_STATUS_FAILURE_CANNOT_PROCESS_METHOD.toInt(); 
			} 			

			retVal = m_FIXSTDHelperInclude.HelperInc_CopyTablePtr(messageTbl, returnTable);	
			if (retVal != 1)
			{
				Str.xstringAppend(xstring, "ProcessFixInc_GetReconcilationRequest: m_FIXSTDHelperInclude.HelperInc_CopyTablePtr  fail to run " + Str.xstringGetString(err_xstring));
				Str.xstringDestroy(err_xstring); 
				return retVal;
			}
			Str.xstringDestroy(err_xstring);

			return retVal;   			
		} catch (Throwable t) {
			Logging.error("Exception while processing: ", t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				Logging.error(ste.toString());
			}
			throw t;
		}finally {
			Logging.close();
		}
	}
	public String get_argument_from_argt(Table argumentTables, String propName, String default_str) throws OException
	{
		try {
			init();
			Table argumentTable, tblValues;
			int intRowNum;
			String returnStr; 

			returnStr = default_str;

			if (argumentTables != null)
			{
				argumentTable = argumentTables.getTable( ConnexUtility.getArgumentColName(OC_ARGUMENT_TAGS_ENUM.OC_ANALYSIS_ARGUMENTS_ARGUMENT_TAG.toInt()), 1);
				if (argumentTable != null)
				{
					intRowNum = argumentTable.unsortedFindString( 
							ConnexUtility.getArgumentColName(OC_ARGUMENT_TAGS_ENUM.OC_ANALYSIS_ARGUMENTS_NAME_TAG.toInt()), 
							propName, 
							SEARCH_CASE_ENUM.CASE_SENSITIVE);
					if (intRowNum > 0)
					{
						tblValues = argumentTable.getTable( ConnexUtility.getArgumentColName(OC_ARGUMENT_TAGS_ENUM.OC_ANALYSIS_ARGUMENTS_VALUES_TAG.toInt()), intRowNum);
						if (tblValues != null)
							returnStr = tblValues.getString( ConnexUtility.getArgumentColName(OC_ARGUMENT_TAGS_ENUM.OC_ANALYSIS_ARGUMENTS_VALUES_VALUE_TAG.toInt()), 1);
					}
				}
			}			
			return returnStr;
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

	/**
	 * Initialise the class loggers.
	 *
	 * @throws Exception the exception
	 */
	private void init() {

		try {
			Logging.init(FIXCustomProcessFIXIncludeBBG.class, CONTEXT, SUBCONTEXT);
			constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
			requiredSenderCompId = DEFAULT_EXPECTED_SENDER_COMP_ID;
			requiredSenderCompId = constRep.getStringValue(BBG_SENDER_COMP_ID, requiredSenderCompId);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}

	}
}