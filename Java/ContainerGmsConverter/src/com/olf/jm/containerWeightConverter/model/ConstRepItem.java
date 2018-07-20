package com.olf.jm.containerWeightConverter.model;

/*
 * History:
 * 2017-10-18	V1.0	scurran	-	Initial Version
 */

public interface ConstRepItem {
	public abstract String getValue();
	
	public abstract String getContext();
	
	public abstract String getSubContext ();
	
	public abstract String getVarName ();
	
	public abstract String getDefaultValue ();
}
