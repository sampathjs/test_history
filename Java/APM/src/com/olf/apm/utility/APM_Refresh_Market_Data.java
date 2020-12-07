package com.olf.apm.utility;
import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
public class APM_Refresh_Market_Data implements IScript {
   //#SDBG

/* Released with version 05-Feb-2020_V17_0_126 of APM */

/**********************************************************************************
 Script Name:  APM_Refresh_Market_Data
 
 Description:  This script refreshes any loaded indexex and volatilities.
               
 Expected use: Some clients run deal monitor(s) on a different server to the main
               batch. However, deal booking does not cause the market data to be 
               refreshed. To overcome this, this script should be scheduled to
               run at regular intervals. 
 **********************************************************************************

-----------------------------------------------------------------------------------------------------------
 Revision No.  Date        Who  Description
-----------------------------------------------------------------------------------------------------------
 1.5.0         Jun 06      DC   First version
-----------------------------------------------------------------------------------------------------------*/

// type of messages written to log file/console
final int cMsgTypeWarning = 1;
final int cMsgTypeError   = 2;
final int cMsgTypeInfo    = 3;
final int cMsgTypeDebug   = 4;

// enumuration for required perform operation
int APM_REFRESH_MARKET_DATA = 13;

// switch to 1 if debug messages are required
int gDebugOn = 0;

/* 
 * function prototypes 
 */
////////////////////////////////////////////////////////////////////////////////////
//
// This 'main' section should NOT be modified by USERs.
//
// Note: Presently log messages are only output to the console (we need the service
// name(v8+) or monitor id (v7 and below) to determine correct log file).
//
////////////////////////////////////////////////////////////////////////////////////
public void execute(IContainerContext context) throws OException
{
Table argt = context.getArgumentsTable();
Table returnt = context.getReturnTable();

   Table tParams;
   String sScriptName = "APM_Refresh_Market_Data";
   XString xsError = Str.xstringNew();
   int iRetVal = 1;

   ////////////////////////////////////////////////////////////////////////////////////
   // Refresh Market Data
   ////////////////////////////////////////////////////////////////////////////////////
   APM_LogMessage( cMsgTypeInfo, sScriptName, "Starting Market Data refresh..." );

   /* we have have no parameters */
   tParams = Table.tableNew("tParams");
   iRetVal = Apm.performOperation( APM_REFRESH_MARKET_DATA, 0, tParams, xsError );
   if (iRetVal == 0)
   {
      iRetVal = 0;
      APM_LogMessage( cMsgTypeError, sScriptName, Str.xstringGetString(xsError) );
   }
   
   ////////////////////////////////////////////////////////////////////////////////////
   // Clean up...
   ////////////////////////////////////////////////////////////////////////////////////
   if(Table.isTableValid( tParams ) != 0)
      tParams.destroy();
   
   Str.xstringDestroy( xsError );

   if(iRetVal == 0) 
   {
      APM_LogMessage( cMsgTypeError, sScriptName, "Market Data refresh failed!" );
      Util.exitFail();
   }
      
   APM_LogMessage( cMsgTypeInfo, sScriptName, "Finished Market Data refresh" );
   
   Util.exitSucceed();
}

/*-------------------------------------------------------------------------------
Name:          APM_LogMessage

Description:   The message will be written to the oListen console

Parameters:    iMsgType - error, warning,  informational, debug
               sScriptName - name of this script
               sMsg - Message to log
               
Return Values: None
-------------------------------------------------------------------------------*/
void APM_LogMessage( int iMsgType, String sScriptName, String sMsg ) throws OException
{
   String sMsgType = "";
   String sLogMsg;
   
   switch (iMsgType)
   {
   case cMsgTypeDebug:
      if(gDebugOn == 0)     /* only log debug messages if debug enabled */
         return;
      sMsgType = "DEBUG";
      break;   
   case cMsgTypeInfo:
      sMsgType = "INFO";
      break;
   case cMsgTypeWarning:
      sMsgType = "WARNING";
      break;
   case cMsgTypeError:
      sMsgType = "ERROR";
      break;
   }
   
   sLogMsg = sScriptName + ": " + sMsg;
     
   /* write message to console */
   sLogMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ": " + sLogMsg +"\n";
   OConsole.oprint( sLogMsg );   
}


}
