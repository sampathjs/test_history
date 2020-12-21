package com.olf.jm.operation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.embedded.trading.AbstractFieldListener;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.Field;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;

/**
 * 
 * When the field value is entered into the field it is parsed to validate
 * whether it the value entered is a valid date.
 * 
 * Revision History:
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	01			Ivan Fernandes		01-Dec-2020					Initial version
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcTranfield })
public class ValidateCNDeliveryDate extends AbstractFieldListener {

	/** The const repository used to initialise the logging classes. */
	private ConstRepository constRep;

	/** The Constant CONTEXT used to identify entries in the const repository. */
	public static final String CONTEXT = "";

	/**
	 * The Constant SUBCONTEXT used to identify entries in the const
	 * repository..
	 */
	public static final String SUBCONTEXT = "";

	/**
	 * Initialise the class loggers.
	 * 
	 * @throws Exception
	 *             the exception
	 */
	private void init() throws OException {
		constRep = new ConstRepository(CONTEXT, SUBCONTEXT);

		String logLevel = "Info";
		String logFile = getClass().getSimpleName() + ".log";
		String logDir = null;

		try {

			logLevel = constRep.getStringValue("logLevel", logLevel);
			logFile = constRep.getStringValue("logFile", logFile);
			logDir = constRep.getStringValue("logDir", logDir);

			Logging.init(this.getClass(), CONTEXT, SUBCONTEXT);
		} catch (Exception e) {
			throw new OException("Error initialising logging. " + e.getMessage());
		}

	}

	@Override
	public PreProcessResult preProcess(final Context context, final Field field, final String oldValue, final String newValue, final Table clientData) {

		try {
			
			init();

			boolean blnIsValidDate = isValidDate(newValue);
			
			if(blnIsValidDate  == false){
				
				Logging.info("Invalid date entered into Delivery Date. Please use the format DD-MMM-YYYY");
				return PreProcessResult.failed("Invalid date entered into Delivery Date. Please use the format DD-MMM-YYYY");
			}
			else{
				
				Logging.info("CN Delivery Date entered succesfully");
			}
			
		} catch (OException exp) {
			Logging.error("Error validating Delivery Date input" + exp.getMessage());
			return PreProcessResult.failed(exp.getMessage());
		}finally{
			Logging.close();
		}
		return PreProcessResult.succeeded();
	}
	
	
    public boolean isValidDate(String inDate) {
        
        DateTimeFormatter dtf =  DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        
        try {
            
            LocalDate parsedDate = LocalDate.parse(inDate, dtf);
            
        } catch ( DateTimeParseException pe) {
            return false;
        }
        return true;
    }
	
}