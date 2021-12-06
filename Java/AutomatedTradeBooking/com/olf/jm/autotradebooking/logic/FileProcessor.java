package com.olf.jm.autotradebooking.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.trading.EnumTranStatus;
import com.olf.openrisk.trading.Field;
import com.olf.openrisk.trading.Leg;
import com.olf.openrisk.trading.Transaction;
import com.openlink.util.constrepository.ConstRepository;


public class FileProcessor {
	private final Session session;

	private boolean firstLine=true;
	private Transaction newDeal= null;

	private final ConstRepository constRepo;
	private boolean executeDebugCommands;
	private LogTable logTable;
	private int currentLine;

	public FileProcessor(final Session session, ConstRepository constRepo) {
		this.session = session;
		this.constRepo = constRepo;
		try {
			executeDebugCommands = Boolean.parseBoolean(constRepo.getStringValue("executeDebugCommands", "false"));			
		} catch (Exception ex) {			
			Logging.error("Could not read or parse Const Repso entry " + constRepo.getContext() 
				+ "\\" + constRepo.getSubcontext() + "\\executeDebugCommands that is expected to contain the String"
				+ " values 'true' or 'false'. Defaulting to false");
			executeDebugCommands = false;
		}
	}

	public void processFile (String fullPath) {
		logTable = new LogTable(session, fullPath);
		firstLine = true;
		newDeal = null;
		currentLine = 0;
		try (Stream<String> stream = Files.lines(Paths.get(fullPath))) {
			stream.forEachOrdered(this::processLine);
		} catch (IOException e) {
			Logging.error("Error while reading file '" + fullPath + "': " + e.toString());
			for (StackTraceElement ste : e.getStackTrace()) {
				Logging.error(ste.toString());
			} 
		}
		if (executeDebugCommands) {
			logTable.showLogTableToUser();
		}
	}

	private void processLine (String line) {
		try {
			String message="";
			if (firstLine) {
				if (!processLoadTemplate (line)) {
					message = "Error parsing input file: first line does not contain valid load template command";
					logTable.addLogEntry(currentLine, false, message);
					throw new RuntimeException (message);
				}
				firstLine = false;
			} else if (checkForSetTranField(line)) {
				message = setTranField(line);
			} else if (checkForProcessDeal(line)) {
				message = processDeal(line);
			} else if (checkForSetLegField(line)) {
				message = setLegField (line);
			} else if (checkForAddLeg(line)) {
				message = addLeg (line);
			} else if (checkForSetResetDefinitionValue(line)) {
				message = setResetDefinitionField (line);
			} else if (checkForDebugShowTransaction(line)) {
				if (executeDebugCommands) {
					message = debugShowToUser (line);
				} else {
					message = "debugShowToUser() not executed as debug comnand execution is disabled";
					Logging.info(message);
				}
			} else { // if line is not known
				String errorMsg = "Could not process line '" + line + "' as it was not categorised into any of the existing commands";
				Logging.error(errorMsg);
				throw new RuntimeException (errorMsg);
			}
			logTable.addLogEntry(currentLine, true, message);
		} catch (Exception ex) {
			logTable.addLogEntry(currentLine, false, ex.getMessage());	
			throw ex;
		} finally {
			currentLine++;			
		}
	}

	private boolean checkForDebugShowTransaction(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("debugShowToUser()")) {
			return false;
		}		
		return true;
	}
	
	private String debugShowToUser(String line) {
		Logging.info("Line '" + line + "' categorised as debugShowToUser()");
		session.getTradingFactory().viewTransaction(newDeal);
		String msg = "Successfully showed Transaction to User";
		Logging.info(msg);
		return msg;
	}
	
	private String addLeg(String line) {
		Logging.info("Line '" + line + "' categorised as addLeg");
		Leg newLeg = newDeal.getLegs().addItem();
		String msg = "Successfully added a new leg.";
		Logging.info(msg);
		return msg;
	}

	private boolean checkForAddLeg(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("addLeg()")) {
			return false;
		}		
		return true;
	}
	
	private String setResetDefinitionField(String line) {
		Logging.info("Line '" + line + "' categorised as setResetDefinitionField");
		String lineNormalised = line.trim();
		String legNumber = lineNormalised.substring(24, lineNormalised.indexOf(",")).trim();
		String resetDefField = lineNormalised.substring(lineNormalised.indexOf(",")+1, lineNormalised.lastIndexOf(",")).trim();
		String newValue = lineNormalised.substring(lineNormalised.lastIndexOf(",")+1, lineNormalised.indexOf(")")).trim();
		Logging.info("On Leg #'" + legNumber + " setting the field '" + resetDefField + "' on the reset definition to new value '" + newValue + "'");
		int legNo;
		try {
			legNo = Integer.parseInt(legNumber);
		} catch (NumberFormatException ex) {
			String errorMsg = "The provided leg '" + legNumber + "' in the first parameter is  not a number.";
			Logging.error(errorMsg);
			throw new RuntimeException (errorMsg);
		}
		Leg leg = newDeal.getLeg(legNo);
		Field field = leg.getResetDefinition().getField(resetDefField);
		if (field == null) {
			String errorMsg ="The field '"  + resetDefField + "' was not found on the reset definition.";
			Logging.error(errorMsg);	
			throw new RuntimeException(errorMsg);
		}
		field.setValue(newValue);
		String msg = "Successfully set On Leg #'" + legNumber + " the field '" + resetDefField + "' on the reset definition to new value '" + newValue + "'";
		Logging.info(msg);
		return msg;
	}

	private boolean checkForSetResetDefinitionValue(String line) {
		String lineNormalised = line.trim();
		if (!lineNormalised.startsWith("setResetDefinitionField(")) {
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

	private String setLegField(String line) {
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
		String msg = "Successfully set On Leg #'" + legNumber + " the field '" + legField + "' to new value '" + newValue + "'";
		Logging.info(msg);
		return msg;
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

	private String setTranField(String line) {
		Logging.info("Line '" + line + "' categorised as setTranfield");
		String lineNormalised = line.trim();
		String tranfieldToSet = lineNormalised.substring(13, lineNormalised.indexOf(",")).trim();
		String newValue = lineNormalised.substring(lineNormalised.indexOf(",")+1, lineNormalised.indexOf(")")).trim();
		Logging.info("Setting tranField '" + tranfieldToSet + "' to new value '" + newValue + "'");
		Field field = newDeal.getField(tranfieldToSet);
		field.setValue(newValue);
		String msg = "Successfully set tranField '" + tranfieldToSet + "' to new value '" + newValue + "'";
		Logging.info(msg);
		return msg;
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

	private String processDeal(String line) {
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
		String msg = "Successfully processed deal to new status '" + newStatus + "'";
		Logging.info(msg);
		return msg;
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
