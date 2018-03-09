package com.matthey.openlink.reporting.runner.generators;

import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.olf.openrisk.application.Session;


/** Report generator for reports that do not need any parameters
 * at runtime and no post processing of the output.
 * 
 *
 */
public class GenerateNoParameters extends ReportGeneratorBase {

	/** 
	 * @param newParameters parameters defining the report execution
	 */
	public GenerateNoParameters(final Session session, final IReportParameters newParameters) {
		super(session, newParameters);
	}

	@Override
	protected final boolean initialise() {
		return true;
	}

	@Override
	protected final Boolean postProcess()  {
		
		return true;
	}

}
