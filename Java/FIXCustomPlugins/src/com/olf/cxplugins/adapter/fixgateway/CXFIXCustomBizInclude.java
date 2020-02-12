package com.olf.cxplugins.adapter.fixgateway;
import com.olf.openjvs.*; 

/*
      Name       : CXFIXCustom_BizInclude.java

      Description: The script provides a framework for the user to add all the  
      custom logics for "CXFIXTradePreProcess", "CXFIXTradePostProcess", "CXFIXOutput"
      and "CXFIXGeneration"

      TODO section should be modified to reflect the user defined scripts.

      Release    : Original 6/1/2007
*/ 

public class CXFIXCustomBizInclude implements ICXFIXCustomBizInclude
{
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

      /* TODO - Add custom logic here */ 
      /* END TODO */

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
      /* TODO - Add custom logic here */

      /* END TODO */ 
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

      tran = argTbl.getTran( "tran", 1);
      old_tran = argTbl.getTran( "oldtran", 1);

      /* TODO - Add custom logic here */		 
      //int tradeDesignatedId = 0;
      //String tradeDesignatedTag = null;
      //	
      //tradeDesignatedId = BizInc_IsTradeDesignated(tran);
      //tradeDesignatedTag = BizInc_TagDesignatedTrade(tradeDesignatedId);
      /* END TODO */

      return retVal;
   }

   /*-------------------------------------------------------------------------------
   Name:             BizInc_IsTradeDesignated  
   Description:      This function will be invoked by function BizInc_TradePreProcessing to
                     determine whether the deal is to be designated to be processed by the Settlement desktop.

                     Suggestions can be put on here are: 
                     i) You can return enumerator on its candidate 
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

      /* TODO - Add custom logic here */

      /* END TODO */

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

      /* TODO - Add custom logic here */

      /* END TODO */

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

      /* TODO - Add custom logic here */

      /* END TODO */

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

      /* TODO - Add custom logic here */
      //for (row = 1; row <= dealInfo.getNumRows(); row++)
      //{	
      //	dealInfo.setString( "SettlementDefinition", row, definition_name);
      //}
      /* END TODO */

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

      /* TODO - Add custom logic here */

      /* END TODO */

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

      /* TODO - Add custom logic here */

      /* END TODO */

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
      /* TODO - Add custom logic here */

      /* END TODO */
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
      /* TODO - Add custom logic here */

      /* END TODO */ 
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

      /* TODO - Add custom logic here */ 
      /* END TODO */

      return retVal;
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

      /* TODO - Add custom logic here */ 
      /* END TODO */

      return retVal;
   }
   
   /*-------------------------------------------------------------------------------
   Name:             BizInc_PostPushLogActivity
   Description:      This function will be invoked by CXFIXPostPushTrade script to 
			   		 perform optional testing logging activity.  
			
			   		 if success - 1 should be returned
			   		 if fail, error code -1 should be returned and xstring should be populated with error 

   Parameters: 	argTbl  (Table) - Mapped tradebuilder table for corresponding transaction
		   		returnTbl (Table) - return table with structure expected by oc engine. 
		   		xstring (XString ) - Should populated with error when fail  

   Remark:		User can call ConnexUtility.scriptGetTran() to get the Transaction pointer instead calling Transaction.retrieve function

   Return Values:   1 = Success
   		  			0 = Fail

   Release    : Original 6/1/2007
   -------------------------------------------------------------------------------*/
   public int BizInc_PostPushLogActivity (Table argTbl, Table returnTbl, XString xstring)
   {
		int retVal = 1; 

		/* TODO - Add custom logic here */ 
		/* END TODO */

		return retVal;   		
   }
   
   /*-------------------------------------------------------------------------------
   Name:              BizInc_UpdStatusLogActivity
   Description: 	  This function will be invoked by CXFIXUpdStatusFromExtSys script to 
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
}
