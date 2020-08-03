/********************************************************************************
 * Script Name:   LockedDeals
 * Script Type:   main
 * Status:        complete
 *
 * Revision History:
 * 1.0 - 11.01.2010 - jbonetzk: initial version
 * 1.1 - 23.06.2011 - dmoebius: refactoring code
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.openlink.alertbroker.AlertBroker;
import  com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.*;
/**
 * This script will ...
 * It will exit with a status of FAILED if ...
 * It will exit with a status of SUCCEEDED if ...
 * @author jbonetzk
 * @version 1.1
 *
 * Dependencies:
 * setup via Constants Repository
 */
public class SaveTimeSeries implements IScript
{
    ConstRepository repository = null;
    
    public void execute (IContainerContext context) throws OException
    {       
        repository = new ConstRepository ("EOD");
        
        initLogging ();
        
        // 'try'-wrap for unexpected errors: e.g. within use of database functions in jvs
        try
        {
            saveTimeSeries ();
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-STS-004", strMessage);
        }finally{
        	Logging.close();
        }
        
        
    }
    
    void initLogging () throws OException
    {
        String logLevel = repository.getStringValue ("logLevel", "Error");
        String logFile = repository.getStringValue ("logFile", "");
        String logDir = repository.getStringValue ("logDir", "");
        
        try
        {
        	Logging.init(this.getClass(), repository.getContext(),repository.getSubcontext());
        }
        catch (Exception ex)
        {
            String strMessage = getClass ().getSimpleName () + " - Failed to initialize log.";
            OConsole.oprint (strMessage + "\n");
            AlertBroker.sendAlert ("EOD-STS-001", strMessage);
            Util.exitFail ();
        }
    }

    void saveTimeSeries() throws OException
    {
        Table time_series_data = Util.NULL_TABLE;
        
        time_series_data = TimeSeries.generateCurrentData ();
        if (Table.isTableValid (time_series_data) == 0)
        {
            String strMessage = "Generate time series data failed.";
            Logging.error (strMessage);
            AlertBroker.sendAlert ("EOD-STS-002", strMessage);
        }
        else
        {
            if(TimeSeries.saveData (time_series_data) == 0)
            {
                String strMessage = "Save time series data failed.";
                Logging.error (strMessage);
                AlertBroker.sendAlert ("EOD-STS-003", strMessage);
            }
            else
                Logging.info ("Save time series data succeeded.");
                
            time_series_data.destroy ();
        }
    }
}