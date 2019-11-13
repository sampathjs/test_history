package com.matthey.openlink.reporting.runner.generators;

import java.util.ArrayList;
import java.util.List;

import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportWithOutputParameter;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table; 


/** Report generator for reports that do not need any parameters
 * at runtime and no post processing of the output.
 * 
 *
 */
public class GenerateAndOverrideParameters extends ReportGeneratorBase implements IReportWithOutputParameter {

	private Table results = null;
	/** 
	 * @param newParameters parameters defining the report execution
	 */
	public GenerateAndOverrideParameters(final Session session, final IReportParameters newParameters) {
		super(session, newParameters);
	}

	@Override
	protected final boolean initialise() {
		return true;
	}

	@Override
	protected final Boolean postProcess() {
		results = super.output.cloneData();
		output.setName(getParameters().getParameterValue(NAME_PARAMETER));
		return super.postProcess();
	}
	

    @Override
    public List<String> getRequiredParameters() {

        List<String> requiredParameters = new ArrayList<String>(1);
        requiredParameters.add(IReportWithOutputParameter.OUTPUT_LOCATION_PARAMETER);
        requiredParameters.addAll(super.getRequiredParameters());
        return requiredParameters;
    }

    public Table getResults() {
    	return results;
    }

	@Override
	protected void finalize() throws Throwable {
		if (null != results) {
			/*** Instead of using Openlink logger use JM's JMLogging. ***/ 
			Logging.info("DESTROYING InMemory RB:" + results.getName());
			results.dispose();
		}
		super.finalize();
	}
	
	
}
