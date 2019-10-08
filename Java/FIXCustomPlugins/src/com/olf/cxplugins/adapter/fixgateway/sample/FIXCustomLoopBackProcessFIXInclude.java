package com.olf.cxplugins.adapter.fixgateway.sample;
import com.olf.cxplugins.adapter.fixgateway.FIXSTDHelperInclude;
import com.olf.cxplugins.adapter.fixgateway.IFIXCustomProcessFIXInclude;
import com.olf.openjvs.ConnexUtility;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Transaction;
import com.olf.openjvs.Util;
import com.olf.openjvs.XString;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.DATE_LOCALE;
import com.olf.openjvs.enums.OC_ARGUMENT_TAGS_ENUM;
import com.olf.openjvs.enums.OC_REQUEST_STATUS_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.TOOLSET_ENUM;
import com.olf.openjvs.enums.TRANF_FIELD;
import com.olf.openjvs.enums.TRAN_STATUS_ENUM;

/*
        Name       : FIXCustomProcessFIXInclude.java

        Description: The script provides a framework for the user to process incoming FIX message. 

        User should reference this script as part of INCLUDE script to invoke
        any built-in functions listed here.

        Release    : Original 6/1/2007
 */
public class FIXCustomLoopBackProcessFIXInclude implements IFIXCustomProcessFIXInclude
{
	private FIXSTDHelperInclude m_FIXSTDHelperInclude;

	public FIXCustomLoopBackProcessFIXInclude(){
		m_FIXSTDHelperInclude = new FIXSTDHelperInclude();
	}
	
	public String  FIX_FIELD_TEXT = "Text";

	public String  FIX_FIELD_VERSION = "BeginString";
	public String  FIX_FIELD_MSGTYPE = "MsgType";
	public String  FIX_FIELD_ORDSTATUS = "OrdStatus";

	public String  FIX_FIELD_EXECID = "ExecID";  
	public String  FIX_FIELD_CLORDERID = "ClOrdID";
	public String  FIX_FIELD_ORDERID = "OrderID"; 

	public String  FIX_FIELD_BUYSELL = "Side";
	public String  FIX_FIELD_SYMBOL = "Symbol"; 
	public String  FIX_FIELD_PRODUCT= "Product";
	public String  FIX_FIELD_IDSOURCE= "IDSource";
	public String  FIX_FIELD_PRICE = "Price";	
	public String  FIX_FIELD_ORDERQTY = "OrderQty";
	public String  FIX_FIELD_AVGPX = "AvgPx";	

	public String  FIX_FIELD_CCY = "Currency";
	public String  FIX_FIELD_SETTLECCY = "SettlCurrency";
	public String  FIX_FIELD_TRADEDATE = "TradeDate";
	public String  FIX_FIELD_SETTLEDATE = "SettlDate";
	public String  FIX_FIELD_TRADETIME = "TransactTime";  
	
	public String  FIX_FIELD_INTERNAL = "clearing_firm";
	public String  FIX_FIELD_INTPORTFOLIO = "client_id";
	public String  FIX_FIELD_TRADER = "executing_trader"; 
	public String  FIX_FIELD_BROKER = "entering_firm"; 	
	public String  FIX_FIELD_EXTERNAL = "contra_firm";	  
	public String  FIX_FIELD_EXTPORTFOLIO = "customer_account"; 
	
	public String  FIX_FIELD_SENDER_HEADER = "SenderCompID";
	public String  FIX_FIELD_SENDERSUB_HEADER = "SenderSubID";
	public String  FIX_FIELD_SENDERLOC_HEADER = "SenderLocationID";
	public String  FIX_FIELD_TARGET_HEADER = "TargetCompID";
	public String  FIX_FIELD_TARGETSUB_HEADER = "TargetSubID";
	public String  FIX_FIELD_TARGETLOC_HEADER = "TargetLocationID";  
	
	public String  FIX_INTERFACE = "FIXGateway";

	/*-------------------------------------------------------------------------------
        Name:             ProcessFixInc_GetToolset
        Description:      This function should return toolset

        Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
                          message_name - incoming fix message name with version number
                          incomingFixTable - table contains incoming FIX message data
                          xstring (XString ) - Should populated with error when fail  

        Return Values:    If fail, null will be returned and xstring should be populated with error 
                          If success, return toolset id (TOOLSET_ENUM)

        Release    : Original 6/1/2007
        -------------------------------------------------------------------------------*/
	public TOOLSET_ENUM ProcessFixInc_GetToolset(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		TOOLSET_ENUM toolset = null;

		/* TODO - Add custom logic here */ 
		toolset = TOOLSET_ENUM.FIN_FUT_TOOLSET;
		/* END TODO */

		return toolset;	      
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

		/* TODO - Add custom logic here */ 
		/* END TODO */

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
		TRAN_STATUS_ENUM status = null;
		/* TODO - Add custom logic here */ 
		String orderstatus = incomingFixTable.getString( FIX_FIELD_ORDSTATUS, 1); 	

		if(message_name.equalsIgnoreCase("NewOrderSingle44"))
		{
			status = TRAN_STATUS_ENUM.TRAN_STATUS_NEW;        
		}
		else if(message_name.equalsIgnoreCase("ExecutionReport44"))
		{
			status = TRAN_STATUS_ENUM.TRAN_STATUS_NEW;        
			if(orderstatus.equalsIgnoreCase("New"))
			{
				status = TRAN_STATUS_ENUM.TRAN_STATUS_NEW;   
			}
			else if(orderstatus.equalsIgnoreCase("Validated"))
			{
				status = TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED;   
			}
			else if(orderstatus.equalsIgnoreCase("Cancelled"))
			{
				status = TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED;
			}
			else if(orderstatus.equalsIgnoreCase("Amended"))
			{
				status = TRAN_STATUS_ENUM.TRAN_STATUS_AMENDED; 				
			}
		}
		/* END TODO */
		return status;   
	} 

	/*-------------------------------------------------------------------------------
	   	Name:            ProcessFixInc_GetFutIntrumentTranf
	   	Description:     This function should perform data gather from incoming FIX table,
	                     create tradefield table and add values to it.  Return tradefield table 
	                     of future instrument in tranf structure.

	   	Parameters:      argTbl (Table) - argument table contains incoming FIX message data 
	                     message_name - incoming fix message name with version number
	                     incomingFixTable - table contains incoming FIX message data
	                     xstring (XString ) - Should populated with error when fail  

	   	Return Values:   If fail, tradeFieldTbls with empty row will be returned and xstring should be populated with error 
	                     If success, return Table containing tranf table(s) filled with trade values  

	   	Release    : Original 6/1/2007
        -------------------------------------------------------------------------------*/
	public Table ProcessFixInc_GetFutIntrumentTranf(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		Table holders = m_FIXSTDHelperInclude.createHolderTables();  

		/* TODO - Add custom logic here */ 
		// General Declaration 
		String ticker = null, price, product = null;	 
		String templatereference = null; 

		//Step 1 - Data gathering from incoming FIX Table
		/* Sample codes to retrieve value from icomingFixTable */  
		Table instrumentTbl = null;
		instrumentTbl = incomingFixTable.getTable("Instrument", 1);

		//Step 1 - Data gathering from incoming FIX Table
		/* Sample codes to retrieve value from icomingFixTable */  
		instrumentTbl = incomingFixTable.getTable("Instrument", 1);
		for (int row = 1; row <= instrumentTbl.getNumRows(); row++)
		{
			if (ticker == null)
				ticker = instrumentTbl.getString( FIX_FIELD_SYMBOL, row); 
			if (product == null)
				product  = instrumentTbl.getString( FIX_FIELD_PRODUCT, row);
			
			if (ticker != null && product != null)
				break;
		}
		if (product != null) 
		{
			//reference is first two letter of ticker
			templatereference = ticker.substring(0, 2); 
		}   
		price = incomingFixTable.getString( FIX_FIELD_AVGPX, 1); 
		if(price == null)
			price = incomingFixTable.getString( FIX_FIELD_PRICE, 1); 		
		/* END Sample codes to retrieve value from icomingFixTable */

		//Step 2 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table() to create TradeField table.
		Table tradeFieldTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table();

		//Step 3 - Call m_FIXSTDHelperInclude.HelperInc_Add_TradeField() to add data to TradeField table 
		//These fields will be set on a template to create a new ticker.
		/* Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INS_TYPE.toInt(), "0", "", "", "EQT-FUT");
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_REFERENCE.toInt(), "0", "", "", ticker);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TICKER.toInt(), "0", "", "", ticker);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_CUSIP.toInt(), "0", "", "", ticker);
		/* END Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */ 

		//Step 4 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table() to wrap TradeField table into TradeBuilder table 
		Table tradeBuilderTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFieldTbl, templatereference);			

		//Step 5 - wrap into holders and return 
		m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilderTbl);	 	   
		/* END TODO */

		return holders;    	
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
	public Table ProcessFixInc_GetTransactionTranf(Table argTbl, String message_name, Table incomingFixTable, TOOLSET_ENUM toolset, TRAN_STATUS_ENUM tranStatus, XString xstring) throws OException
	{
		Table holders = m_FIXSTDHelperInclude.createHolderTables(); 

		/* TODO - Add custom logic here */ 
		// General Declaration
		String orderId, clordid, execId,  msgtype, version, sender, target; 
		String orderstatus, transtatus, buysell, cusip,  product = null, ticker = null, price, position, reference;
		String transacttime, tradedate, settledate, tradetime; 
		String trader = null, broker = null, internalportfolio = null, externalportfolio = null; 
		int tmpTodayDate;
		String internalBUnit = null, externalBUnit = null; 

		//Step 1 - Data gathering from incoming FIX Table
		/* Sample codes to retrieve value from icomingFixTable */
		msgtype = incomingFixTable.getString( FIX_FIELD_MSGTYPE, 1); 
		orderstatus = incomingFixTable.getString( FIX_FIELD_ORDSTATUS, 1); 		
		version = incomingFixTable.getString( FIX_FIELD_VERSION, 1); 		
		execId = incomingFixTable.getString( FIX_FIELD_EXECID, 1);	
		orderId = incomingFixTable.getString( FIX_FIELD_ORDERID, 1);
		clordid = incomingFixTable.getString( FIX_FIELD_CLORDERID, 1);
		reference = incomingFixTable.getString( FIX_FIELD_TEXT, 1);

		Table instrumentTbl = null;
		instrumentTbl = incomingFixTable.getTable("Instrument", 1); 
		for (int row = 1; row <= instrumentTbl.getNumRows(); row++)
		{
			if (ticker == null)
				ticker = instrumentTbl.getString( FIX_FIELD_SYMBOL, row); 
			if (product == null)
				product  = instrumentTbl.getString( FIX_FIELD_PRODUCT, row);
			
			if (ticker != null && product != null)
				break;
		}  

		buysell = incomingFixTable.getString( FIX_FIELD_BUYSELL, 1); 				 
		price = incomingFixTable.getString( FIX_FIELD_AVGPX, 1); 
		if(price == null)
			price = incomingFixTable.getString( FIX_FIELD_PRICE, 1); 

		Table orderQtyDataTbl = null;
		orderQtyDataTbl = incomingFixTable.getTable("OrderQtyData", 1);
		position = orderQtyDataTbl.getString( FIX_FIELD_ORDERQTY, 1); 
		settledate = incomingFixTable.getString( FIX_FIELD_SETTLEDATE, 1); 	

		transacttime = incomingFixTable.getString( FIX_FIELD_TRADETIME, 1);  
		if ( Str.len(transacttime) > 8 )
		{
			tradedate = Str.substr(transacttime, 0, 8);
			tradetime = Str.substr(transacttime, Str.len(transacttime) - 8, 8);
		} 
		else
		{
			tmpTodayDate = OCalendar.today();
			tradedate = OCalendar.formatDateInt(tmpTodayDate, DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);
			tradetime = "00:00:00";
		}

		internalBUnit = incomingFixTable.getString( FIX_FIELD_INTERNAL, 1);  
		internalportfolio = incomingFixTable.getString( FIX_FIELD_INTPORTFOLIO, 1);  
		externalBUnit = incomingFixTable.getString( FIX_FIELD_EXTERNAL, 1);  
		externalportfolio = incomingFixTable.getString( FIX_FIELD_EXTPORTFOLIO, 1);  
		broker = incomingFixTable.getString( FIX_FIELD_BROKER, 1);  
		trader = incomingFixTable.getString( FIX_FIELD_TRADER, 1);   
		/* END Sample codes to retrieve value from icomingFixTable */

		//Step 2 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table() to create TradeField table.
		Table tradeFieldTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table();

		//Step 3 - Call m_FIXSTDHelperInclude.HelperInc_Add_TradeField() to add data to TradeField table
		/* Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */    
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), "0", "", "", "FinFut");
		
		//Set the Source Interface
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRAN_INFO.toInt(), "AdapterTradeConstants.InfoNameTypes.SourceInterface.toName()", "0", "", "", FIX_INTERFACE);
		
		transtatus = "New";
		if(message_name.equalsIgnoreCase("NewOrderSingle44"))
		{
			transtatus = "New";
		}
		else if(message_name.equalsIgnoreCase("ExecutionReport44"))
		{
			if(orderstatus.equalsIgnoreCase("New"))
			{
				transtatus = "New";
			}
			else if(orderstatus.equalsIgnoreCase("Validated"))
			{
				transtatus = "Validated";
			}
			else if(orderstatus.equalsIgnoreCase("Cancelled"))
			{
				transtatus = "Cancelled";
			}
			else if(orderstatus.equalsIgnoreCase("Amended"))
			{
				transtatus = "Amended";
			}
			m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INS_TYPE.toInt(), "0", "", "", "EQT-FUT");
			m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_CONTACT.toInt(), "0", "", "", trader);
		}
		else if(message_name.equalsIgnoreCase("TradeCapture44"))
		{
			//TODO: figure out which extra fields (if any) need to be set for trade capture
		}
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), "0", "", "", transtatus);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TICKER.toInt(), "0", "", "", ticker); 
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_PRICE.toInt(), "0", "", "", price);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_POSITION.toInt(), "0", "", "", position);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRADE_DATE.toInt(), "0", "", "", tradedate + " " + tradetime);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_SETTLE_DATE.toInt(), "0", "", "", settledate);
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(), "0", "", "", internalBUnit);			 
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(), "0", "", "",internalportfolio);		 
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(), "0", "", "", externalBUnit);			 
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt(), "0", "", "", externalportfolio); 
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_BROKER_ID.toInt(), "0", "", "", broker);
		//set the reference field
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_REFERENCE.toInt(), "0", "", "", reference);
		//set executionid in tran info field
		m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRAN_INFO.toInt(), "AdapterTradeConstants.InfoNameTypes.TradeExecutionVenueTradeID.toName()", "0", "", "", execId);
		/* END Sample codes using m_FIXSTDHelperInclude.HelperInc_Add_TradeField */

		//Step 4 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table() to wrap TradeField table into TradeBuilder table 
		Table tradeBuilderTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFieldTbl, "");			

		//Step 5 - wrap into holders and return 
		m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilderTbl);	 	   
		/* END TODO */

		return holders;    
	}

   /*-------------------------------------------------------------------------------
       Name:             ProcessFixInc_GetUserTable
       Description:      This function should return user table 'OL_FixGateway_TranAux'
                         populated with an incoming FIX message data. 
                         * If you do not wish to use this user table, return null.
                         
       Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
                         message_name - incoming fix message name with version number
                         incomingFixTable - table contains incoming FIX message data
                         xstring (XString ) - Should populated with error when fail  
    
       Return Values:    return user table
    
       Release    : Original 6/1/2007
       -------------------------------------------------------------------------------*/
	public Table ProcessFixInc_GetUserTable(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
	{
		Table userTbl = null;

		int row = 0;
		String version, msgtype, clordid, execid, orderid = null;
		String userTblName = "OL_FixGateway_TranAux";
		
		if (DBUserTable.userTableIsValid(userTblName) == 1)
		{
			userTbl = Table.tableNew(userTblName);
			DBUserTable.structure( userTbl );
			row = userTbl.addRow();

			version = incomingFixTable.getString( FIX_FIELD_VERSION, 1); 
			msgtype = incomingFixTable.getString( FIX_FIELD_MSGTYPE, 1); 
			clordid = incomingFixTable.getString( FIX_FIELD_CLORDERID, 1); 
			execid = incomingFixTable.getString( FIX_FIELD_EXECID, 1); 
			orderid = incomingFixTable.getString( FIX_FIELD_ORDERID, 1);  

			if(version != null)
				userTbl.setString( "version", row, version);
			if(msgtype != null)
				userTbl.setString( "messagetype", row, msgtype); 
			if(clordid != null)
				userTbl.setString( "clordid", row, clordid); 
			if(execid != null)
				userTbl.setString( "execid", row, execid); 
			if(orderid != null)
				userTbl.setString( "orderid", row, orderid);   
		}
		
		return userTbl;    
	}
	
    /*-------------------------------------------------------------------------------
    Name:             ProcessFixInc_PreAction
    Description:      This function will perform any pre-processing action.  
    
    Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
                      message_name - incoming fix message name with version number
                      incomingFixTable - table contains incoming FIX message data
                      tran - transaction object returned from OC_Adapter_Trade_Process. 
                      xstring (XString ) - Should populated with error when fail  
    
    Return Values:    if success - 1 should be returned
                      if fail, error code should be returned and xstring should be populated with error 
    
    Release    : Original 5/23/2016
    -------------------------------------------------------------------------------*/
    public int ProcessFixInc_PreAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, XString xstring) throws OException
    {
        int retval = 1; 
    
        return retval;              
    }
       
    /*-------------------------------------------------------------------------------
    Name:             	ProcessFixInc_PostAction
    Description:      	This function will perform any post processing action.  Post processing such as replying 
    					FIX message to sender can be performed in here.  
    
    Parameters:       	argTbl (Table) - argument table contains incoming FIX message data 
    			        message_name - incoming fix message name with version number
    			        incomingFixTable - table contains incoming FIX message data
    			        tran - transaction returned from processTran 
    			        returnTable - table required passing back to oc engine
    			        xstring (XString ) - Should populated with error when fail  
    
    Return Values:    	if success - 1 should be returned
    					if fail, error code should be returned and xstring should be populated with error 
    
    Release    : Original 6/1/2007
    -------------------------------------------------------------------------------*/
	public int ProcessFixInc_PostAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, Table returnTable, XString xstring) throws OException
	{
		int retVal = 1;

		//In this script, user allows to send the ExecutableReport(8) back to sender in respone to the incoming OrderSingle message (D)
		String acknowledge_8_On_D = null;

		String senderid, sendersubid, senderlocationid;
		String targetid, targetsubid, targetlocationid; 
		String savSenderid, savSendersubid, savSenderlocationid;

		String orderId, execId, clordid = null;
		String buysell, ticker, price, position, reference, version = null;
		String tran_num, transtatus = null;
		int orderstatus = 0;

		int currenttime; 

		Table messageTbl, processTbl, dataTbl, targetTbl, documentTbl = null;
		XString err_xstring; 

		// Only do it when incoming message is NewOrderSingle44
		if(message_name.equalsIgnoreCase("NewOrderSingle44"))
		{
			//We have a property called "FIX.Script.SingleDelivery
			acknowledge_8_On_D = m_FIXSTDHelperInclude.HelperInc_Retrieve_GatewayProperty("FIXGateway", "FIX.Script.SingleDelivery");
			if(acknowledge_8_On_D.equalsIgnoreCase("FALSE")) 
			{
				tran_num = tran.getField( TRANF_FIELD.TRANF_TRAN_NUM.toInt(), 0);
				transtatus = tran.getField( TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0); 

				//reference is required for response message since it is used to recognize if it is a valid message	
				reference = tran.getField( TRANF_FIELD.TRANF_REFERENCE.toInt(), 0); 

				version = incomingFixTable.getString( FIX_FIELD_VERSION, 1); 
				buysell = incomingFixTable.getString( FIX_FIELD_BUYSELL, 1); 				 
				price = incomingFixTable.getString( FIX_FIELD_AVGPX, 1); 
				if(price == null)
					price = incomingFixTable.getString( FIX_FIELD_PRICE, 1); 	
				Table orderQtyDataTbl = null;
				orderQtyDataTbl = incomingFixTable.getTable("OrderQtyData", 1);
				position = orderQtyDataTbl.getString( FIX_FIELD_ORDERQTY, 1); 

				Table instrumentTbl = null;
				instrumentTbl = incomingFixTable.getTable("Instrument", 1);
				ticker = instrumentTbl.getString( FIX_FIELD_SYMBOL, 1);

				senderid =  incomingFixTable.getString( FIX_FIELD_SENDER_HEADER, 1); 
				sendersubid =  incomingFixTable.getString( FIX_FIELD_SENDERSUB_HEADER, 1); 
				senderlocationid =  incomingFixTable.getString( FIX_FIELD_SENDERLOC_HEADER, 1); 
				targetid =  incomingFixTable.getString( FIX_FIELD_TARGET_HEADER, 1); 
				targetsubid =  incomingFixTable.getString( FIX_FIELD_TARGETSUB_HEADER, 1); 
				targetlocationid = incomingFixTable.getString( FIX_FIELD_TARGETLOC_HEADER, 1);  

				//Switch sender/target position
				savSenderid = senderid;
				savSendersubid = sendersubid;
				savSenderlocationid = senderlocationid;

				senderid = targetid;
				sendersubid = targetsubid;
				senderlocationid = targetlocationid;

				targetid = savSenderid;
				targetsubid = savSendersubid;
				targetlocationid = savSenderlocationid; 

				currenttime = Util.timeGetServerTime();

				clordid = incomingFixTable.getString( "ClOrdID", 1); 
				execId = tran_num + Str.intToStr(currenttime); 
				orderId = tran_num;  

				dataTbl = m_FIXSTDHelperInclude.HelperInc_Create_Data_Table(); 

				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "37", orderId, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//OrderId - unique chain of order			
				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "11", clordid, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//clordid -  affix an identification number to an order to coincide with internal systems	
				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "17", execId, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Combination of Dealnum and current date time in Julian format

				if(transtatus.equalsIgnoreCase("New"))	
				{
					orderstatus = 0; 
				}
				else if(transtatus.equalsIgnoreCase("Validated"))	
				{
					orderstatus = 2; 
				}
				else if(transtatus.equalsIgnoreCase("Cancelled"))	
				{
					orderstatus = 4; 
				} 

				if(buysell.equalsIgnoreCase("Buy"))										//TODO - call mappingfunction
					buysell = "1";
				else
					buysell = "2";		

				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "20", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ExecTransType
				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "54", buysell, COL_TYPE_ENUM.COL_STRING.toInt(), "0");			  
				m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "55", ticker, COL_TYPE_ENUM.COL_STRING.toInt(), "0");	
				if(tran_num == null)								//it was called by another script and last ran script cannot be run successfully
				{
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "6", price, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//average price = 0 for nonfilled order   
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "14", position, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//Cumulative qty = 0 since no qty left
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "151", position, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//Leave qty = 0 for reject order	
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "39", "8", COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//orderstatus = 9 = Suspended  (waiting for hospital process )(TODO - should be simply reject it? or set = 8 = Reject -> business decision)				
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "150", Str.intToStr(orderstatus), COL_TYPE_ENUM.COL_STRING.toInt(), "0");		//ExecType
				}
				else
				{													//if it was called by another script and last ran script is sucessful one
					// or if it was called by another script 
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "6", price, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//average price is the price being used
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "14", position, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//Cumulative qty = orderqty since no qty left
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "151", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//leave qty = orderqty - cumqty  
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "39", Str.intToStr(orderstatus), COL_TYPE_ENUM.COL_STRING.toInt(), "0");		//orderstatus		 
					m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "150", Str.intToStr(orderstatus), COL_TYPE_ENUM.COL_STRING.toInt(), "0");		//ExecType
				}

				targetTbl = m_FIXSTDHelperInclude.HelperInc_Create_Target_Table(); 
				m_FIXSTDHelperInclude.HelperInc_Add_Target (targetTbl, version, targetid, targetsubid, targetlocationid);

				processTbl = m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table();
				m_FIXSTDHelperInclude.HelperInc_Add_Message (processTbl, "", "O", "8", "", reference, 
						senderid, sendersubid, senderlocationid, 
						documentTbl, targetTbl, dataTbl); 

				err_xstring = Str.xstringNew(); 
				messageTbl = m_FIXSTDHelperInclude.HelperInc_BuildMessage(processTbl, err_xstring);
				if (messageTbl == null)
				{
					Str.xstringAppend(xstring, "ProcessFixInc_PostAction: m_FIXSTDHelperInclude.HelperInc_BuildMessage fail to run " + Str.xstringGetString(err_xstring));
					Str.xstringDestroy(err_xstring);
					return OC_REQUEST_STATUS_ENUM.OC_STATUS_FAILURE_CANNOT_PROCESS_METHOD.toInt(); 
				} 			

				retVal = m_FIXSTDHelperInclude.HelperInc_CopyTablePtr(messageTbl, returnTable);	
				if (retVal != 1)
				{
					Str.xstringAppend(xstring, "ProcessFixInc_PostAction: m_FIXSTDHelperInclude.HelperInc_CopyTablePtr  fail to run " + Str.xstringGetString(err_xstring));
					Str.xstringDestroy(err_xstring); 
					return retVal;
				}
				Str.xstringDestroy(err_xstring);
			}
		}

		return retVal;              
	}
	
	/*-------------------------------------------------------------------------------
	   Name:             ProcessFixInc_GetReconcilationRequest
	   Description:      This function will get the reconcilation request.   

	   Note:  	     The return message must use the word "reconciliation" on reference.

	   Parameters:       argTbl (Table) - argument table contains 
	                     returnTable - table required passing back to oc engine
	                     xstring (XString ) - Should populated with error when fail  

	   Return Values:    if success - 1 should be returned
	                     if fail, error code should be returned and xstring should be populated with error 

	   Release    : Original 6/1/2012
	-------------------------------------------------------------------------------*/
	public int ProcessFixInc_GetReconcilationRequest(Table argTbl, Table returnTable, XString xstring) throws OException {
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

		m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "7", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//BeginSeqNo
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
	}
	
	public String get_argument_from_argt(Table argumentTables, String propName, String default_str) throws OException
	{
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
}
