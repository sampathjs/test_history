/**
 * 
 * 
 * 
 * 
 * 
 * 
 * Remarks:
 * - when debugging, significant Process Data is logged; see getProcessDataColumns()
 * 
 */

package com.openlink.jm.bo.docoutput;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.STLDOC_OUTPUT_TYPES_ENUM;
import com.openlink.jm.bo.docoutput.BaseClass;
import com.olf.jm.logging.Logging;

/*
 * History:
 * 2020-03-25	V1.1	YadavP03	- memory leaks, remove console print & formatting changes
 */

abstract class DocOutput_Base extends BaseClass
{
	private boolean isPreview = false, isPreviewModeDetectable = false;

	// name of argt column holding the Process Data table
	private final String PROCESS_DATA = "process_data";
	// embedded tables in Process Data table
	private final String USER_DATA   = "user_data",
						 OUTPUT_DATA = "output_data",
						 FORMAT_DATA = "format_data",
						 XML_DATA    = "xml_data";

	// for determining the current Output Type
	private final String OUTPUT_TYPE_ID = "output_type_id";
	// key/value pairs in Output Data table
	private final String OUTPUT_PARAM_NAME  = "outparam_name",
						 OUTPUT_PARAM_VALUE = "outparam_value";

	private int _waitForFileMaxSeconds = 20;

	protected boolean
	isDocumentPreviewed = false,
	isDocumentExported = false,
	isDocumentForMail = true;
	protected String
	documentExportPath = null;

	@Override
	protected void process(IContainerContext context) throws OException
	{
		// TODO Auto-generated method stub
//		super.process(context);

		ProcessData processData = new ProcessData(context);
		Logging.debug(String.format("Output Script '%s' invoked for Output Type '%s'", getClass().getSimpleName(), processData.getOutputTypeName()));

		isPreviewModeDetectable = isPreviewModeDetectable(context);
		isPreview = isPreview(context);
		Logging.info("Output is previewed? " + (isPreviewModeDetectable?isPreview?"yes":"no":"n/a"));

		// if (debug) OConsole.oprint(processData.toString() + "\n");
		Logging.debug("Process Data - " + processData.toString());

		//if (debug) OConsole.oprint(processData.OutputData.toString() + "\n");
		Logging.debug("Output Params - " + processData.OutputData.toString());
	}

	boolean isCancellationDoc = false;
	String cancellationDocSuffix = null;
	String docStatusCancelled = null;
	// this method is called from outside for handing over parameters
	// it's required to provide a sufficient suffix string (neither null nor empty) in case of 'true'
	void setCancellationHandlingParams(boolean isCancellationDoc, String cancellationDocSuffix, String docStatusCancelled, 
			boolean isInvoice)
	{
		this.isCancellationDoc = isCancellationDoc;
		this.cancellationDocSuffix = (isInvoice)?("C_%olfStlDocInfo_CancelDocNum%"):"C";//cancellationDocSuffix;
		this.docStatusCancelled = docStatusCancelled;
	}

	private boolean isPreviewModeDetectable(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		return (argt.getColNum("preview_flag") > 0 && argt.getNumRows() > 0);
	}

	private boolean isPreview(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
		return (isPreviewModeDetectable && argt.getInt("preview_flag", 1) != 0);
	}

	// interesting columns of ProcessData
	private List<String> getProcessDataColumns()
	{
		String list = ""
					+ "document_num,"
					+ "doc_version,"
					+ "doc_output_seqnum,"
					+ "doc_type,"
					+ "last_doc_status,"
					+ "next_doc_status,"
					+ "next_doc_status_orig,"
					+ "stldoc_def_id,"
					+ "stldoc_template_id,"
					+ "output_form_id,"
					+ "output_type_id,"
					;
		return Arrays.asList(list.trim().replaceAll("\\s*,\\s*", ",").split(","));
	}

	class ProcessData
	{
		// non-freeable tables as either embedded in argt or null
		private Table argt, tblProcessData = null, tblUserData = null, tblOutputData = null, tblFormatData = null, tblXmlData = null;

		public  UserData   UserData   = null;
		public  OutputData OutputData = null;
		public  FormatData FormatData = null;
		public  XmlData    XmlData    = null;

		public  int DocumentNum     = 0;
		public  int DocOutputSeqnum = 0;

		private HashMap<String, String> processDataMap = null;

		public ProcessData(IContainerContext context) throws OException
		{
			argt = context.getArgumentsTable();
			if (argt.getNumRows() <= 0 || argt.getColNum(PROCESS_DATA) <= 0)
				throw new OException("No process data found!");

			// argt will have 1 row only - also in case of multiple documents to be put out
			tblProcessData = argt.getTable(PROCESS_DATA, 1);

			// Process Data will have 1 row only - also in case of multiple documents to be put out

			// set DocumentNum, DocOutputSeqnum
			DocumentNum     = tblProcessData.getInt("document_num", 1);
			DocOutputSeqnum = tblProcessData.getInt("doc_output_seqnum", 1);

			// set UserData, OutputData, FormatData, XmlData
			try { UserData   =   new UserData(tblProcessData); } catch (Throwable t) { UserData   = null; }
			try { OutputData = new OutputData(tblProcessData); } catch (Throwable t) { OutputData = null; }
			try { FormatData = new FormatData(tblProcessData); } catch (Throwable t) { FormatData = null; }
			try { XmlData    =    new XmlData(tblProcessData); } catch (Throwable t) { XmlData    = null; }
		}

		public STLDOC_OUTPUT_TYPES_ENUM getOutputType() throws OException
		{
			return STLDOC_OUTPUT_TYPES_ENUM.fromInt(tblProcessData.getInt(OUTPUT_TYPE_ID, 1));
		}

		public String getOutputTypeName() throws OException
		{
			return Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_OUTPUT_TYPES_TABLE, tblProcessData.getInt(OUTPUT_TYPE_ID, 1));
		}

		public String toString()
		{
			List<String> columns = null;
			String str = "[n/a]";

			try
			{
				str = getTableName(tblProcessData, argt, PROCESS_DATA);
				columns = getProcessDataColumns();

				// return if nothing's to do
				if (columns == null || columns.isEmpty())
					return str;

				// create copy for to string converted columns
				Table tblProcessDataReduced = tblProcessData.copyTable();
				try
				{
					// remove uninteresting columns, but keep title and format if interesting
					for (int col = tblProcessData.getNumCols(); col > 0; --col)
						if (!columns.contains(tblProcessData.getColName(col)))
							tblProcessDataReduced.delCol(col);
						else
						{
							tblProcessDataReduced.colShow(col);
							tblProcessDataReduced.convertColToString(col);
							tblProcessDataReduced.setColTitle(col, tblProcessData.getColTitle(col));
						}

					HashMap<String, String> map = getColumnData(tblProcessDataReduced);
					str += getMapAsString(map, true); // collect column data (<Name>:<Value>), sorted
					map.clear(); map = null;
				}
				finally { tblProcessDataReduced.destroy(); }
			}
			catch (OException e)
			{
				// TODO Auto-generated catch block
			}

			return str;
		}

		protected final String getStringValue(String key)
		{
			//TODO
			return "[not implemented]";
		}

		private HashMap<String, String> getColumnData(Table tbl) throws OException
		{
			int numData = tbl.getNumCols();
			HashMap<String, String> map = new HashMap<String, String>(numData);

			String name;
			for (int col = 0; ++col <= numData; )
			{
				name = tbl.getColTitle(col);
				name = name == null || name.trim().length() == 0 ? tbl.getColName(col) 
						: name.trim().replaceAll("\\n", " ").replaceAll(" \\s*", " ");
				map.put(name, tbl.getString(col, 1));
			}

			return map;
		}

		private String getMapAsString(HashMap<String, String> map, boolean sort)
		{
			String str = "";

			// return if nothing's to do
			if (map == null)
				return str;
			if (map.isEmpty())
			{	map = null; return str; }

			// for friendlier output
			int maxCharCount = 0;
			for (String key:map.keySet())
				if (maxCharCount < key.length())
					maxCharCount = key.length();

			// output format: NewLine+TabStop+<key>PadRight + <value>
			String fmt = "\n\t%-"+maxCharCount+"s - %s";

			if (sort)
			{
				// output sorted by keys
				List<String> sortedList = new ArrayList<String>(map.keySet());
				Collections.sort(sortedList);

				for (String key: sortedList)
					str += String.format(fmt, key, map.get(key));

				sortedList.clear(); sortedList = null;
			}
			else // unsorted output
				for (String key:map.keySet())
					str += String.format(fmt, key, map.get(key));

			return str;
		}

		// used for initializing enclosed classes
		private Table initializeData(Table tblProcessData, String colName, String errMsg) throws OException
		{
			if (tblProcessData.getNumRows() <= 0 || tblProcessData.getColNum(colName) <= 0)
				throw new OException(errMsg);
			Table tbl = tblProcessData.getTable(colName, 1);
			return tbl == null || Table.isTableValid(tbl) != 1 ? null : tbl;
		}

		// retrieve (friendly) table name
		private String getTableName(Table table, Table parent, String colName) throws OException
		{
			String str = table.getTableTitle();
			if (str == null || str.trim().length() == 0)
				str = table.getTableName();
			if (str == null || str.trim().length() == 0)
				str = parent.getColTitle(colName);
			if (str == null || str.trim().length() == 0)
				str = colName;
			return str;
		}

		class UserData
		{
			public UserData(Table tblProcessData) throws OException
			{
				tblUserData = initializeData(tblProcessData, USER_DATA, "No user data found!");
			}

			public String getStringValue(String key) throws OException
			{
				try
				{
					tblUserData.group("col_name, col_type");
					int row = tblUserData.findString("col_name", key, SEARCH_ENUM.FIRST_IN_GROUP);
					if (row < 1)
						throw new OException("parameter not found");
					String value;
					if (tblUserData.getInt("col_type", row) != 2)
						value = tblUserData.getString("col_data", row);
					else
						value = tblUserData.getTable("doc_table", row).getString(1, 1);
					return value;
				}
				catch (OException e)
				{
					throw new OException("Failed to get value for key '" + key + "' from user data table: " + e.getMessage());
				}
			}

			Table getTable()
			{
				return tblUserData;
			}
		}

		class OutputData
		{
			private HashMap<String, String> outputParams = null;

			public OutputData(Table tblProcessData) throws OException
			{
				tblOutputData = initializeData(tblProcessData, OUTPUT_DATA, "No output data found!");
			}

			private HashMap<String, String> getOutputParams() throws OException
			{
				int nameCol   = tblOutputData.getColNum(OUTPUT_PARAM_NAME),
					valueCol  = tblOutputData.getColNum(OUTPUT_PARAM_VALUE),
					numParams = tblOutputData.getNumRows();
				HashMap<String, String> map = new HashMap<String, String>(numParams);

				for (int row = 0; ++row <= numParams; )
					map.put(tblOutputData.getString(nameCol, row), tblOutputData.getString(valueCol, row));

				return map;
			}

			public boolean containsKey(String name) throws OException
			{
				if (outputParams == null)
					outputParams = getOutputParams();
				return outputParams.containsKey(name);
			}
			public String getValue(String name) throws OException
			{
				if (outputParams == null)
					outputParams = getOutputParams();
				try
				{
					return outputParams.get(name);
				}
				catch (Throwable t)
				{
					Logging.warn("Key not found in Output Params: "+name);
				}
				return "";
			}

			public String toString()
			{
				String str = "[n/a]";

				try
				{
					str = getTableName(tblOutputData, tblProcessData, OUTPUT_DATA);

					HashMap<String, String> map = getOutputParams();
					str += getMapAsString(map, true); // collect param data (<Name>-<Value>), sorted
					map.clear(); map = null;
				}
				catch (OException e)
				{
					// TODO Auto-generated catch block
				}

				return str;
			}
		}

		class FormatData
		{
			public FormatData(Table tblProcessData) throws OException
			{
				tblFormatData = initializeData(tblProcessData, FORMAT_DATA, "No format data found!");
			}

			protected Table getTable()
			{
				return tblFormatData;
			}
		}

		class XmlData
		{
			public XmlData(Table tblProcessData) throws OException
			{
				tblXmlData = initializeData(tblProcessData, XML_DATA, "No xml data found!");
			}

		/*	protected Table getTable()
			{
				return tblXmlData;
			}*/

			protected String getXmlDataString() throws OException
			{
				return tblXmlData.getTable(1,1).getString(1,1);
			}

			protected String getXmlDataMapString() throws OException
			{
				return tblXmlData.getTable(2,1).getString(1,1);
			}
		}
	}

	protected boolean existsFile(String fileName) throws OException
	{
		File file = new File(fileName);
		return file.exists() && file.canWrite() && file.canRead();
	}
	protected void waitForFile(String fileName) throws OException
	{
		int loopCount = 0, loopMax = 60; long sleepSeconds = 1;
	/*	try
		{
			loopMax = _constRepo.getIntValue(WaitForFile_MaxSeconds, intWaitForFileMaxSeconds);
		}
		catch (Throwable t) {}
		finally
		{
			if (loopMax <= 0)
			{
				Logging.warn("Invalid setting for '"+WaitForFile_MaxSeconds+"':"+loopMax);
				loopMax = intWaitForFileMaxSeconds;
			}
		}*/

		Logging.info("Waiting maximum "+loopMax+" seconds for file creation");
		while (!existsFile(fileName) && ++loopCount <= loopMax)
			try
			{
				PluginLog.info("DocsOutput - Waiting for file creation ("+loopCount+"/"+loopMax+")");
				Thread.sleep(sleepSeconds*1000);
			}
			catch (Throwable t) {}
		Logging.info("Waited "+loopCount+" seconds for file creation");

		if (loopCount>=loopMax)
			throw new OException("Aborted - file not created in time - "+fileName);
	}

	private final static char[] idchars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	protected final static String createId(int len)
	{
		char[] id = new char[len];
		Random r = new Random(System.currentTimeMillis());
		for (int i = 0; i < len; i++)
			id[i] = idchars[r.nextInt(idchars.length)];
		return new String(id);
	}

}