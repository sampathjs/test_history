package com.olf.jm.autosipopulation.model;

import java.util.HashSet;
import java.util.Set;

import com.olf.jm.autosipopulation.model.Pair;

/*
 * History:
 * 2015-DD-MM	V1.0	jwaechter	- initial version
 * 2016-01-28	V1.1	jwaechter	- added useShortList
 */

/**
 * Class containing all data from settlement instruction / account
 * that are relevant to be matched to {@link DecisionData}.
 * @author jwaechter
 * @version 1.1
 */
public class SettleInsAndAcctData {
	private final int accountId;
	private final int siId;
	private int currencyId;
	private int siPartyId;
	private int internalExternal;
	private String loco;
	private String form;
	private String allocationType;
	private boolean useShortList;
	
	/**
	 * Set of all allowed combinations of currency / delivery type 
	 */
	private final Set<Pair<Integer, Integer>> deliveryInfo;

	/**
	 * Set of all allowed combinations of allowed instruments
	 */
	private final Set<Integer> instrumentInfo;

	
	public SettleInsAndAcctData (final int accountId, final int siId) {
		this.accountId = accountId;
		this.siId = siId;
		this.deliveryInfo = new HashSet<> ();
		this.instrumentInfo = new HashSet<> ();
	}
	
	public void addDeliveryInfo (final int currencyId, final int deliveryType) {
		deliveryInfo.add(new Pair<> (currencyId, deliveryType));
	}

	public void addDeliveryInfo (final Pair<Integer, Integer> curAndDel) {
		deliveryInfo.add(curAndDel);
	}

	public void addInstrument (final int insNum) {
		instrumentInfo.add(insNum);
	}
	
	public boolean containsInstrument (final int insNum) {
		return instrumentInfo.contains(insNum);
	}

	public int getSiPartyId() {
		return siPartyId;
	}

	public void setSiPartyId(int siPartyId) {
		this.siPartyId = siPartyId;
	}

	public int getInternalExternal() {
		return internalExternal;
	}

	public void setInternalExternal(int internalExternal) {
		this.internalExternal = internalExternal;
	}

	public String getLoco() {
		return loco;
	}

	public void setLoco(String loco) {
		this.loco = loco;
	}

	public String getForm() {
		return form;
	}

	public void setForm(String form) {
		this.form = form;
	}

	public String getAllocationType() {
		return allocationType;
	}

	public void setAllocationType(String allocationType) {
		this.allocationType = allocationType;
	}

	public int getAccountId() {
		return accountId;
	}

	public int getSiId() {
		return siId;
	}

	/**
	 * Returns set of all allowed combinations of currency / delivery type 
	 */
	public Set<Pair<Integer, Integer>> getDeliveryInfo() {
		return deliveryInfo;
	}

	public boolean isUseShortList() {
		return useShortList;
	}

	public void setUseShortList(boolean useShortList) {
		this.useShortList = useShortList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + accountId;
		result = prime * result + siId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SettleInsAndAcctData other = (SettleInsAndAcctData) obj;
		if (accountId != other.accountId)
			return false;
		if (siId != other.siId)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SettleInsAndAcctData [accountId=" + accountId + ", siId="
				+ siId + ", currencyId=" + currencyId + ", siPartyId="
				+ siPartyId + ", internalExternal=" + internalExternal
				+ ", loco=" + loco + ", form=" + form + ", allocationType="
				+ allocationType + ", deliveryInfo=" + deliveryInfo
				+ ", instrumentInfo=" + instrumentInfo + "]";
	}
}
