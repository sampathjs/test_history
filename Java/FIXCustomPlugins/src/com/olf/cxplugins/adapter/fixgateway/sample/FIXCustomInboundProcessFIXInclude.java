package com.olf.cxplugins.adapter.fixgateway.sample;
import com.olf.cxplugins.adapter.fixgateway.FIXSTDHelperInclude;
import com.olf.cxplugins.adapter.fixgateway.IFIXCustomProcessFIXInclude;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
      Name       : FIXCustomProcessFIXInclude.java

      Description: The script provides a framework for the user to process incoming FIX message. 

                  User should reference this script as part of INCLUDE script to invoke
                  any built-in functions listed here.

      Release    : Original 6/1/2007
*/

public class FIXCustomInboundProcessFIXInclude implements IFIXCustomProcessFIXInclude
{
   private FIXSTDHelperInclude m_FIXSTDHelperInclude;

   public FIXCustomInboundProcessFIXInclude(){
      m_FIXSTDHelperInclude = new FIXSTDHelperInclude();
   }

   public String  FIX_FIELD_VERSION = "BeginString";
   public String  FIX_FIELD_MSGTYPE = "MsgType";
   public String  FIX_FIELD_ORDSTATUS = "OrdStatus";
   public String  FIX_FIELD_EXECTRANSTYPE = "ExecTransType";
   public String  FIX_FIELD_EXECTYPE = "ExecType";

   public String  FIX_FIELD_EXECID = "ExecID";  
   public String  FIX_FIELD_CLORDERID = "ClOrdID";
   public String  FIX_FIELD_ORDERID = "OrderID"; 

   public String  FIX_FIELD_EXDESTINATION = "ExDestination";
   public String  FIX_FIELD_INSTRUMENT = "Instrument";
   public String  FIX_FIELD_SECURITYTYPE = "SecurityType";  
   public String  FIX_FIELD_BUYSELL = "Side";
   public String  FIX_FIELD_SYMBOL = "Symbol";
   public String  FIX_FIELD_SECURITYID= "SecurityID";
   public String  FIX_FIELD_IDSOURCE= "IDSource"; 
   public String  FIX_FIELD_POSITION_DATA = "OrderQtyData";
   public String  FIX_FIELD_POSITION= "OrderQty";
   public String  FIX_FIELD_PRICE = "LastPx"; 
   public String  FIX_FIELD_LASTSHARES = "LastQty";
   public String  FIX_FIELD_PUTORCALL = "PutOrCall";
   public String  FIX_FIELD_STRIKEPRICE = "StrikePrice";
   public String  FIX_FIELD_MATURITYDATE = "MaturityMonthYear";
   public String  FIX_FIELD_FUTSETTLEYDATE = "FutSettDate";

   public String  FIX_FIELD_CCY = "Currency"; 
   public String  FIX_FIELD_TRADETIME = "TransactTime"; 

   public String  FIX_FIELD_INTERNAL = "TargetCompID";
   public String  FIX_FIELD_INTPORTFOLIO = "Account";
   public String  FIX_FIELD_EXTERNAL = "SenderCompID";

   public String  FIX_FIELD_INTCONTACT = "ClientID";
   public String  FIX_FIELD_BROKERID = "ExecBroker";   

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
	   String errMsg = null;
	   String securitytype, ticker, cusip, idsource;
	   String putcall, strikeprice, maturitydate, tickerPattern; 
	   String exdestination;
	   Table instrument, ticker_table= null;
	   int toolsetid, dashPosition;
	   int retval; 

	   instrument = incomingFixTable.getTable(FIX_FIELD_INSTRUMENT, 1);
	   securitytype = instrument.getString( FIX_FIELD_SECURITYTYPE, 2); 
	   exdestination = incomingFixTable.getString( FIX_FIELD_EXDESTINATION, 1);
	   ticker = instrument.getString( FIX_FIELD_SYMBOL, 1);
	   cusip  = incomingFixTable.getString( FIX_FIELD_SECURITYID, 1);
	   idsource = incomingFixTable.getString( FIX_FIELD_IDSOURCE, 1);
	   if (cusip != null && idsource.equalsIgnoreCase("CUSIP"))
	   { 
		   ticker = cusip;     //blank it if it is [N/A]
	   } 
	   putcall = incomingFixTable.getString( FIX_FIELD_PUTORCALL, 1);   
	   strikeprice = incomingFixTable.getString( FIX_FIELD_STRIKEPRICE, 1);  
	   maturitydate = instrument.getString( FIX_FIELD_MATURITYDATE, 3);    

	   dashPosition = Str.findSubString(ticker, "-");
	   if (dashPosition != -1)
	   {
		   tickerPattern = Str.substr( ticker, 0, dashPosition);
		   tickerPattern = tickerPattern + "%";
	   }
	   else
	   {
		   tickerPattern = ticker + "%";
	   }

	   ticker_table = Table.tableNew("Ticker Table"); 
	   if(securitytype.equalsIgnoreCase("FUTURE"))
	   {
		   retval = query_for_ticker_by_ticker(tickerPattern, maturitydate, exdestination, ticker_table); 
		   if (retval != 1)
		   {
			   ticker_table.destroy();
			   errMsg = "FIXCustomInboundProcessFIXInclude - query_for_ticker_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else  if(securitytype.equalsIgnoreCase("OPTION"))
	   {
		   retval = query_for_option_by_ticker(tickerPattern, maturitydate, exdestination, putcall, strikeprice, ticker_table); 
		   {
			   ticker_table.destroy();
			   errMsg = "FIXustomProcessFIXInclude - query_for_option_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else
	   {	
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: security type must be FUTURE or OPTION";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }


	   if (ticker_table.getNumRows() <= 0)
	   {
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: No holding instruments with expiration date " + maturitydate + " exist"; 
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }

	   toolsetid = ticker_table.getInt( "toolset", 1);   
	   toolset = TOOLSET_ENUM.fromInt(toolsetid);
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
	   // General Declaration
	   int tran_num;
	   int retval;
	   String errMsg = null;
	   String orderId, clordid, execId, msgtype, version, securitytype, toolset, proj_index_name; 
	   String orderstatus, execTransType, execType, transtatus, buysell, cusip, idsource, ticker, price, position, total_position, templatereference;  
	   String exdestination, ccy, reference;
	   String transacttime, tradedate, settledate, tradetime; 
	   String putcall, strikeprice, maturitydate, tickerPattern; 
	   String total_price, total_quantity, avg_price;
	   int tmp_total_quantity, tmp_total_position;

	   int toolsetid, proj_index, ins_type, dashPosition, tmpPosition, tmpTodayDate;

	   String internalportfolio; 
	   String internalBUnit, externalBUnit, internalContact = null;
	   String execBroker = null;
	   Table tradeFieldTbl, tTranAux, instrument, qtyData, ticker_table= null;

	   //Step 1 - Data gathering from incoming FIX Table  
	   msgtype = incomingFixTable.getString( FIX_FIELD_MSGTYPE, 1); 
	   orderstatus = incomingFixTable.getString( FIX_FIELD_ORDSTATUS, 1);  
	   execTransType = incomingFixTable.getString( FIX_FIELD_EXECTRANSTYPE, 1); 
	   execType  = incomingFixTable.getString( FIX_FIELD_EXECTYPE, 1); 
	   instrument = incomingFixTable.getTable(FIX_FIELD_INSTRUMENT, 1);
	   securitytype = instrument.getString( FIX_FIELD_SECURITYTYPE, 2);

	   version = incomingFixTable.getString( FIX_FIELD_VERSION, 1);   
	   execId = incomingFixTable.getString( FIX_FIELD_EXECID, 1); 
	   orderId = incomingFixTable.getString( FIX_FIELD_ORDERID, 1);
	   clordid = incomingFixTable.getString( FIX_FIELD_CLORDERID, 1);

	   exdestination = incomingFixTable.getString( FIX_FIELD_EXDESTINATION, 1);
	   ticker = instrument.getString( FIX_FIELD_SYMBOL, 1);
	   cusip  = incomingFixTable.getString( FIX_FIELD_SECURITYID, 1);
	   idsource = incomingFixTable.getString( FIX_FIELD_IDSOURCE, 1);
	   if (cusip != null && idsource.equalsIgnoreCase("CUSIP"))
	   {
		   templatereference = cusip;
		   ticker = cusip;     //blank it if it is [N/A]
	   }

	   buysell = incomingFixTable.getString( FIX_FIELD_BUYSELL, 1);      
	   price = incomingFixTable.getString( FIX_FIELD_PRICE, 1);  
	   position = incomingFixTable.getString( FIX_FIELD_LASTSHARES, 1);	//lastQty
	   qtyData = incomingFixTable.getTable( FIX_FIELD_POSITION_DATA, 1);
	   total_position = qtyData.getString( FIX_FIELD_POSITION, 1); 	
	   ccy = incomingFixTable.getString( FIX_FIELD_CCY, 1);         

	   if(buysell.equalsIgnoreCase("BUY_SELL_ENUM.SELL"))
	   {
		   tmpPosition = Str.strToInt(position) * -1;
		   position = Str.intToStr(tmpPosition);

	   }
	   putcall = incomingFixTable.getString( FIX_FIELD_PUTORCALL, 1);   
	   strikeprice = incomingFixTable.getString( FIX_FIELD_STRIKEPRICE, 1);  
	   maturitydate = instrument.getString( FIX_FIELD_MATURITYDATE, 3);    
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

	   internalContact = incomingFixTable.getString( FIX_FIELD_INTCONTACT, 1);
	   execBroker = incomingFixTable.getString( FIX_FIELD_BROKERID, 1);

	   reference = version + ":" + FIX_FIELD_ORDERID + "=" + orderId; 

	   //Pre-processing validation 
	   if (orderId == null || execId == null)
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: The following field(s) cannot be blank:" + FIX_FIELD_EXECID + "/" + FIX_FIELD_ORDERID + " on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   if (position == null || price == null || total_position == null)
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: The following field(s) cannot be blank:" + FIX_FIELD_POSITION + "/" + FIX_FIELD_PRICE + "/" + FIX_FIELD_LASTSHARES + " on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   if(execTransType != null && execTransType.equalsIgnoreCase("Status"))
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Unable to process FIX message with ExecTransType = 3 (Status) on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   internalContact = get_qualified_trader(internalBUnit, internalContact);
	   execBroker = get_qualified_execBroker(execBroker);

	   //Step 2 - Retreive transaction number
	   tran_num = query_for_tranaction_number(version, msgtype, orderId, tradedate, xstring);
	   if (tran_num < 0)
	   {       
		   return null;
	   } 

	   //if it is Correct or Cancel, the transaction must exist.  If not, fail it.  (it can be caused by
	   //the initial deal was not able to transmit correctly
	   if (tran_num == 0)
	   {
		   if (execTransType != null && (execTransType.equalsIgnoreCase("Cancel") || execTransType.equalsIgnoreCase("Correct")))
		   {
			   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Original transaction tran# " + tran_num + " not found for cancel/correct request.";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;              
		   }
	   }

	   //Step 3 - retrieve average price, total price and total quantity
	   if (tran_num > 0 && execTransType != null && execTransType.equalsIgnoreCase("New"))
	   {
		   tTranAux = Table.tableNew();
		   retval = Get_OL_FixGateway_TranAux_Detail (tran_num, Str.strToDouble(price), Str.strToDouble(position), tTranAux, xstring);
		   if (retval <= 0)
		   {      
			   tTranAux.destroy();
			   return null;
		   }

		   total_price = Str.doubleToStr(tTranAux.getDouble( "total_price", 1));
		   total_quantity = Str.doubleToStr(tTranAux.getDouble( "total_quantity", 1));
		   avg_price = Str.doubleToStr(tTranAux.getDouble( "avg_price", 1)); 

		   tTranAux.destroy();
	   }
	   else
	   {
		   total_price = price;
		   total_quantity = position; 
		   avg_price = price; 
	   } 

	   //	if total quantity being traded is greater than number of share allowed
	   if (java.lang.Math.abs(Integer.parseInt(total_quantity)) > java.lang.Math.abs(Integer.parseInt(total_position)))
	   {
		   tmp_total_quantity = java.lang.Math.abs(Str.strToInt(total_quantity));
		   tmp_total_position = java.lang.Math.abs(Str.strToInt(total_position));
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Error on tran# " + tran_num + ": Execution quantity (OrderQty = " + Str.intToStr(tmp_total_quantity) + ") exceeds the total order size (LastShares = " + Str.intToStr(tmp_total_position) + ")";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;      
	   }

	   //Step 5 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table() to create TradeField table.
	   tradeFieldTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table();

	   //Step 6 - Search for toolset
	   //Ticker Pattern in tradefield will be used to search the holding instrument in which the assoicated ticker has the text
	   //pattern specified.  The pattern will adapt SQL99 LIKE syntax in which "%", percentage sign will be the wildcard
	   dashPosition = Str.findSubString(ticker, "-");
	   if (dashPosition != -1)
	   {
		   tickerPattern = Str.substr( ticker, 0, dashPosition);
		   tickerPattern = tickerPattern + "%";
	   }
	   else
	   {
		   tickerPattern = ticker + "%";
	   }

	   ticker_table = Table.tableNew("Ticker Table"); 
	   if(securitytype.equalsIgnoreCase("FUTURE"))
	   {
		   retval = query_for_ticker_by_ticker(tickerPattern, maturitydate, exdestination, ticker_table); 
		   if (retval != 1)
		   {
			   ticker_table.destroy();
			   errMsg = "FIXCustomInboundProcessFIXInclude - query_for_ticker_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else  if(securitytype.equalsIgnoreCase("OPTION"))
	   {
		   retval = query_for_option_by_ticker(tickerPattern, maturitydate, exdestination, putcall, strikeprice, ticker_table); 
		   {
			   ticker_table.destroy();
			   errMsg = "FIXustomProcessFIXInclude - query_for_option_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else
	   {	
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: security type must be FUTURE or OPTION";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }


	   if (ticker_table.getNumRows() <= 0)
	   {
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: No holding instruments with expiration date " + maturitydate + " exist"; 
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }

	   toolsetid = ticker_table.getInt( "toolset", 1);
	   toolset = Table.formatRefInt(toolsetid, SHM_USR_TABLES_ENUM.TOOLSETS_TABLE);
	   ticker = ticker_table.getString( "ticker", 1);
	   templatereference = ticker;
	   proj_index = ticker_table.getInt( "proj_index", 1);
	   proj_index_name = Table.formatRefInt(proj_index, SHM_USR_TABLES_ENUM.INDEX_TABLE);

	   ticker_table.destroy();

	   if(execTransType != null && execTransType.equalsIgnoreCase("Correct"))
	   {
		   status = TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED;     
	   }
	   else if(execTransType != null && execTransType.equalsIgnoreCase("Cancel"))
	   {
		   status = TRAN_STATUS_ENUM.TRAN_STATUS_DELETED;    
	   }
	   else
	   {
		   if (java.lang.Math.abs(Str.strToInt(total_quantity)) == java.lang.Math.abs(Str.strToInt(total_position)))
		   {
			   status = TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED; 
		   }
		   else
		   {
			   status = TRAN_STATUS_ENUM.TRAN_STATUS_PENDING; 
		   } 
	   } 
	   /* END TODO */
	   return status;  
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
                     If success, return Table containing tradebuilders table(s)

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public Table ProcessFixInc_GetFutIntrumentTranf(Table argTbl, String message_name, Table incomingFixTable, XString xstring) throws OException
   {
	   Table holders = m_FIXSTDHelperInclude.createHolderTables();   

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
	   
	   Transaction tran = null; 
	   int tran_num, iTranVersionNum, iTranStatus;
	   int retval; 
	   String errMsg = null;

	   // General Declaration
	   String orderId, clordid, execId, msgtype, version, securitytype, proj_index_name; 
	   String orderstatus, execTransType, execType, transtatus, buysell, cusip, idsource, ticker, price, position, total_position, templatereference;  
	   String exdestination, ccy, reference;
	   String transacttime, tradedate, settledate, tradetime; 
	   String putcall, strikeprice, maturitydate, tickerPattern; 
	   String total_price, total_quantity, avg_price;
	   int tmp_total_quantity, tmp_total_position;

	   int proj_index, dashPosition, tmpPosition, tmpTodayDate;

	   String internalportfolio; 
	   String internalBUnit, externalBUnit, internalContact = null;
	   String execBroker = null;
	   Table tTranAux, instrument, qtyData, ticker_table= null;

	   //Step 1 - Data gathering from incoming FIX Table  
	   msgtype = incomingFixTable.getString( FIX_FIELD_MSGTYPE, 1); 
	   orderstatus = incomingFixTable.getString( FIX_FIELD_ORDSTATUS, 1);  
	   execTransType = incomingFixTable.getString( FIX_FIELD_EXECTRANSTYPE, 1); 
	   execType  = incomingFixTable.getString( FIX_FIELD_EXECTYPE, 1); 
	   instrument = incomingFixTable.getTable(FIX_FIELD_INSTRUMENT, 1);
	   securitytype = instrument.getString( FIX_FIELD_SECURITYTYPE, 2);

	   version = incomingFixTable.getString( FIX_FIELD_VERSION, 1);   
	   execId = incomingFixTable.getString( FIX_FIELD_EXECID, 1); 
	   orderId = incomingFixTable.getString( FIX_FIELD_ORDERID, 1);
	   clordid = incomingFixTable.getString( FIX_FIELD_CLORDERID, 1);

	   exdestination = incomingFixTable.getString( FIX_FIELD_EXDESTINATION, 1);
	   ticker = instrument.getString( FIX_FIELD_SYMBOL, 1);
	   cusip  = incomingFixTable.getString( FIX_FIELD_SECURITYID, 1);
	   idsource = incomingFixTable.getString( FIX_FIELD_IDSOURCE, 1);
	   if (cusip != null && idsource.equalsIgnoreCase("CUSIP"))
	   {
		   templatereference = cusip;
		   ticker = cusip;     //blank it if it is [N/A]
	   }

	   buysell = incomingFixTable.getString( FIX_FIELD_BUYSELL, 1);      
	   price = incomingFixTable.getString( FIX_FIELD_PRICE, 1);  
	   position = incomingFixTable.getString( FIX_FIELD_LASTSHARES, 1);	//lastQty
	   qtyData = incomingFixTable.getTable( FIX_FIELD_POSITION_DATA, 1);
	   total_position = qtyData.getString( FIX_FIELD_POSITION, 1); 	
	   ccy = incomingFixTable.getString( FIX_FIELD_CCY, 1);         

	   if(buysell.equalsIgnoreCase("BUY_SELL_ENUM.SELL"))
	   {
		   tmpPosition = Str.strToInt(position) * -1;
		   position = Str.intToStr(tmpPosition);

	   }
	   putcall = incomingFixTable.getString( FIX_FIELD_PUTORCALL, 1);   
	   strikeprice = incomingFixTable.getString( FIX_FIELD_STRIKEPRICE, 1);  
	   maturitydate = instrument.getString( FIX_FIELD_MATURITYDATE, 3);    
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

	   internalContact = incomingFixTable.getString( FIX_FIELD_INTCONTACT, 1);
	   execBroker = incomingFixTable.getString( FIX_FIELD_BROKERID, 1);

	   reference = version + ":" + FIX_FIELD_ORDERID + "=" + orderId; 

	   //Pre-processing validation 
	   if (orderId == null || execId == null)
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: The following field(s) cannot be blank:" + FIX_FIELD_EXECID + "/" + FIX_FIELD_ORDERID + " on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   if (position == null || price == null || total_position == null)
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: The following field(s) cannot be blank:" + FIX_FIELD_POSITION + "/" + FIX_FIELD_PRICE + "/" + FIX_FIELD_LASTSHARES + " on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   if(execTransType != null && execTransType.equalsIgnoreCase("Status"))
	   {
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Unable to process FIX message with ExecTransType = 3 (Status) on FIX message ExecID# " + execId + " OrderID# " + orderId + ".";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;              
	   }

	   internalContact = get_qualified_trader(internalBUnit, internalContact);
	   execBroker = get_qualified_execBroker(execBroker);

	   //Step 2 - Retreive transaction number
	   tran_num = query_for_tranaction_number(version, msgtype, orderId, tradedate, xstring);
	   if (tran_num < 0)
	   {       
		   return null;
	   } 

	   //if it is Correct or Cancel, the transaction must exist.  If not, fail it.  (it can be caused by
	   //the initial deal was not able to transmit correctly
	   if (tran_num == 0)
	   {
		   if (execTransType != null && (execTransType.equalsIgnoreCase("Cancel") || execTransType.equalsIgnoreCase("Correct")))
		   {
			   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Original transaction tran# " + tran_num + " not found for cancel/correct request.";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;              
		   }
	   }

	   //Step 3 - retrieve average price, total price and total quantity
	   if (tran_num > 0 && execTransType != null && execTransType.equalsIgnoreCase("New"))
	   {
		   tTranAux = Table.tableNew();
		   retval = Get_OL_FixGateway_TranAux_Detail (tran_num, Str.strToDouble(price), Str.strToDouble(position), tTranAux, xstring);
		   if (retval <= 0)
		   {      
			   tTranAux.destroy();
			   return null;
		   }

		   total_price = Str.doubleToStr(tTranAux.getDouble( "total_price", 1));
		   total_quantity = Str.doubleToStr(tTranAux.getDouble( "total_quantity", 1));
		   avg_price = Str.doubleToStr(tTranAux.getDouble( "avg_price", 1)); 

		   tTranAux.destroy();
	   }
	   else
	   {
		   total_price = price;
		   total_quantity = position; 
		   avg_price = price; 
	   }

	   //	Step 4 - Get current status of the deal in ab_tran 
	   if (tran_num > 0)
	   {
		   tran = Transaction.retrieve(tran_num); 
		   if(Transaction.isNull(tran) != 0)
		   {
			   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Unable to retreive transaction on tran# " + tran_num;
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }

		   iTranStatus = tran.getFieldInt( TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0);  
		   iTranVersionNum = tran.getFieldInt( TRANF_FIELD.TRANF_VERSION_NUM.toInt(), 0);        

		   errMsg = null;
		   //The transaction must in Proposed or Pending.  
		   if ((iTranStatus != TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt()) && (iTranStatus != TRAN_STATUS_ENUM.TRAN_STATUS_PENDING.toInt()))
		   {
			   if (execTransType != null && (execTransType.equalsIgnoreCase("Cancel") || execTransType.equalsIgnoreCase("Correct")))
			   {
				   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Transaction on tran# " + tran_num + " is not in pending or proposed status.";
			   } 
		   }
		   else
		   {
			   if(execTransType != null && execTransType.equalsIgnoreCase("New"))
			   {
				   //It makes no sense if tran status is proposed (filled up) and incoming message is partial fill or fill (again?)
				   if (iTranStatus == TRAN_STATUS_ENUM.TRAN_STATUS_PROPOSED.toInt())
				   {
					   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Transaction on tran# " + tran_num + " was already marked as proposed.";
				   } 
			   } 
		   }
		   if(errMsg != null)
		   {
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   } 

	   //	if total quantity being traded is greater than number of share allowed
	   if (java.lang.Math.abs(Integer.parseInt(total_quantity)) > java.lang.Math.abs(Integer.parseInt(total_position)))
	   {
		   tmp_total_quantity = java.lang.Math.abs(Str.strToInt(total_quantity));
		   tmp_total_position = java.lang.Math.abs(Str.strToInt(total_position));
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: Error on tran# " + tran_num + ": Execution quantity (OrderQty = " + Str.intToStr(tmp_total_quantity) + ") exceeds the total order size (LastShares = " + Str.intToStr(tmp_total_position) + ")";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;      
	   }

	   //Step 5 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table() to create TradeField table.
	   Table tradeFieldTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeField_Table();

	   //Step 6 - Search for toolset
	   //Ticker Pattern in tradefield will be used to search the holding instrument in which the assoicated ticker has the text
	   //pattern specified.  The pattern will adapt SQL99 LIKE syntax in which "%", percentage sign will be the wildcard
	   dashPosition = Str.findSubString(ticker, "-");
	   if (dashPosition != -1)
	   {
		   tickerPattern = Str.substr( ticker, 0, dashPosition);
		   tickerPattern = tickerPattern + "%";
	   }
	   else
	   {
		   tickerPattern = ticker + "%";
	   }

	   ticker_table = Table.tableNew("Ticker Table"); 
	   if(securitytype.equalsIgnoreCase("FUTURE"))
	   {
		   retval = query_for_ticker_by_ticker(tickerPattern, maturitydate, exdestination, ticker_table); 
		   if (retval != 1)
		   {
			   ticker_table.destroy();
			   errMsg = "FIXCustomInboundProcessFIXInclude - query_for_ticker_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else  if(securitytype.equalsIgnoreCase("OPTION"))
	   {
		   retval = query_for_option_by_ticker(tickerPattern, maturitydate, exdestination, putcall, strikeprice, ticker_table); 
		   {
			   ticker_table.destroy();
			   errMsg = "FIXustomProcessFIXInclude - query_for_option_by_ticker: unable to run sql";
			   OConsole.oprint("\n" + errMsg + "\n");
			   Str.xstringAppend(xstring, errMsg);
			   return null;
		   }
	   }
	   else
	   {	
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: security type must be FUTURE or OPTION";
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }


	   if (ticker_table.getNumRows() <= 0)
	   {
		   ticker_table.destroy();
		   errMsg = "FIXCustomInboundProcessFIXInclude - ProcessFixInc_GetTradeBuilder: No holding instruments with expiration date " + maturitydate + " exist"; 
		   OConsole.oprint("\n" + errMsg + "\n");
		   Str.xstringAppend(xstring, errMsg);
		   return null;
	   }

	   ticker = ticker_table.getString( "ticker", 1);
	   templatereference = ticker;
	   proj_index = ticker_table.getInt( "proj_index", 1);
	   proj_index_name = Table.formatRefInt(proj_index, SHM_USR_TABLES_ENUM.INDEX_TABLE);

	   ticker_table.destroy();

	   //Step 7 - Call m_FIXSTDHelperInclude.HelperInc_Add_TradeField() to add data to TradeField table 
	   if(execTransType != null && execTransType.equalsIgnoreCase("Correct"))
	   {
		   transtatus = "Proposed";     
	   }
	   else if(execTransType != null && execTransType.equalsIgnoreCase("Cancel"))
	   {
		   transtatus = "Deleted";     
	   }
	   else
	   {
		   if (java.lang.Math.abs(Str.strToInt(total_quantity)) == java.lang.Math.abs(Str.strToInt(total_position)))
		   {
			   transtatus = "Proposed"; 
		   }
		   else
		   {
			   transtatus = "Pending"; 
		   } 
	   } 

	   if (tran_num >0)
		   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRAN_NUM.toInt(), "0", "", "", Str.intToStr(tran_num)); 

	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), "0", "", "", transtatus); 
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), "0", "", "", String.valueOf(toolset.toInt()));  
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_PRICE.toInt(), "0", "", "", avg_price);
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_POSITION.toInt(), "0", "", "", total_quantity);
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRADE_DATE.toInt(), "0", "", "", tradedate + " " + tradetime); 

	   if(securitytype.equalsIgnoreCase("OPTION"))
	   { 
		   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_PUT_CALL.toInt(), "0", "", "", putcall); 
		   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_STRIKE.toInt(), "0", "", "", strikeprice);  
	   } 

	   if (toolset == TOOLSET_ENUM.FIN_FUT_TOOLSET)
		   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_TRADE_TIME.toInt(), "0", "", "", tradetime); 
	   else if (toolset == TOOLSET_ENUM.COM_FUT_TOOLSET || toolset == TOOLSET_ENUM.COM_OPT_FUT_TOOLSET)
		   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_PROJ_INDEX.toInt(), "0", "", "", proj_index_name);

	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(), "0", "", "", internalBUnit);  
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(), "0", "", "",internalportfolio);
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(), "0", "", "", externalBUnit);
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_REFERENCE.toInt(), "0", "", "", reference);  
	   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_INTERNAL_CONTACT.toInt(), "0", "", "", internalContact);  

	   //Only populate the field during first time to book the deal, will keep the same value on modified one.
	   if (tran_num == 0)
	   {
		   if(execBroker != null)
			   m_FIXSTDHelperInclude.HelperInc_Add_TradeField(tradeFieldTbl, TRANF_FIELD.TRANF_BROKER_ID.toInt(), "0", "", "", execBroker);  
	   }

	   //Step 4 - Call m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table() to wrap TradeField table into TradeBuilder table 
	   Table tradeBuilderTbl = m_FIXSTDHelperInclude.HelperInc_Create_TradeBuilder_Table(tradeFieldTbl, templatereference);			
       
	   //Step 5 - wrap into holders and return 
       m_FIXSTDHelperInclude.addTableToTables(holders, tradeBuilderTbl);	 	   
       /* END TODO */
	   
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
      Table userTbl = null;

      int row = 0;
      String version, msgtype, clordid, execid, orderid, execTransType, buysell, price, position, transacttime = null;
      String tradedate, tradetime = null;
      ODateTime transact_date, transact_datetime = ODateTime.dtNew();
      int tmpPosition, tmpTodayDate;

      userTbl = Table.tableNew("OL_FixGateway_TranAux");
      DBUserTable.structure( userTbl );
      row = userTbl.addRow();

      version = incomingFixTable.getString( FIX_FIELD_VERSION, 1); 
      msgtype = incomingFixTable.getString( FIX_FIELD_MSGTYPE, 1); 
      clordid = incomingFixTable.getString( FIX_FIELD_CLORDERID, 1); 
      execid = incomingFixTable.getString( FIX_FIELD_EXECID, 1); 
      orderid = incomingFixTable.getString( FIX_FIELD_ORDERID, 1);  
      execTransType = incomingFixTable.getString( FIX_FIELD_EXECTRANSTYPE, 1);  
      price = incomingFixTable.getString( FIX_FIELD_PRICE, 1); 
      position = incomingFixTable.getString( FIX_FIELD_LASTSHARES, 1); 
      buysell = incomingFixTable.getString( FIX_FIELD_BUYSELL, 1);      

      if(buysell.equalsIgnoreCase("BUY_SELL_ENUM.SELL"))
      {
         tmpPosition = Str.strToInt(position) * -1;
         position = Str.intToStr(tmpPosition);

      }
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
      tradedate = OCalendar.formatDateInt(OCalendar.convertYYYYMMDDToJd(tradedate), DATE_FORMAT.DATE_FORMAT_MDSY_SLASH, DATE_LOCALE.DATE_LOCALE_US);
      transact_date = ODateTime.strToDateTime( tradedate );
      transact_datetime = ODateTime.strToDateTime( tradedate + " " + tradetime);

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
      if(execTransType != null)
         userTbl.setString( "exectranstype", row, execTransType);     
      if(price != null)
         userTbl.setDouble( "price", row, Str.strToDouble(price));   
      if(position != null)
         userTbl.setDouble( "quantity", row, Str.strToDouble(position));   
      if(transacttime != null)
         userTbl.setDateTime( "transact_date", row, transact_date);   
      if(transacttime != null)
         userTbl.setDateTime( "transact_time", row, transact_datetime);   

      transact_date.destroy();
      transact_datetime.destroy();

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
   Name:             ProcessFixInc_PostAction
   Description:      This function will perform any post processing action.  Post processing such as replying 
                     FIX message to sender can be performed in here.  

   Parameters:       argTbl (Table) - argument table contains incoming FIX message data 
                     message_name - incoming fix message name with version number
                     incomingFixTable - table contains incoming FIX message data
                     tran - transaction object returned from OC_Adapter_Trade_Process. 
                     returnTable - table required passing back to oc engine
                     xstring (XString ) - Should populated with error when fail  

   Return Values:    if success - 1 should be returned
                     if fail, error code should be returned and xstring should be populated with error 

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int ProcessFixInc_PostAction(Table argTbl, String message_name, Table incomingFixTable, Transaction tran, Table returnTable, XString xstring) throws OException
   {
      int retval = 1; 

      return retval;              
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
   public int ProcessFixInc_GetReconcilationRequest(Table argTbl, Table returnTable, XString xstring) throws OException {
	      int retval = 1; 
	      return retval;    
   }
   
   private int Get_OL_FixGateway_TranAux_Detail (int tran_num, double price, double quantity, Table returnTbl, XString xstring) throws OException
   {
      double total_price, total_quantity, avg_price = 0;
      String selectClause, whereClause, fromClause = null;
      Table tTranAux = Table.tableNew();        
      int row, retval;

      selectClause = "price, quantity";
      fromClause = "OL_FixGateway_TranAux";
      whereClause = "tran_num =" + tran_num; 

      OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - query_for_tranaction_number: " + " select " + selectClause 
         + "\nfrom " + fromClause 
         + "\nwhere " + whereClause + "\n");

      retval = run_sql_with_retry_for_table(tTranAux, selectClause, fromClause, whereClause);
      if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         tTranAux.destroy();
         Str.xstringAppend(xstring, "Get_OL_FixGateway_TranAux_Detail: failed to retrieve detail from OL_FixGateway_TranAux");
         return 0;
      }

      total_price = price *  java.lang.Math.abs(quantity);
      total_quantity = quantity;

      for (row = 1; row <= tTranAux.getNumRows(); row++)
      {
         total_price += tTranAux.getDouble( "price", row) * java.lang.Math.abs(tTranAux.getDouble( "quantity", row));
         total_quantity += tTranAux.getDouble( "quantity", row);  
      } 

      avg_price = total_price/total_quantity;

      if (returnTbl.getColNum( "total_price") <= 0)
         returnTbl.addCol( "total_price", COL_TYPE_ENUM.COL_DOUBLE);

      if (returnTbl.getColNum( "total_quantity") <= 0)
         returnTbl.addCol( "total_quantity", COL_TYPE_ENUM.COL_DOUBLE);

      if (returnTbl.getColNum( "avg_price") <= 0)
         returnTbl.addCol( "avg_price", COL_TYPE_ENUM.COL_DOUBLE);

      if (returnTbl.getNumRows() <= 0)
         returnTbl.addRow();

      returnTbl.setDouble( "total_price", 1, total_price); 
      returnTbl.setDouble( "total_quantity", 1, total_quantity); 
      returnTbl.setDouble( "avg_price", 1, java.lang.Math.abs(avg_price));  

      tTranAux.destroy(); 
      return 1;
   }

   public int query_for_tranaction_number(String version, String messagetype, String orderid, String transactdate, XString xstring) throws OException
   {
      int retval;
      int tran_num, iTransactDate;
      String dbTranactDate;
      String whereStr, whatStr, fromStr;
      Table tTranQuery = Table.tableNew();

      if(transactdate == null)
         iTransactDate = OCalendar.today();
      else
         iTransactDate = OCalendar.convertYYYYMMDDToJd(transactdate);

      dbTranactDate = OCalendar.formatJdForDbAccess(iTransactDate);  

      whatStr = "tran_num" ;
      fromStr = "OL_FixGateway_TranAux";

      whereStr = " version = '" + version + "'" +
         " and messagetype = '" + messagetype + "'" +
         " and orderid = '" + orderid + "'" + 
         " and transact_date = '" + dbTranactDate + "'" ;

      OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - query_for_tranaction_number: " + " select " + whatStr 
         + "\nfrom " + fromStr 
         + "\nwhere " + whereStr + "\n");

      retval = run_sql_with_retry_for_table(tTranQuery, whatStr, fromStr, whereStr);   
      if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         Str.xstringAppend(xstring, "query_for_tranaction_number: failed to retrieve tran_num from OL_FixGateway_TranAux");
         tTranQuery.destroy(); 
         return -1;
      }

      tran_num = tTranQuery.getInt( "tran_num", 1);

      tTranQuery.destroy();

      return tran_num;        
   }

   public int run_sql_with_retry_for_table(Table data_tbl, String what_str, String from_str, String where_str) throws OException
   {
      int num_tries = 0;
      int retval;
      int total_retries = 15;

      if(Table.isTableValid(data_tbl) == 0)
         return -99;

      if (what_str == null || from_str == null || where_str == null)
         return -99;

      while (num_tries <= total_retries)
      {
         data_tbl.clearRows();

         retval = DBaseTable.loadFromDbWithSQL(data_tbl, what_str, from_str, where_str);
         if (retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         {
            if (retval == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt())
            {
               num_tries++;
               continue;
            }
            else
            { 
               return retval;
            }
         }
         else
            break;
      }

      if (num_tries > total_retries)
      {
         return -98;
      }
      else
         return DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
   }

   public int query_for_ticker_by_ticker(String tickerPattern, String expDate, String exchange, Table ticker_table ) throws OException
   {
      int i, isettltmentDate;
      int retval;
      String tmpMaturityDate;
      String whereStr, whatStr, fromStr;
      fromStr = "ab_tran, header, misc_ins, param_reset_header";

      if(exchange != null)
         fromStr = fromStr + ", party";

      whatStr = "ab_tran.tran_num, header.ticker, ab_tran.toolset, misc_ins.expiration_date, param_reset_header.proj_index" ;
      whereStr = "header.toolset in (" + TOOLSET_ENUM.BONDFUT_TOOLSET.toInt() + "," + TOOLSET_ENUM.COM_FUT_TOOLSET.toInt() + "," + TOOLSET_ENUM.FIN_FUT_TOOLSET.toInt() + "," + TOOLSET_ENUM.DEPFUT_TOOLSET.toInt() + ")" +
         " and header.ticker LIKE '" + tickerPattern + "'";
      if(exchange != null)
      {
         whereStr = whereStr + " and party.short_name = '" + exchange + "'" +
            " and party.party_id = ab_tran.external_bunit ";
      }
      whereStr = whereStr + " and ab_tran.tran_status = " + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() +
         " and ab_tran.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_HOLDING.toInt() + 
         " and header.toolset = ab_tran.toolset " + 
         " and ab_tran.ins_num = header.ins_num  " +
         " and ab_tran.tran_num = header.tran_num " +
         " and misc_ins.ins_num = ab_tran.ins_num  " +
         " and misc_ins.ins_num = header.ins_num  " +
         " and misc_ins.ins_num = param_reset_header.ins_num  " +
         " and param_reset_header.ins_num = header.ins_num " +
         " and param_reset_header.ins_num = ab_tran.ins_num " +
         " and param_reset_header.param_seq_num = 0 " +
         " and param_reset_header.param_reset_header_seq_num = 0";

      OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - query_for_ticker_by_ticker: " + " select " + whatStr 
         + "\nfrom " + fromStr 
         + "\nwhere " + whereStr + "\n");

      retval = run_sql_with_retry_for_table(ticker_table, whatStr, fromStr, whereStr);   
      if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         return retval;
      }

      for(i = ticker_table.getNumRows(); i >0; i--)
      { 
         isettltmentDate = ticker_table.getInt( "expiration_date", i);

         tmpMaturityDate =  OCalendar.formatDateInt(isettltmentDate, DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);
         // -> YYYYMM format
         if(tmpMaturityDate != null)
            tmpMaturityDate = Str.substr(tmpMaturityDate, 0, 6);            

         if (!tmpMaturityDate.equalsIgnoreCase(expDate))
         {
            ticker_table.delRow( i); 
         } 
      } 

      return 1;
   }

   public int query_for_option_by_ticker(String tickerPattern, String expDate, String exchange, String putCall, String strike, Table ticker_table ) throws OException  
   {
      int i, isettltmentDate;
      int retval; 
      String tmpMaturityDate;
      String whereStr, whatStr, fromStr;

      fromStr = "ab_tran ab, header h, misc_ins mi, param_reset_header prh, ins_option o, ins_parameter ins "; 
      if(exchange != null)
         fromStr = fromStr + ", party p";

      whatStr = "ab.tran_num, h.ticker, ab.toolset, mi.expiration_date, prh.proj_index" ;
      whereStr = "  ab.ins_num = h.ins_num and " +
         "  ab.tran_num = h.tran_num and " +
         "  prh.ins_num = h.ins_num and " +
         "  prh.ins_num = ab.ins_num and " +
         "  prh.param_seq_num = 0 and " +
         "  prh.param_reset_header_seq_num = 0 and " +
         "  o.ins_num = h.ins_num and " +
         "  o.ins_num = ab.ins_num and " +                     
         "  mi.ins_num = ab.ins_num and " +
         "  mi.ins_num = o.ins_num and " +
         "  mi.ins_num = h.ins_num and " +
         "  mi.ins_num = ins.ins_num and " +
         "  o.param_seq_num = 0 and " +
         "  o.option_seq_num = 0 and " +
         "  ins.ins_num = h.ins_num and " +
         "  ins.ins_num = ab.ins_num and " +
         "  ins.param_seq_num = 0 " +
         "  and h.toolset in (" + TOOLSET_ENUM.COM_OPT_FUT_TOOLSET.toInt() + "," + TOOLSET_ENUM.OPT_RATE_FUT_TOOLSET.toInt() + ")" +
         "  and h.ticker LIKE '" + tickerPattern + "'";

      if(exchange != null)
      {
         whereStr = whereStr + " and p.short_name = '" + exchange + "'" +
            " and p.party_id = ab.external_bunit ";
      }

      whereStr = whereStr + "  and ab.toolset = h.toolset " +
         "  and ab.tran_status =  "  + TRAN_STATUS_ENUM.TRAN_STATUS_VALIDATED.toInt() +
         "  and ab.tran_type = " + TRAN_TYPE_ENUM.TRAN_TYPE_HOLDING.toInt() +
         "  and o.put_call = " + Ref.getValue(SHM_USR_TABLES_ENUM.PUT_CALL_TABLE, putCall) + 
         "  and ins.rate = " + strike;

      OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - query_for_option_by_ticker: " + " select " + whatStr 
         + "\nfrom " + fromStr 
         + "\nwhere " + whereStr + "\n");

      retval = run_sql_with_retry_for_table(ticker_table, whatStr, fromStr, whereStr);
      if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {		
         return retval;
      }

      for(i = ticker_table.getNumRows(); i >0; i--)
      { 
         isettltmentDate = ticker_table.getInt( "expiration_date", i);

         tmpMaturityDate =  OCalendar.formatDateInt(isettltmentDate, DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);
         // -> YYYYMM format
         if(tmpMaturityDate != null)
            tmpMaturityDate = Str.substr(tmpMaturityDate, 0, 6);            

         if (!tmpMaturityDate.equalsIgnoreCase(expDate))
         {
            ticker_table.delRow( i); 
         } 
      }    

      return 1;
   }

   public String get_qualified_trader(String internalBUnit, String internalContact) throws OException  
   { 	
      String qualified_trader = null;
      String whereStr, whatStr, fromStr = null;
      int retval;
      Table pp_table = null;

      if(internalContact == null)
      {
         qualified_trader = Ref.getUserName();
      }
      else
      {

         pp_table = Table.tableNew("Trader Table"); 

         whatStr = "pl.name" ;

         fromStr = "personnel pl, party_personnel pp, party p"; 

         whereStr = " p.short_name = '" + internalBUnit + "' " +
            "  and p.party_status = 1 " +
            "  and p.party_id = pp.party_id " +
            "  and pp.personnel_id = pl.id_number " +
            "  and pl.personnel_type >0 " +
            "  and pl.name = '" + internalContact + "'"; 

         OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - get_qualified_trader: " + " select " + whatStr 
            + "\nfrom " + fromStr 
            + "\nwhere " + whereStr + "\n");

         retval = run_sql_with_retry_for_table(pp_table, whatStr, fromStr, whereStr); 
         if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         {
            pp_table.destroy();
            return qualified_trader;
         }

         if (pp_table.getNumRows() > 0)
         {
            qualified_trader = internalContact;
         }
         else
         {
            qualified_trader = Ref.getUserName();
         }
         pp_table.destroy();
      }

      return qualified_trader;
   }

   public String get_qualified_execBroker(String execBroker) throws OException
   { 	
      String qualified_execBroker = null;
      String whereStr, whatStr, fromStr = null;
      int retval;
      Table pp_table = null;

      if(execBroker != null)
      {

         pp_table = Table.tableNew("execBroker Table"); 

         whatStr = "p.short_name" ;

         fromStr = "party p, party_function pf"; 

         whereStr = " p.short_name = '" + execBroker + "' " +
            "  and p.party_status = 1 " +
            "  and pf.party_id = p.party_id " +
            "  and pf.function_type = 6 ";				//Execution Broking 

         OConsole.oprint("\nFIXCustomInboundProcessFIXInclude - get_qualified_execBroker: " + " select " + whatStr 
            + "\nfrom " + fromStr 
            + "\nwhere " + whereStr + "\n");

         retval = run_sql_with_retry_for_table(pp_table, whatStr, fromStr, whereStr);
         if ( retval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         {
            pp_table.destroy();
            return qualified_execBroker;
         }

         if (pp_table.getNumRows() > 0)
         {
            qualified_execBroker = execBroker;
         } 

         pp_table.destroy();
      }

      return qualified_execBroker;
   }

}
