package com.matthey.reporting.transformer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.olf.embedded.application.Context;
import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.internal.OpenRiskException;
import com.olf.openrisk.io.Directory;
import com.olf.openrisk.io.File;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.Table;
/**
 * Transform ReportBuilder results using supplied xslt
 * 
 * 
 */
@ScriptCategory({ EnumScriptCategory.Generic })
public class Transformer extends AbstractGenericScript {


    private static final String OVERRIDE_OUTPUT_DIRECTORY = "TARGET_DIR";
	private static final String RB_OUTPUT_DIR = "OUTPUT_DIR";
    private static final String OVERRIDE_OUTPUT_FILENAME = "TARGET_FILENAME";
	private static final String RB_REPORT_NAME = "REPORT_NAME";
	private static final String REPORT_BUILDER_OUTPUT = "$$ReportBuilder_output$$";
	/* ConstRepository items follow
     */
    private static final String CONST_REPO_CONTEXT = "Reporting";
    private static final String CONST_REPO_SUBCONTEXT = "Transformer";
    private static final String DEFAULT_TRANSFORMATIONS_PATH = "Reporting\\Transformations";

    /*
     * Following constants are core values so are not suitable to change via ConstRepository
     */
	private static final String REPORTBUILDER_PARAMETER_NAME = "_name";
	private static final String REPORTBUILDER_PARAMETER_VALUE = "_value";

    private static final String REPORTBUILDER_RESULTS = "output_data";
    private static final String REPORTBUILDER_PARAMETERS = "output_parameters";

    private Session session = null;

    private static final Map<String, String>overrides = new HashMap<>(0);
    static {
    	overrides.put(RB_OUTPUT_DIR, OVERRIDE_OUTPUT_DIRECTORY);
    	overrides.put(RB_REPORT_NAME, OVERRIDE_OUTPUT_FILENAME);
    }
    
    @Override
    public Table execute(Context context, EnumScriptCategory category, ConstTable table) {

        session = context;

        String[] transformers = new String[ ] { "COMPANY_TRANSFORMATION",
                								"INSTRUMENT_TRANSFORMATION", 
                								"REGIONAL_TRANSFORMATION" };
        String[] transformersPath = DEFAULT_TRANSFORMATIONS_PATH.split("\\\\");

        //initialize(transformers, transformersPath);
		Logging.init(session, this.getClass(), "Tranformation", "");
		Logging.info("Started Tranformation");

        StringBuilder customXML = new StringBuilder();
        if (null == table) {
			Logging.info("Tranformation SKIPPED no data available...");
            return null;
        }
        int totalRows = table.getRowCount();

        String transformationTemplate = "UNDEFINED";
        String[] args = null;
        for (int row = 0; row < totalRows; row++) {
            try {

                Table parameters = table.getTable(REPORTBUILDER_PARAMETERS, row);
                for (String transformer : transformers) {

                    int templateRow = parameters.find(
							parameters.getColumnId(fecthPrefix(parameters) + REPORTBUILDER_PARAMETER_NAME), transformer,
							0);
                    if (templateRow >= 0) {
                        if (null == args)
                            args = determinePersistanceInformation(parameters, new String[ ] {
                                    RB_OUTPUT_DIR, RB_REPORT_NAME });
						transformationTemplate = parameters
								.getString(fecthPrefix(parameters) + 
                                REPORTBUILDER_PARAMETER_VALUE, templateRow);

						Logging.info("Perform transformation using " + transformationTemplate);
                        customXML.append(applyXSLTToXML(table.getTable(REPORTBUILDER_RESULTS, row)
                                .asXmlString(),
                                getXSLTResource(transformersPath, transformationTemplate,table.getTable(REPORTBUILDER_RESULTS, row).getName().replaceAll("[^A-Za-z0-9\\-]", "_"))));
                    }
                }
                parameters = null;

            } catch (IOException | TransformerException e) {

				Logging.error(String.format("ERR: Error performing transformation using %s", transformationTemplate),
						e);
                e.printStackTrace();
                throw new OpenRiskException("Error processing Transformation! ", e);
            }
        }

		Logging.info("Saving transformation result");
        persistTransformation(args, customXML.toString());
		Logging.info("Tranformation COMPLETED");
		Logging.close();
        return table.cloneData();
    }

    /**
     * Store the results of the transformation based on the runtime arguments
     * 
     * @param string
     *            is the result
     * @return
     */
    private void persistTransformation(String[] arguments, String string) {

        if (null == arguments)
            return;

        String outputFileName = arguments[0] + "\\" + arguments[1] +".xml";
        
		Logging.info(String.format("transformation filespec %s", outputFileName));
		Logging.info(String.format("Store transformation file %s", string));
        java.io.File destFile = new java.io.File(outputFileName);

        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(destFile));
            output.write(string);
            output.flush();
            output.close();

        } catch (Exception e) {
            String errorMessage = "Error creating result " + destFile.getAbsolutePath() + ". "
                    + e.getMessage();
			Logging.error(errorMessage, e);
            throw new com.matthey.reporting.transformer.TransformerException(errorMessage, e);
        }

    }

    /**
     * Obtain arguments for transformation output from ReportBuilder parameters<br>
     * Assumptions: The first two arguments are:-<br>
     * the path <br>
     * the filename<br>
     * [optionally] any other values recovered
     * 
     * @param parameters
     *            are the ReportBuilder parameters
     * @param args
     *            are the parameters to use to from the supplied table
     */
    private String[] determinePersistanceInformation(Table parameters, String[] args) {

        String[] arguments = new String[2];

        int targetArgument = 0;
        for (String string : args) {
            if (targetArgument < arguments.length) {
            	int rowFound = -1;
            	if (overrides.containsKey(string) /*string.equalsIgnoreCase(RB_OUTPUT_DIR)*/) {
                    rowFound = parameters.find(0, overrides.get(string)/*OVERRIDE_OUTPUT_DIRECTORY*/, 0);
                    if (rowFound < 0) 
                    	rowFound = parameters.find(0, string, 0);
                    
            	} else 
            		rowFound = parameters.find(0, string, 0);
            	
                if (rowFound >= 0) {

					arguments[targetArgument] = parameters
							.getString(fecthPrefix(parameters) + REPORTBUILDER_PARAMETER_VALUE,
                            rowFound);
                    targetArgument++;
                }

            }
        }
        return arguments;

    }

    /**
     * recover @param transformer from user sub-directory, actual path configurable
     * @param reportOutput 
     * 
     * @return transformation template, @Transfexception if not valid
     */
    private String getXSLTResource(String[] transformersPath, String transformer, String reportOutput) {

		Logging.info(String.format("Retieve Transformation script(%s)", transformer));
        Directory directory = session.getIOFactory().getRootDirectory();
        for (String path : transformersPath) {

			Logging.info(String.format("Checking for directory %s", directory.getPath() + path));
            if (directory.containsDirectory(path)) {
                directory = directory.getDirectory(path); // TODO: determine if we are leaking resources!
            }
        }

        if (!directory.getName().equalsIgnoreCase(transformersPath[transformersPath.length - 1])) {
            StringBuilder repositoryPath = new StringBuilder();
            for (String currentPath : transformersPath) {
                repositoryPath.append("/");
                repositoryPath.append(currentPath);
            }

            throw new com.matthey.reporting.transformer.TransformerException(String.format(
                    "Missing XLST repository! %s", repositoryPath.toString()));
        }

		Logging.info(String.format("Checking script exists(%s)?", transformer));
        if (!directory.containsFile(transformer))
            throw new com.matthey.reporting.transformer.TransformerException(String.format(
                    "XSLT repository file (%s) not found", transformer));

        File xslt = directory.getFile(transformer);

        String result;
        try {
            result = new String(xslt.retrieve(), "UTF-8");
            int customRBTable = result.indexOf(REPORT_BUILDER_OUTPUT);
            if (customRBTable>=0) {
            result=result.substring(0, customRBTable)+reportOutput+result.substring(customRBTable+REPORT_BUILDER_OUTPUT.length());
            }

        } catch (UnsupportedEncodingException e) {
			Logging.error(String.format("Unable to translate xslt file(%s)", transformer), e);
            result = "";
        }
		Logging.info("Transformation script loaded.");
        return result;
    }

    /**
     * transform XML based on supplied XSLT
     * 
     * @return
     * @throws IOException
     * @throws TransformerConfigurationException
     * @throws TransformerException
     */
    private String applyXSLTToXML(String sourceXML, String transformationTemplate)
            throws IOException, TransformerConfigurationException, TransformerException {

		Logging.info("Get transformer engine");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

		Logging.info("Create transformer instance");
        javax.xml.transform.Transformer transformer = transformerFactory
                .newTransformer(new StreamSource(new StringReader(transformationTemplate)));

        StringWriter out = new StringWriter();
		Logging.info("Transform data");
        transformer.transform(new StreamSource(new StringReader(sourceXML)), new StreamResult(out));

        out.flush();
        out.close();
        String result = out.toString();

		Logging.info(String.format("Return transformed result of %d bytes", result.length()));
        return result;
    }

    /**
     * apply argument overrides from ConstRepository This includes transformation arguments and control of logging
     * detail
     */
    private void initialize(String[] transformationGroup, String[] transformationPath) {

    }

	private String fecthPrefix(Table paramTable) {

		/* v17 change - Structure of output parameters table has changed. */

		String prefixBasedOnVersion = paramTable.getColumnNames().contains("expr_param_name") ? "expr_param"
				: "parameter";

		return prefixBasedOnVersion;
	}
}
