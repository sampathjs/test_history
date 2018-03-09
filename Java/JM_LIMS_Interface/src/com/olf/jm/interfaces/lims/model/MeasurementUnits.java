package com.olf.jm.interfaces.lims.model;

public enum MeasurementUnits {
	PPMMETAL ("ppm"), PERCENTWW ("%"), PPM_WW("ppm"), NONE("currency");
	
	private final String endurName;
	
	private MeasurementUnits (final String endurName) {
		this.endurName = endurName;
	}
	
	public String getEndurName () {
		return endurName;
	}
}
