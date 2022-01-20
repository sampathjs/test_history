package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public abstract class  AbstractToDiffService <T> {
    private static final Logger logger = LogManager.getLogger(AbstractToDiffService.class);

	protected Session session;

	public abstract String getSyncCategory ();
	
	protected abstract boolean isDiffInAuxFields(T knownTo, T updatedTo);

	protected abstract T updateLifeCycleStatus(T knownTo, DefaultReference lifecycleStatus);	
	
	protected abstract List<T> convertReportToTransferObjects(Table endurSideData);
	
	protected abstract void syncEndurSideIds (List<T> knownTos, List<T> endurSideTos);
	
	public AbstractToDiffService (final Session session) {
		this.session = session;
	}
	
	public List<T> createToListDifference(List<T> knownParties) {
		logger.info("Starting creation of list of differences");
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory (session.getIOFactory(), getSyncCategory());
		logger.info("Retrieved report name '" + reportName + "'");
		Table endurSideData  = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		logger.info("Successfully executed report '" + reportName + "'");
		List<T> endurSideDataAsTo = convertReportToTransferObjects (endurSideData);
		logger.info("Successfully converted Endur table to TOs");
		List<T> diffList = createToDiff (knownParties, endurSideDataAsTo);
		logger.info("Finished creation of list of differences");
		return diffList;
	}
	
	private List<T> createToDiff( List<T> knownTos, List<T> endurSideDataAsTo) {
		logger.info("Synchronising Endur Side IDs");
		syncEndurSideIds (knownTos, endurSideDataAsTo);
		logger.info("Successfully synchronised Endur Side IDs");
		List<T> diffList = new ArrayList<T>(knownTos.size() + endurSideDataAsTo.size());
		// update existing TOs present in TOMS that either have been modified 
		// or deleted. Ignore identical values.
		for (T knownTo : knownTos) {
			T updatedTo;
			if (!endurSideDataAsTo.contains(knownTo)) {
				logger.info("Found TO '" + knownTo + "' that is not present on Endur. Set lifecycle status to deleted");
				updatedTo = updateLifeCycleStatus (knownTo, DefaultReference.LIFECYCLE_STATUS_DELETED);
				diffList.add(updatedTo);
			} else {
				updatedTo = endurSideDataAsTo.get(endurSideDataAsTo.indexOf(knownTo));
				if (isDiffInAuxFields (knownTo, updatedTo)) {
					logger.info("Found TO '" + knownTo + "' that is present on Endur and contains changes in auxilliary fields :" + updatedTo);
					diffList.add(updatedTo);					
				}
			}
		}
		// add missing TOs not present in TOMS yet, but present in Endur
		for (T endurSideTo : endurSideDataAsTo) {
			if (!knownTos.contains(endurSideTo)) {
				logger.info("Found TO '" + endurSideTo + "' that is present on Endur but not in TOMS");
				diffList.add(endurSideTo);
			}
		}
		return diffList;
	}
}
