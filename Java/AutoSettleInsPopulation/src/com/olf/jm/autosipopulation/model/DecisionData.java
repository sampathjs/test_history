package com.olf.jm.autosipopulation.model;

import java.util.Set;
/*
 * History:
 * 2015-06-12	V1.0	jwaechter	- initial version
 * 2015-08-24   V1.1 	jwaechter	- added isCommPhys
 * 2015-09-29	V1.2	jwaechter	- added cloning constructor
 * 2016-01-28	V1.3	jwaechter	- added useShortList
 * 2016-02-23	V1.4	jwaechter	- added siPhys
 * 2016-06-06	V1.5	jwaechter	- added siPhysInternal
 * 2017-05-31	V1.6	jwaechter	- replaced placeholder for offset tran type
 *                                    with real value in the flipInternalExternal method                                   
 * 2018-01-08   V1.7    scurran     - add support for offset type Pass Thru Offset
 * 2021-02-26   V1.8    Prashanth   - EPI-1825   - Fix for missing SI for FX deals booked via SAP and re-factoring logs
 * 
 *  */


import com.olf.jm.autosipopulation.model.Pair;

/**
 * Class containing all data of a transaction necessary to decide which settlement
 * instructions are allowed according to the custom logic. 
 * 
 * @author jwaechter
 * @version 1.7
 */

public class DecisionData {
	private final int tranNum;
	private final int legNum;
	private int intPartyId;
	private int extPartyId;
	private int ccyId;
	private int intSettleId;
	private int extSettleId;
	private int deliveryTypeId;
	private int internalExternal;
	private int insType;
	private String intSettleName;
	private String extSettleName;
	private SavedUnsaved savedUnsavedInt;
	private SavedUnsaved savedUnsavedExt;
	private String loco;
	private String form;
	private String formPhys;
	private String allocationType;
	private boolean isCashTran;
	private boolean isCommPhys;
	private String offsetTranType;
	private boolean useShortList;
	private int	siPhys=-1;
	private int	siPhysInternal=-1;
	private boolean hasBeenFlipped = false;
	
	private Set<Pair<Integer, String>> posCoreIntSIs;
	private Set<Pair<Integer, String>> posCoreExtSIs;
	
	/**
	 * Cloning constructor
	 * @param toBeCloned
	 */
	public DecisionData (int tranNum, int legNum, DecisionData toBeCloned) {
		this.tranNum = tranNum;
		this.legNum = legNum;
		this.intPartyId = toBeCloned.intPartyId;
		this.extPartyId = toBeCloned.extPartyId;
		this.ccyId = toBeCloned.ccyId;
		this.intSettleId = toBeCloned.intSettleId;
		this.extSettleId = toBeCloned.extSettleId;
		this.deliveryTypeId = toBeCloned.deliveryTypeId;
		this.internalExternal = toBeCloned.internalExternal;
		this.insType = toBeCloned.insType;
		this.intSettleName = toBeCloned.intSettleName;
		this.extSettleName = toBeCloned.extSettleName;
		this.savedUnsavedInt = toBeCloned.savedUnsavedInt;
		this.savedUnsavedExt = toBeCloned.savedUnsavedExt;
		this.loco = toBeCloned.loco;
		this.form = toBeCloned.form;
		this.formPhys = toBeCloned.formPhys;
		this.allocationType = toBeCloned.allocationType;
		this.isCashTran = toBeCloned.isCashTran;
		this.isCommPhys = toBeCloned.isCommPhys;
		this.offsetTranType = toBeCloned.offsetTranType;
		this.posCoreIntSIs = toBeCloned.posCoreIntSIs;
		this.posCoreExtSIs = toBeCloned.posCoreExtSIs;
		this.useShortList = toBeCloned.useShortList;
		this.siPhys = toBeCloned.siPhys;
		this.siPhysInternal = toBeCloned.siPhysInternal;
		this.hasBeenFlipped = toBeCloned.hasBeenFlipped;
	}
	
	public DecisionData (int tranNum, int legNum) {
		this.tranNum = tranNum;
		this.legNum = legNum;
	}
	
	
	/**
	 * Flips internal with external Settle IDs and names if processing a mirror deal
	 */
	public void flipInternalExternal () {
		if (!(offsetTranType.equals("Generated Offset") ||  offsetTranType.equals("Pass Thru Offset") ) || hasBeenFlipped) {
			return;
		}
		int tempId = extSettleId;
		String tempName = extSettleName;
		extSettleId    = intSettleId;
		extSettleName  = intSettleName;
		intSettleId = tempId;
		intSettleName = tempName;
		tempId = siPhys;
		siPhys = siPhysInternal;
		siPhysInternal = tempId;
		tempId = intPartyId;
		intPartyId = extPartyId;
		extPartyId = tempId;
		savedUnsavedExt = (savedUnsavedExt == SavedUnsaved.SAVED)?SavedUnsaved.UNSAVED:SavedUnsaved.SAVED;
		savedUnsavedInt = (savedUnsavedInt == SavedUnsaved.SAVED)?SavedUnsaved.UNSAVED:SavedUnsaved.SAVED;
		hasBeenFlipped = true;
	}

	public int getCcyId() {
		return ccyId;
	}

	public void setCcyId(int ccyId) {
		this.ccyId = ccyId;
	}

	public int getDeliveryTypeId() {
		return deliveryTypeId;
	}

	public void setDeliveryTypeId(int deliveryTypeId) {
		this.deliveryTypeId = deliveryTypeId;
	}

	public int getInternalExternal() {
		return internalExternal;
	}

	public void setInternalExternal(int internalExternal) {
		this.internalExternal = internalExternal;
	}

	public SavedUnsaved getSavedUnsavedInt() {
		return savedUnsavedInt;
	}

	public void setSavedUnsavedInt(SavedUnsaved savedUnsaved) {
		this.savedUnsavedInt = savedUnsaved;
	}

	public SavedUnsaved getSavedUnsavedExt() {
		return savedUnsavedExt;
	}

	public void setSavedUnsavedExt(SavedUnsaved savedUnsaved) {
		this.savedUnsavedExt = savedUnsaved;
	}

	
	public int getTranNum() {
		return tranNum;
	}

	public int getInsType() {
		return insType;
	}

	public void setInsType(int insType) {
		this.insType = insType;
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
	
	public String getFormPhys() {
		return formPhys;
	}

	public void setFormPhys(String formPhys) {
		this.formPhys = formPhys;
	}

	public String getAllocationType() {
		return allocationType;
	}

	public void setAllocationType(String allocationType) {
		this.allocationType = allocationType;
	}

	public int getLegNum() {
		return legNum;
	}

	public boolean isCashTran() {
		return isCashTran;
	}

	public void setCashTran(boolean isCashTran) {
		this.isCashTran = isCashTran;
	}

	public int getIntPartyId() {
		return intPartyId;
	}

	public void setIntPartyId(int intPartyId) {
		this.intPartyId = intPartyId;
	}

	public int getExtPartyId() {
		return extPartyId;
	}

	public void setExtPartyId(int extPartyId) {
		this.extPartyId = extPartyId;
	}

	public int getIntSettleId() {
		return intSettleId;
	}

	public void setIntSettleId(int intSettleId) {
		this.intSettleId = intSettleId;
	}

	public int getExtSettleId() {
		return extSettleId;
	}

	public void setExtSettleId(int extSettleId) {
		this.extSettleId = extSettleId;
	}

	public String getIntSettleName() {
		return intSettleName;
	}

	public void setIntSettleName(String intSettleName) {
		this.intSettleName = intSettleName;
	}

	public String getExtSettleName() {
		return extSettleName;
	}

	public void setExtSettleName(String extSettleName) {
		this.extSettleName = extSettleName;
	}
	
	

	public Set<Pair<Integer, String>> getPosCoreIntSIs() {
		return posCoreIntSIs;
	}

	public void setPosCoreIntSIs(Set<Pair<Integer, String>> corePossIntSIs) {
		this.posCoreIntSIs = corePossIntSIs;
	}

	public Set<Pair<Integer, String>> getPosCoreExtSIs() {
		return posCoreExtSIs;
	}

	public void setPosCoreExtSIs(Set<Pair<Integer, String>> corePossExtSIs) {
		this.posCoreExtSIs = corePossExtSIs;
	}

	public boolean isCommPhys() {
		return isCommPhys;
	}

	public void setCommPhys(boolean isCommPhys) {
		this.isCommPhys = isCommPhys;
	}
	
	public boolean isUseShortList() {
		return useShortList;
	}

	public void setUseShortList(boolean useShortList) {
		this.useShortList = useShortList;
	}
	
	

	public int getSiPhys() {
		return siPhys;
	}

	public void setSiPhys(int siPhys) {
		this.siPhys = siPhys;
	}
	
	public int getSiPhysInternal() {
		return siPhysInternal;
	}

	public void setSiPhysInternal(int siPhysInternal) {
		this.siPhysInternal = siPhysInternal;
	}

	public String getOffsetTranType() {
		return offsetTranType;
	}

	public void setOffsetTranType(String offsetTranType) {
		this.offsetTranType = offsetTranType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((allocationType == null) ? 0 : allocationType.hashCode());
		result = prime * result + ccyId;
		result = prime * result + deliveryTypeId;
		result = prime * result + extPartyId;
		result = prime * result + extSettleId;
		result = prime * result + ((extSettleName == null) ? 0 : extSettleName.hashCode());
		result = prime * result + ((form == null) ? 0 : form.hashCode());
		result = prime * result + ((formPhys == null) ? 0 : formPhys.hashCode());
		result = prime * result + insType;
		result = prime * result + intPartyId;
		result = prime * result + intSettleId;
		result = prime * result	+ ((intSettleName == null) ? 0 : intSettleName.hashCode());
		result = prime * result + internalExternal;
		result = prime * result + (isCashTran ? 1231 : 1237);
		result = prime * result + (isCommPhys ? 1231 : 1237);
		result = prime * result + legNum;
		result = prime * result + ((loco == null) ? 0 : loco.hashCode());
		result = prime * result	+ ((offsetTranType == null) ? 0 : offsetTranType.hashCode());
		result = prime * result	+ ((posCoreExtSIs == null) ? 0 : posCoreExtSIs.hashCode());
		result = prime * result	+ ((posCoreIntSIs == null) ? 0 : posCoreIntSIs.hashCode());
		result = prime * result	+ ((savedUnsavedExt == null) ? 0 : savedUnsavedExt.hashCode());
		result = prime * result	+ ((savedUnsavedInt == null) ? 0 : savedUnsavedInt.hashCode());
		result = prime * result + siPhys;
		result = prime * result + siPhysInternal;
		result = prime * result + tranNum;
		result = prime * result + (useShortList ? 1231 : 1237);
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
		DecisionData other = (DecisionData) obj;
		if (allocationType == null) {
			if (other.allocationType != null)
				return false;
		} else if (!allocationType.equals(other.allocationType))
			return false;
		if (ccyId != other.ccyId)
			return false;
		if (deliveryTypeId != other.deliveryTypeId)
			return false;
		if (extPartyId != other.extPartyId)
			return false;
		if (extSettleId != other.extSettleId)
			return false;
		if (extSettleName == null) {
			if (other.extSettleName != null)
				return false;
		} else if (!extSettleName.equals(other.extSettleName))
			return false;
		if (form == null) {
			if (other.form != null)
				return false;
		} else if (!form.equals(other.form))
			return false;
		if (formPhys == null) {
			if (other.formPhys != null)
				return false;
		} else if (!formPhys.equals(other.formPhys))
			return false;
		if (insType != other.insType)
			return false;
		if (intPartyId != other.intPartyId)
			return false;
		if (intSettleId != other.intSettleId)
			return false;
		if (intSettleName == null) {
			if (other.intSettleName != null)
				return false;
		} else if (!intSettleName.equals(other.intSettleName))
			return false;
		if (internalExternal != other.internalExternal)
			return false;
		if (isCashTran != other.isCashTran)
			return false;
		if (isCommPhys != other.isCommPhys)
			return false;
		if (legNum != other.legNum)
			return false;
		if (loco == null) {
			if (other.loco != null)
				return false;
		} else if (!loco.equals(other.loco))
			return false;
		if (offsetTranType == null) {
			if (other.offsetTranType != null)
				return false;
		} else if (!offsetTranType.equals(other.offsetTranType))
			return false;
		if (posCoreExtSIs == null) {
			if (other.posCoreExtSIs != null)
				return false;
		} else if (!posCoreExtSIs.equals(other.posCoreExtSIs))
			return false;
		if (posCoreIntSIs == null) {
			if (other.posCoreIntSIs != null)
				return false;
		} else if (!posCoreIntSIs.equals(other.posCoreIntSIs))
			return false;
		if (savedUnsavedExt != other.savedUnsavedExt)
			return false;
		if (savedUnsavedInt != other.savedUnsavedInt)
			return false;
		if (siPhys != other.siPhys)
			return false;
		if (siPhysInternal != other.siPhysInternal)
			return false;
		if (tranNum != other.tranNum)
			return false;
		if (useShortList != other.useShortList)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DecisionData [tranNum=" + tranNum + ", legNum=" + legNum + ", intPartyId=" + intPartyId
				+ ", extPartyId=" + extPartyId + ", ccyId=" + ccyId + ", intSettleId=" + intSettleId + ", extSettleId="
				+ extSettleId + ", deliveryTypeId=" + deliveryTypeId + ", internalExternal=" + internalExternal
				+ ", insType=" + insType + "\n, intSettleName=" + intSettleName + ", extSettleName=" + extSettleName
				+ ", savedUnsavedInt=" + savedUnsavedInt + ", savedUnsavedExt=" + savedUnsavedExt + "\n, loco=" + loco
				+ ", form=" + form + ", formPhys=" + formPhys + ", allocationType=" + allocationType + ", isCashTran="
				+ isCashTran + ", isCommPhys=" + isCommPhys + ", offsetTranType=" + offsetTranType + ", useShortList="
				+ useShortList + ", siPhys=" + siPhys + ", siPhysInternal=" + siPhysInternal + "\n, posCoreIntSIs="
				+ posCoreIntSIs + "\n, posCoreExtSIs=" + posCoreExtSIs + "]";
	}
}
