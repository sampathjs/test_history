package com.olf.jm.autotradebooking.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;


public class FileProcessor {
	private final Session session;

	private boolean firstLine=true;
	private Transaction newDeal= null;

	public FileProcessor(final Session session) {
		this.session = session;
	}
	
	public String processFile (String fullPath) {
		firstLine = true;
		newDeal = null;
		try (Stream<String> stream = Files.lines(Paths.get(fullPath))) {
			stream.forEach(this::processLine);
		} catch (IOException e) {
			Logging.error("Error while reading file '" + fullPath + "': " + e.toString());
    		for (StackTraceElement ste : e.getStackTrace()) {
    			Logging.error(ste.toString());
    		} 
		}
		return null;
	}
	
	private void processLine (String line) {
		if (firstLine) {
			if (!processLoadTemplate (line)) {
				throw new RuntimeException ("Error parsing input file: first line does not contain valid load template command");
			}
			firstLine = false;
		} else if (checkForSetTranField(line)) {
			setTranField(line);
		} else if (checkForProcessDeal(line)) {
			processDeal(line);
		} else if (checkForSetLegField(line)) {
			setLegField (line);
		}else if (checkForAddLeg(line)) {
			addLeg (line);
		} else { // if line is not known
			String errorMsg = "Could not process line '" + line + "' as it was not categorised into any of the existing commands";
			Logging.error(errorMsg);
			throw new RuntimeException (errorMsg);
		}
	}
	
	private void addLeg(String line) {
		Logging.info("Line '" + line + "' categorised as addLeg");
		Leg newLeg = newDeal.getLegs().addItem();
		Logging.info("Successfully added a new leg.");
	}

	private boolean checkForAddLeg(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("addLeg()")) {
			return false;
		}		
		return true;
	}
	
	private void setLegField(String line) {
		Logging.info("Line '" + line + "' categorised as setLegField");
		String lineNormalised = line.trim();
		String legNumber = lineNormalised.substring(12, lineNormalised.indexOf(",")).trim();
		String legField = lineNormalised.substring(lineNormalised.indexOf(",")+1, lineNormalised.lastIndexOf(",")).trim();
		String newValue = lineNormalised.substring(lineNormalised.lastIndexOf(",")+1, lineNormalised.indexOf(")")).trim();
		Logging.info("On Leg #'" + legNumber + " setting the field '" + legField + "' to new value '" + newValue + "'");
		int legNo;
		try {
			legNo = Integer.parseInt(legNumber);
		} catch (NumberFormatException ex) {
			String errorMsg = "The provided leg '" + legNumber + "' in the first parameter is  not a number.";
			Logging.error(errorMsg);
			throw new RuntimeException (errorMsg);
		}
		Leg leg = newDeal.getLeg(legNo);
		Field field = leg.getField(legField);
		field.setValue(newValue);
		Logging.info("Successfully set On Leg #'" + legNumber + " the field '" + legField + "' to new value '" + newValue + "'");
	}

	private boolean checkForSetLegField(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("setLegField(")) {
			return false;
		}		
		if (!lineNormalised.endsWith(")")) {
			return false;
		}
		if (lineNormalised.split(",").length != 3) {
			return false;
		}
		return true;
	}

	private void setTranField(String line) {
		Logging.info("Line '" + line + "' categorised as setTranfield");
		String lineNormalised = line.trim();
		String tranfieldToSet = lineNormalised.substring(13, lineNormalised.indexOf(",")).trim();
		String newValue = lineNormalised.substring(lineNormalised.indexOf(",")+1, lineNormalised.indexOf(")")).trim();
		Logging.info("Setting tranField '" + tranfieldToSet + "' to new value '" + newValue + "'");
		Field field = newDeal.getField(tranfieldToSet);
		field.setValue(newValue);
		Logging.info("Successfully set tranField '" + tranfieldToSet + "' to new value '" + newValue + "'");
	}

	private boolean checkForSetTranField(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("setTranField(")) {
			return false;
		}		
		if (!lineNormalised.endsWith(")")) {
			return false;
		}
		if (lineNormalised.split(",").length != 2) {
			return false;
		}
		return true;
	}
	
	private void processDeal(String line) {
		Logging.info("Line '" + line + "' categorised as process (the deal to book)");
		String lineNormalised = line.trim();
		String newStatus = lineNormalised.substring(8, lineNormalised.indexOf(")")).trim();
		Logging.info("Processing deal to book to new status '" + newStatus + "'");
		EnumTranStatus newStatusEnum;
		try {
			newStatusEnum = EnumTranStatus.valueOf(newStatus);
		} catch (IllegalArgumentException ex) {
			Logging.error("The tran status '" + newStatus + "' is not valid. Allowed values are " + EnumTranStatus.values());
			throw ex;
		}
		try {
			newDeal.process(newStatusEnum);			
		} catch (OpenRiskException ex) {
			Logging.error("Error while processing transaction to status '" + newStatus + "': " + ex.toString() + "\n" + ex.getMessage());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			Logging.error(ste.toString());
    		}
			throw ex;
		}
		Logging.info("Successfully processed deal to new status '" + newStatus + "'");
	}
	
	private boolean checkForProcessDeal(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("process(")) {
			return false;
		}		
		if (!lineNormalised.endsWith(")")) {
			return false;
		}
		return true;
	}


	private boolean processLoadTemplate(String line) {
		Logging.info("Processing load template command in first line (mandatory)");
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("loadTemplate(")) {
			Logging.error("Could not find load template command (expected to be in first line of file). Syntax is loadTemplate(<Template Reference>)"
					+ " but found '" + lineNormalised + "'");
			return false;
		}
		if (!lineNormalised.endsWith(")")) {
			Logging.error("Could not find load template command (expected to be in first line of file). Syntax is loadTemplate(<Template Reference>)"
					+ " but found '" + lineNormalised + "' - missing closing brackets");
			return false;
		}
		String templateReference = lineNormalised.substring(13, lineNormalised.length()-1);
		Logging.info("Loading template having reference '" + templateReference + "'");
		try (Transaction template = session.getTradingFactory().retrieveTransactionByReference(templateReference, EnumTranStatus.Template)){
			newDeal = session.getTradingFactory().createTransactionFromTemplate(template);			
		}
		return true;
	}
}
