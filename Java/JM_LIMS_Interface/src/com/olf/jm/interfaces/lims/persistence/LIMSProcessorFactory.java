package com.olf.jm.interfaces.lims.persistence;

import com.olf.embedded.application.Context;
import com.olf.jm.interfaces.lims.model.RelNomField;
import com.olf.openrisk.scheduling.Batch;
import com.olf.openrisk.scheduling.Nomination;
import com.olf.jm.logging.Logging;


/**
 * A factory for creating LIMSProcessor objects to set the measures on JM brand and non JM brand nominations.
 */
public class LIMSProcessorFactory {
	
	/** The context the script is running in. */
	private Context context;
	
	/**
	 * Instantiates a new LIMS processor factory.
	 *
	 * @param context the context the script is running in
	 */
	public LIMSProcessorFactory(Context context) {
		this.context = context;
	}

	/**
	 * Gets the processor relevant for the nomination being processed.
	 *
	 * @param nom the nomination to be processed
	 * @return the processor
	 */
	public LIMSProcessor getProcessor(Nomination nom) {
		if(!isRelevant(nom)) {
			return null;
		}
		
		if(isJohnsonMattheyBrand(nom)) {
			Logging.debug("Creating a JM Brand Processor");
			return new JmBrandProcessor(nom, context);
					
		} else {
			Logging.debug("Creating a non JM Brand Processor");
			return new NonJmBrandProcessor(nom, context);
		}
	}
	
	/**
	 * Checks if the nomination is johnson matthey brand.
	 *
	 * @param nom the nomination being processed
	 * @return true, if is johnson matthey brand
	 */
	private boolean isJohnsonMattheyBrand(Nomination nom) {
		//return RelNomField.BRAND.guardedGetString(nom).contains("Johnson Matthey");
		return RelNomField.BRAND.guardedGetString(nom).contains("Johnson Matthey UK");
	}
	
	/**
	 * Checks if is relevant for setting measures on.
	 *
	 * @param nom the nomination being processed
	 * @return true, if is relevant for processing
	 */
	private boolean isRelevant(Nomination nom) {
		if (!(nom instanceof Batch)) {
			Logging.debug("Nom is not of type bactch skipping");
			return false;
		}
		Batch batch = (Batch) nom;
		if (!RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Receipt") 
				&& !RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Inventory")
				) {
			Logging.debug("Noms activity id is not Warehouse Receipt/Warehouse Inventory skipping");
			return false;
		}


		final String brand = RelNomField.BRAND.guardedGetString(nom);
		final String category = RelNomField.CATEGORY.guardedGetString(nom);
		if (brand.isEmpty() || category.isEmpty()) {
			Logging.debug("Noms brand or category is missing skipping");
			return false;
		}


		if ( RelNomField.ACTIVITY_ID.guardedGetString(batch).equals("Warehouse Inventory")) {
			Logging.debug("Noms is Warehouse Inventory so skipping");
			return false;
		}
			
		Logging.debug("Noms is valid for setting measures");
		return true;
	}	
}
