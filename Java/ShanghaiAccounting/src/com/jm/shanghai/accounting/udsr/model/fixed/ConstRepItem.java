package com.jm.shanghai.accounting.udsr.model.fixed;

public interface ConstRepItem {
	public static final String PR = "-pc-";

	public abstract String getValue();
	
	public abstract String getContext();
	
	public abstract String getSubContext ();
	
	public abstract String getVarName ();
	
	public abstract String getDefaultValue ();
}