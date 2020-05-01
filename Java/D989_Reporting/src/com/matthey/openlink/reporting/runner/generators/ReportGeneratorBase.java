package com.matthey.openlink.reporting.runner.generators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import com.matthey.openlink.reporting.runner.ReportRunnerException;
import com.matthey.openlink.reporting.runner.generators.legacyapi.ReportBuilder;
import com.matthey.openlink.reporting.runner.parameters.IReportNoParameters;
import com.matthey.openlink.reporting.runner.parameters.IReportParameters;
import com.matthey.openlink.utilities.Repository;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.Table; 
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;


/**
 * Report generator base class.
 */
public abstract class ReportGeneratorBase implements IReportGenerator, IReportNoParameters, IRequiredParameters {

    private static final String RB_DEF_PARAMETER_NAME = "parameter_name";
	private static final String RB_DEF_DATA_SOURCE = "data_source";
	private static final String RB_DEF_PARAMETER_DIRECTION = "parameter_direction";
	private static final String RB_DEF_PARAMETER_VALUE = "parameter_value";
	private ConstRepository implementationConstants;
	/** constants repository .
	 * 
	 */
	public static final String CONST_REPO_CONTEXT = "ReportRunner"; 
	public static final String CONST_REPO_SUBCONTEXT = ""; 
	
	
	/**
	 * Report name identifier in user table.
	 */
	protected static final String RB_DEF_REPORT_NAME_PARAMETER = "report_name";
	protected static final String REPORT_NAME_PARAMETER="Report Name";
	protected static final String RB_DEF_OUTPUT_NAME_PARAMETER ="OUTPUT_FILENAME";
	protected static final String OUTPUT_NAME_PARAMETER="Output Name";
	protected static final String RB_DEF_ALL_DATA_SOURCE = "ALL";
	protected static final String DATA_SOURCE = RB_DEF_DATA_SOURCE;
	private static final String DATA_SOURCES_TO_REPORTNAME = "OVERRIDE DATASOURCE";
	
	protected static HashMap<String, String> configuration;
    static
    {
    	configuration = new HashMap<String, String>(0);
    	configuration.put(REPORT_NAME_PARAMETER,RB_DEF_REPORT_NAME_PARAMETER);
    	configuration.put(OUTPUT_NAME_PARAMETER,RB_DEF_OUTPUT_NAME_PARAMETER );
    	configuration.put("ALL_DATASOURCE",RB_DEF_ALL_DATA_SOURCE);
    	configuration.put(DATA_SOURCES_TO_REPORTNAME, "TBD");
    }

	private Properties properties;
	/**
	 * Parameters used to control the report generation.
	 */
	private IReportParameters parameters;
	
	/**
	 * Report builder object used to generate the report.
	 */
	private ReportBuilder reportBuilder;
	
	private Session session;
	
	
	protected Table output;
	private Set<DefinitionParameter> resultingParameters; 
	/**
	 * Construct a new generator base.
	 *
	 * @param newParameters parameters used to control the report creation.
	 * @throws OException
	 */
	public ReportGeneratorBase(final Session session, final IReportParameters newParameters) {
		parameters = newParameters;
		this.session = session;
		reportBuilder = null;
		
		try {
			//implementationConstants = new ConstRepository(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			Logging.init(session, ReportGeneratorBase.class, CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
			properties = Repository.getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);			
			
/*			PluginLog.init(implementationConstants.getStringValue("logLevel", "info"), 
					implementationConstants.getStringValue("logDir", this.session.getIOFactory().getReportDirectory()),
					implementationConstants.getStringValue("logFile", this.getClass().getSimpleName() + ".log"));
*/			
		} catch ( ConstantTypeException
				| ConstantNameException e) {
			throw new ReportRunnerException("Constant Repository Error:" +e.getLocalizedMessage(), e);
			
//		} catch (OException e) {
//			throw new ReportRunnerException("Legacy API Error:" +e.getLocalizedMessage(), e);

		} catch (Exception e) {
			throw new ReportRunnerException("Unexpected Error:" +e.getLocalizedMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see ReportRunner.Generators.IReportGenerator#generate()
	 */
	@Override
	public final boolean generate()  {
		try {
			createReportBuilder();
			
			if (!initialise()) {
				return false;
			}
			
			if (!runReport()) {
				return false;
			}
			
			if (!postProcess()) {
				return false;
			}
			
		} catch (Exception e) {
			String errorMessage = "Error running the report. " + e.getMessage();
			/*** Instead of using Openlink logger use JM's JMLogging. ***/ 
			Logging.error (errorMessage, e); 
			throw new ReportRunnerException(errorMessage, e);
			
		} finally {
			Logging.close();
			// TODO: this finally block need refactoring because current there are too many things happen inside it, but refactoring is out of scope for the upgrade project
			if (reportBuilder != null) {
				if (resultingParameters == null || resultingParameters.isEmpty()){
					resultingParameters=getRBDefParameters();
				}
				reportBuilder.dispose();
			}
			
		}
		
		return true;
	}
	
	/**
	 * Hook to allow derived classes to perform additional initialisation.
	 * @return true on success false otherwise
	 * @throws OException if error initialising the report
	 */
	protected abstract boolean initialise();
	
	/**
	 * Hook to allow derived classes to perform post report generation processing.
	 * @return true on success false otherwise
	 * * @throws OException if error postprocessing the report
	 * @throws OException 
	 */
	protected Boolean postProcess() {
		//destroy memory instance of report output
		if (null != output) {
			output.dispose();
		}
		return true;
	}
	
	/**
	 * Run the report builder task.
	 * @return true on success false otherwise
	 *
	 * @throws OException if error running the report
	 */
	protected final boolean runReport() /*throws OException*/ {
		
		output = session.getTableFactory().createTable("RESULT");
		reportBuilder.setOutputTable(output);
		//session.getDebug().viewTable(reportBuilder.getAllParameters())
		int result = reportBuilder.runReport();
		Logging.info( "Executed report " 
				+ reportBuilder.getParameter(RB_DEF_ALL_DATA_SOURCE, properties.getProperty(REPORT_NAME_PARAMETER).toUpperCase()) 
				+ " output file " + reportBuilder.getParameter(RB_DEF_ALL_DATA_SOURCE, properties.getProperty(OUTPUT_NAME_PARAMETER)));
		boolean returnCode = true;
		
		if (result != 1) {
			returnCode = false;
		}
		return returnCode;
	}
	
	/**
	 * Helper method to construct the report builder object.
	 *
	 */
	private void createReportBuilder() {
		
		if (!parameters.hasParameter(properties.getProperty(REPORT_NAME_PARAMETER))) {
			String errorMessage = "Parameter " + properties.getProperty(REPORT_NAME_PARAMETER) + " is not defined.";
			Logging.info(errorMessage);
			throw new ReportRunnerException(errorMessage);
		}
		
		String reportName = parameters.getParameterValue(properties.getProperty(REPORT_NAME_PARAMETER));
		reportBuilder = ReportBuilder.createNew(reportName);
		if (reportBuilder == null) {
			String errorMessage = "Error creating report builder for report " + reportName;
			Logging.info(errorMessage); 
			throw new ReportRunnerException(errorMessage);
		}
		overrideReportBuilderParameters(this,parameters, reportName);
	}
	
	
	/**
	 * TODO: review
	 * Override parameters if they are set to.
	 * @param reportBuilder
	 * @param parameters
	 */
	public void overrideReportBuilderParameters(IRequiredParameters report, IReportParameters parameters, String rpt) {
		
	    IReportParameters customParameters = parameters.getCustomParameters(report.getRequiredParameters());
	    // Get Any Report builder parameters to override
			Table params;
			try {
				params = this.reportBuilder.getAllParameters();
				
			} catch (/*O*/Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Logging.error("Unable to get parameters!!!", e); 
				throw new ReportRunnerException(e.getLocalizedMessage(), e);
			}
			Table dataSourceNames = session.getTableFactory().createTable();
			String reportParamPrefix = "#1";//FIXME
			try {
				if (params != null) {
					int totalParameters = params.getRowCount();
					for (int parameterRow = 0; parameterRow < totalParameters; parameterRow++) {
						//FIXME String rbParameterName=params.getString(colName, parameterRow);
						String rbDataSource=params.getString(RB_DEF_DATA_SOURCE, parameterRow);
						String rbParameter=params.getString(RB_DEF_PARAMETER_NAME, parameterRow);
						String rbDirection=params.getString(RB_DEF_PARAMETER_DIRECTION, parameterRow);
						String rbLookUp = rbParameter;
						
						if (customParameters.hasParameter(rbLookUp)) {
						    if (this.properties.getProperty(DATA_SOURCES_TO_REPORTNAME).equalsIgnoreCase(rbDataSource)){
						    	rbDataSource=rpt;
						    }
							String rbParameterValue = customParameters.getParameterValue(rbLookUp)/*parameters.getParameterValue(rbLookUp)*/;
							setReportParameter(reportBuilder, rbDataSource, rbParameter, rbParameterValue, params, parameterRow);
						}
							
					}
				}
			}
			finally {
				params.dispose();
				dataSourceNames.dispose();
			}

	}
	
	/**
	 * get <b>all</b> ReportBuilder definition parameters defined and the current value
	 */
	
	public Set<DefinitionParameter> getDefinitonParameters() {
		if (resultingParameters.isEmpty()){
			resultingParameters = getRBDefParameters();
		}
		return resultingParameters;
	}
	
	private Set<DefinitionParameter> getRBDefParameters() {
		
		Table params;
		try {
			params = this.reportBuilder.getAllParameters();
			
			
		} catch (/*O*/Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Logging.error("Unable to get parameters!!!", e); 
			throw new ReportRunnerException(e.getLocalizedMessage(), e);
		}
		
		Set<DefinitionParameter> parameters = new HashSet<>(0);
		try {
			if (params != null) {
				int totalParameters = params.getRowCount();
				for (int parameterRow = 0; parameterRow < totalParameters; parameterRow++) {
					String rbDataSource=params.getString(RB_DEF_DATA_SOURCE, parameterRow);
					String rbParameter=params.getString(RB_DEF_PARAMETER_NAME, parameterRow);
					String rbDirection=params.getString(RB_DEF_PARAMETER_DIRECTION, parameterRow);
					parameters.add(new DefinitionParameter(params.getString(RB_DEF_DATA_SOURCE, parameterRow), params.getString(RB_DEF_PARAMETER_NAME, parameterRow), params.getString(RB_DEF_PARAMETER_VALUE, parameterRow)));				
				}
			}
		}
		finally {
			params.dispose();
		}
		return parameters;
	}

	
    /**
     * Sets the ReportBuilder object's parameters to the provided value for the provided datasources.
     * TODO: Making ReportRunnerUtils an instantiatable class would allow it to have it's own reference 
     * to the log file for better progress logging. 
     * @param rb
     * @param dataSources
     * @param parameterName
     * @param parameterValue
     * @param params
     * @return
     * @throws OException
     */
    public void setReportParameter(ReportBuilder rb, String dataSource, String parameterName, String parameterValue, Table params, int row) {

        try {

			int numRows = params.getRowCount();
			if ((params.getString(RB_DEF_DATA_SOURCE, row).equals(this.properties.getProperty(DATA_SOURCES_TO_REPORTNAME)) ||params.getString(RB_DEF_DATA_SOURCE, row).equals(dataSource)) && params.getString(RB_DEF_PARAMETER_NAME, row).equals(parameterName)) {
				String value = params.getString("parameter_value", row);
				
				Logging.info(String.format("Parameter %s for datasource %s is currently %s", parameterName, dataSource, value));
				rb.setParameter(dataSource, parameterName, parameterValue);
				Logging.info(String.format("Parameter %s for datasource %s now set to %s", parameterName, dataSource, parameterValue));
				
			}
			
		} catch (Exception oex) {
			Logging.error(oex.getLocalizedMessage(), oex);
			oex.printStackTrace();
			throw new ReportRunnerException("ERR: " + oex.getLocalizedMessage(), oex);
		}
        
        return;
    }

	
	/**
	 * Parameters used to control the report generation.
	 */
	@java.lang.SuppressWarnings("all")
	protected IReportParameters getParameters() {
		return this.parameters;
	}
	
	/**
	 * Report builder object used to generate the report.
	 */
	//@java.lang.SuppressWarnings("all")
	protected ReportBuilder getReportBuilder() {
		return this.reportBuilder;
	}
	
	
	protected Session getSession() {
		return this.session;
	}

	
   @Override
    public List<String> getRequiredParameters() {
        
        return Arrays.asList(new String[ ] {IRequiredParameters.TYPE_PARAMETER, IRequiredParameters.NAME_PARAMETER});
   }

	
}