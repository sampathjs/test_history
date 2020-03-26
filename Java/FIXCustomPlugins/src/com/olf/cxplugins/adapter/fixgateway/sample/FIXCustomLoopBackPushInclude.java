package com.olf.cxplugins.adapter.fixgateway.sample;

import com.olf.cxplugins.adapter.fixgateway.FIXSTDHelperInclude;
import com.olf.cxplugins.adapter.fixgateway.IFIXCustomPushInclude;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;


/*
      Name       :   FIXCustomPushInclude.java

      Description:   The script provides a framework for the user to add custom logics 
                     on push trade when FIXSTDPushTrade was used. 

                     User should reference this script as part of INCLUDE script to invoke
                     any built-in functions listed here.

      Release    :   Original 6/1/2007
*/ 

public class FIXCustomLoopBackPushInclude implements IFIXCustomPushInclude
{
   private FIXSTDHelperInclude m_FIXSTDHelperInclude;

   public FIXCustomLoopBackPushInclude(){
      m_FIXSTDHelperInclude = new FIXSTDHelperInclude();
   }

   /*-------------------------------------------------------------------------------
   Name:             PushInc_RunScriptExit 

   Description:      This function will be invoked by FIXSTDPushTrade script perform script exiting activity.		 

   Parameters:       argTbl  (Table) - system argument table passed.
                     returnTbl (Table) - table contained FIX message expected by gateway. (return from m_FIXSTDHelperInclude.HelperInc_BuildMessage)
                     errorcode (int) - error code
                     error (String) - error description 

   Return Values:    1 =  Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int PushInc_RunScriptExit(Table argTbl, Table returnTbl, int errCode, String errMsg) throws OException
   {
      int retVal = 0; 	

      if (argTbl.getColNum( "errorCode") <= 0)
         argTbl.addCol( "errorCode", COL_TYPE_ENUM.COL_INT); 
      if (argTbl.getColNum( "errorMsg") <= 0)
         argTbl.addCol( "errorMsg", COL_TYPE_ENUM.COL_STRING); 

      if (errCode > 0)
      {
         argTbl.setTable( 1, 1, null);  
         argTbl.setInt( "errorCode", 1, errCode);
         argTbl.setString( "errorMsg", 1, errMsg);
         ConnexUtility.setReturnStatus(errCode, errMsg, null);
      }
      else
      {
         returnTbl.copyCol( 1, argTbl, 1);
         argTbl.setColName( 1, returnTbl.getColName( 1));
         ConnexUtility.setReturnTable(returnTbl.copyTable(), OC_DATATYPES_ENUM.OC_DATATYPE_CUSTOM.toInt());
      }
      retVal = 1;
      return retVal; 
   }

   /*-------------------------------------------------------------------------------
   Name:             PushInc_PushTradeProcessing 

   Description:      This function will be invoked by FIXSTDPushTrade script to build the FIX table 
                     based on the deal information.  

   Parameters:       argTbl  (Table) - system argument table passed.
                     connexInfo (Table) - table contained from ConnexUtility.scriptGetRequestDetailsTable  
                     gatewayDetail (Table - Table contained the name of Gateway, Name of Protocal,
                     Complete connection setup in method configuration

                     column name/type/description			  
                     GatewaySystem/COL_TYPE_ENUM.COL_TABLE/Name of connected Gateway 
                     GatewayProtocol/COL_TYPE_ENUM.COL_TABLE/Name of connected Protocal 
                     GatewayConnection/COL_TYPE_ENUM.COL_TABLE/Complete connection setup in method configuration  
                     tranfTbl (Table) - Mapped tradebuilder table for corresponding transaction
                     connectionDetailTbl (Table) - custom connection detail table returned from PushInc_GetConnectionDetail
                     sender (String) - FIX sender seperated by slash contains SenderId/SenderSubId/SenderLocationId
					 target (String) - FIX target seperated by slash contains TargetId/TargetSubId/TargetLocationId
                     xstring (XString ) - Should populated with error when fail  

   Return Values:    if success - Table pointer contained connection detail with selected field
                     if fail - Util.NULL_TABLE will be returned, xstring should be populated with error 

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public Table PushInc_PushTradeProcessing (Table argTbl, Table connexInfo, Table gatewayDetail, Table tranfTbl, Table connectionDetailTbl, String sender, String target, XString xstring) throws OException
   {
      ODateTime trade_time = ODateTime.dtNew(); 
      int currenttime;

      Table pushTradeTbl = null; 
      Table requestTbl = null;
      Table documentTbl = null;
      Table targetTbl, dataTbl = null;
      Table fixGtwTranAuxTable;

      int documentId = 0, documentOpSeqNum = 0, documentVersionNum = 0;
      String service_id = null;

      Table senderPtr = null;
      String senderId = null, senderSubId = null, senderLocationId = null; 
      String targetVersion = null, targetId = null, targetSubId = null, targetLocationId = null;

      String olfTranNum = null;
      int num_rows, row;

      int totalMessageType; 

      String olfIntPortfolio, olfExtPortfolio;
      String olfDealNum = null, olfInstrumentType = null, olfProduct = null, olfToolset = null, olfBuySell = null;
      String olfPosition, olfTradeDateTime, olfTradeDate, olfTradeTime, olfPrice, olfBroker, olfCusip, olfTicker;
      String reference = null;
      String olfBaseCurrency, olfSettleDate, olfMaturityDate;
      String olfTrader; 

      int olfToolsetId = 0;
      double olfPriceInDouble = 0; 
      int olfOrderStatus;
      String outputMsgType;
      int olfMaturityDateJd;
      String olfTranStatus;
      String olfInternalBUnit, olfExternalBUnit;
      int retVal = 0;

      /* TODO - Get documentId, documentVersionNum, documentOpSeqNum */
      /* When the deal was triggered by Settlement Desktop, it will contain document information inside the request argument table 
      The following section will get the document information from the table */
      requestTbl = ConnexUtility.scriptGetRequestArgTable();

      if (requestTbl != null && requestTbl.getNumRows() > 0)
      {
         if (requestTbl.getColNum( "doc_id") > 0)
            documentId = requestTbl.getInt( "doc_id", 1);

         if (requestTbl.getColNum( "doc_version") > 0)
            documentVersionNum = requestTbl.getInt( "doc_version", 1);

         if (requestTbl.getColNum( "doc_op_seqnum") > 0)
            documentOpSeqNum = requestTbl.getInt( "doc_op_seqnum", 1);
      }
      /* END TODO - Get documentId, documentVersionNum, documentOpSeqNum */

      /* Get job_id to identify the deal in FIX message */
      service_id = connexInfo.getString( "job_id", 1);
      if(service_id == null)
      {	
         service_id = requestTbl.getString( "opservice_logid", 1);
         if(service_id == null)
         {	
            Str.xstringAppend(xstring, "PushInc_PushTradeProcessing: fatal error - service_id not found.");
            return null;
         }
      }

      /* Call m_FIXSTDHelperInclude.HelperInc_ParseSender to break the sender into senderId, sendersubid, senderlocationid fields
      for FIX message. sender must be seperated by slash in order to get the correct value */		
      senderPtr = m_FIXSTDHelperInclude.HelperInc_ParseSender(sender);
      if (senderPtr == null)
      {
         Str.xstringAppend(xstring, "PushInc_PushTradeProcessing: failed in FixHelper_ParseSender.");
         return null;
      }

      senderId = senderPtr.getString( "SenderId", 1);
      if (senderPtr.getString( "SenderSubId", 1) != null)
            senderSubId = senderPtr.getString( "SenderSubId", 1);
      if (senderPtr.getString( "SenderLocationId", 1) != null)
            senderLocationId = senderPtr.getString( "SenderLocationId", 1);

      String defaultOutgoingMessageType = "ExecutionReport"; // default message type
	  String tranOutgoingMessageType = "";
      // look for tran reference field for outgoing message type
      if (tranfTbl != null && tranfTbl.getNumRows() > 0)
      {
		 Table tradeField = tranfTbl.getTable("tb:tradeField", 1);
		 int tbNameCol = tradeField.getColNum("tb:name"); 
		 int tbValCol = tradeField.getColNum("tb:value");
		 int refRow = tradeField.unsortedFindString(tbNameCol, "Reference", SEARCH_CASE_ENUM.CASE_INSENSITIVE );

		 if (refRow > 0)
		 {
			tranOutgoingMessageType = tradeField.getString(tbValCol, refRow);
		 }		 

         if (tranOutgoingMessageType.equalsIgnoreCase("8") || tranOutgoingMessageType.equalsIgnoreCase("ExecutionReport") ||
             tranOutgoingMessageType.equalsIgnoreCase("D") || tranOutgoingMessageType.equalsIgnoreCase("NewOrderSingle")) 
         {
            defaultOutgoingMessageType = tranOutgoingMessageType;
         }
      }
      else
      {
         Str.xstringAppend(xstring, "PushInc_PushTradeProcessing: trade builder table is empty.");
         return null;
      }

      /* Call  m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table to build FIX message table */
      pushTradeTbl = m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table(); 	

      /* Gather deal information from tranfTbl using m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField function */
      olfTranStatus = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), xstring);
      olfDealNum = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_DEAL_TRACKING_NUM.toInt(), xstring);
      olfTranNum = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TRAN_NUM.toInt(), xstring);
      olfInstrumentType = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_INS_TYPE.toInt(), xstring); 
      olfToolset = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), xstring);
      olfPosition = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_POSITION.toInt(), xstring);
      olfInternalBUnit = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_INTERNAL_BUNIT.toInt(), xstring);
      olfExternalBUnit = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_EXTERNAL_BUNIT.toInt(),  xstring);
      olfIntPortfolio = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_INTERNAL_PORTFOLIO.toInt(), xstring);
      olfExtPortfolio = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_EXTERNAL_PORTFOLIO.toInt(), xstring);
      olfPrice = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_PRICE.toInt(), xstring);
      olfCusip = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_CUSIP.toInt(), xstring);
      olfTicker = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TICKER.toInt(), xstring);
      olfBroker = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_BROKER_ID.toInt(), xstring);
      olfTrader = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_INTERNAL_CONTACT.toInt(), xstring);      //Internal Contact = trader 

      //example to use m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetFieldInt
      olfToolsetId = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetFieldInt(tranfTbl, TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), xstring);
      //example to use m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetFieldDouble
      olfPriceInDouble = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetFieldDouble(tranfTbl, TRANF_FIELD.TRANF_PRICE.toInt(), xstring);

      olfBaseCurrency = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_BASE_CURRENCY.toInt(), xstring); 
      olfSettleDate = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_SETTLE_DATE.toInt(), xstring);
      olfMaturityDate = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_MAT_DATE.toInt(), xstring);

      olfTradeDate = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TRADE_DATE.toInt(), xstring);
      olfTradeTime = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TRADE_TIME.toInt(), xstring); 

      if (olfPosition != null) {
      if (Str.strToInt(olfPosition)>0)
         olfBuySell = "1";
      else
         olfBuySell = "2"; 
      }

      // user table is used to track deals booked with incoming FIX messages
      if (DBUserTable.userTableIsValid("ol_fixgateway_tranaux") == 1)
      {
          fixGtwTranAuxTable = m_FIXSTDHelperInclude.HelperInc_Get_UserTable_On_Tran(olfTranNum);
          if(Table.isTableValid(fixGtwTranAuxTable) != 0)
          {
            if (fixGtwTranAuxTable.getNumRows() > 0)
            {
                reference = "";
                if (fixGtwTranAuxTable.getColNum( "version") > 0 && (fixGtwTranAuxTable.getString( "version", 1)) != null)
                    reference = reference + "fix_version=" + fixGtwTranAuxTable.getString( "version", 1);
                    
                if (fixGtwTranAuxTable.getColNum( "messagetype") > 0 && (fixGtwTranAuxTable.getString( "messagetype", 1)) != null)
                    reference = reference + "+message_type=" + fixGtwTranAuxTable.getString( "messagetype", 1);
                    
                if (fixGtwTranAuxTable.getColNum( "clordid") > 0 && (fixGtwTranAuxTable.getString( "clordid", 1)) != null)
                    reference = reference + "+clordid=" + fixGtwTranAuxTable.getString( "clordid", 1);
                    
                if (fixGtwTranAuxTable.getColNum( "execid") > 0 && (fixGtwTranAuxTable.getString( "execid", 1)) != null)
                    reference = reference + "+execid=" + fixGtwTranAuxTable.getString( "execid", 1);
                     
                if (fixGtwTranAuxTable.getColNum( "orderid") > 0 && (fixGtwTranAuxTable.getString( "orderid", 1)) != null)
                    reference = reference + "+orderid=" + fixGtwTranAuxTable.getString( "orderid", 1);  
            }
            fixGtwTranAuxTable.destroy();
          }
      }
      
      //Convert mm/dd/yyyy -> yyymmdd
      olfTradeDate  = OCalendar.formatDateInt(OCalendar.parseString(olfTradeDate), DATE_FORMAT.DATE_FORMAT_MDSY_SLASH, DATE_LOCALE.DATE_LOCALE_US);   	
      olfTradeDateTime = olfTradeDate + " " + olfTradeTime; 
      trade_time = ODateTime.strToDateTime(olfTradeDateTime); 
      olfTradeTime = Str.dtToString(trade_time, DATE_FORMAT.DATE_FORMAT_ISO8601.toInt(), TIME_FORMAT.TIME_FORMAT_HMS24_END.toInt());
      trade_time.destroy(); 

      olfTradeDate = Str.substr(olfTradeTime, 0, 8);
      olfTradeTime = Str.substr(olfTradeTime, Str.len(olfTradeTime) - 8, 8);

      olfSettleDate  = OCalendar.formatDateInt(OCalendar.parseString(olfSettleDate), DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);   	
      olfMaturityDate =  OCalendar.formatDateInt(OCalendar.parseString(olfMaturityDate), DATE_FORMAT.DATE_FORMAT_ISO8601, DATE_LOCALE.DATE_LOCALE_US);
      // -> YYYYMM format
      if(olfMaturityDate != null)
         olfMaturityDate = Str.substr(olfMaturityDate, 0, 6);		

      dataTbl = m_FIXSTDHelperInclude.HelperInc_Create_Data_Table(); 

      //Required field
      if (defaultOutgoingMessageType.equalsIgnoreCase("8") || defaultOutgoingMessageType.equalsIgnoreCase("ExecutionReport")) 
      {
         currenttime = Util.timeGetServerTime(); 

         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "37", olfTranNum, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//OrderId - is required field for ExecutionRpt.  However, we are not booking any trade here, so default to TRANNUM
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "17", olfTranNum + Str.intToStr(currenttime), COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ExecId 

         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "39", olfTranStatus, COL_TYPE_ENUM.COL_STRING.toInt(), "0");			//OrdStatus
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "150", olfTranStatus, COL_TYPE_ENUM.COL_STRING.toInt(), "0");			//ExecType = status report = I
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "20", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//ExecTransType

         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "6", olfPrice, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//AvgPx
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "14", olfPosition, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//CumQty = orderqty since no qty left
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "151", "0", COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//leave qty = orderqty - cumqty 

         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "75", olfTradeDate, COL_TYPE_ENUM.COL_DATE_TIME.toInt(), "0");				//TradeDate    
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "200", olfMaturityDate, COL_TYPE_ENUM.COL_STRING.toInt(), "0");				//MaturityMonthYear   
      }
      else if (defaultOutgoingMessageType.equalsIgnoreCase("D") || defaultOutgoingMessageType.equalsIgnoreCase("NewOrderSingle")) 
      {
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "11", olfTranNum, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ClOrdID

         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "40", "1", COL_TYPE_ENUM.COL_STRING.toInt(), "0");          //ORDERTYPE = MARKET 
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "44", olfPrice, COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//Price 		
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "21", "3", COL_TYPE_ENUM.COL_STRING.toInt(), "0");							//HandlInstr = manual order		
      }

      //Common fields
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "54", olfBuySell, COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//Side 
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "15", olfBaseCurrency, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Currency
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "120", olfBaseCurrency, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Settlement currency
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "38", olfPosition, COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//OrderQty

      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "60", olfTradeDate + "-" + olfTradeTime, COL_TYPE_ENUM.COL_STRING.toInt(), "0");			//TransactTime
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "64", olfSettleDate, COL_TYPE_ENUM.COL_DATE_TIME.toInt(), "0");					//FutSettleDate   

      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "439", olfInternalBUnit, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ClearingFirm
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "440", olfIntPortfolio, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ClearingAccount

      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "109", olfExternalBUnit, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//ClientID
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "1", olfExtPortfolio, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Account
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "76", olfBroker, COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//ExecBroker	 	

      //Symbol, SecurityType, and MaturityMonthYear are required for future
      if(olfCusip != null)
      {
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "48", olfCusip, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Security Id
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "22", "1", COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//IDSOURCE : 1 = CUSIP, 2 = SEDOL, 3 = QUIK, 4 = ISIN number, 5 = RIC code, 6 = ISO Currency Code
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "55", "[N/A]", COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//Symbol
      }
      else
      {
         m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "55", olfTicker, COL_TYPE_ENUM.COL_STRING.toInt(), "0");						//Symbol
      }
      m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "167", olfInstrumentType, COL_TYPE_ENUM.COL_STRING.toInt(), "0");					//security type 

      targetTbl = m_FIXSTDHelperInclude.HelperInc_Create_Target_Table();
      num_rows = connectionDetailTbl.getNumRows();

      String isInitiator;
      for (row = 1; row <= num_rows; row++)
      {
         isInitiator = connectionDetailTbl.getString("isInitiator", row);
         if (isInitiator != null && isInitiator.equalsIgnoreCase("Y"))
         {
            targetId = connectionDetailTbl.getString( "TargetCompID", row); 
            targetSubId = connectionDetailTbl.getString( "TargetSubID", row); 
            targetLocationId = connectionDetailTbl.getString( "TargetLocationID", row); 
            targetVersion = connectionDetailTbl.getString( "Fixversion", row); 

            if (Str.findSubString(targetVersion, "FIX.") == -1 )
               targetVersion = "FIX." + targetVersion;

            m_FIXSTDHelperInclude.HelperInc_Add_Target(targetTbl, targetVersion, targetId, targetSubId, targetLocationId);
         }
      }

      if (documentId > 0)
      {
         documentTbl = m_FIXSTDHelperInclude.HelperInc_Create_Document_Table(documentId, documentOpSeqNum, documentVersionNum); 
      }

      m_FIXSTDHelperInclude.HelperInc_Add_Message (pushTradeTbl, "", "O", defaultOutgoingMessageType, service_id, reference, 
         senderId, senderSubId, senderLocationId, 
         documentTbl, targetTbl, dataTbl);

      return pushTradeTbl;
   }

   /*-------------------------------------------------------------------------------
   Name:             PushInc_GetConnectionDetail 

   Description:      This function will be invoked by FIXSTDPushTrade script to get information from connection detail.
                     By default, selectClause was passed in as compid, subid, locationid, fixversion
                     and whereClauses was passed in as isbuyer EQ true.

                     You can customize the selectClause and whereClause in TODO section to obtain any additional fields
                     from connection table for associated method.

   Parameters:       gatewayDetail  (Table) - Complete gateway detail table

   Return Values:    if success - Table pointer contained connection detail with selected field
                     if fail, Util.NULL_TABLE will be returned

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public Table PushInc_GetConnectionDetail (Table gatewayDetail) throws OException
   {
      Table connectDetail = null; 	
      
      /* TODO - Add custom logic here */
      Table connection  = gatewayDetail.getTable( "GatewayConnection", 1); 
      connectDetail = connection.copyTable();
      /* END TODO */
      
      return connectDetail; 
   }

   /*-------------------------------------------------------------------------------
   Name:             PushInc_GetSenderParty		  

   Description:      This function will be invoked by FIXSTDPushTrade script to get sender information.
                     This function is optional for user to modify it since by default, the sender information 
                     should be obtained by gateway property.

                     You can customize the TODO section to obtain sender information.  
                     Sender information should be delimited by slash on SenderId/SenderSubId/SenderLocationId

   Parameters:       argTbl  (Table) - incoming argument table (trade information)
                     connexInfo (Table ) - table contained from ConnexUtility.scriptGetRequestDetailsTable  
                     gatewayDetail  (Table) - Complete gateway detail table      

   Return Values:    if success - sender (String) will be returned.
                     if fail, NULL_STRING will be returned.

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public String PushInc_GetSenderParty (Table argTbl, Table connexInfo, Table gatewayDetail) throws OException
   {
	   String senderParty = null; 

	   /* TODO - Add custom logic here */
	   String gatewaySystem = gatewayDetail.getString( "GatewaySystem", 1);      
	   senderParty = m_FIXSTDHelperInclude.HelperInc_Retrieve_GatewayProperty(gatewaySystem, "FIX.Application.Sender");
	   
	   // Default sender party to 'PartyA', so that user doesn't have to specify on the gateway property.
	   // This is the party to use with the receiver simulator when FIX Gateway is an initiator.
	   if (senderParty == null || senderParty.trim().length() == 0)
	   {
		   senderParty = "PartyA";
	   }
	   /* END TODO */

	   return senderParty; 
   }
   
   /*-------------------------------------------------------------------------------
   Name:             PushInc_GetTargetParty		  

   Description:      This function will be invoked by FIXSTDPushTrade script to get target information.
                     This function is optional for user to modify it since by default, the target information 
                     should be obtained by gateway property.

                     You can customize the TODO section to obtain target information.  
                     Target information should be delimited by slash on TargetId/TargetSubId/GergetLocationId

   Parameters:       argTbl  (Table) - incoming argument table (trade information)
                     connexInfo (Table ) - table contained from ConnexUtility.scriptGetRequestDetailsTable 
                     gatewayDetail  (Table) - Complete gateway detail table       

   Return Values:    if success - sender (String) will be returned.
                     if fail, NULL_STRING will be returned.

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public String PushInc_GetTargetParty (Table argTbl, Table connexInfo, Table gatewayDetail) throws OException
   {
	      String targetParty = null;

	      /* TODO - Add custom logic here */ 

	      /* END TODO */

	      return targetParty; 
   }
}

