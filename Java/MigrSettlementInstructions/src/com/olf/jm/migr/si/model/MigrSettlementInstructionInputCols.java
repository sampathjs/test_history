package com.olf.jm.migr.si.model;

public class MigrSettlementInstructionInputCols {
	private final String tableName;
	private final String oldTransactionIdColName;
	private final String ccyColName;
	private final String deliveryTypeColName;
	private final String businessUnitColName;
	private final String isInternalColName;
	private final String siColName;

	public MigrSettlementInstructionInputCols (final String tableName, final String oldTransactionIdColName,
			final String ccyColName, final String businessUnitColName, final String isInternalColName,
			final String deliveryTypeColName, final String siColName) {
		this.tableName = tableName;
		this.oldTransactionIdColName = oldTransactionIdColName;
		this.ccyColName = ccyColName;
		this.businessUnitColName = businessUnitColName;
		this.isInternalColName = isInternalColName;
		this.siColName = siColName;
		this.deliveryTypeColName = deliveryTypeColName;
	}

	public String getTableName() {
		return tableName;
	}

	public String getOldTransactionIdColName() {
		return oldTransactionIdColName;
	}

	public String getCcyColName() {
		return ccyColName;
	}
	
	public String getBusinessUnitColName() {
		return businessUnitColName;
	}

	public String getIsInternalColName() {
		return isInternalColName;
	}

	public String getSiColName() {
		return siColName;
	}

	protected String generateSql (String oldTransactionId) {
		StringBuilder sb = new StringBuilder();
		sb.append("\nSELECT ").append("tn.").append(ccyColName).append(" as ccy");
		sb.append("\n  , ").append("tn.").append(businessUnitColName).append(" as bu");
		sb.append("\n  , ").append("tn.").append(isInternalColName).append(" as is_internal");
		sb.append("\n  , ").append("tn.").append(deliveryTypeColName).append(" as dt");
		sb.append("\n  , ").append("tn.").append(siColName).append(" as si");
		sb.append("\nFROM ").append(tableName).append(" tn");
		sb.append("\nWHERE ").append("tn.").append(oldTransactionIdColName)
		.append(" = '").append(oldTransactionId).append("'");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((businessUnitColName == null) ? 0 : businessUnitColName
						.hashCode());
		result = prime * result
				+ ((ccyColName == null) ? 0 : ccyColName.hashCode());
		result = prime
				* result
				+ ((deliveryTypeColName == null) ? 0 : deliveryTypeColName
						.hashCode());
		result = prime
				* result
				+ ((isInternalColName == null) ? 0 : isInternalColName
						.hashCode());
		result = prime
				* result
				+ ((oldTransactionIdColName == null) ? 0
						: oldTransactionIdColName.hashCode());
		result = prime * result
				+ ((siColName == null) ? 0 : siColName.hashCode());
		result = prime * result
				+ ((tableName == null) ? 0 : tableName.hashCode());
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
		MigrSettlementInstructionInputCols other = (MigrSettlementInstructionInputCols) obj;
		if (businessUnitColName == null) {
			if (other.businessUnitColName != null)
				return false;
		} else if (!businessUnitColName.equals(other.businessUnitColName))
			return false;
		if (ccyColName == null) {
			if (other.ccyColName != null)
				return false;
		} else if (!ccyColName.equals(other.ccyColName))
			return false;
		if (deliveryTypeColName == null) {
			if (other.deliveryTypeColName != null)
				return false;
		} else if (!deliveryTypeColName.equals(other.deliveryTypeColName))
			return false;
		if (isInternalColName == null) {
			if (other.isInternalColName != null)
				return false;
		} else if (!isInternalColName.equals(other.isInternalColName))
			return false;
		if (oldTransactionIdColName == null) {
			if (other.oldTransactionIdColName != null)
				return false;
		} else if (!oldTransactionIdColName
				.equals(other.oldTransactionIdColName))
			return false;
		if (siColName == null) {
			if (other.siColName != null)
				return false;
		} else if (!siColName.equals(other.siColName))
			return false;
		if (tableName == null) {
			if (other.tableName != null)
				return false;
		} else if (!tableName.equals(other.tableName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MigrSettlementInstructionInputCols [tableName=" + tableName
				+ ", oldTransactionIdColName=" + oldTransactionIdColName
				+ ", ccyColName=" + ccyColName + ", deliveryTypeColName="
				+ deliveryTypeColName + ", businessUnitColName="
				+ businessUnitColName + ", isInternalColName="
				+ isInternalColName + ", siColName=" + siColName + "]";
	}
}
