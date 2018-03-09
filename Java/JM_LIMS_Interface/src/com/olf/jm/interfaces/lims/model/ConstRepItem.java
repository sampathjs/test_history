package com.olf.jm.interfaces.lims.model;

public interface ConstRepItem {
	public static final String PR = "-pc-";

	public abstract String getValue();
	
	public abstract String getContext();
	
	public abstract String getSubContext ();
	
	public abstract String getVarName ();
	
	public abstract String getDefaultValue ();
}