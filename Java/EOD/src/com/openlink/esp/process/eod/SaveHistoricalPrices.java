/********************************************************************************
 * Status:        completed
 *
 * Revision History:
 * 1.0 - 09.09.09 - nschwedler: initial version
 * 1.1 - 12.01.10 - jbonetzky:  adapted package name; added AlertBroker
 * 1.2 - 12.07.10 - sehlert:    adapted package name
 * 1.3 - 09.05.11 - dmoebius:  only Validated prices will be saved 
 * 1.4 - 23.06.2011 - dmoebius: refactoring code
 * 1.5 - 27.09.2011 - nschwedler: add prints
 * 1.6 - 14.10.2011 - maubrey: excluded historical prices for holidays
 * 1.7 - 25.10.2011 - rnasrun: undo 1.5, changed method for loading closing prices
 * 1.8 - 09.11.2011 - rnasrun: bugfix regarding to logic of 1.6 (exclusion of historical prices for holidays)
 * 1.9 - 28.11.2011 - fix sql for idx with no holiday schedule selected
 * 2.0 - 29.12.2011 - rnasrun: fix for weekends, correction in 1.9
 * 2.1 - 12.04.2012 - jwaechter: added filter so only "validated" indices are retrieved in method "getAllDayCurves"
 * 2.2 - 23.04.2013 - dlinke: added a empty dummy method (to modify the list of indexes) 
 *
 ********************************************************************************/

package com.openlink.esp.process.eod;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.alertbroker.AlertBroker;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.constrepository.*;

/**
 * This script saves Historical Prices for all configured (Historical) index.
 * @author nschwedler
 * @version 1.9
 */
public class SaveHistoricalPrices implements IScript
{
	// Object variables
	int		intRet
		  ;
	
	String	strSQL
		  , strMessage
	      ;
	
	Table	tblHistIndexList = Util.NULL_TABLE
	      ;
	
	ConstRepository repository = null;
	private boolean modified = true;
	
    public void execute(IContainerContext context) throws OException
    {
        repository = new ConstRepository ("EOD");
        
        initPluginLog();

        try
        {
            saveHistoricalPrices(OCalendar.today());
        }
        catch (OException oe)
        {
            String strMessage = "Unexpected: " + oe.getMessage ();
            PluginLog.error (strMessage);
			AlertBroker.sendAlert ("EOD-SHP-004", strMessage);
        }

		// CleanUp and Exit
    	if(Table.isTableValid(tblHistIndexList)==OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) tblHistIndexList.destroy();
        PluginLog.exitWithStatus();
    }    	
    	

	void initPluginLog () throws OException
	{
	    String logLevel = repository.getStringValue ("logLevel", "Error");
	    String logFile = repository.getStringValue ("logFile", getClass().getSimpleName() + ".log");
	    String logDir = repository.getStringValue ("logDir", SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs" );
	    
	    try
	    {
	        if (logDir.trim ().equals (""))
	        {
	        	PluginLog.init (logLevel);
	        }
	        else
	        {
	        	PluginLog.init (logLevel, logDir, logFile);
	        }
	    }
	    catch (Exception ex)
	    {
	        String strMessage = getClass ().getSimpleName () + " - Failed to initialize log.";
			AlertBroker.sendAlert ("EOD-SHP-001", strMessage);
	        Util.exitFail ();
	    }
	}

	void saveHistoricalPrices(int iToday) throws OException
	{
		int dayOfWeek			=	-1;
    	Table allDayCurves		=	null;

		//1.9 fix sql for idx with no holiday schedule selected
    	// Get list of system configured index
    	strSQL  = "SELECT"
    			 + "\n ihc.index_id,id.currency,ih.holiday_id, hd.holiday_date, hd.active, hl.id_number, hl.name, hl.weekend " 
    			 + "\n FROM"
    			 + "\n idx_history_config ihc " 
    			 + " \n JOIN idx_def id ON (ihc.index_id = id.index_id) " 
    			 + " \n LEFT JOIN idx_holiday ih ON (id.index_version_id = ih.index_version_id) "
    			 + " \n LEFT JOIN holiday_detail hd ON (ih.holiday_id = hd.holiday_num) " 
    			 + " \n LEFT JOIN holiday_list hl ON (ih.holiday_id = hl.id_number) "
    			 + "\n WHERE"
    			 + "\n id.db_status = 1" // Validated
    			 + "\n AND hd.active = 1" // holiday is active
    			;

    	tblHistIndexList = Table.tableNew();
    	intRet = DBaseTable.execISql(tblHistIndexList, strSQL);
    	if(intRet != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
    	{
            strMessage = "Retrieve Index List failed.";
			AlertBroker.sendAlert ("EOD-SHP-002", strMessage);
			return;
      	}
    	
    	
    	intRet	=	tblHistIndexList.colConvertDateTimeToInt("holiday_date");
    	
    	//flag indexthat has a holiday today.
    	allDayCurves	=	getAllDayCurves(tblHistIndexList);
    	intRet			=	allDayCurves.copyRowAddAll(tblHistIndexList);
    	
    	dayOfWeek	=	OCalendar.getDayOfWeek(OCalendar.today());
    	determineWeekendByHolidayCalendar(tblHistIndexList);
    	int iNumRows = tblHistIndexList.getNumRows();
    	tblHistIndexList.addCol("delete_record",COL_TYPE_ENUM.COL_INT);
    	for(int i=1;i<=iNumRows;i++)
    	{
    		int iIndex = tblHistIndexList.getInt("index_id",i);
    		int iCurrency = tblHistIndexList.getInt("currency",i);
    		int iHoliday = tblHistIndexList.getInt("holiday_date",i);
    		int active = tblHistIndexList.getInt("active",i);
    		if(iHoliday == iToday && active == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		
    		//check weekend

    		if(dayOfWeek == 0 && tblHistIndexList.getInt("sun", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 1 && tblHistIndexList.getInt("mon", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 2 && tblHistIndexList.getInt("tue", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 3 && tblHistIndexList.getInt("wed", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 4 && tblHistIndexList.getInt("thu", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 5 && tblHistIndexList.getInt("fri", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    		if(dayOfWeek == 6 && tblHistIndexList.getInt("sat", i) == 1)
				tblHistIndexList.setInt("delete_record", i, 1);
    	}
    	
    	Table tblTemp = tblHistIndexList.cloneTable();
    	tblTemp.select(tblHistIndexList,"index_id","delete_record EQ 1");
    	tblTemp.deleteWhere("index_id",tblHistIndexList,"index_id"); //delete index with holiday
    	tblTemp.destroy();
    	
    	//remove extra records
    	tblHistIndexList.delCol("delete_record");
    	tblHistIndexList.delCol("currency");
    	tblHistIndexList.delCol("holiday_id");
    	tblHistIndexList.delCol("holiday_date");
    	tblHistIndexList.delCol("active");
    	tblHistIndexList.delCol("id_number");    	
    	tblHistIndexList.delCol("name");    	
    	tblHistIndexList.delCol("weekend");
    	tblHistIndexList.delCol("sun");
    	tblHistIndexList.delCol("mon");
    	tblHistIndexList.delCol("tue");
    	tblHistIndexList.delCol("wed");
    	tblHistIndexList.delCol("thu");
    	tblHistIndexList.delCol("fri");
    	tblHistIndexList.delCol("sat");

    	tblHistIndexList.makeTableUnique();
    	
    	//dummy to modify the curves you want to save with custom logic
    	//make sure the method does not change the table structure
    	try
    	{
    		modifyIndexTable(tblHistIndexList);
    	}catch (Throwable ex)
    	{
    		PluginLog.error("Error in modifyIndexTable");
    	}
    	
    	
    	// Save Historical Prices
    	try 
    	{
			PluginLog.info("SQL for saving historical prices:" + strSQL);
			//Index.refreshList(tblHistIndexList, 0);
			Index.refreshShm(1);
			Sim.loadAllCloseMktd(iToday);
			Index.saveHistoricalPrices(tblHistIndexList);
		} 
    	catch (OException e) 
		{
    		if (modified)
    		{
    			PluginLog.warn("INdex table was modified external");
    		}
            strMessage = e.getMessage() + " - Save Historical Prices failed.";
            PluginLog.error(strMessage);
			AlertBroker.sendAlert ("EOD-SHP-003", strMessage);
			return;
		}finally{
			if (Table.isTableValid(allDayCurves) == 1) allDayCurves.destroy();
		}

	}
	
    protected void modifyIndexTable(Table indexList) throws OException
    { //dummy
		modified = false;		
	}


	private Table getAllDayCurves(Table tbl) throws OException{
    	int ret					=	-1;
    	String sql              =   null;
    	Table allDayCurves		=	null;
    	
    	// jwaechter: adjusted this SQL to filter out indices that are not validated
    	sql = "SELECT index_id, 0 currency, 0 holiday_id, 0 holiday_date, 0 active, 0 id_number, 0 name, 0 weekend FROM idx_def WHERE idx_def.db_status = " 
    		+	IDX_DB_STATUS_ENUM.IDX_DB_STATUS_VALIDATED.toInt();
    	
    	allDayCurves = Table.tableNew();
    	
    	ret = DBaseTable.execISql(allDayCurves, sql);
    	
    	if(intRet != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) 
    	{
            strMessage = "Retrieve Index List of 24/7 curves failed.";
			AlertBroker.sendAlert ("EOD-SHP-002", strMessage);
      	}
    	tbl.deleteWhere("index_id", allDayCurves,"index_id"); //delete all curves with holiday
    	
    	return allDayCurves;
    }
    
    private void determineWeekendByHolidayCalendar(Table tbl) throws OException{
    	int 	ret				=	-1;
    	int 	weekend			=	-1;
    	int 	rest			=	-1;
    	String 	sql				=	null;
    	Table 	holidayList		=	null;
    	
    	holidayList = Table.tableNew();
    	sql			=	"select * from holiday_list";
    	ret = DBaseTable.execISql(holidayList, sql);
    	
    	holidayList.addCol("sun", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("mon", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("tue", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("wed", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("thu", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("fri", COL_TYPE_ENUM.COL_INT);
    	holidayList.addCol("sat", COL_TYPE_ENUM.COL_INT);

    	for (int i = 1; i <= holidayList.getNumRows(); i++){
    		weekend		=	holidayList.getInt("weekend", i);
    		if (weekend / 64 == 1){
    			holidayList.setInt("sat", i, 1);
    		}
    		rest	=	weekend % 64;
    		if (rest / 32 == 1){
    			holidayList.setInt("fri", i, 1);
    		}
    		rest	=	weekend % 32;
    		if (rest / 16 == 1){
    			holidayList.setInt("thu", i, 1);
    		}
    		rest	=	weekend % 16;
    		if (rest / 8 == 1){
    			holidayList.setInt("wed", i, 1);
    		}
    		rest	=	weekend % 8;
    		if (rest / 4 == 1){
    			holidayList.setInt("tue", i, 1);
    		}
    		rest	=	weekend % 4;
    		if (rest / 2 == 1){
    			holidayList.setInt("mon", i, 1);
    		}
    		rest	=	weekend % 2;
    		if (rest / 1 == 1){
    			holidayList.setInt("sun", i, 1);
    		}
    	}	
		ret		=	tbl.select(holidayList, "sun, mon, tue, wed, thu, fri, sat", "id_number EQ $id_number");
		PluginLog.info("Weekken Day determined");
    }
	
}
