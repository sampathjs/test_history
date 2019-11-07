package com.olf.jm.containerWeightConverter.app;

import com.olf.embedded.scheduling.AbstractNominationProcessListener;
import com.olf.embedded.application.Context;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.generic.PreProcessResult;
import com.olf.jm.containerWeightConverter.model.ConfigurationItem;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.EnumNomfField;
import com.olf.openrisk.scheduling.Field;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.DeliveryTicket;
import com.olf.openrisk.trading.EnumDeliveryTicketFieldId;
import com.olf.openrisk.trading.Transactions;
import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2017-10-18	V1.0	scurran	-	Initial Version
 */

@ScriptCategory({ EnumScriptCategory.OpsSvcNomBooking })
public class TozToGmsConverter extends AbstractNominationProcessListener {

	
	@Override
	public PreProcessResult preProcess(Context context,
			Nominations nominations, Nominations originalNominations,
			Transactions transactions, Table clientData) {
    	try {
    		init (context);
    		
    		return process(context, nominations);

    	} catch (Throwable t) {
    		PluginLog.error("Error executing class " + this.getClass().getName() + ":\n" + t);
    		// Don't stop the operation from continuing, user will have to enter the converted weight manually on dispatch
    	}
        return PreProcessResult.succeeded();	
		
	}
	
	private PreProcessResult process(Context context, Nominations nominations) {
		for (Nomination nomination : nominations) {		
			if (!isRelevant(nomination)) {
				continue;
			}	
			
			Batch batch = (Batch)nomination;
			
			for( DeliveryTicket ticket : batch.getBatchContainers()) {
				Double netWeight  = ticket.getValueAsDouble(EnumDeliveryTicketFieldId.Volume);
				
				try(com.olf.openrisk.trading.Field field = ticket.getField("Gms Weight Entered")) {
					if(field.getValueAsString() == null || field.getValueAsString().length() == 0) {
						
						double weightInGms = convertWeight(netWeight);

						int decimalPlaces = new Integer(ConfigurationItem.NUM_DECIMAL_PLACES.getValue()).intValue();
						int rounding = new Double(Math.pow(10, decimalPlaces)).intValue();
						double rounderdWeightInGms = (Math.round(weightInGms * rounding)) / (rounding * 1.0);
						// Set value as a string due to rounding issues when setting as a double
						PluginLog.debug("netWeight: " + netWeight + " weightInGms: " + weightInGms + ", rounderdWeightInGms: " + rounderdWeightInGms + ", decimalPlaces:" + decimalPlaces);
						field.setValue( new Double(rounderdWeightInGms).toString());
					}
				}
			}
		}
		
		return PreProcessResult.succeeded();
	}
	
	private double convertWeight( double weight) {
		
		double convFactor = 0;
		if(ConfigurationItem.HK_CONVERSION_FACTOR.getValue() == null || ConfigurationItem.HK_CONVERSION_FACTOR.getValue().length() == 0) {
			try {
				int srcUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "TOz");
				int destUnit = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, "gms");

				convFactor = Util.unitconversionFactor(srcUnit, destUnit);
			} catch (OException e) {
				String errorMessage = "Error calcualting conversion factor. " + e.getMessage();
				PluginLog.error(errorMessage);
				throw new RuntimeException(errorMessage);
			}			
		} else {
			convFactor = new Double(ConfigurationItem.HK_CONVERSION_FACTOR.getValue()).doubleValue();
		}
		

		return  convFactor * weight;
	
	}
	
	private boolean isRelevant(Nomination nom) {
	
		if (!(nom instanceof Batch)) {
			return false;
		}
		Batch batch = (Batch) nom;

		try(Field field = batch.retrieveField(EnumNomfField.NomCmotionCsdActivityId, 0)) {
			if(!field.getValueAsString().equals("Warehouse Receipt")) {
				return false;
			}
		}
		
		return true;
	}
	
	private void init (Context context) {
		String logLevel;
		try {

			logLevel = ConfigurationItem.LOG_LEVEL.getValue();
			String logFile = ConfigurationItem.LOG_FILE.getValue();
			//String logDir = ConfigurationItem.LOG_DIRECTORY.getValue();
			String logDir = SystemUtil.getEnvVariable("AB_OUTDIR") + "\\error_logs";
			
			PluginLog.init(logLevel, logDir, logFile);
			PluginLog.info("*************** Operation Service run (" + this.getClass().getName() +  " ) started ******************");
		}  catch (OException e) {
			throw new RuntimeException(e);
		}  catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
}
