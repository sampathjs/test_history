package com.olf.jm.warehouse.autocontainerid.persistence;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.olf.jm.warehouse.autocontainerid.model.ConfigurationItem;
import com.olf.jm.warehouse.autocontainerid.model.RelDelTicketField;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.openrisk.scheduling.Nominations;
import com.olf.openrisk.staticdata.Person;
import com.olf.openrisk.staticdata.SecurityGroup;
import com.olf.openrisk.trading.DeliveryTicket;
import com.openlink.util.logging.PluginLog;

public class AutoContainerIdOpsInterface {
	private static final int INITIAL_ID = 1;
	private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
	
	private final Session session;
	
	public AutoContainerIdOpsInterface (final Session session) {
		this.session = session;
	}
	
	/**
	 * Initialises PluginLog from ConstRepository
	 */
	public void init() {
		String abOutdir = session.getSystemSetting("AB_OUTDIR") + "\\error_logs";
		String logLevel = ConfigurationItem.LOG_LEVEL.getValue();
		String logFile = ConfigurationItem.LOG_FILE.getValue();
		String logDir = abOutdir; // ConfigurationItem.LOG_DIRECTORY.getValue();
		if (logDir.trim().equals("")) {
			logDir = abOutdir;
		}
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException (e);
		}
		PluginLog.info("**********" + this.getClass().getName() + " started **********");
	}

	/**
	 * Checks if the provided personnel is a safe desktop user.
	 * This is done by checking if the user is  belonging to the security group 
	 * @param p
	 * @return
	 */
	public boolean isSafeUser (final Person p) {
		String safeUserSecGroup = ConfigurationItem.SAFE_SECURITY_GROUP.getValue();
		for (SecurityGroup group : p.getSecurityGroups()) {
			if (group.getName().equals(safeUserSecGroup)) {
				return true;
			}
		}
		return false;
	}
	
	public void processNomInitialBooking(Nominations nominations) {
		for (Nomination nom : nominations) {
			if (nom instanceof Batch) {
				Batch batch = (Batch) nom;
				int newId = getMaxUsedId(batch);
				for (DeliveryTicket ticket : batch.getBatchContainers()) {
					if (RelDelTicketField.ACTIVITY_ID.guardedGetString(ticket).trim().length() == 0) {
						RelDelTicketField.ACTIVITY_ID.guardedSet(ticket, generateContainerID(newId++));
					}
				}				
			} else {
				PluginLog.debug("skipping nomination #" + nom.getId() + " as it is no batch");
			}
		}
	}
	
	
	private int getMaxUsedId (Batch batch) {
		int maxId = INITIAL_ID;
		for (DeliveryTicket ticket : batch.getBatchContainers()) {
			String containerId = RelDelTicketField.ACTIVITY_ID.guardedGetString(ticket);
			if (matchesIdFormat(containerId)) {
				int id = getIDNumFromFormatted(containerId);
				if (id >= maxId) {
					maxId = id+1;
				}
			}
		}
		return maxId;
	}
	
	private String generateContainerID (int id) {
		String pattern = ConfigurationItem.PATTERN.getValue();
		return String.format(pattern, Integer.toString(id));
	}
	
	private boolean matchesIdFormat (String possibleId) {
		String pattern = ConfigurationItem.PATTERN.getValue();
		pattern = pattern.replaceAll("%s", "\\\\d+");
		return possibleId.matches(pattern);
	}
	
	private int getIDNumFromFormatted (String formattedId) {
		Matcher m = NUMBER_PATTERN.matcher(formattedId);
		while (m.find()) {
			String idAsString = m.group(0);
			return Integer.parseInt(idAsString);
		}
		return INITIAL_ID;
	}
}
