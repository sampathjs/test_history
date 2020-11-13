package com.olf.cxplugins.adapter.fixgateway;

import com.olf.jm.fixGateway.messageMapper.MessageMapperException;
import com.olf.jm.fixGateway.messageProcessor.MessageProcessor;
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

public abstract class FIXCustomProcessFIXIncludeJPM implements GuardedCustomFixProcessFIXInclude{

	private static final String INSTRUMENT_COL_NAME = "Instrument";

	/**
	 * The constants repository variable name of the variable containing the expected
	 * start of the sender comp ID. The default value if no such value is defined 
	 * is defined in {@link #DEFAULT_EXPECTED_SENDER_COMP_ID}
	 */
	private static final String JPM_SENDER_COMP_ID = "JPMSenderCompId";

	/**
	 * The default value used in case the variable {@link #JPM_SENDER_COMP_ID} is not defined
	 * in the ConstantsRepository.
	 */
	private static final String DEFAULT_EXPECTED_SENDER_COMP_ID = "JPMFX";

	/**
	 * The column name within the execution report table containing the Sender Comp ID.
	 */
	private static final String SENDER_COMP_ID_COL_NAME = "SenderCompID";

	/**
	 * Contains the name of the column within the execution report table containing the SecuritySubType.
	 * TODO: Confirm the actual table contains the name and not just an anonymous value.
	 */
	protected static final String SECURITY_SUB_TYPE_COL_NAME = "SecuritySubType";

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "Connex";

	/** The Constant SUBCONTEXT used to identify entries in the const repository.. */
	public static final String SUBCONTEXT = "FIXGateway";

	protected FIXSTDHelperInclude m_FIXSTDHelperInclude = new FIXSTDHelperInclude();


	//  has to be lower case
	protected String requiredSenderCompId="jpm";

	public FIXCustomProcessFIXIncludeJPM() {
		super();
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
					if (senderCompId != null && senderCompId.trim().length() > 0 && senderCompId.toLowerCase().startsWith(requiredSenderCompId)) {
						// we are processing a deal incoming from JPM, but are we processing the right deal type?
						for (int col=executionReportTable.getNumCols(); col >= 1; col--) {
							Logging.debug("Column " + col + " in execution report table has name '" + executionReportTable.getColName(col) + "'");
						}
						if (executionReportTable.getColNum(INSTRUMENT_COL_NAME) > 0) {
							Table instrumentTable = executionReportTable.getTable(INSTRUMENT_COL_NAME, 1);
							for (int col=instrumentTable.getNumCols(); col >= 1; col--) {
								Logging.info("Column " + col + " in instrument table has name '" + instrumentTable.getColName(col) + "'" );
								for (int row=instrumentTable.getNumRows(); row >= 1; row--) {
									Logging.info("Column " + col + " in instrument table row " + row + " has value '" + instrumentTable.getString(col, row) + "'" );
								}
							}						
							if (instrumentTable.getColNum(SECURITY_SUB_TYPE_COL_NAME) > 0) {
								if (instrumentTable.getColType(SECURITY_SUB_TYPE_COL_NAME) == COL_TYPE_ENUM.COL_STRING.toInt()) {
									// col num = table row num containing the value in instrument table
									String secSubType = instrumentTable.getString(SECURITY_SUB_TYPE_COL_NAME, instrumentTable.getColNum(SECURITY_SUB_TYPE_COL_NAME));
									if (secSubType != null && secSubType.trim().length() > 0 && secSubType.toLowerCase().startsWith(getExpectedSecuritySubtype().toLowerCase())) { 	
										Logging.info(this.getClass().getSimpleName() + " is processing incoming request");
										return true;
									} else {
										Logging.info(this.getClass().getSimpleName() + " is NOT processing incoming request as the security sub type '" + secSubType 
												+ "' does not start with '" + getExpectedSecuritySubtype() + "'");
									}
								} else {
									Logging.warn("Found instrument table and it does contain column '" + SECURITY_SUB_TYPE_COL_NAME + "' but this column is not of type String");
								}
							} else {
								Logging.warn("Found instrument table but it does not contain column '" + SECURITY_SUB_TYPE_COL_NAME + "'");
							}
						} else {
							Logging.warn("Found execution report table but it does contain column '" + INSTRUMENT_COL_NAME + "'");						
						}
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

	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		return TOOLSET_ENUM.FX_TOOLSET;	   
	}

	public String ProcessFixInc_GetFixTableName(Table argTbl) throws OException {
		String tableName = null;
		return tableName;
	}

	public TRAN_STATUS_ENUM ProcessFixInc_GetTranStatus(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		return TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED;   
	}

	public Table ProcessFixInc_GetFutIntrumentTranf(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		try {
			init ();
			Logging.info("Start ProcessFixInc_GetFutIntrumentTranf");
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
			Logging.info("End ProcessFixInc_GetFutIntrumentTranf");
			return holders;      	   
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

	public Table ProcessFixInc_GetTransactionTranf(Table argTbl, String message_name,
			Table incomingFixTable, TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring)
					throws OException {
		try {
			init();
			Logging.info("Start ProcessFixInc_GetTransactionTranf");
			String templatereference = null;  

			MessageProcessor messageProcessor = getMessageProcessor();
			Table holders = null;
			try {

				holders = m_FIXSTDHelperInclude.createHolderTables(); 
				Logging.info("Checking if message has been accepted:");
				if(!messageProcessor.acceptMessage(incomingFixTable)) {
					Logging.info("Skipping message, not valid for processing.");
					throw new RuntimeException("This ends in FIXCustomProcessFIXIncludeJPM");
					//				return holders;
				}
				Logging.info("Message has been accepted");

				Logging.info("Now processing message");
				Table tradeFields = messageProcessor.processMessage(incomingFixTable);
				Logging.info("Finished processing message");

				Table tradeBuilder = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFields, templatereference);			

				m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilder);	

				if (constRep.getStringValue("logLevel", "INFO").equalsIgnoreCase("debug")) {
					Logging.debug("Trade builder: " + tradeBuilder.tableToXMLString() );

				}	  
			} catch (Exception e) {
				if(holders != null) {
					holders.destroy();
				}
				holders = null;
				String errorMessage = "Error building the tradebuilder message. " + e.getLocalizedMessage();
				Logging.error(errorMessage);
				Str.xstringAppend(xstring, errorMessage);
			}
			Logging.info("End ProcessFixInc_GetTransactionTranf");

			return holders;   				
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

	public Table ProcessFixInc_GetUserTable(Table argTbl, String message_name,
			Table incomingFixTable, XString xstring) throws OException {
		try {
			init();
			Logging.info("ProcessFixInc_GetUserTable");
			Table userTbl = Table.tableNew("User Table");
			userTbl.addCol("dummy", COL_TYPE_ENUM.COL_DATE_TIME);
			/* TODO - Add custom logic here */ 
			/* END TODO */

			return userTbl;    
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

	public int ProcessFixInc_PreAction(Table argTbl, String message_name,
			Table incomingFixTable, Transaction tran, XString xstring) throws OException {
		try {
			init();
			int retVal = 1;
			Logging.info("ProcessFixInc_PreAction");
			MessageProcessor messageProcessor = getMessageProcessor();
			try {
				messageProcessor.preProcess (incomingFixTable, tran);
			} catch (MessageMapperException e) {
				Logging.error ("Error while applying pre process mappings: " + e.toString());
				for (StackTraceElement ste : e.getStackTrace()) {
					Logging.error (ste.toString());						
				}
				retVal = 0;
			}
			return retVal;              
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

	public int ProcessFixInc_PostAction(Table argTbl, String message_name,
			Table incomingFixTable, Transaction tran, Table returnTable, XString xstring)
					throws OException {
		try {
			init();
			Logging.info("ProcessFixInc_PostAction");
			int retVal = 1;

			/* TODO - Add custom logic here */ 
			/* END TODO */

			return retVal;              
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

	public int ProcessFixInc_GetReconcilationRequest(Table argTbl,
			Table returnTable, XString xstring) throws OException {
		try {
			init();
			Logging.info("Start ProcessFixInc_GetReconcilationRequest");
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
			Logging.info("End ProcessFixInc_GetReconcilationRequest");
			return retVal;
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

	public String get_argument_from_argt(Table argumentTables, String propName, String default_str)
			throws OException {
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
	}

	/**
	 * Initialise the class loggers.
	 * @throws OException 
	 *
	 * @throws Exception the exception
	 */
	private void init(){	
		try {
			constRep = new ConstRepository(CONTEXT, SUBCONTEXT);
			requiredSenderCompId = DEFAULT_EXPECTED_SENDER_COMP_ID;
			requiredSenderCompId = constRep.getStringValue(JPM_SENDER_COMP_ID, requiredSenderCompId).toLowerCase();

			Logging.init(FIXCustomProcessFIXIncludeJPM.class, CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException("Error initialising logging. " + e.getMessage());
		}

	}

	public abstract String getExpectedSecuritySubtype();

	public abstract MessageProcessor getMessageProcessor();
}