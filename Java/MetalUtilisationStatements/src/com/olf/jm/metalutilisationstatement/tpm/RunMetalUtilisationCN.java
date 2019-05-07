package com.olf.jm.metalutilisationstatement.tpm;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;

/**
 * 
 * TPM plugin that check is the workflow should run or just complete.
 * 
 * @author Shaun Curran
 *
 */
/* History
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | Rev | Date        | Change Id     | Author          | Description                                                                     |
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * | 001 | 27-Nov-2018 |               | S Curran        | Initial version.                                                                |
 * -----------------------------------------------------------------------------------------------------------------------------------------
*/
@ScriptCategory({ EnumScriptCategory.Generic })
public class RunMetalUtilisationCN extends AbstractGenericScript{
	
	private static final String tpmName = "Metals Utilisation_CN"; 
			

	public Table execute(Context context,  ConstTable table) {
		
        Logging.init(context, this.getClass(), "Metals Utilisation Statement", "Run Metal Utilisation CN");

        try {
        	
	        CalendarFactory cf = context.getCalendarFactory();
	        
	        HolidaySchedule eodHolidaySchedule = cf.getHolidaySchedule("EOD_HOLIDAY_SCHEDULE");
	        
	        //getting the calculated run date
	        GregorianCalendar temp = new GregorianCalendar();
	        temp.setTime(context.getBusinessDate());
	        Logging.info("Current Date "+ temp);
	        int reportdate = new MetalsUtilisationConstRepository().getCnReportDate();
	        
	        GregorianCalendar periodEnd = new GregorianCalendar(
	        		temp.get(Calendar.YEAR),
	        		temp.get(Calendar.MONTH),
	        		reportdate);
	        Date calculatedRunDate = eodHolidaySchedule.getNextGoodBusinessDay(periodEnd.getTime());
	        
	        Calendar calculatedToday = new GregorianCalendar();
	        calculatedToday.setTime(calculatedRunDate);
	        int calculatedRunDateIntValue = calculatedToday.get(Calendar.DAY_OF_MONTH);
	        
	        // Getting the real time today date
	        Calendar today = new GregorianCalendar();
	        today.setTime(context.getBusinessDate());
	        int todayIntValue = today.get(Calendar.DAY_OF_MONTH);
	        
	        
	        Logging.info("Current Date "+ temp);       
	        Logging.info("Next Run Date "+ calculatedRunDate);
	        
	        
	        
	        boolean runToday = (calculatedRunDateIntValue == todayIntValue);
	        if(runToday)
	        {
	        	Logging.info("Running CN Metal Rentals TPM workflow");
	        	
	        	ProcessDefinition tpmProcesDefinition = context.getTpmFactory().getProcessDefinition(tpmName);
	        	tpmProcesDefinition.start();
	        	
	        }
	        else
	        {
	        	Logging.info("Will not Run CN Metal Rentals TPM workflow today");
	        	Logging.info("Next Run Date "+ calculatedRunDate);
	        }
        
			return null;

        } catch (Throwable e) {
            Logging.error("Process failed", e);
            throw new RuntimeException(e);
		}
        finally {
        	Logging.close();
        }
	}

}
