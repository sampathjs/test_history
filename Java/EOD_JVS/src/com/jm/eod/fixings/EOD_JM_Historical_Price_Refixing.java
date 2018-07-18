package com.jm.eod.fixings;


import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import standard.include.*;	

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_GENERIC)
@PluginType(SCRIPT_TYPE_ENUM.MAIN_SCRIPT)
public class EOD_JM_Historical_Price_Refixing implements IScript {
	private JVS_INC_Standard m_INCStandard;
	public EOD_JM_Historical_Price_Refixing(){
		m_INCStandard = new JVS_INC_Standard();
	}

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();

		int iScriptStatus = 1;
		int iRetVal = 1;
		Date date = new Date();  
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_hhmmss");  
		String sReportTitle = "Historical Price Refixing Report";
		String sFileName = "STD_Historical_Price_Refixing_"+formatter.format(date);
		String error_log_file = Util.errorInitScriptErrorLog(sFileName);
		String msg;
		String errorMessages = "";
		String sSql;

		Table tIdxIds;
		Table tIdxList;
		Table tModPrices = Table.tableNew("tModPrices");
		Table tReport;
		Table tParam;
		Table tCrystal;
		Table tTemp;

		m_INCStandard.Print(error_log_file, "START", "*** Start of " + sFileName + " script ***");

		// Verify argt or get list of indexes if no param script
		if(ArgtIsValid(argt) == 0)
		{
			tIdxList = Table.tableNew("tIdxList");
			sSql = "SELECT index_id FROM idx_historical_prices " 
				+ "UNION "
				+ "SELECT index_id FROM idx_hourly_history";
			DBaseTable.execISql(tIdxList, sSql);

		}
		else 
		{
			// Get list of indexes from argt
			tIdxIds = argt.getTable( "index_list", 1);
			tIdxList = Table.tableNew("tIdxList");
			tIdxList.addCol( "index_id", COL_TYPE_ENUM.COL_INT);
			tIdxIds.copyCol( 1, tIdxList, 1);
		}


		// Load list of modified prices that has deals that have deals associated with it 
		iRetVal = Index.refixingGetListOfModifiedHistPrices(tModPrices, tIdxList);
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			msg = "Unable to retrieve list of modified prices.  Exiting script. ";
			errorMessages += msg + "\n";
			m_INCStandard.Print(error_log_file, "ERROR", msg);
			iScriptStatus = 0;
		}

		ThereAreItemsToProcess(tModPrices, error_log_file);

		iRetVal = Index.refixingInsertTransAffectedByPriceChanges(tModPrices);
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			msg = "Unable to retrieve list of trades affected by modified prices. ";
			errorMessages += msg + "\n";
			m_INCStandard.Print(error_log_file, "ERROR", msg);
			iScriptStatus = 0;
		}

		iRetVal = Index.refixingRefixTransAffectedByPriceChanges(tModPrices);
		if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
		{
			msg = "Unable to refix list of trades affected by modified prices. ";
			errorMessages += msg + "\n";
			m_INCStandard.Print(error_log_file, "ERROR", msg);
			iScriptStatus = 0;
		}

		tReport = Table.tableNew("Report Table");
		PrepareReportTable(tReport);
		FillReport(tModPrices, tReport);

		tModPrices.destroy();

		if(m_INCStandard.STD_UseCrystalOutput() != 0){

			tParam = m_INCStandard.STD_CreateParameterTable(sReportTitle, sFileName);

			tCrystal = tReport.copyTableFormatted( 0);
			//tCrystal.printTableToTtx( "c:\\temp\\"+ sFileName + ".ttx");

//			if(m_INCStandard.STD_OutputCrystal(tCrystal, tParam, sFileName, sFileName, error_log_file) == 0){
//				errorMessages += "Error value returned from m_INCStandard.STD_OutputCrystal. ";
//				iScriptStatus = 1;
//			}

			tCrystal.destroy();
			tParam.destroy();
		}

		/*** Dump to CSV ***/
		if(m_INCStandard.csv_dump != 0)
		{
			m_INCStandard.STD_PrintTableDumpToFile(tReport, sFileName, sReportTitle, error_log_file);
		}   

		/*** Create USER Table ***/            
		if(m_INCStandard.user_table != 0)
		{
			tTemp = Table.tableNew();
			tTemp.select( tReport, "*", "tran_num GT -1");
			if(m_INCStandard.STD_SaveUserTable(tTemp, "USER_Historical_Price_Refixing", error_log_file) != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()){
				errorMessages += "Error value returned from m_INCStandard.STD_SaveUserTable. "; 
				iScriptStatus = 1;        
			}
			tTemp.destroy();
		}

		/*** View Table ***/
		if(m_INCStandard.view_table != 0)
		{
			m_INCStandard.STD_ViewTable(tReport, sReportTitle, error_log_file);
		}


		/*** Create Report Viewer EXC File ***/
		if(m_INCStandard.report_viewer != 0)
		{
			m_INCStandard.STD_PrintTextReport(tReport, sFileName + ".exc", sReportTitle, sReportTitle + "\nCurrent Date: " + OCalendar.formatDateInt(OCalendar.today()), error_log_file);
		}

		tReport.destroy();


		m_INCStandard.Print(error_log_file, "END", "*** End of " + sFileName + " script ***");

		tIdxList.destroy();

		if(iScriptStatus != 0) 
		   return;
		else             
		   throw new OException( errorMessages ); 

	}

	/***************************************************************************/
	int ArgtIsValid(Table argt) throws OException
	{   

		Table temp;

		if (argt.getColNum( "index_list") < 1)
			return 0;

		if (argt.getColType( "index_list") != COL_TYPE_ENUM.COL_TABLE.toInt())
			return 0;

		if (argt.getNumRows() != 1)
			return 0;

		temp = argt.getTable( "index_list", 1);
		if (temp.getColNum("index_id") < 1)
			return 0;

		return 1;
	}

	/***************************************************************************/
	int ThereAreItemsToProcess(Table tModPrices, String error_log_file) throws OException
	{
		int iRetVal;
		int iDaily;
		int iHourly;

		Table tDaily;
		Table tHourly;

		//check if there are daily modified historical prices that need to be processed
		tDaily = tModPrices.getTable( 2, 1);
		iDaily = tDaily.getNumRows();
		if ( iDaily <= 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "There are no modified daily historical prices that need to be processed");
		}

		// Check if there are hourly modified historical prices that need to be proccessed
		tHourly = tModPrices.getTable( 2, 2);
		iHourly = tHourly.getNumRows();
		if ( iHourly <= 0)
		{
			m_INCStandard.Print(error_log_file, "INFO", "There are no modified hourly historical prices that need to be processed");
		}

		// If there are no items to set the iRetVal flag to 0
		if (iHourly <= 0 && iDaily <= 0)
			iRetVal = 0;
		else iRetVal = 1;         

		return iRetVal; 
	}

	/*****************************************************************************/
	void PrepareReportTable(Table tReport) throws OException
	{

		tReport.addCol(  "index_id",   COL_TYPE_ENUM.COL_STRING, "Index");
		tReport.addCol(  "reset_date", COL_TYPE_ENUM.COL_STRING, "Reset\nDate");
		tReport.addCol(  "start_date", COL_TYPE_ENUM.COL_STRING, "Start\nDate");
		tReport.addCol(  "end_date",   COL_TYPE_ENUM.COL_STRING, "End\nDate");
		tReport.addCol(  "yield_basis",COL_TYPE_ENUM.COL_STRING, "Yield\nBasis");
		tReport.addCol(  "ref_source", COL_TYPE_ENUM.COL_STRING, "Reference\nSource");
		tReport.addCol(  "index_location",COL_TYPE_ENUM.COL_STRING, "Index\nLocation");

		tReport.addCol( "tran_num", COL_TYPE_ENUM.COL_INT, "Transaction\nNumber");
		tReport.addCol( "param_seq_num", COL_TYPE_ENUM.COL_INT, "Parameter\nSequence");
		tReport.addCol( "profile_seq_num", COL_TYPE_ENUM.COL_INT, "Profile\nSequence");
		tReport.addCol( "param_reset_header_seq_num", COL_TYPE_ENUM.COL_INT, "Parameter\nReset\nHeader\nSequence");
		tReport.addCol( "reset_seq_num", COL_TYPE_ENUM.COL_INT, "Reset\nSequence");

		tReport.addCol(  "result", COL_TYPE_ENUM.COL_STRING, "Result");

		tReport.setRowHeaderWidth( 1);
	}

	/*********************************************************************************/
	void FillReport(Table tModPrices, Table tReport) throws OException
	{
		Table tDummy;
		Table tDummier;
		Table tCopy;
		Table t;

		int iNewRow;
		int iColType;
		int i, j, k, m;
		int iColNum;

		String dummy;

		for ( i = 1; i <= 2; i++ )
		{
			tDummy = tModPrices.getTable( 2, i);
			iColNum = tDummy.getNumCols();                           //bjs removed "-1"

			tDummier = tDummy.copyTable();
			tDummier.setColFormatAsRef( "index_id",   SHM_USR_TABLES_ENUM.INDEX_TABLE);
			tDummier.setColFormatAsDate( "reset_date", DATE_FORMAT.DATE_FORMAT_MDY_SLASH);
			tDummier.setColFormatAsDate( "start_date", DATE_FORMAT.DATE_FORMAT_MDY_SLASH);
			tDummier.setColFormatAsDate( "end_date",   DATE_FORMAT.DATE_FORMAT_MDY_SLASH);
			tDummier.setColFormatAsRef( "yield_basis",SHM_USR_TABLES_ENUM.YIELD_BASIS_TABLE);
			tDummier.setColFormatAsRef( "ref_source", SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE);
			tDummier.setColFormatAsRef( "index_location",SHM_USR_TABLES_ENUM.INDEX_LOCATION_TABLE );
			tDummier.colHide("time");

			for( j = 1; j <=  tDummier.getNumRows(); j++)
			{                          
				t = tDummy.getTable( iColNum, j);

				if(t.getNumRows() == 0)
				{
					continue;
				}

				tCopy = t.copyTable();
				tCopy.colHide( tCopy.getNumCols() - 1);

				tCopy.addCol( "index_id",   COL_TYPE_ENUM.COL_STRING);
				dummy = tDummier.formatCellData("index_id", j);
				tCopy.setColValString( "index_id", dummy );

				tCopy.addCol(  "reset_date", COL_TYPE_ENUM.COL_STRING);
				dummy = tDummier.formatCellData("reset_date", j);	
				tCopy.setColValString( "reset_date", dummy );

				if( i == 1)
				{
					tCopy.addCol(  "start_date", COL_TYPE_ENUM.COL_STRING);
					dummy = tDummier.formatCellData("start_date", j);
					tCopy.setColValString( "start_date", dummy );

					tCopy.addCol(  "end_date",   COL_TYPE_ENUM.COL_STRING);
					dummy = tDummier.formatCellData("end_date", j);
					tCopy.setColValString( "end_date", dummy );

					tCopy.addCol(  "yield_basis",COL_TYPE_ENUM.COL_STRING);
					dummy = tDummier.formatCellData("yield_basis", j);
					tCopy.setColValString( "yield_basis", dummy );
				}

				tCopy.addCol(  "ref_source", COL_TYPE_ENUM.COL_STRING);
				dummy = tDummier.formatCellData("ref_source", j);	
				tCopy.setColValString( "ref_source", dummy );

				tCopy.addCol(  "index_location",COL_TYPE_ENUM.COL_STRING );
				dummy = tDummier.formatCellData("index_location", j);
				tCopy.setColValString( "index_location", dummy );

				for ( k = tCopy.getNumRows(); k > 0; k--)
				{

					iNewRow = tReport.addRow(); 
					for ( m = 1; m <= tCopy.getNumCols(); m++ )
					{
						iColType = tCopy.getColType( m);
						if(tReport.getColNum( tCopy.getColName( m)) > 0 )
						{

							if(iColType == COL_TYPE_ENUM.COL_STRING.toInt())
							{
								tReport.setString(tCopy.getColName( m),iNewRow,
										tCopy.getString(tCopy.getColName( m),k ) );
							}
							if(iColType == COL_TYPE_ENUM.COL_INT.toInt())
							{
								tReport.setInt(tCopy.getColName( m),iNewRow,
										tCopy.getInt(tCopy.getColName( m),k ) );
							}
						}
					}// for m
					if(tCopy.getInt( "result", k) <= 0)
						tReport.setString( "result", iNewRow, "Failed"); 
					else
						tReport.setString( "result", iNewRow, "Succeeded");  

				}// for k
				tCopy.destroy();
			}//for j

			tDummier.destroy();

		}//for i = 1...

		tReport.groupFormatted( "index_id, reset_date, start_date");

		return;
	}


}
