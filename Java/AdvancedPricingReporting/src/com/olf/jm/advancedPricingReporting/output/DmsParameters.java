package com.olf.jm.advancedPricingReporting.output;

import com.olf.embedded.application.Context;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;


/*
 * History:
 * 2017-07-23 - V0.1 - scurran - Initial Version
 */

/**
 * The Class DmsParameters. Defines the report writer parameters when outputting via DMS
 */
public class DmsParameters implements ReportWriterParameters {

	/** The current script context. */
	private final Context currentContext;
	
	/** The const repository used to load the template name. */
	private final ConstRepository constRep;
	
	/** The name of the default template to use with DMS */
	final static String DEFAULT_TEMPLATE =  "/User/DMS_Repository/Categories/AdvancedDeferredPricing/DocumentTypes/ApDpReport/Templates/AdvancedDeferredPricingMultiMetal.olt";
	/**
	 * Instantiates a new dms parameters.
	 *
	 * @param context the script context
	 */
	public DmsParameters( Context context, ConstRepository constRep) {
		currentContext = context;
			
		this.constRep = constRep;
		
		if(this.constRep == null) {
			throw new RuntimeException("Error initialising the dms parameters object. Invalid const repository.");
		}
	}
	
	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.output.ReportWriterParameters#getTemplateName()
	 */
	@Override
	public String getTemplateName() {
		String template;
		try {
			template = constRep.getStringValue("dms_template_name", DEFAULT_TEMPLATE);
		} catch (Exception e) {
			String errorMessage = "Error reading the DMS template name. " + e.getMessage();
			Logging.error(errorMessage);
			throw new RuntimeException(errorMessage);
		}

		return template;
	}

	/* (non-Javadoc)
	 * @see com.olf.jm.advancedPricingReporting.output.ReportWriterParameters#getOutputLocation()
	 */
	@Override
	public String getOutputLocation() {
		return currentContext.getIOFactory().getReportDirectory();
	}

}
