// !!! see also the twin called JM_OUT_DocOutput_wMail differing only in super class !!!
package com.openlink.jm.bo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

/*
 * History:
 * 2020-01-31	V1.0	-	YadavP03	- Added method to set/Reset Document Info field when the script succeeds and fails
 * 2020-03-25   V1.1        YadavP03  	- memory leaks, remove console prints & formatting changes 
 */

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_OUTPUT)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
public class JM_OUT_DocOutput extends com.openlink.jm.bo.docoutput.BO_DocOutput
{
	public static Properties getConfiguration(final String context,
			final String subContext, final Map<String, String> properties) {

		Properties config = new Properties();
		try {
			ConstRepository implementationConstants = null;
			
			if (context != null){
				implementationConstants = new ConstRepository(context,subContext);
			}
			
			for (java.util.Map.Entry<String, String> property : properties.entrySet()) {
				if (context != null) {
					config.put( property.getKey(), implementationConstants.getStringValue(property.getKey(), property.getValue()));
				} else {
					config.put(property.getKey(), property.getValue());
				}
				OConsole.message(String.format("KEY: %s \t\t VALUE:%s\n",property.getKey(), config.getProperty(property.getKey())));
			}

		} catch (OException e) {
			OConsole.message("constant repository problem" + e.toString());
			throw new RuntimeException("constant repository problem:CAUSE>" + e.getLocalizedMessage(), e);
		}
		return config;
	}
	
	private final String DOC_STATUS_CANCELLED = "Cancelled";

	public void execute(IContainerContext context) throws OException
	{
		properties = getConfiguration(CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT, configuration);
		initLogging ();
		resetRegenrateDocInfo(context.getArgumentsTable().getTable("process_data", 1), EnumRegenrateOutput.YES);
		Table argt = context.getArgumentsTable();
		
		Table tblProcessData = argt.getTable("process_data", 1);
		if (DOC_STATUS_CANCELLED.equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, tblProcessData.getInt("doc_status", 1)))) {
			
			String xmlData = tblProcessData.getTable("xml_data", 1).getTable("XmlData",1).getString("XmlData", 1);
			Table tblUserData = tblProcessData.getTable("user_data", 1);

			int userDataNumcols = tblUserData.getNumCols();
			for (int i = 0; ++i <=userDataNumcols;){
				argt.insertCol(tblUserData.getColName(i), i, COL_TYPE_ENUM.fromInt(tblUserData.getColType(i)));
			}
			
			tblUserData.copyRowAddAllByColName(argt);

			int row = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			Table tblEventData = argt.getTable("doc_table", row);
			tblEventData.select(tblProcessData, "doc_status(next_doc_status),last_doc_status(curr_doc_status)", "document_num EQ $document_num");

			JM_GEN_DocNumbering dn = new JM_GEN_DocNumbering();
			dn.setXmlData(xmlData);
			dn.execute(context);
			xmlData = dn.getXmlData();
			
			argt.deleteWhereValue("user_id", 0);
			for (int i = 0; ++i <=userDataNumcols;){
				argt.delCol(1);
			}

			tblProcessData.getTable("xml_data", 1).getTable("XmlData",1).setString("XmlData", 1, xmlData);
			
		} else if ("JM-Confirm-XML".equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_FORMS_TABLE,tblProcessData.getInt("output_form_id",1))) 
				|| "JM_Confirm_Acks-XML".equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_FORMS_TABLE,tblProcessData.getInt("output_form_id",1)))
				|| "JM_Confirm_MTrans-XML".equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_FORMS_TABLE,tblProcessData.getInt("output_form_id",1)))) {
			
			Table tblUserData = tblProcessData.getTable("user_data", 1);
			
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat(Integer.parseInt(properties.getProperty(WEIGHT_WIDTH)), Integer.parseInt(properties.getProperty(WEIGHT_PRECISION)), "olfMtlTfStratInfo_Qty", "COL_DOUBLE"));
			
			} catch (Exception e1) {
				Logging.error("Error formatting field olfDAmt, skipping field. " + e1.getMessage());
			}
			
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat(Integer.parseInt(properties.getProperty(WEIGHT_WIDTH)), Integer.parseInt(properties.getProperty(WEIGHT_PRECISION)), "olfDAmt", "COL_DOUBLE"));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfDAmt, skipping field. " + e1.getMessage());
			}
			
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat(Integer.parseInt(properties.getProperty(WEIGHT_WIDTH)), Integer.parseInt(properties.getProperty(WEIGHT_PRECISION)), "olfNotnl", "COL_DOUBLE"));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfNotnl, skipping field. " + e1.getMessage());
			}			
			
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat(Integer.parseInt(properties.getProperty(PRICE_WIDTH)), Integer.parseInt(properties.getProperty(PRICE_PRECISION)), "olfTranInfo_TradePrice", "COL_DOUBLE"));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfTranInfo_TradePrice, skipping field. " + e1.getMessage());
			}
			
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat(Integer.parseInt(properties.getProperty(PRICE_WIDTH)), Integer.parseInt(properties.getProperty(PRICE_PRECISION)), "final_price", "COL_DOUBLE"));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfPrice, skipping field. " + e1.getMessage());
			}	
			
			try {			
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat("olfSettleDate", "COL_DATE",  DATE_FORMAT.fromInt(Integer.parseInt(properties.getProperty(FORMAT_DATE)))));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfSettleDate, skipping field. " + e1.getMessage());
			}
			try {
				updateGenDataField(tblProcessData, tblUserData, 
					new GenDataFieldFormat("olfTradeDate", "COL_DATE", DATE_FORMAT.fromInt(Integer.parseInt(properties.getProperty(FORMAT_DATE)))));
			} catch (Exception e1) {
				Logging.error("Error formatting field olfTradeDate, skipping field. " + e1.getMessage());
			}
			try {			
				updateGenDataField(tblProcessData, tblUserData,
					new GenDataFieldFormat("olfStartDate", "COL_DATE", DATE_FORMAT.fromInt(Integer.parseInt(properties.getProperty(FORMAT_DATE)))));	
			} catch (Exception e1) {
				Logging.error("Error formatting field olfStartDate, skipping field. " + e1.getMessage());
			}

			try {
				updateGenDataField(tblProcessData, tblUserData,
					new GenDataFieldFormat("olfTradeDateStr", "COL_DATE", DATE_FORMAT.fromInt(Integer.parseInt(properties.getProperty(FORMAT_DATE)))));	
			} catch (Exception e1) {
				Logging.error("Error formatting field olfTradeDateStr, skipping field. " + e1.getMessage());
			}
			
			try {
				updateGenDataField(tblProcessData, tblUserData,
					new GenDataFieldFormat("olfMaturityDate", "COL_DATE", DATE_FORMAT.fromInt(Integer.parseInt(properties.getProperty(FORMAT_DATE)))));	
			} catch (Exception e1) {
				Logging.error("Error formatting field olfMaturityDateStr, skipping field. " + e1.getMessage());
			}
			
			try {
				super.execute(context);
				// Rename file to XML.
				String origFileName = argt.getString("output_filename", 1);
				String newFileName = origFileName.replace("txt", "xml");
				try {
					File sourceFile = new File(origFileName);
					if (sourceFile.exists()) {
						Files.copy(sourceFile.toPath(), new File(newFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);						
					}
				} catch (IOException e) {
					throw new OException("Error moving file. " + e.getLocalizedMessage());
				}
				resetRegenrateDocInfo(context.getArgumentsTable().getTable("process_data", 1), EnumRegenrateOutput.NO);
			} finally {
				Logging.close();
			}
			return;
		} else {
			super.execute(context);
		}
	}
	
    protected void resetRegenrateDocInfo(Table tblProcessData, EnumRegenrateOutput enumVal)throws OException {
        
        int docNum = tblProcessData.getInt("document_num", 1);
        if(enumVal == EnumRegenrateOutput.NO){
            StlDoc.saveInfoValue(docNum, "Regenerate XML", EnumRegenrateOutput.NO.name());
        }else {
            StlDoc.saveInfoValue(docNum, "Regenerate XML", EnumRegenrateOutput.YES.name());
        
        }
        Logging.info("Setting Regenerate XML on document# " + docNum + " to " + enumVal.name());
    }

	private void initLogging() throws OException{
		String abOutdir = Util.getEnv("AB_OUTDIR");
		String logLevel = properties.getProperty(LOG_LEVEL);
		String logDir = properties.getProperty(LOG_DIR);
		String logFile = properties.getProperty(LOG_FILE);
		try {
			Logging.init( this.getClass(), CONST_REPO_CONTEXT, CONST_REPO_SUBCONTEXT);
		} catch (Exception e) {
			throw new RuntimeException ("Could not initialize Logging: " + e.getMessage());
		}
	}



	private Set<Integer> getTransactionsForTranGroup(int tranGroup) throws OException {
		Set<Integer> transForGroup = new HashSet<Integer>();
		
		String sql = String.format(
				"\nSELECT ab2.tran_num "
			+   "\nFROM ab_tran ab"
			+   "\nINNER JOIN ab_tran ab2 ON ab2.tran_group = ab.tran_group AND ab2.current_flag=1"
			+   "\nWHERE ab.tran_num = %d",
				tranGroup);
		Table sqlResult = null;			
		try {
			sqlResult = Table.tableNew("Trannums for tran group");
			int ret = DBaseTable.execISql(sqlResult, sql);
			if (ret != OLF_RETURN_SUCCEED) {
				throw new OException (DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + sql));
			} 
			for (int row=sqlResult.getNumRows(); row >= 1; row--) {
				int tranNum = sqlResult.getInt("tran_num", row);
				transForGroup.add(tranNum);
			}
		} finally {
			TableUtilities.destroy(sqlResult);
		}
		return transForGroup;
	}

	/**
	 * Apply custom formatting 
	 * @param genData The Endur argument data table
	 * @param tblUserData This is a table based collection of data field
	 * @param field the data to undergo custom formatting
	 * <br>
	 * @see GenDataFieldFormat
	 */ 
	private void updateGenDataField(final Table genData, final Table tblUserData, GenDataFieldFormat field) throws OException {
		
		int genDataFieldRow = tblUserData.unsortedFindString("col_name", field.getName(), SEARCH_CASE_ENUM.CASE_INSENSITIVE);

		if (genDataFieldRow>0) {
			String xmlData = genData.getTable("xml_data", 1).getTable("XmlData",1).getString("XmlData", 1);
			
			switch (field.getType()) {
			
			case COL_DOUBLE:
				if (tblUserData.getInt("ColType", genDataFieldRow) == 2) { // if its OLF string
					xmlData = updateField(tblUserData, xmlData,  field.getName(), 
						Str.formatAsDouble(Double.parseDouble( tblUserData.getString("col_data", genDataFieldRow).replace(",", "")), 
						field.getWidth(), field.getPrecision()));
				} else if (tblUserData.getInt("ColType", genDataFieldRow) == 3) { // if its OLF table
					
					Table docTable = tblUserData.getTable("doc_table", genDataFieldRow);
					
					for(int i = 1; i <= docTable.getNumRows(); i++) {
						xmlData = updateTableField(tblUserData, xmlData, field.getName(), 
							Str.formatAsDouble(Double.parseDouble(docTable.getString(field.getName(), i).replace(",", "")),
							field.getWidth(), field.getPrecision()), i);
					}
				}
				break;
				
				case COL_DATE:
				case COL_DATE_TIME:
					if (tblUserData.getInt("IntData", genDataFieldRow)>0) { // if its OLF int 
						xmlData = updateField(tblUserData, xmlData, field.getName(), 
							OCalendar.formatDateInt(tblUserData.getInt("IntData", genDataFieldRow), field.getFormat()));
						
					} else if (tblUserData.getInt("ColType", genDataFieldRow)==2){
						xmlData = updateField(tblUserData, xmlData,  field.getName(), 
							OCalendar.formatDateInt(OCalendar.parseString(tblUserData.getString("col_data", genDataFieldRow)), field.getFormat()));
					}
					break;
					
				default:   // log unhandled columnType!!!
					Logging.warn(String.format("FIELD:%s custom processing SKIPPED",field.getName()));
			}
			
			genData.getTable("xml_data", 1).getTable("XmlData",1).setString("XmlData", 1, xmlData);
		}
	}
	
	private String updateTableField(Table argt, String xmlData, String genDataField, String targetValue, int rowToSet) throws OException
	{
		
		if (argt == null) {
			return xmlData;
		}
		
		int row = argt.unsortedFindString("col_name", genDataField, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		//update the table resulting data
		argt.getTable("doc_table", row).setString(genDataField, rowToSet, targetValue);

		StringBuilder builder = new StringBuilder(xmlData);

		//update the xml of resulting data
		String field = updateXMLNode(genDataField, targetValue, builder, rowToSet);

		return builder.toString();
	}
	
	private String updateField(Table argt, String xmlData, String genDataField, String targetValue) throws OException
	{
		if (argt == null){ 
			return xmlData;
		}

		int row = argt.unsortedFindString("col_name", genDataField, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		//update the table resulting data
		argt.setString("col_data", row, targetValue);

		StringBuilder builder = new StringBuilder(xmlData);

		//update the xml of resulting data
		String field = updateXMLNode(genDataField, targetValue, builder);

		return builder.toString();
	}


	/**
	 * Update supplied XML matching on nodeName with value  
	 */
	private String updateXMLNode(final String nodeName, final String value, StringBuilder builder) {

		int posValueStart = -1;
		int posClosingTag = -1;
		while ((posValueStart=builder.indexOf("<"+nodeName+" ", posValueStart))>=0) {
			
			int lengthBefore = builder.length();
			if (builder.indexOf("/", posValueStart)<builder.indexOf(">", posValueStart)) {
				// value is empty
				posValueStart = builder.indexOf("/", posValueStart);
				builder.replace(posValueStart, posValueStart+1, ">"+value+"</"+nodeName);
				
			} else {
				posValueStart = builder.indexOf(">", posValueStart) + 1;
				posClosingTag = builder.indexOf("<", posValueStart);
				builder.replace(posValueStart, posClosingTag, value);
			}
			posValueStart += builder.length()-lengthBefore;
		}
		
		if(posValueStart>0 && posClosingTag>0){
			return builder.substring(posValueStart, posClosingTag);
		} else {
			return "";
		}
	}
	
	private String updateXMLNode(final String nodeName, final String value, StringBuilder builder, int row) {

		int match = 1;
		int posValueStart = -1;
		int posClosingTag = -1;
		while ((posValueStart=builder.indexOf("<"+nodeName, posValueStart))>=0) {
			
			int lengthBefore = builder.length();
			if (builder.indexOf("/", posValueStart)<builder.indexOf(">", posValueStart)) {
				// value is empty
				posValueStart = builder.indexOf("/", posValueStart);
				if(match == row) {
					builder.replace(posValueStart, posValueStart+1, ">"+value.trim()+"</"+nodeName);
				}
				
			} else {
				posValueStart = builder.indexOf(">", posValueStart) + 1;
				posClosingTag = builder.indexOf("<", posValueStart);
				if(match == row) {
					builder.replace(posValueStart, posClosingTag, value.trim());
				}
			}
			//posValueStart += builder.length()-lengthBefore;
			posValueStart += nodeName.length()+1;
			match++;
		}
		
		if(posValueStart>0 && posClosingTag>0){
			return builder.substring(posValueStart, posClosingTag);
		} else{ 
			return "";
		}
}
	
	static final String CONST_REPO_CONTEXT = "JM_GENDATA";
	static final String CONST_REPO_SUBCONTEXT = "Formatting";
	static final String WEIGHT_WIDTH = "Weight Width";
	static final String WEIGHT_PRECISION = "Weight Precision";
	static final String PRICE_WIDTH = "Price Width";
	static final String PRICE_PRECISION = "Price Precision";
	static final String VALUE_WIDTH = "Value Width";
	static final String VALUE_PRECISION = "Value Precision";
	static final String FORMAT_DATE = "Date Format";
	static final String LOG_FILE = "logFile";
	static final String LOG_DIR = "logDir";
	static final String LOG_LEVEL = "logLevel";
	
	private static final Map<String, String> configuration;
	    static
	    {
	    	configuration = new HashMap<String, String>(6);
	    	configuration.put(WEIGHT_WIDTH,Integer.toString(Util.NOTNL_WIDTH));
	    	configuration.put(WEIGHT_PRECISION, Integer.toString(4));
	    	configuration.put(PRICE_WIDTH, Integer.toString(Util.NOTNL_WIDTH));
	    	configuration.put(PRICE_PRECISION, Integer.toString(3));
	    	configuration.put(VALUE_WIDTH, Integer.toString(Util.NOTNL_WIDTH));
	    	configuration.put(VALUE_PRECISION, Integer.toString(2));
	    	configuration.put(FORMAT_DATE, Integer.toString(DATE_FORMAT.DATE_FORMAT_ISO8601.toInt()));
	    	configuration.put(LOG_FILE, "JM_OUT_DocOutput.log");
	    	configuration.put(LOG_DIR, "");
	    	configuration.put(LOG_LEVEL, "info");
	    }
	
	private Properties properties;


	
	/**
	 * encapsulate the details relating to custom formatting for specified GenData
	 *<br>
	 *  <p><table border=0 style="width:15%;">
	 *  <caption style="background-color:#0070d0;color:white"><b>ConstRepository</caption>
	 *	<th><b>context</b></th>
	 *	<th><b>subcontext</b></th>
	 *	</tr><tbody>
	 *	<tr>
	 *	<td align="center">{@value #CONST_REPO_CONTEXT}</td>
	 *	<td align="center">{@value #CONST_REPO_SUBCONTEXT}</td>
	 *  </tbody></table></p>
	 *	<p>
	 *	<table border=2 bordercolor=black>
	 *	<tbody>
	 *	<tr>
	 *	<th><b>Variable</b></th>
	 *	<th><b>Default</b></th>
	 *	<th><b>Description</b></th>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #WEIGHT_WIDTH}</b></font></td>
	 *	<td>{@link Util.NOTNL_WIDTH}</td>
	 *	<td>This is the maximum expected width of the field
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #WEIGHT_PRECISION}</b></font></td>
	 *	<td>{@literal 4}</td>
	 *	<td>The default number of decimal places to report this field with
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #PRICE_WIDTH}</b></font></td>
	 *	<td>{@link  Util.NOTNL_WIDTH}</td>
	 *	<td>The expected maximum width for value fields
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #PRICE_PRECISION}</b></font></td>
	 *	<td>{@literal 3}</td>
	 *	<td>The number of decimal places to report this value field to
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #VALUE_WIDTH}</b></font></td>
	 *	<td>{@link Util.NOTNL_WIDTH}</td>
	 *	<td>The expected maximum width for value fields
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #VALUE_PRECISION}</b></font></td>
	 *	<td>{@literal 2}</td>
	 *	<td>The number of decimal places to report this value field to
	 *	</td>
	 *	</tr>
	 *	<tr>
	 *	<td><font color="blue"><b>{@value #FORMAT_DATE}</b></font></td>
	 *	<td>{@literal YYYYMMDD}</td>
	 *	<td>The output format for a date field
	 *	</td>
	 *	</tr>
	 *	</tbody>
	 *	</table> 
	 */
	class GenDataFieldFormat {
		
		private final int width;
		private final int decimalPlaces;
		private final String name;
		private final String type;
		private final DATE_FORMAT dateFormat;
		
		public GenDataFieldFormat(int width, int decimalPlaces, String name,String type, DATE_FORMAT format) {
			this.width = width;
			this.decimalPlaces = decimalPlaces;
			this.name = name;
			this.type = type;
			this.dateFormat = format;
		}
		
		public GenDataFieldFormat(String name,String type) {
			this(-1,-1, name, type, null);
		}

		public GenDataFieldFormat(String name,String type, DATE_FORMAT format) {
			this(-1,-1, name, type, format);
		}
		
		public GenDataFieldFormat(int width, int precision, String name,String type) {
			this(width,precision, name, type, null);
		}

		public int getWidth() {
			return width;
		}

		public int getPrecision() {
			return decimalPlaces;
		}

		public String getName() {
			return name;
		}

		public COL_TYPE_ENUM getType() {
			return COL_TYPE_ENUM.valueOf(type);
		}
		
		public DATE_FORMAT getFormat() {
			if (null == this.dateFormat) 
				return DATE_FORMAT.DATE_FORMAT_DEFAULT;
			else
				return this.dateFormat;
				
		}
		
	}
}


