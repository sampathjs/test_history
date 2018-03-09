package com.olf.jm.fxcounteramountrounding.model;

public interface ConstRepItem {
	public static final String PR = "-pc-";

	public abstract String getValue();
	
	public abstract String getContext();
	
	public abstract String getSubContext ();
	
	public abstract String getVarName ();
	
	public abstract String getDefaultValue ();
}