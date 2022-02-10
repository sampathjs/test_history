package com.olf.jm.reportbuilder;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ask;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IScript;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.JvsExitException;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.ASK_SELECT_TYPES;
import com.olf.openjvs.enums.ASK_TEXT_DATA_TYPES;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

public class ITSMReportParam implements IScript {

	@Override
	public void execute(IContainerContext context) throws OException {
		
		Table argt = context.getArgumentsTable();
		
		argt.addCol("Start_Date", COL_TYPE_ENUM.COL_DATE_TIME);

		argt.addCol("End_Date", COL_TYPE_ENUM.COL_DATE_TIME);
		argt.addCol("Country_ID", COL_TYPE_ENUM.COL_STRING);
		
		if(argt.getNumRows() < 1) {
			argt.addRow();
		}
		
		if (Util.canAccessGui() == 1) {
			// GUI access prompt the user for the process date to run for
			Table tAsk = Table.tableNew ("ITSM Report");
			Ask.setTextEdit (tAsk
					,"Start Date"
					,OCalendar.formatDateInt (OCalendar.getServerDate())
					,ASK_TEXT_DATA_TYPES.ASK_DATE
					,"Please select Start date"
					,1);
			Ask.setTextEdit (tAsk
					,"End Date"
					,OCalendar.formatDateInt (OCalendar.getServerDate())
					,ASK_TEXT_DATA_TYPES.ASK_DATE
					,"Please select End date"
					,1);

			Table country = getTypeOfUser();
			Table selectedCountry =  Table.tableNew();
			selectedCountry.select(country, "*", "country_id EQ 20250");
			  
			Ask.setAvsTable(tAsk, country, "Select Country",1,ASK_SELECT_TYPES.ASK_MULTI_SELECT.toInt(),2,selectedCountry);
			
			/* Get User to select parameters */
			if(Ask.viewTable (tAsk,"Storage Deal Management","Please select the processing date.") == 0)
			{
				tAsk.destroy();

				throw new OException( "User Clicked Cancel" );
			}

			/* Verify Start and End Dates */
			 
			int startDate = OCalendar.parseString (tAsk.getTable( "return_value", 1).getString("return_value", 1));
			
			
			argt.setDateTime(1, 1, new ODateTime(startDate)); 
			int endDate = OCalendar.parseString (tAsk.getTable( "return_value", 2).getString("ted_str_value", 1));
			String contryStr =  getCountryIDStr(tAsk) ;
			
 

		 	argt.setDateTime(2, 1, new ODateTime(endDate));
		  	argt.setString(3, 1, contryStr);
		 	 
			
			tAsk.destroy();
		} else {
			// no gui so default to the current EOD date. 
			argt.setDateTime(1, 1, new ODateTime(OCalendar.getServerDate()));
		}
		
		
	}
	private String getCountryIDStr(Table tAsk) throws OException {
		int rowCount = tAsk.getTable( "return_value", 3).getNumRows();
		String val = null;
		for(int i=1;i<= rowCount;i++){
			if(i==1){
				val = tAsk.getTable( "return_value", 3).getInt(1, i)+"";
			}else
			  val = val+","+ tAsk.getTable( "return_value", 3).getInt(1, i) ;
		} 
		return val;
	}
	private Table getTypeOfUser() {

		Table tblData = null;
		StringBuilder sql = null;

		try {
			tblData = Table.tableNew();

			sql = new StringBuilder();
			 
			sql.append("SELECT country_name AS country ,  country_id ").append("\n FROM USER_country_name ") ;

			DBaseTable.execISql(tblData, sql.toString());

		} catch (OException e) {

		}
		return tblData;
	}
}
