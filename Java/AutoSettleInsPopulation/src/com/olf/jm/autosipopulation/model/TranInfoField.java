package com.olf.jm.autosipopulation.model;

/*
 * History:
 * 2015-04-28	V1.0	jwaechter	- initial version
 * 2015-11-27	V1.1	jwaechter	- added SI_PHYS
 * 2016-01-28	V1.2	jwaechter	- added USE_AUTO_SI_SHORTLIST
 *                                  - added USE_AUTO_SI
 * 2016-03-16	V1.3	jwaechter	- added SI_PHYS_TRAN	
 * 2016-06-06	V1.4	jwaechter	- added SI_PHYS_INTERNAL, SI_PHYS_INTERNAL_TRAN
 */

/**
 * Enum containing relevant tran info fields (on tran level) for the 
 * settlement assignment instructions. 
 * @author jwaechter
 * @version 1.4
 */
public enum TranInfoField {
	Loco("Loco"), Form ("Form"), AllocationType ("Allocation Type"),
	SI_PHYS("SI-Phys"), SI_PHYS_INTERNAL ("SI-Phys Internal"),	
	SI_PHYS_TRAN ("SI-Phys-Tran"), SI_PHYS_INTERNAL_TRAN ("SI-Phys Internal-Tran"), 
	DISPATCH_STATUS("Dispatch Status"),
	USE_AUTO_SI_SHORTLIST("Auto SI Shortlist"),
	USE_AUTO_SI("Auto SI Check")
	;
	
	private final String name;
	
	private TranInfoField (String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

}
