package com.olf.jm.interfaces.lims.persistence;

import com.olf.jm.interfaces.lims.model.MeasuresWithSource;


public interface LIMSProcessor {

		
	boolean coaBypass();
	
	boolean specComplete();
	
	MeasuresWithSource setMeasures();
}
