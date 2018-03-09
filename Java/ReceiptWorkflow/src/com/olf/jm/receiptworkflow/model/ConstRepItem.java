package com.olf.jm.receiptworkflow.model;

public interface ConstRepItem {

	public abstract String getValue();
	
	public abstract String getContext();
	
	public abstract String getSubContext ();
	
	public abstract String getVarName ();
	
	public abstract String getDefaultValue ();
}