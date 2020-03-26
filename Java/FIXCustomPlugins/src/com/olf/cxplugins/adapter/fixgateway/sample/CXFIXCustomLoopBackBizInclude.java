package com.olf.cxplugins.adapter.fixgateway.sample;
import com.olf.cxplugins.adapter.fixgateway.ICXFIXCustomBizInclude;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
      Name       : CXFIXCustom_BizInclude.java

      Description: The script provides a framework for the user to add all the  
      custom logics for "CXFIXTradePreProcess", "CXFIXTradePostProcess", "CXFIXOutput"
      and "CXFIXGeneration"

      TODO section should be modified to reflect the user defined scripts.

      Release    : Original 6/1/2007
*/ 

public class CXFIXCustomLoopBackBizInclude implements ICXFIXCustomBizInclude
{
   public int RETURNED_SIGNED = 11;
   public int INCOMPLETE = 15;
   public int OUTBOUND = 16;
   public int INCOMPLETE_CANCEL = 18;
   public int CANCELLED = 23;
   public int CLOSED_CANCEL = 25;
   public int CANCELLED_DOC_STATUS = 8;	

   public int INTERNAL_OUTPUT_ERROR_FIVETIMES = 101;
   public int INTERNAL_OUTPUT_ERROR_RUNTWOTIMES = 102;

   public int ERROR_CODE_NO_TRANSACTION		= 0;
   public int ERROR_CODE_DB_ERROR			= 1;
   public int ERROR_CODE_NON_DESTINATED_CANDIDATE = 2;

   public String CANCELLED_MSG = "CANCELLED: Newer Deal Version Exists";
   public String CLOSED_CANCEL_MSG = "CANCELLED: At FIX";
   public String OC_FAILED_STR  = "BLOCKED: The document failed more than two times in OC, should be submitted manually.";
   public String OC_BLOCKED_STR = "BLOCKED: The document has been output five times without any response from FIX gateway"; 

   public int external_Designation_None	= 0;
   public int external_Designation_Fix	= 1; 	 
   public String external_Designation_None_Label   = "Not Designated";
   public String external_Designation_Fix_Label	 = "FIX Designated"; 
   public String external_Status_New_Label            = "New";
   public String external_Status_Change_Label         = "Change";
   public String external_Status_Cancelled_Label      = "Cancelled";
   public String external_Status_Generated_Label      = "Generated"; 
   public String ExtDesignatedFieldName   = "Ext-Designations";
   public String ExtStatusFieldName = "Ext-Status";  
   String errMsg;

   /*-------------------------------------------------------------------------------
   Name:             BizInc_MapToObject
   Description:      This function will be invoked by CXFIXPostPushTrade script to 
                     perform all the push activities by oc engine.  

                     if success - 1 should be returned
                     if fail, error code -1 should be returned and xstring should be populated with error 

   Parameters:       argTbl  (Table) - Mapped tradebuilder table for corresponding transaction
                     returnTbl (Table) - return table with structure expected by oc engine. 
                     xstring (XString ) - Should populated with error when fail  

   Remark:		      User can call ConnexUtility.scriptGetTran() to get the Transaction pointer instead calling Transaction.retrieve function

   Return Values:    1  = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_MapToObject (Table argTbl, Table returnTbl, XString xstring) throws OException
   {
      int retVal = 1; 

      Transaction tran = null;	 
      Table copyArgTbl, tranfTable, tranInfoTbl;
      String tran_num = null; 

      if (argTbl.getNumRows() < 1) 
      {     
    	  OConsole.oprint ("argTbl returned no data!");
    	  Str.xstringAppend(xstring, "argTbl returned no data!"); 
    	  return -1;
      }
      else
      {		
    	  if (argTbl.getColNum( "tb:tradeBuilder") <= 0)
    	  {
    		  Str.xstringAppend(xstring, "Unable to retrieve tb:tradeField structure"); 
    		  return -1;
    	  }

    	  tranfTable = argTbl.getTable( "tb:tradeBuilder", 1);

    	  tran_num = ConnexUtility.tradeBuilderGetField(tranfTable, TRANF_FIELD.TRANF_TRAN_NUM.toInt(), 0, ConnexUtility.getTradeBuilderFieldName( TRANF_FIELD.TRANF_TRAN_NUM.toInt()), -1, -1, -1, -1, xstring); 
    	  if (Str.strToInt(tran_num) <= 0)
    	  {
    		  Str.xstringAppend(xstring, "Invalid tran_num specified in tran_num column of argTbl table."); 
    		  return -1;
    	  }

    	  tran = ConnexUtility.scriptGetTran();
    	  if(tran == null)
    	  { 
    		  Str.xstringAppend(xstring, "Unable to retrieve transaction structure for tran_num = " + tran_num ); 
    		  return -1;
    	  }		 

    	  tranInfoTbl = tran.getTranInfo();
    	  retVal = is_tran_designated(tran, tranInfoTbl);
    	  if (retVal < 0)
    	  {
    		  /* Database Failure */ 
    		  Str.xstringAppend(xstring, "Unable to call is_tran_designated ."); 
    		  return -1;
    	  }
    	  else if (retVal == 0)
    	  { 
    		  Str.xstringAppend(xstring, "Transaction is not an FIX Candidate."); 
    		  return -1;
    	  }

    	  copyArgTbl = argTbl.copyTable();
    	  retVal = Util.runScript("FIXSTDPushTrade", copyArgTbl, null);
    	  if(retVal <= 0)
    	  { 	
    		  Str.xstringAppend(xstring, "FIXSTDPushTrade process failed:" + copyArgTbl.getString( "errorMsg", 1)); 
    		  return -1;
    	  }

    	  retVal = returnTbl.addCol( "FIXMessages", COL_TYPE_ENUM.COL_TABLE); 
    	  retVal = copyArgTbl.copyCol("FIXMessages", returnTbl, "FIXMessages"); 

    	  copyArgTbl.destroy();
      }   
         
      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_UpdateMapToObjectStatus 
   Description:      This function will be invoked by CXFIXPostPushTrade script to 
                     update document status.  

                     User should call "StlDoc.updateOutputReturnStatus" and/or "StlDoc.processDocToStatus" 
                     to update document based on the passing documentStatus accordingly.

   Parameters:       argTbl (Table) - argument table 
                     returnTbl (Table) - return table with structure expected by oc engine. 
                     errorcode (int) - error code
                     error (String) - error description 

   Document information can be obtain from argTbl
                     column name/type/description		
                     tb:tradeField/COL_TYPE_ENUM.COL_TABLE/mapped tradebuilder table
                     column name/type/description		
                     tran_num/COL_TYPE_ENUM.COL_INT/transaction number 
                     doc_id/COL_TYPE_ENUM.COL_INT/document number
                     doc_op_seqnum/COL_TYPE_ENUM.COL_INT/document sequence
                     doc_version/COL_TYPE_ENUM.COL_INT/document version  

   Remark:		      User can call ConnexUtility.scriptGetTran() to get the Transaction pointer instead calling Transaction.retrieve function

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_UpdateMapToObjectStatus (Table argTbl, Table returnTbl, int errorcode, String error) throws OException
   { 
      int retVal = 1;
      Table requestTbl;
      int documentId, documentOpSeqNum = 0;

      requestTbl = ConnexUtility.scriptGetRequestArgTable();

      documentId = 0;
      if (requestTbl.getColNum( "doc_id") > 0) 
    	  documentId = requestTbl.getInt( "doc_id", 1);	 


      documentOpSeqNum = 0;
      if (requestTbl.getColNum( "doc_op_seqnum") > 0)
    	  documentOpSeqNum = requestTbl.getInt( "doc_op_seqnum", 1); 

      if (errorcode != 1)
      {
    	  retVal = StlDoc.updateOutputReturnStatus(documentId, 
    			  documentOpSeqNum, 15, error);
      }  

      return retVal;
   } 

   /*-------------------------------------------------------------------------------
   Name:             BizInc_TradePreProcessing
   Description:      This function will be invoked by CXFIXTradePreProcess script to 
   determine whether the deal is to be retrieved by the Settlement desktop 
   for confirmation matching by the third party systems.

   Suggestions can be put on here are: 
                     i) Determine if the deal is a third party candidate (has right to send the deal).
                     ii)Determine amendment flag of the deal.  Check if the deal needs to be resent based 
                     on changes to the systems' specified required fields.
                     iii)Call BizInc_IsTradeDesignated to determine if the trade was designated to be processed.
                     iii)Set traninfo or user_table_field to indicate its candidate which can be used by 
                     PostProcess.

   Parameters:       Table argTbl - 
                     column name/type/description
                     tran/COL_TYPE_ENUM.COL_TRAN/new transaction pointer
                     oldtran/COL_TYPE_ENUM.COL_TRAN/old transaction pointer
                     amendmentFlag/COL_TYPE_ENUM.COL_INT/deal amendment indication

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_TradePreProcessing (Table argTbl) throws OException
   {
      Transaction tran, old_tran;
      int retVal = 1;
      int tradeDesignatedId = 0;
      String tradeDesignatedTag = null;

      tran = argTbl.getTran( "tran", 1);
      old_tran = argTbl.getTran( "oldtran", 1);

      tradeDesignatedId = BizInc_IsTradeDesignated(tran);  
      if (tradeDesignatedId <= 0)
      {
         errMsg = "Transaction is not an FIX Candidate.";
         OConsole.oprint(errMsg);
         return tradeDesignatedId;
      }

      tradeDesignatedTag = BizInc_TagDesignatedTrade(tradeDesignatedId);

      update_tran_traninfo(tran, ExtDesignatedFieldName, tradeDesignatedTag); 
      update_tran_traninfo(tran, ExtStatusFieldName, external_Status_New_Label);

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_IsTradeDesignated  
   Description:      This function will be invoked by function BizInc_TradePreProcessing to
                     determine whether the deal is to be designated to be processed by the Settlement desktop.

                     Suggestions can be put on here are: 
                     i) You can return enumerator on its candiate 
                     ii)or You can return true for designated or false if not

   Parameters:       Transaction* tran - transaction pointer 

   Return Values:    user-defined int
                     = 0 for not designated
                     < 0 for error

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_IsTradeDesignated (Transaction tran) throws OException
   {
      int retVal = 0; 
      retVal = external_Designation_Fix;	

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_TagDesignatedTrade  
   Description:      This function will be invoked by function BizInc_TradePreProcessing to
                     return the tag for designated trade based on return value
                     from BizInc_IsTradeDesignated routine.

   Parameters:       tradeDesignatedId (int) - return value from BizInc_IsTradeDesignated

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public String BizInc_TagDesignatedTrade (int tradeDesignatedId) throws OException
   {					   
      String tagDesignatedTag = null;

      if (tradeDesignatedId == external_Designation_None)
         tagDesignatedTag = external_Designation_None_Label;
      else if (tradeDesignatedId == external_Designation_Fix)
         tagDesignatedTag = external_Designation_Fix_Label; 

      return tagDesignatedTag;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Check_Confirm_Eligibility
   Description:      This function will be invoked by CXFIXTradePostProcess script to 
                     determine eligibility of associated transaction for confirmation. 

                     Suggestions input are: 
                        i) Look at traninfo or user_table_field for any indication of confirm
                        eligibility set previously in BizInc_TradePreProcessing		 

   Parameters:       Transaction* tran - transaction pointer  

   Return Values:    1 = eligible
                     0 = not eligble

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_Check_Confirm_Eligibility (Transaction tran) throws OException
   {
      int retVal = 1;
      Table tTranInfo = null;
      int iFoundRow = 0;
      String externalStatus = null; 

      if(tran == null)
         return retVal;
 
      tTranInfo = tran.getTranInfo();
      if(tTranInfo == null)
    	  return retVal; 

      tTranInfo.group( "Type");

      iFoundRow = tTranInfo.findString( "Type", ExtStatusFieldName, SEARCH_ENUM.FIRST_IN_GROUP);
      if (iFoundRow < 0)
    	  return retVal;

      externalStatus = tTranInfo.getString( "Value", iFoundRow);

      if (externalStatus.equalsIgnoreCase("external_Status_New_Label")
    		  ||  externalStatus.equalsIgnoreCase("external_Status_Change_Label"))
      {
    	  retVal = 1;
      }    
      else if(external_Status_Generated_Label.equalsIgnoreCase(externalStatus))
      {
    	  if (tran.getFieldInt( TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0, "", -1, -1, -1) == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED.toInt() ||
    			  tran.getFieldInt( TRANF_FIELD.TRANF_TRAN_STATUS.toInt(), 0, "", -1, -1, -1) == TRAN_STATUS_ENUM.TRAN_STATUS_CANCELLED_NEW.toInt())
    	  {
    		  update_tran_traninfo(tran, ExtStatusFieldName, external_Status_Cancelled_Label);
    	  }
      }		 

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Retrieve_Settlement_Definition
   Description:      This function will be invoked by CXFIXTradePostProcess script to 
                     get the settlement definition name on each record found in incoming 
                     dealInfo table.

                     User should populate the settlement definition name under SettlementDefinition
                     column.  

   Parameters:       dealInfo (Table) - information of the deal 			

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_Retrieve_Settlement_Definition(Table dealInfo) throws OException
   {
      int row = 0;
      int retVal = 1;
      String definition_name = null; 
      int toolsetId, instrumenttype = 0;

      for (row = 1; row <= dealInfo.getNumRows(); row++)
      {	
         toolsetId = dealInfo.getInt( "toolset", row);

         if(toolsetId == TOOLSET_ENUM.FIN_FUT_TOOLSET.toInt())
            definition_name = "FIX_Definition";

         dealInfo.setString( "SettlementDefinition", row, definition_name);
      } 

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Retrieve_SDOutputMethodName
   Description:      This function will be invoked by CXFIXOutput script to 
                     get the method name of the push method which will be triggered
                     when the document was sent from settlement desktop. 

                     Parameters:       documentNumber (int) - document number
                     documentSeq (int) - document sequence
                     documentVersion (int) - document version 
                     tran_num (int) - document sequence 

   Return Values:    method_name (String)

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public String BizInc_Retrieve_SDOutputMethodName (int documentNumber, int documentSeq, int documentVersion, int tran_num) throws OException
   {
      String method_name = null; 

      Transaction tran = null;
      int toolsetId = 0;
 
      tran = Transaction.retrieve(tran_num);	
      if(tran == null)
      { 
    	  return method_name;
      }
      toolsetId = tran.getFieldInt( TRANF_FIELD.TRANF_TOOLSET_ID.toInt(), 0, "", -1, -1, -1);

      if(toolsetId == TOOLSET_ENUM.FIN_FUT_TOOLSET.toInt())
    	  method_name = "pushFIXTrade";

      return method_name;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Retreive_DocumentOutputState
   Description:      This function will be invoked by CXFIXOutput script to 
                     icons_enum.check if the document fulfills business rules 
                     for outputting to third party systems.  

   Parameters:       documentNumber (int) - document number
                     documentSeq (int) - document sequence
                     documentVersion (int) - document version 
                     tran_num (int) - document sequence 

   Return Values:    <=0 fail
                     > 0 document status

   Expected result of Return Values
                     i) documentStatus <= 0 - Fail status when TriggerPush was called
                     ii) documentStatus > 0 - 
                     1 = TriggerPush will be called
                     >1 = document will be updated in BizInc_Update_SDOutputStatus

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_Retreive_DocumentOutputState (int documentNumber, int documentSeq, int documentVersion, int tran_num) throws OException
   {
      int  document_status = 1;  

      int retVal = 0;
      Table tDocInfo;
 
      tDocInfo  = Table.tableNew("StlDoc Output"); 

      retVal = retrieve_document_info( documentNumber, documentVersion, tDocInfo);
      if ( retVal != 1)
      {
    	  tDocInfo.destroy();
    	  return -1;
      }    

      if ( tDocInfo.getNumRows() > 0 ) 
      {
    	  //Check if OUTBOUND more than 5 times
    	  retVal =  check_document_output_multiple_times(tDocInfo);
    	  if(retVal == 1)
    	  {
    		  if ( retVal == -1 )
    			  OConsole.oprint("Error outputting document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq);
    		  else 
    		  {
    			  OConsole.oprint("Cannot resend document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq +  OC_BLOCKED_STR);

    			  retVal = INTERNAL_OUTPUT_ERROR_FIVETIMES;
    		  }

    		  tDocInfo.destroy();
    		  return retVal;
    	  } 

    	  //Check INCOMPLETE state, cannot run more than 2 times automatically
    	  retVal =  check_document_in_error_state(tDocInfo);
    	  if(retVal == 1)
    	  {
    		  if ( retVal == -1 )
    			  OConsole.oprint("Error outputting document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq);
    		  else 
    		  {
    			  OConsole.oprint("Cannot resend document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq +  OC_FAILED_STR);

    			  retVal = INTERNAL_OUTPUT_ERROR_RUNTWOTIMES;
    		  }

    		  tDocInfo.destroy();
    		  return retVal;
    	  }

    	  //Check CANCELLED/CLOSED_CANCEL state
    	  retVal = check_document_is_cancelled (tDocInfo);
    	  if(retVal == 1)
    	  {
    		  if ( retVal == -1 )
    			  OConsole.oprint("Error outputting document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq);

    		  else 
    		  {
    			  if ( retVal == CLOSED_CANCEL)
    			  {
    				  retVal = CLOSED_CANCEL;
    			  }
    			  else
    			  {
    				  retVal = CANCELLED;

    			  }
    			  OConsole.oprint("Cannot resend document number " + documentNumber + "\t" + "output seq num = "
    					  + documentSeq + ", the document has been Cancelled .");
    		  }

    		  tDocInfo.destroy();
    		  return retVal;
    	  }
      }

      tDocInfo.destroy(); 

      //Check OPEN/CANCEL/AMENDED/AMENDED OPEN on deal > current tran number
      retVal = cancel_if_old_deal_version( tran_num, documentNumber, documentSeq);
      if(retVal == 1)
      {
    	  if ( retVal == -1 )
    		  OConsole.oprint("Error outputting document number " + documentNumber + "\t" + "output seq num = "
    				  + documentSeq);
    	  else 
    		  OConsole.oprint("Cannot resend document number " + documentNumber + "\t" + "output seq num = "
    				  + documentSeq + ", newer deal version exists , the document has been Cancelled .");

    	  return retVal;
      } 

      return document_status;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Update_SDGenerationStatus
   Description:      This function will be invoked by CXFIXGeneration script to 
                     update the field(s) in the table to signal the trade was already
                     been processed by settlement desktop and should not be picked up 
                     by it.

   Parameters:       tran_num (int) - transaction number

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_Update_SDGenerationStatus(int tran_num) throws OException
   {
      int retVal = 1;
      int iFoundRow; 
      Table tTranInfo = null;
      String currentStatus = null;
      Transaction tran = null;

      tran = Transaction.retrieve(tran_num);	
      if(tran == null)
      {
         OConsole.oprint("\nFIX_SDGeneration error: Invalid transaction on tran# " + tran_num + "\n");
         return 1;
      }

      tTranInfo = tran.getTranInfo();
      if(tTranInfo == null)
      {
         OConsole.oprint("\nFIX_SDGeneration error: Cannot get traninfo table on tran# " + tran_num + "\n");
         return 1;
      }
      tTranInfo.group( "Type");

      iFoundRow = tTranInfo.findString( "Type", ExtStatusFieldName, SEARCH_ENUM.FIRST_IN_GROUP);
      if (iFoundRow < 0)
      {
         OConsole.oprint("\nFIX_SDGeneration error: Cannot retrieve current status from traninfo table on tran# " + tran_num + "\n");
         return 1;
      }
      currentStatus = tTranInfo.getString( "Value", iFoundRow);
      retVal = update_tran_traninfo(tran, ExtStatusFieldName, external_Status_Generated_Label); 

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_Update_SDOutputStatus
   Description:      This function will be invoked by CXFIXOutput script to 
                     update document status.  

                     User should call "StlDoc.updateOutputReturnStatus" and/or "StlDoc.processDocToStatus" 
                     to update document based on the passing documentStatus accordingly.

                     Expected result of documentStatus
                     i) documentStatus <= 0 - Fail status when TriggerPush was called
                     ii) documentStatus > 0 - Required to determine if it is op_services_run_id from triggerPush
                     or document status returned from BizInc_Retreive_DocumentOutputState

   Parameters:       documentNumber (int) - document number
                     documentSeq (int) - document sequence
                     documentVersion (int) - document version 
                     tran_num (int) - document sequence
                     documentStatus (int) - document status
                     documentReference (String) - document reference/error

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_Update_SDOutputStatus(int documentNumber, int documentSeq, int documentVersion, int tran_num, int documentStatus, String documentReference) throws OException
   { 
      int retVal = 1;

      int docStatus = 0;
      String docReference = null;

      if(documentStatus == 0){			
         docStatus = INCOMPLETE;
         docReference = documentReference;
      }
      else if(documentStatus == INTERNAL_OUTPUT_ERROR_FIVETIMES){ 
         docStatus = INCOMPLETE_CANCEL;
         docReference = OC_BLOCKED_STR;
      }
      else if(documentStatus == INTERNAL_OUTPUT_ERROR_RUNTWOTIMES){ 
         docStatus = INCOMPLETE_CANCEL;
         docReference = OC_FAILED_STR;
      }
      else if(documentStatus == CLOSED_CANCEL){ 
         docStatus = CLOSED_CANCEL;
         docReference = CLOSED_CANCEL_MSG;
      }
      else if(documentStatus == CANCELLED){
         docStatus = CANCELLED;
         docReference = CANCELLED_MSG;
      }
      else{
         docStatus = OUTBOUND;
         docReference = "SENT TO GATEWAY";
      }

      retVal = StlDoc.updateOutputReturnStatus(documentNumber, documentSeq, docStatus, docReference);

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_SDOutputPostProcessing
   Description:      This function will be invoked by CXFIXOutput script to 
                     perform post-processing for clean up purpose.

   Parameters:       documentNumber (int) - document number
                     documentSeq (int) - document sequence
                     documentVersion (int) - document version 
                     tran_num (int) - document sequence  

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_SDOutputPostProcessing (int documentNumber, int documentSeq, int documentVersion, int tran_num) throws OException
   {
      int retVal = 1; 
      retVal = cancel_if_old_deal_version( tran_num, documentNumber, documentSeq);      
      return retVal;
   }

   public int update_tran_traninfo(Transaction tran, String tranInfoFieldName, String tranInfoValue) throws OException
   {
      int foundRow;
      int retVal;
      Table tranInfoTbl;
      String currentFieldValue;

      if(tran == null)
         return 0;

      tranInfoTbl = tran.getTranInfo();
      if(tranInfoTbl == null)
         return 0;

      tranInfoTbl.group( "Type");

      foundRow = tranInfoTbl.findString( "Type", tranInfoFieldName, SEARCH_ENUM.FIRST_IN_GROUP);
      if (foundRow < 0)
         return 0;

      currentFieldValue = tranInfoTbl.getString( "Value", foundRow);

      OConsole.oprint("FIX_TradePreProcess: Updating traninfo field " + tranInfoFieldName + " from value " + currentFieldValue + " to value " + tranInfoValue + "\n");
      retVal = tran.setField( TRANF_FIELD.TRANF_TRAN_INFO.toInt(), 0, tranInfoFieldName, tranInfoValue, 0, 0, -1, -1 );

      return retVal;
   } 

   public int check_document_already_released(Table tDocInfo) throws OException
   {
      Table tTempValues;
      int iAlreadySent = 0;
      int iRetval, iPrevSentTime,iServerTime;
      String sWhat;
      String sWhere = null;
      ODateTime pSentTime;
      String sErrorMsg;
      tTempValues = Table.tableNew("Stldoc Output Table");

      sWhat  = "return_date";
      sWhere += "return_status EQ " + OUTBOUND;

      iRetval = tTempValues.select( tDocInfo, sWhat, sWhere);
      if ( iRetval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         tTempValues.destroy();
         sErrorMsg = " Error retrieving documents having outbound status.";
         OConsole.oprint(sErrorMsg);
         return -1;
      }

      tTempValues.sortCol( "return_date");

      if ( tTempValues.getNumRows() > 0 )
      {
         pSentTime = tTempValues.getDateTime( 1, tTempValues.getNumRows());
         if ( pSentTime.getDate() == OCalendar.getServerDate() )
         {
            iPrevSentTime = pSentTime.getTime();
            iServerTime = Util.timeGetServerTime();  

            if ( iServerTime - iPrevSentTime   <= 300 )
               iAlreadySent = 1;
         }
      }

      tTempValues.destroy();

      return iAlreadySent;

   }

   public int check_document_is_cancelled(Table tDocInfo) throws OException
   {
      Table tTempValues;
      int iCancelled  = 0;
      int iReturnStatus, iRow;

      tTempValues = Table.tableNew("Stldoc Output Table");
      tDocInfo.sortCol( "return_date");

      for ( iRow = tDocInfo.getNumRows(); iRow > 0  ; iRow-- )
      {
         iReturnStatus  = tDocInfo.getInt( 1, iRow );
         if ( iReturnStatus == CANCELLED ||  iReturnStatus == CLOSED_CANCEL )
         {
            iCancelled = iReturnStatus;
            break;
         }
      }

      tTempValues.destroy();

      return iCancelled ; 
   }

   public int check_document_output_multiple_times(Table tDocInfo) throws OException
   {
      int iErrState = 0;
      int iRow, iReturnStatus;
      int iErrCount = 0;

      tDocInfo.sortCol( "return_date");
      if ( tDocInfo.getNumRows() > 5 )
      {
         for ( iRow = tDocInfo.getNumRows(); iRow > 0  ; iRow-- )
         {
            iReturnStatus  = tDocInfo.getInt( 1, iRow );
            if ( iReturnStatus == OUTBOUND )
            {
               iErrCount++;
               if ( iErrCount > 5)
               {
                  iErrState = 1;
                  break;
               }
            }
            else
            { 
               iErrState = 0;
               break;
            }
         } 
      }
      return iErrState;
   }

   public int check_document_in_error_state(Table tDocInfo) throws OException
   {
      int iErrState = 0;
      int iRow, iReturnStatus;
      int iErrCount = 0;

      tDocInfo.sortCol( "return_date");
      if ( tDocInfo.getNumRows() > 2 )
      {
         for ( iRow = tDocInfo.getNumRows(); iRow > 0  ; iRow-- )
         {
            iReturnStatus  = tDocInfo.getInt( 1, iRow );
            if ( iReturnStatus == INCOMPLETE )
            {
               iErrCount++;
               if ( iErrCount > 2)
               {
                  iErrState = 1;
                  break;
               }
            }
            else
            { 
               iErrState = 0;
               break;
            }
         } 
      }
      return iErrState;
   }


   public int retrieve_document_info(int iDocumentNumber, int iDocumentVersion,  Table tDocInfo) throws OException
   {

      String sQuery;
      int iRetval; 

      sQuery   = "select  return_status, return_date from stldoc_output_log op";
      sQuery  += " where op.document_num = " + iDocumentNumber + " and op.doc_version = " + iDocumentVersion;
      sQuery  += " and op.output_type_id = 99";
      iRetval = DBase.runSql (sQuery);

      if ( iRetval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         errMsg = " Error during retrieval of stldoc_output_log " + DBUserTable.dbRetrieveErrorInfo(iRetval, "");
         OConsole.oprint(errMsg);
         return -1;
      }

      DBase.createTableOfQueryResults (tDocInfo);     
      return 1;

   }

   public int cancel_if_old_deal_version( int tran_num, int iDocumentNum, int iOutputSeqNum) throws OException
   {	
      int iRetval;
      Table tDocument = null;
      String sWhat = "sd.document_num, sd.doc_version";
      String sFrom = "stldoc_details sd";
      String sWhere = "sd.tran_num > " + tran_num + " and sd.deal_tracking_num = (select distinct deal_tracking_num from stldoc_details where tran_num =" + tran_num + ")";
      sWhere += " and sd.event_type in (2, 10, 19, 21)";

      tDocument = Table.tableNew("Document Table");

      iRetval = DBaseTable.loadFromDbWithSQL(tDocument, sWhat, sFrom, sWhere);        
      if(iRetval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         OConsole.oprint("Could not retrieve document num for amendments for Tran Num: " + tran_num);
         tDocument.destroy();
         return -1;
      }

      if ( tDocument.getNumRows() <= 0 )
      {
         tDocument.destroy();
         return 1;
      }

      StlDoc.updateOutputReturnStatus(iDocumentNum, iOutputSeqNum, CANCELLED, CANCELLED_MSG);
      StlDoc.processDocToStatus(iDocumentNum, CANCELLED_DOC_STATUS);

      tDocument.destroy();
      return 1;        
   }

   public int cancel_previous_documents_for_amendment(int tran_num, int iDocumentNum) throws OException
   {
      int iRow, iSeqNum, iRetval;
      int iReturnStatus = 0;
      Table tDocument = null;
      String sWhat = "so.document_num, so.doc_version, so.doc_output_seqnum, so.return_status";
      String sFrom = "stldoc_details sd, stldoc_output_log so";
      String sWhere = "sd.tran_num <= " + tran_num + " and sd.deal_tracking_num = (select distinct deal_tracking_num from stldoc_details where tran_num =" + tran_num + ")";
      sWhere += " and sd.event_type in (2, 10, 19, 21) and  sd.document_num =  so.document_num and sd.doc_version = so.doc_version and sd.document_num != " + iDocumentNum;
      sWhere += " and so.doc_output_seqnum = (select Math.max(doc_output_seqnum)  from stldoc_output_log where sd.document_num = document_num and sd.doc_version = doc_version)";

      tDocument    = Table.tableNew("Document Table");

      iRetval = DBaseTable.loadFromDbWithSQL(tDocument, sWhat, sFrom, sWhere);        
      if(iRetval != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         OConsole.oprint("Could not retrieve document num for amendments for Tran Num: " + tran_num);
         tDocument.destroy();
         return -1;
      }

      for ( iRow = 1; iRow <= tDocument.getNumRows(); iRow++) 
      {
         iSeqNum = tDocument.getInt( 3, iRow);
         iReturnStatus = tDocument.getInt( 4, iRow);

         if ( iSeqNum > 0 && iReturnStatus != CANCELLED && iReturnStatus != CLOSED_CANCEL) 
         {
            iDocumentNum = tDocument.getInt( 1, iRow);
            StlDoc.updateOutputReturnStatus(iDocumentNum, iSeqNum, CANCELLED, CANCELLED_MSG);
            StlDoc.processDocToStatus(iDocumentNum, CANCELLED_DOC_STATUS);
         }

      }
      return iReturnStatus;    
   }

   public String retrieve_property_value(String propName) throws OException
   {
      Table tGatewayProperties;
      String returnStr;
      String GATEWAY_NAME = "FIXGateway";
      String FIELD_COL = "pfield_name";
      String VALUE_COL = "pfield_value";
      int iFoundRow;

      tGatewayProperties = ConnexUtility.retrieveGatewayProperties( GATEWAY_NAME, 1);
      if(tGatewayProperties == null)
      {
         OConsole.oprint("Could not retrieve properties for Gateway: " + GATEWAY_NAME);
         return "";
      }

      tGatewayProperties.group( FIELD_COL);
      iFoundRow = tGatewayProperties.findString( FIELD_COL, propName, SEARCH_ENUM.FIRST_IN_GROUP);
      if (iFoundRow < 0)
      {
         OConsole.oprint("Could not retrieve " + propName + " property from " + GATEWAY_NAME + " Gateway properties.\n");
         tGatewayProperties.destroy();
         return "";
      }

      returnStr = tGatewayProperties.getString( VALUE_COL, iFoundRow);
      if(returnStr == null)
      {
         OConsole.oprint("FIX_TradePostProcess: Could not retrieve " + propName + " property. Value is empty.\n");
         tGatewayProperties.destroy();
         return "";
      }
      tGatewayProperties.destroy();

      return returnStr;
   }

   public int is_tran_designated(Transaction tran, Table tranInfoTbl)
   {
      return 1;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_UpdStatusFromExtSystem
   Description:      This function will be invoked by CXFIXUpdStatusFromExtSys script to 
                     perform any updating activities based upon the return
                     code and description from the gateway.  

                     User required to populate complete_flag field to 1 if successful update status occurred or 
                     update with error code if it fails.

                     if success - 1 should be returned
                     if fail, error code -1 should be returned and xstring should be populated with error 

   Parameters:       gateway (String) - name of gateway
                     auxTbl  (Table) - multiple row table with the following structure
                     column name/type/description		
                     opservice_id/COL_TYPE_ENUM.COL_STRING/opservice id
                     return_status/COL_TYPE_ENUM.COL_STRING/return code from external system.
                     return_code/COL_TYPE_ENUM.COL_INT/return code from external system.
                     description/COL_TYPE_ENUM.COL_STRING/description returned from push activity
                     client_tag/COL_TYPE_ENUM.COL_STRING/custom tag.  Usually, it contains the document information such as doc_id, doc_op_seqnum, doc_version 
                     complete_flag/COL_TYPE_ENUM.COL_INT/completion flag on each row update.  1 = success, else failure.

                     xstring (XString ) - Should populated with error when fail  

   Return Values:    1 = Success
                     0 <= Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_UpdStatusFromExtSystem (String gateway, Table auxTbl, XString xstring) throws OException
   { 
      int retVal = 1;
      int row;
      int iDocumentNumber, iDocOutputSeqNum, iDocStatus;
      int iDocVersion = 0;
      String sDocDescription; 

      String clienttag = null;
      int pos = 0;
 
      for ( row = 1; row <= auxTbl.getNumRows(); row++)
      {
    	  clienttag = auxTbl.getString( "client_tag", row);
    	  if(clienttag != null)
    	  {
    		  pos = Str.findSubString(clienttag, ".");
    		  if ( pos == -1 )
    		  {
    			  iDocumentNumber = Str.strToInt(clienttag);
    		  }
    		  else
    		  {
    			  iDocumentNumber = Str.strToInt(Str.substr(clienttag, 0, pos));
    			  clienttag = Str.substr(clienttag, pos+1, Str.len(clienttag) - (pos+1));
    			  pos = Str.findSubString(clienttag, ".");

    			  if ( pos == -1 )
    				  iDocOutputSeqNum = Str.strToInt(clienttag);
    			  else
    			  {
    				  iDocOutputSeqNum = Str.strToInt(Str.substr(clienttag, 0, pos));
    				  clienttag = Str.substr(clienttag, pos+1, Str.len(clienttag) - (pos+1));
    				  iDocVersion = Str.strToInt(clienttag);
    			  }
    		  }

    		  /* for message failed for mapping errors, version is not returned by FIX, use 1 as default */
    		  if ( iDocVersion <= 0 )
    			  iDocVersion = 1;

    		  sDocDescription = auxTbl.getString( "description", row);

    		  iDocStatus = get_return_status_from_desc(sDocDescription);
    		  if (iDocStatus <= 0)
    		  {	
    			  Str.xstringAppend(xstring, "No document status found on description:" + sDocDescription); 
    			  return -1;
    		  } 

    		  iDocOutputSeqNum = get_document_opsequence_num(iDocumentNumber, iDocVersion);
    		  if ( iDocOutputSeqNum == -1 )
    		  {
    			  Str.xstringAppend(xstring, "Output SeqNum Not Found For Document Number: " + iDocumentNumber + " iDocVersion: " + iDocVersion); 
    			  return -1;
    		  }

    		  retVal =  StlDoc.updateOutputReturnStatus(iDocumentNumber, iDocOutputSeqNum, iDocStatus, sDocDescription);
    		  if (retVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
    		  {
    			  Str.xstringAppend(xstring, "StlDoc.updateOutputReturnStatus falure: on Document Number: " + iDocumentNumber +
    					  " DocVersion: " + iDocVersion + " OutputSeqNum: "+ iDocOutputSeqNum); 
    			  return retVal;
    		  }
    	  }
      } 

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:              BizInc_UpdStatusLogActivity
   Description: 	  This function will be invoked by BizInc_UpdStatusFromExtSystem script to 
			   		  perform optional testing logging activity.  
			
			   		  Since each gateway has its unique way to define if it is running the testing mode, so
			   		  user required to return correct testing mode status.
			
			   		  if success, 1 should be returned if it is in testing mode and 0 if it is not in testing mode
			   		  if fail, -1 should be returned if it is running in error  

   Parameters: 	  gateway (String) - name of gateway
	   		  	  auxTbl  (Table) - multiple row table with the following structure
	   			  column name/type/description		
	   			  opservice_id/COL_TYPE_ENUM.COL_STRING/opservice id
	   			  testing_mode/COL_TYPE_ENUM.COL_INT/testing_mode (optional)
	   			  return_status/COL_TYPE_ENUM.COL_STRING/return code from external system.
	   			  return_code/COL_TYPE_ENUM.COL_INT/return code from external system.
	   			  description/COL_TYPE_ENUM.COL_STRING/description returned from push activity
	   			  client_tag/COL_TYPE_ENUM.COL_STRING/custom tag.  Usually, it contains the document information such as doc_id, doc_op_seqnum, doc_version 
	   			  complete_flag/COL_TYPE_ENUM.COL_INT/completion flag on each row update.  1 = sucess, else failure.

   		  		  xstring (XString ) - Should populated with error when fail  

   Return Values:    1 = Success
   		  0 = Fail

   Release    : Original 2/11/2008
   -------------------------------------------------------------------------------*/
   public int BizInc_UpdStatusLogActivity (Table argTbl, XString xstring)
   {
	   int retVal = 0;

	   /* TODO - Add custom logic here */ 
	   /* END TODO */

	   return retVal;
   }
   
   public int get_document_opsequence_num(int iDocumentNumber, int iDocumentVersion) throws OException
   {
      Table tTempValues;
      String sWhat, sFrom, sWhere;
      int iOpSeqNum = -1;
      int iRetVal;

      tTempValues  = Table.tableNew("Stldoc Output Table");
      sWhat  = "Math.max(op.doc_output_seqnum)";
      sFrom  = "stldoc_output_log op";
      sWhere = "op.document_num = " + iDocumentNumber;
      sWhere += " and op.doc_version = " + iDocumentVersion;

      iRetVal = run_sql_with_retry_for_table(tTempValues,  sWhat, sFrom, sWhere);
      if ( iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
      {
         tTempValues.destroy();
         return -1;
      }

      if ( tTempValues.getNumRows() > 0 )
         iOpSeqNum =  tTempValues.getInt( 1, 1);

      tTempValues.destroy();
      return iOpSeqNum;
   }


   public int run_sql_with_retry_for_table(Table tDataValues, String sWhat, String sFrom, String sWhere) throws OException
   {
      int iNumTries = 0;
      int iRetVal;
      int iTotalRetries = 15;

      if(tDataValues == null)
         return -99;

      if (sWhat == null || sFrom == null || sWhere == null)
         return -99;

      while (iNumTries <= iTotalRetries)
      {
         tDataValues.clearRows();
         iRetVal = DBaseTable.loadFromDbWithSQL(tDataValues, sWhat, sFrom, sWhere);
         if (iRetVal != DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt())
         {
            if (iRetVal == DB_RETURN_CODE.SYB_RETURN_DB_RETRYABLE_ERROR.toInt())
            {
               iNumTries++;
               continue;
            }
            else
            {
               /* Not a retryable error. */
               return iRetVal;
            }
         }
         else
            break;
      }

      if (iNumTries > iTotalRetries)
      {
         return -98;
      }
      else
         return DB_RETURN_CODE.SYB_RETURN_SUCCEED.toInt();
   }

   public int get_return_status_from_desc(String return_status_desc) throws OException
   {
      String select, from, where = null; 
      int return_int = 2;
      Table temp = null;
      select = "return_status";
      from = "stldoc_return_status";
      where = "return_status_desc = '" + return_status_desc + "'";

      temp = Table.tableNew();

      DBaseTable.loadFromDbWithSQL(temp, select, from, where);

      if(temp.getNumRows() > 0) 
         return_int = temp.getInt( "return_status", 1); 

      temp.destroy();
      return return_int;
   }
}
