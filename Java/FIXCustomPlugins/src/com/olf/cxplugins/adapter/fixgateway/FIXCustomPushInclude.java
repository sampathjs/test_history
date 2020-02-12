package com.olf.cxplugins.adapter.fixgateway;
import com.olf.cxplugins.adapter.fixgateway.FIXSTDHelperInclude;
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

public class FIXCustomPushInclude implements IFIXCustomPushInclude 
{

	protected FIXSTDHelperInclude m_FIXSTDHelperInclude;

   public FIXCustomPushInclude() {
      m_FIXSTDHelperInclude = new FIXSTDHelperInclude();
   }


   /*-------------------------------------------------------------------------------
   Name:             PushInc_RunScriptExit 

   Description:      This function will be invoked by FIXSTDPushTrade script perform script exiting activity.		 

   Parameters:       argTbl  (Table) - system argument table passed.
                     returnTbl (Table) - table contained FIX message expected by gateway. (return from m_FIXSTDHelperInclude.HelperInc_BuildMessage)
                     errorcode (int) - error code
                     error (String) - error description 

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int PushInc_RunScriptExit(Table argTbl, Table returnTbl, int errCode, String errMsg) throws OException
   {
      int retVal = 1; 	

      /* TODO - Add custom logic here */ 
      /* END TODO */

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

                     xstring (XString ) - Should populated with error when fail  

   Return Values:    if success - Table pointer contained connection detail with selected field
                     if fail - Util.NULL_TABLE will be returned, xstring should be populated with error 

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public Table PushInc_PushTradeProcessing (Table argTbl, Table connexInfo, Table gatewayDetail, Table tranfTbl, Table connectionDetailTbl, String sender, String target, XString xstring) throws OException
   {
      Table pushTradeTbl = Util.NULL_TABLE; 
      Table requestTbl = Util.NULL_TABLE;
      Table documentTbl, targetTbl, dataTbl = Util.NULL_TABLE;
      Table fixGtwTranAuxTable;

      int documentId, documentOpSeqNum, documentVersionNum = 0;
      String service_id = null;

      Table senderPtr = Util.NULL_TABLE;
      String senderId, senderSubId, senderLocationId; 
      String targetVersion, targetId, targetSubId, targetLocationId; 

      String olfTranNum = null;
      int num_rows, row, msg_row;

      /* TODO - Get documentId, documentVersionNum, documentOpSeqNum */
      /* When the deal was triggered by Settlement Desktop, it will contain document information inside the request argument table 
      The following section will get the document information from the table */
      requestTbl = ConnexUtility.scriptGetRequestArgTable();

      documentId = 0;
      if (requestTbl.getColNum( "doc_id") > 0) 
         documentId = requestTbl.getInt( "doc_id", 1);	 

      documentVersionNum = 0;
      if (requestTbl.getColNum( "doc_version") > 0)
         documentVersionNum = requestTbl.getInt( "doc_version", 1); 

      documentOpSeqNum = 0;
      if (requestTbl.getColNum( "doc_op_seqnum") > 0)
         documentOpSeqNum = requestTbl.getInt( "doc_op_seqnum", 1); 		

      /* END TODO - Get documentId, documentVersionNum, documentOpSeqNum */

      /* Get job_id to identify the deal in FIX message */
      service_id = connexInfo.getString( "job_id", 1);
      if(Str.isEmpty(service_id) != 0)
      {
         service_id = requestTbl.getString( "opservice_logid", 1);
         if(Str.isEmpty(service_id) != 0)
         {	
            Str.xstringAppend(xstring, "PushInc_PushTradeProcessing: fatal error - service_id not found.");
            return Util.NULL_TABLE;
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
      senderSubId = senderPtr.getString( "SenderSubId", 1);
      senderLocationId = senderPtr.getString( "SenderLocationId", 1);

      /* Call  m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table to build FIX message table */
      pushTradeTbl = m_FIXSTDHelperInclude.HelperInc_Create_Messages_Table(); 

      /* Gather deal information from tranfTbl using m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField function
      /* TODO - Gather deal information */
      /* Sample codes using m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField */
      //olfDealNum = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_DEAL_TRACKING_NUM, xstring);
      //olfTranNum = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TRAN_NUM, xstring);
      //olfInstrumentType = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_INS_TYPE, xstring); 
      //olfToolset = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_TOOLSET_ID, xstring);
      //olfPosition = m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField(tranfTbl, TRANF_FIELD.TRANF_POSITION, xstring);
      /* END Sample codes using m_FIXSTDHelperInclude.HelperInc_TradeBuilderGetField */		
      /* END TODO - Gather information */

      /* Gather user-table information to build the reference.
      The reference will be populated as part of FIX message for future query/hospitalization.
      Please see user-table section in installation manual how to custom and use the user-table */  
      /* Sample codes how to build reference */
      ////Call m_FIXSTDHelperInclude.HelperInc_Get_UserTable_On_Tran to get user-table for assoicated transaction
      //fixGtwTranAuxTable = m_FIXSTDHelperInclude.HelperInc_Get_UserTable_On_Tran(olfTranNum);
      //if(Table.isTableValid(fixGtwTranAuxTable) != 0)
      //{
      //	if (fixGtwTranAuxTable.getColNum( "version") > 0 && Str.isNotEmpty(fixGtwTranAuxTable.getString( "version", 1)))
      //		reference = reference + "fix_version=" + fixGtwTranAuxTable.getString( "version", 1);
      //		
      //	if (fixGtwTranAuxTable.getColNum( "messagetype") > 0 && Str.isNotEmpty(fixGtwTranAuxTable.getString( "messagetype", 1)))
      //		reference = reference + "+message_type=" + fixGtwTranAuxTable.getString( "messagetype", 1);
      //
      //	if (fixGtwTranAuxTable.getColNum( "clordid") > 0 && Str.isNotEmpty(fixGtwTranAuxTable.getString( "clordid", 1)))
      //		reference = reference + "+clordid=" + fixGtwTranAuxTable.getString( "clordid", 1);
      //
      //	if (fixGtwTranAuxTable.getColNum( "execid") > 0 && Str.isNotEmpty(fixGtwTranAuxTable.getString( "execid", 1)))
      //		reference = reference + "+execid=" + fixGtwTranAuxTable.getString( "execid", 1);
      //
      //	if (fixGtwTranAuxTable.getColNum( "orderid") > 0 && Str.isNotEmpty(fixGtwTranAuxTable.getString( "orderid", 1)))
      //		reference = reference + "+orderid=" + fixGtwTranAuxTable.getString( "orderid", 1);  
      //}  
      //fixGtwTranAuxTable.destroy();
      /* END Sample codes how to build reference */		
      /* TODO - Gather user-table field */

      /* After data gathering, we can start building up the FIX data */
      //Call m_FIXSTDHelperInclude.HelperInc_Create_Data_Table to get the table to host FIX data 
      dataTbl = m_FIXSTDHelperInclude.HelperInc_Create_Data_Table(); 

      /* Use m_FIXSTDHelperInclude.HelperInc_Add_TagValue to add the tag/value to dataTbl */
      /* Usage:  m_FIXSTDHelperInclude.HelperInc_Add_TagValue (dataTbl, tag, value, data type, is security field);
      dataTbl = table created by m_FIXSTDHelperInclude.HelperInc_Create_Data_Table();
      tag = FIX tag
      value = FIX value
      data type = COL_TYPE_ENUM.COL_STRING or COL_TYPE_ENUM.COL_DATE_TIME  
      if COL_TYPE_ENUM.COL_DATE_TIME is being used and value was left blank, current system date time was used
      */

      /* TODO - Add FIX tag/value to dataTbl - user should be responsible for the building FIX message 
      conform with FIX specification */
      /* Sample codes how to build tag/value  */
      //m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "37", olfTranNum, COL_TYPE_ENUM.COL_STRING, "0");
      //m_FIXSTDHelperInclude.HelperInc_Add_TagValue(dataTbl, "60", "", COL_TYPE_ENUM.COL_DATE_TIME, "0");	
      /* END Sample codes how to build  tag/value */ 
      /* END TODO - Add FIX tag/value to dataTbl */


      //Call m_FIXSTDHelperInclude.HelperInc_Create_Target_Table to get the table to host FIX target 
      targetTbl = m_FIXSTDHelperInclude.HelperInc_Create_Target_Table();

      /* TODO - Add FIX target - for the same FIX message, the FIXGateway allows to send more than
      one FIX target.  Below is the sample code to add FIX target to target table. 
      You can uncomment the code and modify the existing code to fix your requirements. 

      targetId, targetSubId, targetLocationId were obtained from connectionDetailTbl
      Call m_FIXSTDHelperInclude.HelperInc_Add_Target to add the target information to targetTbl */		 
      //num_rows = connectionDetailTbl.getNumRows();
      //for (row = 1; row <= num_rows; row++)
      //{			 	
	  //		targetId = null;
	  //		targetSubId = null;
	  //	targetLocationId = null;
	  //	targetVersion = null;
	  //			
	  //	if (connectionDetailTbl.getColNum("compid") > 0)
	  //		targetId = connectionDetailTbl.getString( "compid", row); 
	  //	if (connectionDetailTbl.getColNum("subid") > 0)
	  //		targetSubId = connectionDetailTbl.getString( "subid", row); 
	  //	if (connectionDetailTbl.getColNum("locationid") > 0)
	  //		targetLocationId = connectionDetailTbl.getString( "locationid", row); 
	  //	if (connectionDetailTbl.getColNum("fixversion") > 0)
	  //		targetVersion = connectionDetailTbl.getString( "fixversion", row); 
	  //
	  //	m_FIXSTDHelperInclude.HelperInc_Add_Target(targetTbl, targetVersion, targetId, targetSubId, targetLocationId);
	  //}	
      /* END TODO - Add FIX target */

      //Call m_FIXSTDHelperInclude.HelperInc_Create_Document_Table to get the table to host FIX document 
      if (documentId > 0)
      {
         documentTbl = m_FIXSTDHelperInclude.HelperInc_Create_Document_Table(documentId, documentOpSeqNum, documentVersionNum); 
      }

      //Call m_FIXSTDHelperInclude.HelperInc_Add_Message to finalize FIX message into FIX table 
      /* TODO - Add FIX Message - messagetype should be populated with FIX message type */		 
      //m_FIXSTDHelperInclude.HelperInc_Add_Message (pushTradeTbl, "", "O", messagetype, service_id, reference, 
      //		       senderId, senderSubId, senderLocationId, 
      //		       documentTbl, targetTbl, dataTbl);
      /* END TODO - Add FIX Message */

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
      Table connectDetail = Util.NULL_TABLE; 	

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
      if (senderParty == null || senderParty.trim().length() == 0)
      {
    	  throw new OException("FIXSTDPushTrade.java: FIX.Application.Sender cannnot be found.");
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
