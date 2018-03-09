package com.olf.jm.tranfieldutil.model;

import java.util.List;

import com.olf.openjvs.enums.INS_TYPE_ENUM;

public interface TranFieldMetadata {

	public abstract INS_TYPE_ENUM getTriggeringInsType();

	public abstract TranFieldIdentificator getTrigger();

	public abstract TranFieldIdentificator getGuarded();

	public abstract List<AdditionalFilterCriterium> getAdditionalFilterCriteria();

}