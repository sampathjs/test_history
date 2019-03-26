package com.olf.jm.metalutilisationstatement.tpm;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.olf.embedded.application.Context;
import com.olf.embedded.tpm.AbstractProcessStep;
import com.olf.openjvs.OException;
import com.olf.openjvs.Tpm;
import com.olf.openrisk.calendar.CalendarFactory;
import com.olf.openrisk.calendar.HolidaySchedule;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.tpm.Process;
import com.olf.openrisk.tpm.ProcessDefinition;
import com.olf.openrisk.tpm.Token;
import com.olf.openrisk.tpm.Variables;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.jm.metalutilisationstatement.MetalsUtilisationConstRepository;

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
        	/*
        	ProcessDefinition tpmProcesDefinition = context.getTpmFactory().getProcessDefinition(tpmName);
        	Process tmpProcess = tpmProcesDefinition.start();
        	*/
        	
	        CalendarFactory cf = context.getCalendarFactory();
	        
	        HolidaySchedule eodHolidaySchedule = cf.getHolidaySchedule("EOD_HOLIDAY_SCHEDULE");
	        
	        GregorianCalendar currentDate = new GregorianCalendar();
	        currentDate.setTime(context.getServerTime());
	        Logging.info("Current Date "+ currentDate);
	        
	        int reportdate = new MetalsUtilisationConstRepository().getCnReportDate();
	        
	        GregorianCalendar periodEnd = new GregorianCalendar(
	        		currentDate.get(Calendar.YEAR),
	        		currentDate.get(Calendar.MONTH),
	        		reportdate);
	        Date runDate = eodHolidaySchedule.getNextGoodBusinessDay(periodEnd.getTime());
	        Logging.info("Next Run Date "+ runDate);
	        boolean runToday = runDate.compareTo(currentDate.getTime()) == 0;
        
	        Logging.info("Next Run Date "+ runDate);
	        
        	
    		
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
