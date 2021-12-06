package com.matthey.pmm.toms.service;

import java.util.ArrayList;
import java.util.List;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.service.misc.ReportBuilderHelper;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table;

public abstract class  AbstractToDiffService <T> {
	private Session session;

	public abstract String getSyncCategory ();
	
	protected abstract boolean isDiffInAuxFields(T knownTo, T updatedTo);

	protected abstract T updateLifeCycleStatus(T knownTo, DefaultReference lifecycleStatus);	
	
	protected abstract List<T> convertReportToTransferObjects(Table endurSideData);

	
	public AbstractToDiffService (final Session session) {
		this.session = session;
	}
	
	public List<T> createToListDifference(List<T> knownParties) {
		String reportName = ReportBuilderHelper.retrieveReportBuilderNameForSyncCategory (session.getIOFactory(), getSyncCategory());
		Table endurSideData  = ReportBuilderHelper.runReport(session.getTableFactory(), reportName);
		List<T> endurSideDataAsTo = convertReportToTransferObjects (endurSideData);
		List<T> diffList = createToDiff (knownParties, endurSideDataAsTo);
		return diffList;
	}
	
	private List<T> createToDiff( List<T> knownTos, List<T> endurSideDataAsTo) {
		List<T> diffList = new ArrayList<T>(knownTos.size() + endurSideDataAsTo.size());
		// update existing TOs present in TOMS that either have been modified 
		// or deleted. Ignore identical values.
		for (T knownTo : knownTos) {
			T updatedTo;
			if (!endurSideDataAsTo.contains(knownTo)) {
				updatedTo = updateLifeCycleStatus (knownTo, DefaultReference.LIFECYCLE_STATUS_DELETED);
				diffList.add(updatedTo);
			} else {
				updatedTo = endurSideDataAsTo.get(endurSideDataAsTo.indexOf(knownTo));
				if (isDiffInAuxFields (knownTo, updatedTo)) {
					diffList.add(updatedTo);					
				}
			}
		}
		// add missing TOs not present in TOMS yet, but present in Endur
		for (T endurSideTo : endurSideDataAsTo) {
			if (!knownTos.contains(endurSideTo)) {
				diffList.add(endurSideTo);
			}
		}
		return diffList;
	}
}
