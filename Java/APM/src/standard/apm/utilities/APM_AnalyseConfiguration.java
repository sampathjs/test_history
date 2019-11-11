/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm.utilities;

// ****************************************************************************
// *                                                                          *
// *              Copyright 2013 OpenLink Financial, Inc.                     *
// *                                                                          *
// *                        ALL RIGHTS RESERVED                               *
// *                                                                          *
// ****************************************************************************

// 20May2013 - Fix for cartesian join on service dataset keys
// 20May2013 - Fix for no check made if key not in pages
// 20May2013 - Enhancement to allow output to Excel
// 20May2013 - Fix for username crash


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.olf.openjvs.DBase;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Services;
import com.olf.openjvs.Sim;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;
import com.olf.openjvs.enums.MATCH_CMP_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SEARCH_ENUM;

public class APM_AnalyseConfiguration implements IScript
{
	static int sheetcounter = 0;

	public void execute(IContainerContext context) 
	{
		final int DEFAULT_DAYS_USERS_ACTIVE_FOR = 30;
		
		try
		{
			APM_Print("Running APM_AnalyseConfiguration");
			
			PageParser pages = new PageParser();
			
			//*******************************************************************
			//Add this line for each folder you wish to parse for pages.
			pages.addFolder("C:\\folders\\path\\here\\");
			//Set this to "false" to not generate the excel sheet.
			boolean saveOutputToExcel = true;
			
			// filename to save to
			String excelFileName = "c:\\APM_AnalyseAPMConfig.xls";
			
			//Set this to "false" to not generate Filter Pivot Column analysis.
			boolean generateFilterPivotColumnAnalysis = true;

			//Set this to "false" to not generate DataSetKey analysis.
			boolean generateDataSetKeyAnalysis = true;

			//Set this to "false" to not generate Page Statistics.
			boolean generatePageStatistics = true;

			//Set this to "false" to not generate User Statistics.
			boolean generateUserStatistics = true;
			//*******************************************************************
			
			pages.Parse();
			
			Table pivotFilterReports = Util.NULL_TABLE;
			if (generateFilterPivotColumnAnalysis)
			{
				Table listOfAllUsedPivotsFiltersColumns = pages.generateFiltersPivotsColumnsAnalysis();
				pivotFilterReports = CompareOutputWithDb(listOfAllUsedPivotsFiltersColumns);				
			}

			Table dataSetKeyReports = Util.NULL_TABLE; 
			if (generateDataSetKeyAnalysis)
			{
				DbDatasetKeysByService dbDataSetKeys = new DbDatasetKeysByService();
				dataSetKeyReports = dbDataSetKeys.compareDbDatasetKeysVersusPageDatasetKeys(pages.rawDataOutput());
			}
			
			Table pageStatistics = Util.NULL_TABLE; 
			if (generatePageStatistics)
			{
				pageStatistics = pages.generatePageStatistics();
			}

			Table userReports = Util.NULL_TABLE;
			if (generateUserStatistics)
			{
				UserStatistics userStatistics = new UserStatistics();
				userReports = userStatistics.generate(DEFAULT_DAYS_USERS_ACTIVE_FOR);
			}
			
			DisplayStatistics(pageStatistics, pivotFilterReports, dataSetKeyReports, userReports, saveOutputToExcel, excelFileName);
			
			APM_Print("FINISHED Running APM_AnalyseConfiguration");

		}
		catch (Exception ex)
		{
			try
			{
				String message = "Script failed with the following error(s): " + ex.getMessage();
				APM_Print(message);
				
				Table errors = errorTable();
				errors.setString(1, 1, message);
				errors.viewTable();
			}
			catch (OException oEx)
			{
				//What do we want to do here?
			}
		}

		Util.exitSucceed();
	}

	private void DisplayStatistics(Table pageStatistics, Table pivotFilterReports, Table dataSetKeyReports, Table userReports, boolean saveOutputToExcel, String excelFileName) throws OException
	{
		Table allReports = Table.tableNew("All Reports");
		allReports.setTableTitle("All Reports");
		allReports.addCol("Page_reports", COL_TYPE_ENUM.COL_TABLE);
		allReports.addCol("Pivot_Filter_reports", COL_TYPE_ENUM.COL_TABLE);
		allReports.addCol("DataSetKey_reports", COL_TYPE_ENUM.COL_TABLE);
		allReports.addCol("User_reports", COL_TYPE_ENUM.COL_TABLE);
		allReports.addRow();
		allReports.setTable("Page_reports", 1, pageStatistics);
		allReports.setTable("Pivot_Filter_reports", 1, pivotFilterReports);
		allReports.setTable("DataSetKey_reports", 1, dataSetKeyReports);
		allReports.setTable("User_reports", 1, userReports);
		allReports.viewTable();
		
		if ( saveOutputToExcel )
		{
			APM_Print("Saving output to Excel.  Filename = " + excelFileName);
			PrintToExcel(allReports, excelFileName);
		}
	}
	
	private Table CompareOutputWithDb(Table pageSettings) throws OException
	{
		DbComparer comparer = new DbComparer();
		comparer.CompareAllPageComponentsToDb(pageSettings);
		
		Table missingInDb = comparer.outputMissingFromDB();
		Table missingOnPages = comparer.outputMissingOnPages();
		Table inUse = comparer.outputInUse();
		
		Table output = Table.tableNew();
		output.setTableTitle("Page Components");
		output.addCol("Used in both", COL_TYPE_ENUM.COL_TABLE);
		output.addCol("Turned on in DB only", COL_TYPE_ENUM.COL_TABLE);
		output.addCol("Used in Pages only", COL_TYPE_ENUM.COL_TABLE);
		output.addRow();
		output.setTable("Used in both", 1, inUse);
		output.setTable("Turned on in DB only", 1, missingOnPages);
		output.setTable("Used in Pages only", 1, missingInDb);
		
		return output;
}
	
	private Table errorTable() throws OException
	{
		Table errorTable = Table.tableNew();
		errorTable.setTableTitle("Error");
		
		errorTable.addCol("Description", COL_TYPE_ENUM.COL_STRING);
		errorTable.addRow();
		
		return errorTable;
	}

	class PageParser
	{
		public final static String m_PACKAGE_NAME = "PackageName"; 
		public final static String m_COLUMN_NAME = "Column"; 
		public final static String m_COL_NUM = "ColumnNumber"; 
		public final static String m_ROW_NUM = "RowNumber"; 
		public final static String m_SECTION = "Section"; 
		public final static String m_TYPE = "Type"; 
		public final static String m_NAME = "Name"; 
		public final static String m_VALUE = "Value"; 
		public final static String m_ON_OFF = "OnOff"; 
		public final static String m_PAGE_NAME = "PageName"; 

		private final String m_COLUMN_LABEL = "Label";
		private final String m_COLUMN_LIST_SECTION = "ColumnList";


		private Table m_pageFilters;
		private Table m_columnSplittersFilters;
		private Table m_rowSplitters;
		
		private Table m_rawData;
		private Table m_listOfAllUsedPivotsFiltersColumns;
		private Table m_outputPageStatistics;
		
		private ArrayList<String> _folders = new ArrayList<String> ();
		private String m_pagePath = "";
		private boolean m_parseComplete = false;
		
		public PageParser()
		{
		}
		
		public void addFolder(String folderName)
		{
			APM_Print("Adding folder:" + folderName);			
			_folders.add(folderName);
		}
			
		public Table rawDataOutput()
		{
			return m_rawData;
		}

		public void Parse() throws OException, ParserConfigurationException, SAXException, IOException
		{
			APM_Print("Starting Parse");
			m_rawData = setupRawDataTable("Raw data");			

			parseFolders();
			
			m_parseComplete = true;
		}
		
		public Table generateFiltersPivotsColumnsAnalysis() throws OException
		{
			if (!m_parseComplete)
			{
				throw new OException("Folders must be parsed before FiltersPivotsColumns Analysis be be generated");
			}
			
			getListOfAllAvailableFiltersPivotsColumns();
			return m_listOfAllUsedPivotsFiltersColumns;
		}

		public Table generatePageStatistics() throws OException
		{
			if (!m_parseComplete)
			{
				throw new OException("Folders must be parsed before Page Statistics be be generated");
			}
 
			produceOutputPageStatistics();
			return m_outputPageStatistics;
		}

		private Table setupCellSettingsTable(String tableName) throws OException
		{
			Table settingsTable = Table.tableNew();
			settingsTable.setTableTitle(tableName);
			
			settingsTable.addCol(m_SECTION, COL_TYPE_ENUM.COL_STRING);
			settingsTable.addCol(m_TYPE, COL_TYPE_ENUM.COL_STRING);
			settingsTable.addCol(m_NAME, COL_TYPE_ENUM.COL_STRING);
			settingsTable.addCol(m_VALUE, COL_TYPE_ENUM.COL_STRING);
			settingsTable.addCol(m_ON_OFF, COL_TYPE_ENUM.COL_STRING);
			settingsTable.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			
			return settingsTable;
		}

		private Table setupRawDataTable(String tableName) throws OException
		{
			Table rawDataTable = Table.tableNew();
			rawDataTable.setTableTitle(tableName);
			
			rawDataTable.addCol(m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_SECTION, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_TYPE, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_COLUMN_NAME, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_NAME, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_VALUE, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_COL_NUM, COL_TYPE_ENUM.COL_INT);
			rawDataTable.addCol(m_ROW_NUM, COL_TYPE_ENUM.COL_INT);
			rawDataTable.addCol(m_ON_OFF, COL_TYPE_ENUM.COL_STRING);
			rawDataTable.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			
			return rawDataTable;
		}

		private void parseFolders() throws ParserConfigurationException, SAXException, IOException, OException
		{
			for (Iterator<String> folder = _folders.iterator(); folder.hasNext();) 
			{
				String folderName = folder.next();
				parseFiles(folderName);
			}
		}

        private void parseFiles(String folderName) throws ParserConfigurationException, SAXException, IOException, OException
        {
              File folder = new File(folderName);
              
              if (folder.exists())
              {
                    File[] listOfFiles = folder.listFiles(); 
         
                    for (int counter = 0; counter < listOfFiles.length; counter++) 
                    {
                          processXmlFile(folderName, listOfFiles[counter]);
                    }
              }
              else
              {
                    throw new IOException("Specified folder " + folderName + " does not exist");
              }
        }
		
		private void processXmlFile(String folderName, File file) throws ParserConfigurationException, SAXException, IOException, OException
		{
			final String APM_PAGE_EXTENSION = ".ppg";
			
			if (file.isFile()) 
			{
				String fileName = file.getName();
				
				if (fileName.endsWith(APM_PAGE_EXTENSION))
				{
					m_pagePath = folderName + fileName;
					APM_Print("Parsing file:" + m_pagePath);
					parseApmPage();
				}
			}
		}
		
		private void parseApmPage() throws ParserConfigurationException, SAXException, IOException, OException 
		{
			final String COLUMN_SPLITTER_FILTERS = "SplitterFilterList";
			final String ROW_SPLITTERS = "TreeviewFilterList";

			Document xmlDoc = openXmlFile(m_pagePath);

			m_pageFilters = setupCellSettingsTable("Page Filters");
			m_columnSplittersFilters = setupCellSettingsTable("Column Splitters Filters");
			m_rowSplitters = setupCellSettingsTable("Row Splitters");

			extractPageFilterDetails(xmlDoc);
			extractSplitterFilterDetails(xmlDoc, COLUMN_SPLITTER_FILTERS, m_columnSplittersFilters);
			extractSplitterFilterDetails(xmlDoc, ROW_SPLITTERS, m_rowSplitters);
			extractColumnDetails(xmlDoc);
			
			m_pageFilters.destroy();
			m_columnSplittersFilters.destroy();
			m_rowSplitters.destroy();
		}

		private Document openXmlFile(String fileName) throws ParserConfigurationException, SAXException, IOException
		{
			File xmlFile = new File(fileName);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);

			//optional, but recommended
			//read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			return doc;
		}

		private void extractPageFilterDetails(Document xmlDoc) throws OException
		{
			final String PAGE_FILTER_DEFS = "PageFilterDefs";
			final String PAGE_FILTER_VALUE_DEFS = "PageFilterValueList";
		
			NodeList allPageFilterDefs = xmlDoc.getElementsByTagName(PAGE_FILTER_DEFS);
			Node parentPageFilterDef = allPageFilterDefs.item(0);
			
			if (parentPageFilterDef != null)
			{
				NodeList pageFilterDefNodes = parentPageFilterDef.getChildNodes();
	
				NodeList allPageFilterValues = xmlDoc.getElementsByTagName(PAGE_FILTER_VALUE_DEFS);
				Node parentPageFilterValue = allPageFilterValues.item(0); 
				NodeList pageFilterValueNodes = parentPageFilterValue.getChildNodes();
				
				int numberOfPageFilters = pageFilterDefNodes.getLength();
				for (int count = 0; count < numberOfPageFilters; count++)
				{
					Node pageFilterNode = pageFilterDefNodes.item(count);
					Element pageFilter = (Element) pageFilterNode;
					String type = pageFilter.getNodeName();
					String name = pageFilter.getElementsByTagName(m_NAME).item(0).getTextContent();
					
					Node pageFilterValueNode = pageFilterValueNodes.item(count);
					Element pageFilterValues = (Element) pageFilterValueNode;
					String filterValues = getFilterValues(pageFilterValues);
	
					int row = m_pageFilters.addRow();
					m_pageFilters.setString(m_SECTION, row, PAGE_FILTER_DEFS);
					m_pageFilters.setString(m_TYPE, row, type);
					m_pageFilters.setString(m_NAME, row, name);
					m_pageFilters.setString(m_VALUE, row, filterValues);
					m_pageFilters.setString(m_PAGE_NAME, row, m_pagePath);
				}
			}
		}

		private String getFilterValues(Element filterValue)
		{
			String values = "";
			
			int numberOfValues = filterValue.getElementsByTagName("Id").getLength();
			for (int valuesCount = 0; valuesCount < numberOfValues; valuesCount++)
			{
				values += filterValue.getElementsByTagName("Id").item(valuesCount).getTextContent() + "|";
				values += filterValue.getElementsByTagName("Value").item(valuesCount).getTextContent() + "|";
			}

			return values;
		}
		
		private void extractSplitterFilterDetails(Document xmlDoc, String parentName, Table outputTable) throws OException
		{
			NodeList allColumnSplittersFilters = xmlDoc.getElementsByTagName(parentName);
			Node parentSplittersFilters = allColumnSplittersFilters.item(0);
			NodeList splittersFiltersNodes = parentSplittersFilters.getChildNodes();

			int numberOfSplittersFilters = splittersFiltersNodes.getLength();
			for (int count = 0; count < numberOfSplittersFilters; count++)
			{
				Node splitterFilterNode = splittersFiltersNodes.item(count);
				Element splitterFilter = (Element) splitterFilterNode;
				String type = splitterFilter.getNodeName();
				String name = splitterFilter.getElementsByTagName(m_NAME).item(0).getTextContent();
				
				int row = outputTable.addRow();
				outputTable.setString(m_SECTION, row, parentName);
				outputTable.setString(m_TYPE, row, type);
				outputTable.setString(m_NAME, row, name);
				outputTable.setString(m_PAGE_NAME, row, m_pagePath);
			}
		}

		private void extractColumnDetails(Document xmlDoc) throws OException
		{
			final String DATA_COLUMN = "DataColumn";

			NodeList allColumnsList = xmlDoc.getElementsByTagName(m_COLUMN_LIST_SECTION);
			Node parentColumnList = allColumnsList.item(0);
			NodeList columnNodes = parentColumnList.getChildNodes();

			int numberOfColumns = columnNodes.getLength();
			for (int columnCount = 0; columnCount < numberOfColumns; columnCount++)
			{
				Node columnNode = columnNodes.item(columnCount);
				Element column = (Element) columnNode;
				String type = column.getNodeName();
				
				if (type.equals(DATA_COLUMN))
				{
					int columnNumber = columnCount + 1;
					extractColumnRowDetails(xmlDoc, column, columnNumber, m_COLUMN_LIST_SECTION, DATA_COLUMN);
				}
			}
		}

		private void extractColumnRowDetails(Document xmlDoc, Element column, int columnNumber, String section, String type) throws OException
		{
			final String ROW_LIST = "RowList";
			final String ROW = "Row";

			NodeList allRowsList = xmlDoc.getElementsByTagName(ROW_LIST);
			Node parentRowList = allRowsList.item(0);
			Element rowList = (Element) parentRowList;
			NodeList rowNodes = rowList.getElementsByTagName(ROW);

			int numberOfRows = rowNodes.getLength();
			for (int rowCount = 0; rowCount < numberOfRows; rowCount++)
			{
				Node rowNode = rowNodes.item(rowCount);
				Element row = (Element) rowNode;
				String rowType = row.getNodeName();
				
				if (rowType.equals(ROW))
				{
					int rowNumber = rowCount + 1;

					int rawDataRow = m_rawData.addRow();
					m_rawData.setString(m_PACKAGE_NAME, rawDataRow, column.getElementsByTagName(m_PACKAGE_NAME).item(0).getTextContent());
					m_rawData.setString(m_COLUMN_NAME, rawDataRow, column.getElementsByTagName(m_NAME).item(0).getTextContent());
					m_rawData.setInt(m_COL_NUM, rawDataRow, columnNumber);
					m_rawData.setInt(m_ROW_NUM, rawDataRow, rowNumber);
					m_rawData.setString(m_SECTION, rawDataRow, section);
					m_rawData.setString(m_TYPE, rawDataRow, type);
					m_rawData.setString(m_NAME, rawDataRow, column.getElementsByTagName(m_COLUMN_LABEL).item(0).getTextContent());
					m_rawData.setString(m_PAGE_NAME, rawDataRow, m_pagePath);
		
					setRawDataPageFilters(column, columnNumber, rowNumber, section, type);
					setRawDataSplittersFilters(column, row, columnNumber, rowNumber, m_columnSplittersFilters);
					setRawDataSplittersFilters(column, row, columnNumber, rowNumber, m_rowSplitters);
				}
			}
		}
		
		private void setRawDataPageFilters(Element column, int columnNumber, int rowNumber, String section, String type) throws OException
		{
			String packageName = column.getElementsByTagName(m_PACKAGE_NAME).item(0).getTextContent();
			String columnName = column.getElementsByTagName(m_NAME).item(0).getTextContent();

			int numSettings = m_pageFilters.getNumRows();
			for (int pageFilterRow = 1; pageFilterRow <= numSettings; pageFilterRow++)
			{
				int rawDataRow = m_rawData.addRow();
				m_rawData.setString(m_PACKAGE_NAME, rawDataRow, packageName);
				m_rawData.setString(m_COLUMN_NAME, rawDataRow, columnName);
				m_rawData.setInt(m_COL_NUM, rawDataRow, columnNumber);
				m_rawData.setInt(m_ROW_NUM, rawDataRow, rowNumber);
				m_rawData.setString(m_SECTION, rawDataRow, getStringSafe(m_pageFilters, m_SECTION, pageFilterRow));
				m_rawData.setString(m_TYPE, rawDataRow, getStringSafe(m_pageFilters, m_TYPE, pageFilterRow));
				m_rawData.setString(m_NAME, rawDataRow, getStringSafe(m_pageFilters, m_NAME, pageFilterRow));
				m_rawData.setString(m_VALUE, rawDataRow, getStringSafe(m_pageFilters, m_VALUE, pageFilterRow));
				m_rawData.setString(m_ON_OFF, rawDataRow, getStringSafe(m_pageFilters, m_ON_OFF, pageFilterRow));
				m_rawData.setString(m_PAGE_NAME, rawDataRow, m_pagePath);
			}
		}

		private void setRawDataSplittersFilters(Element column, Element row, int columnNumber, int rowNumber, Table sourceTable) throws OException
		{
			String packageName = column.getElementsByTagName(m_PACKAGE_NAME).item(0).getTextContent();
			String columnName = column.getElementsByTagName(m_NAME).item(0).getTextContent();

			int numSettings = sourceTable.getNumRows();
			for (int settingRow = 1; settingRow <= numSettings; settingRow++)
			{
				int rawDataRow = m_rawData.addRow();
				m_rawData.setString(m_PACKAGE_NAME, rawDataRow, packageName);
				m_rawData.setString(m_COLUMN_NAME, rawDataRow, columnName);
				m_rawData.setInt(m_COL_NUM, rawDataRow, columnNumber);
				m_rawData.setInt(m_ROW_NUM, rawDataRow, rowNumber);
				String section = getStringSafe(sourceTable, m_SECTION, settingRow);
				m_rawData.setString(m_SECTION, rawDataRow, section);
				m_rawData.setString(m_TYPE, rawDataRow, getStringSafe(sourceTable, m_TYPE, settingRow));
				m_rawData.setString(m_NAME, rawDataRow, getStringSafe(sourceTable, m_NAME, settingRow));
				m_rawData.setString(m_PAGE_NAME, rawDataRow, m_pagePath);
				
				if (section.equals("SplitterFilterList"))
				{
					setRawDataColumnSplitterFilterSettings(column, rawDataRow, settingRow);
				}
				else if (section.equals("TreeviewFilterList"))
				{
					setRawDataRowFilterSettings(row, rawDataRow, settingRow);
				}
			}
		}

		private void setRawDataColumnSplitterFilterSettings(Element column, int rawDataRow, int settingRow) throws OException
		{
			Node allSplitterFilterValuesNodes = column.getElementsByTagName("SplitterFilterValueList").item(0);
			NodeList splitterFilterValuesNodes = allSplitterFilterValuesNodes.getChildNodes();
			
			Node settingNode = splitterFilterValuesNodes.item(settingRow - 1);
			Element setting = (Element) settingNode;
			String type = setting.getNodeName();
			
			if (type.equals("SplitterState"))
			{
				String used = safeGetElementByName(setting, "SplitterEnabled");
				m_rawData.setString(m_ON_OFF, rawDataRow, used);
			}
			else if (type.equals("FilterValue"))
			{
				String filterValues = getFilterValues(setting);
				m_rawData.setString(m_VALUE, rawDataRow, filterValues);
			}
		}

		private String safeGetElementByName(Element element, String tagName)
		{
			String value = "";
			
			Node node = element.getElementsByTagName("SplitterEnabled").item(0);
			if (node != null)
			{
				value = node.getTextContent();
			}
			
			return value;
		}
		
		private void setRawDataRowFilterSettings(Element row, int rawDataRow, int rowNumber) throws OException
		{
			Node allTreeviewFilterValuesNodes = row.getElementsByTagName("TreeviewFilterValueList").item(0);
			NodeList treeviewFilterValuesNodes = allTreeviewFilterValuesNodes.getChildNodes();
			
			Node settingNode = treeviewFilterValuesNodes.item(rowNumber - 1);
			Element setting = (Element) settingNode;

			String used = safeGetElementByName(setting, "TreeviewEnabled");
			m_rawData.setString(m_ON_OFF, rawDataRow, used);

			String value = safeGetElementByName(setting, "ParamValue");
			m_rawData.setString(m_VALUE, rawDataRow, value);			
		}
		
		private void produceOutputPageStatistics() throws OException
		{
			final String COLUMNS_BY_PAGE_COUNT = "ColumnsByPageCount";
			final String COLUMNS_BY_PAGE_USE_COUNT = "ColumnsByPageUseCount";
			final String FP_BY_PAGE_COUNT = "FiltersPivotsByPageCount";
			final String FP_BY_PAGE_USE_COUNT = "FiltersPivotsByPageUseCount";
			
			Table columnPageCount = produceOutputByPageCountForColumns();
			Table columnPageUseCount = produceOutputByPageUsageForColumns();

			Table fpPageCount = produceOutputByPageCountForFiltersPivots();
			Table fpPageUseCount = produceOutputByPageUsageForFiltersPivots();

			m_outputPageStatistics = Table.tableNew();
			m_outputPageStatistics.setTableTitle("Page statistics");
			m_outputPageStatistics.addCol(COLUMNS_BY_PAGE_COUNT, COL_TYPE_ENUM.COL_TABLE);
			m_outputPageStatistics.addCol(COLUMNS_BY_PAGE_USE_COUNT, COL_TYPE_ENUM.COL_TABLE);
			m_outputPageStatistics.addCol(FP_BY_PAGE_COUNT, COL_TYPE_ENUM.COL_TABLE);
			m_outputPageStatistics.addCol(FP_BY_PAGE_USE_COUNT, COL_TYPE_ENUM.COL_TABLE);
			
			m_outputPageStatistics.addRow();
			m_outputPageStatistics.setTable(COLUMNS_BY_PAGE_COUNT, 1, columnPageCount);
			m_outputPageStatistics.setTable(COLUMNS_BY_PAGE_USE_COUNT, 1, columnPageUseCount);
			m_outputPageStatistics.setTable(FP_BY_PAGE_COUNT, 1, fpPageCount);
			m_outputPageStatistics.setTable(FP_BY_PAGE_USE_COUNT, 1, fpPageUseCount);
		}

		private Table produceOutputByPageCountForColumns() throws OException
		{
			APM_Print("produceOutputByPageCountForColumns");

			final String NUMBER_OF_PAGES = "NumberOfPages";

			Table columnData = getRawColumnData("Col results by Pages");
			columnData.addCol(NUMBER_OF_PAGES, COL_TYPE_ENUM.COL_INT);

			int numberOfPages = 0;
			String lastRow = "";
			String lastPage = "";
			
			int numRows = columnData.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String thisRow = getStringSafe(columnData, m_PACKAGE_NAME, row) +
								 getStringSafe(columnData, m_COLUMN_NAME, row);
				String thisPage = getStringSafe(columnData, m_PAGE_NAME, row);

				boolean sameColumn = (thisRow.equals(lastRow) || lastRow == "");
				if (sameColumn)
				{
					boolean differentPage = (!thisPage.equals(lastPage) || lastPage == "");
					
					if (differentPage)
					{
						numberOfPages++;
					}
				}
				else
				{
					columnData.setInt(NUMBER_OF_PAGES, row - 1, numberOfPages);
					numberOfPages = 1;
				}

				lastPage = thisPage;
				lastRow = thisRow;
			}
			columnData.setInt(NUMBER_OF_PAGES, numRows, numberOfPages);
			columnData.delCol(m_PAGE_NAME);  

			Table outputByPageCountForColumns = columnData.cloneTable();
			String what = "DISTINCT, " + m_PACKAGE_NAME + ", " + m_COLUMN_NAME + ", " + NUMBER_OF_PAGES;
			String where = NUMBER_OF_PAGES + " GT 0";
			outputByPageCountForColumns.select(columnData, what, where);
			
			columnData.destroy();
			return outputByPageCountForColumns;
		}

		private Table getRawColumnData(String tableTitle) throws OException
		{
			Table columnData = Table.tableNew();
			columnData.setTableTitle(tableTitle);
			columnData.addCol(m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_COLUMN_NAME, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);

			columnData.fillSetSourceTable(m_rawData);
			columnData.fillAddMatchString(m_COLUMN_LIST_SECTION, m_SECTION, MATCH_CMP_ENUM.MATCH_EQ);
			columnData.fillAddMatchInt(1, m_ROW_NUM, MATCH_CMP_ENUM.MATCH_EQ);
			columnData.fillAddData(m_PACKAGE_NAME, m_PACKAGE_NAME);
			columnData.fillAddData(m_COLUMN_NAME, m_COLUMN_NAME);
			columnData.fillAddData(m_PAGE_NAME, m_PAGE_NAME);
			columnData.fill();

			columnData.addGroupBy(m_PACKAGE_NAME);
			columnData.addGroupBy(m_COLUMN_NAME);
			columnData.addGroupBy(m_PAGE_NAME);
			columnData.groupBy();
			
			return columnData;
		}
		
		private Table produceOutputByPageUsageForColumns() throws OException
		{
			APM_Print("produceOutputByUsageCountForColumns");

			final String NUMBER_OF_USES = "NumberOfUses";
			
			Table columnData = getRawColumnData("Col results by Page Use");
			columnData.addCol(NUMBER_OF_USES, COL_TYPE_ENUM.COL_INT);

			int numberOfUses = 0;
			String lastRow = "";
			
			int numRows = columnData.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String thisRow = getStringSafe(columnData, m_PACKAGE_NAME, row) +
								 getStringSafe(columnData, m_COLUMN_NAME, row) +
								 getStringSafe(columnData, m_PAGE_NAME, row);

				boolean samePage = (thisRow.equals(lastRow) || lastRow == "");
				if (samePage)
				{
					numberOfUses++;
				}
				else
				{
					columnData.setInt(NUMBER_OF_USES, row - 1, numberOfUses);
					numberOfUses = 1;
				}

				lastRow = thisRow;
			}
			columnData.setInt(NUMBER_OF_USES, numRows, numberOfUses);

			Table outputByUsageCountForColumns = columnData.cloneTable();
			String what = "DISTINCT, " + m_PACKAGE_NAME + ", " + m_COLUMN_NAME + ", " + m_PAGE_NAME + ", " + NUMBER_OF_USES;
			String where = NUMBER_OF_USES + " GT 0";
			outputByUsageCountForColumns.select(columnData, what, where);
			
			columnData.destroy();
			return outputByUsageCountForColumns; 
		}

		private Table produceOutputByPageCountForFiltersPivots() throws OException
		{
			APM_Print("produceOutputByPageCountForFiltersPivots");

			final String NUMBER_OF_PAGES = "NumberOfPages";

			Table fpData = getRawFilterPivotData("P-F results by Pages");
			fpData.addCol(NUMBER_OF_PAGES, COL_TYPE_ENUM.COL_INT);
			
			int numberOfPages = 0;
			String lastRow = "";
			String lastPage = "";
			
			int numRows = fpData.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String thisRow = getStringSafe(fpData, m_PACKAGE_NAME, row) +
								 getStringSafe(fpData, m_SECTION, row) +
								 getStringSafe(fpData, m_TYPE, row) +
								 getStringSafe(fpData, m_NAME, row);
				String thisPage = getStringSafe(fpData, m_PAGE_NAME, row);

				boolean sameColumn = (thisRow.equals(lastRow) || lastRow == "");
				if (sameColumn)
				{
					boolean differentPage = (!thisPage.equals(lastPage) || lastPage == "");
					
					if (differentPage)
					{
						numberOfPages++;
					}
				}
				else
				{
					fpData.setInt(NUMBER_OF_PAGES, row - 1, numberOfPages);
					numberOfPages = 1;
				}

				lastPage = thisPage;
				lastRow = thisRow;
			}
			fpData.setInt(NUMBER_OF_PAGES, numRows, numberOfPages);
			fpData.delCol(m_PAGE_NAME);  

			Table outputByPageCountForFp = fpData.cloneTable();
			String what = "DISTINCT, " + m_PACKAGE_NAME + ", " + m_SECTION + ", " + m_TYPE + ", " + m_NAME + ", " + NUMBER_OF_PAGES;
			String where = NUMBER_OF_PAGES + " GT 0";
			outputByPageCountForFp.select(fpData, what, where);
			
			fpData.destroy();
			return outputByPageCountForFp;
		}

		private Table getRawFilterPivotData(String tableTitle) throws OException
		{
			Table columnData = Table.tableNew();
			columnData.setTableTitle(tableTitle);
			columnData.addCol(m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_SECTION, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_TYPE, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_NAME, COL_TYPE_ENUM.COL_STRING);
			columnData.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);

			columnData.fillSetSourceTable(m_rawData);
			columnData.fillAddMatchString(m_COLUMN_LIST_SECTION, m_SECTION, MATCH_CMP_ENUM.MATCH_NE);
			columnData.fillAddMatchInt(1, m_ROW_NUM, MATCH_CMP_ENUM.MATCH_EQ);
			columnData.fillAddData(m_PACKAGE_NAME, m_PACKAGE_NAME);
			columnData.fillAddData(m_SECTION, m_SECTION);
			columnData.fillAddData(m_TYPE, m_TYPE);
			columnData.fillAddData(m_NAME, m_NAME);
			columnData.fillAddData(m_PAGE_NAME, m_PAGE_NAME);
			columnData.fillDistinct();

			columnData.addGroupBy(m_PACKAGE_NAME);
			columnData.addGroupBy(m_SECTION);
			columnData.addGroupBy(m_TYPE);
			columnData.addGroupBy(m_NAME);
			columnData.addGroupBy(m_PAGE_NAME);
			columnData.groupBy();
			
			return columnData;
		}

		private Table produceOutputByPageUsageForFiltersPivots() throws OException
		{
			APM_Print("produceOutputByPageUsageForFiltersPivots");

			final String NUMBER_OF_USES = "NumberOfUses";
			
			Table fpData = getRawFilterPivotData("P-F results by Page Use");
			fpData.addCol(NUMBER_OF_USES, COL_TYPE_ENUM.COL_INT);
		
			int numberOfUses = 0;
			String lastRow = "";
			
			int numRows = fpData.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String thisRow = getStringSafe(fpData, m_PACKAGE_NAME, row) +
								 getStringSafe(fpData, m_SECTION, row) +
								 getStringSafe(fpData, m_TYPE, row) +
								 getStringSafe(fpData, m_NAME, row) +
								 getStringSafe(fpData, m_PAGE_NAME, row);

				boolean samePage = (thisRow.equals(lastRow) || lastRow == "");
				if (samePage)
				{
					numberOfUses++;
				}
				else
				{
					fpData.setInt(NUMBER_OF_USES, row - 1, numberOfUses);
					numberOfUses = 1;
				}

				lastRow = thisRow;
			}
			fpData.setInt(NUMBER_OF_USES, numRows, numberOfUses);

			Table outputByUsageCountForFp = fpData.cloneTable();
			String what = "DISTINCT, " + m_PACKAGE_NAME + ", " + m_SECTION + ", " + m_TYPE + ", " + m_NAME + ", " + m_PAGE_NAME + ", " + NUMBER_OF_USES;
			String where = NUMBER_OF_USES + " GT 0";
			outputByUsageCountForFp.select(fpData, what, where);
			
			fpData.destroy();
			return outputByUsageCountForFp; 
		}
		
		private Table DbAvailableFiltersPivots() throws OException
		{
			Table dbFiltersPivots = Table.tableNew();
			
			String what  = "a.package_name, a.filter_name";    
			String from  = "tfe_pkg_folder_filters a, tfe_filter_defs b";    
			String where = "a.filter_name = b.filter_name order by filter_name"; 
			DBaseTable.loadFromDbWithSQL(dbFiltersPivots, what, from, where);  

			return dbFiltersPivots;
		}
		
		private void getListOfAllAvailableFiltersPivotsColumns() throws OException
		{
			APM_Print("getListOfAllAvailableFiltersPivotsColumns");
			
			Table listOfAllUsedPivotsFiltersColumns = Table.tableNew();
			
			String what = "DISTINCT, " + m_SECTION + ", " + m_TYPE + ", " + m_PACKAGE_NAME + ", " + m_NAME + "," + m_COLUMN_NAME;
			String where = m_PAGE_NAME + " NE ''";
			listOfAllUsedPivotsFiltersColumns.select(m_rawData, what, where);
			
			//get the list of all filters and pivots in the db (by package)
			Table dbFiltersPivots = DbAvailableFiltersPivots();
			
			// copy the package name in for filters/pivots
			listOfAllUsedPivotsFiltersColumns.select(dbFiltersPivots, "package_name", "package_name EQ $PackageName AND filter_name EQ $Name" );
			
			// set the package name for columns
			for ( int loopCtr = 1; loopCtr <= listOfAllUsedPivotsFiltersColumns.getNumRows(); loopCtr++)
			{
				if (getStringSafe(listOfAllUsedPivotsFiltersColumns, m_TYPE, loopCtr).equals("DataColumn") )
				{
					listOfAllUsedPivotsFiltersColumns.setString("package_name", loopCtr, getStringSafe(listOfAllUsedPivotsFiltersColumns, "PackageName", loopCtr));
					listOfAllUsedPivotsFiltersColumns.setString(m_NAME, loopCtr, getStringSafe(listOfAllUsedPivotsFiltersColumns, m_COLUMN_NAME, loopCtr));
				}
				else
				{
					// set the type = "Pivot_Filter"
					listOfAllUsedPivotsFiltersColumns.setString(m_TYPE, loopCtr, "Pivot_Filter");
				}
			}
			
			// finally delete the "PackageName" column in favour of the "package_name" column
			listOfAllUsedPivotsFiltersColumns.delCol(m_PACKAGE_NAME);
			listOfAllUsedPivotsFiltersColumns.delCol(m_COLUMN_NAME);
			listOfAllUsedPivotsFiltersColumns.setColName("package_name", m_PACKAGE_NAME);
			
			// now lets dedup the list
			Table dedupedList = Table.tableNew();
			what = "DISTINCT, " + m_TYPE + ", " + m_PACKAGE_NAME + ", " + m_NAME;			
			where = m_NAME + " NE ''";
			dedupedList.select(listOfAllUsedPivotsFiltersColumns, what, where);
			dedupedList.setTableTitle("List Of All Used Pivots, Filters and Columns");
			
			listOfAllUsedPivotsFiltersColumns.destroy();
			m_listOfAllUsedPivotsFiltersColumns = dedupedList;
		}

	}
	
	class DbComparer
	{	
		private Table m_dbFilterPivotComponents;
		private Table m_dbColumnComponents;
		private Table m_mandatoryFilterPivotComponents;
		
		private Table m_usedOnPageNotDb;
		private Table m_usedInDbNotOnPage;
		private Table m_usedInBoth;

		public Table outputMissingFromDB()
		{
			return m_usedOnPageNotDb;
		}

		public Table outputMissingOnPages()
		{
			return m_usedInDbNotOnPage;
		}

		public Table outputInUse()
		{
			return m_usedInBoth;
		}

		public void CompareAllPageComponentsToDb(Table pageComponents) throws OException
		{
			APM_Print("CompareAllPageComponentsToDb");
			
			m_usedOnPageNotDb = setupOutputTable("Components on Pages only");
			m_usedInDbNotOnPage = setupOutputTable("Components in DB only");
			m_usedInBoth = setupOutputTable("Components used in both");

			m_dbFilterPivotComponents = DbFilterPivotComponents();
			m_dbColumnComponents = DbColumnComponents();
			m_mandatoryFilterPivotComponents = MandatoryFilterPivotComponents();
			
			CompareFiltersPivots(pageComponents);
			CompareColumns(pageComponents);
		}

		private void CompareFiltersPivots(Table pageComponents) throws OException
		{
			final String ENRICHMENT_NAME = "enrichment_name";
			final String PACKAGE_NAME = "package_name";
			final String TYPE_TO_CHECK = "Pivot_Filter";

//			pageComponents.viewTable();
//			m_dbFilterPivotComponents.viewTable();
//			m_mandatoryFilterPivotComponents.viewTable();
			
			ComparePageComponentsToDb(pageComponents, TYPE_TO_CHECK, m_dbFilterPivotComponents, m_mandatoryFilterPivotComponents, PACKAGE_NAME, ENRICHMENT_NAME);
			CompareDbToPageComponents(pageComponents, TYPE_TO_CHECK, m_dbFilterPivotComponents, m_mandatoryFilterPivotComponents, PACKAGE_NAME, ENRICHMENT_NAME);
		}

		private void CompareColumns(Table pageComponents) throws OException
		{
			final String COLUMN_NAME = "column_name";
			final String PACKAGE_NAME = "package_name";
			final String TYPE_TO_CHECK = "DataColumn";
			
//			pageComponents.viewTable();
//			m_dbColumnComponents.viewTable();

			ComparePageComponentsToDb(pageComponents, TYPE_TO_CHECK, m_dbColumnComponents, Util.NULL_TABLE, PACKAGE_NAME, COLUMN_NAME);
			CompareDbToPageComponents(pageComponents, TYPE_TO_CHECK, m_dbColumnComponents, Util.NULL_TABLE, PACKAGE_NAME, COLUMN_NAME);
		}

		private Table DbFilterPivotComponents() throws OException
		{
			Table components = Table.tableNew();
			
			String what  = "*";    
			String from  = "apm_pkg_enrichment_config a, apm_enrichment_defs b";    
			String where = "a.enrichment_name = b.enrichment_name and a.on_off_flag = 1 and b.enrichment_type = 1"; 
			DBaseTable.loadFromDbWithSQL(components, what, from, where);  

//		components.viewTable();
			
			return components;
		}

		private Table DbColumnComponents() throws OException
		{
			Table components = Table.tableNew();
			
			String what  = "*";    
			String from  = "tfe_pkg_folder_columns a, tfe_column_defs b";    
			String where = "a.column_name = b.column_name and a.on_off_flag = 1"; 
			DBaseTable.loadFromDbWithSQL(components, what, from, where);  

//		components.viewTable();
			
			return components;
		}

		private Table MandatoryFilterPivotComponents() throws OException
		{
			Table components = Table.tableNew();
			
			String what  = "distinct package_name, filter_name enrichment_name";    
			String from  = "tfe_pkg_folder_filters";    
			String where = "is_visible_admin = 0 order by enrichment_name"; 
			DBaseTable.loadFromDbWithSQL(components, what, from, where);  

//		components.viewTable();
			
			return components;
		}
		
		private Table setupOutputTable(String tableName) throws OException
		{
			Table outputTable = Table.tableNew();
			outputTable.setTableTitle(tableName);

			outputTable.addCol(PageParser.m_TYPE, COL_TYPE_ENUM.COL_STRING);
			outputTable.addCol(PageParser.m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			outputTable.addCol(PageParser.m_NAME, COL_TYPE_ENUM.COL_STRING);
			
			return outputTable;
		}

		private void ComparePageComponentsToDb(Table pageComponents, String typeToCheck, Table dbComponents, Table mandatoryComponents, String dbComponentPackageColumn, String dbComponentColumn) throws OException
		{			
			APM_Print("ComparePageComponentsToDb");
			
			int numRows = pageComponents.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String type = getStringSafe(pageComponents, PageParser.m_TYPE, row);
				if (typeToCheck.equals(type))
				{
					String componentName = getStringSafe(pageComponents, PageParser.m_NAME, row);
					String componentPackageName = getStringSafe(pageComponents, PageParser.m_PACKAGE_NAME, row);
					
					// check whether its a mandatory component - if so - skip
					if ( mandatoryComponents != Util.NULL_TABLE)
					{
						int mandatoryRow = mandatoryComponents.findString(dbComponentColumn, componentName, SEARCH_ENUM.FIRST_IN_GROUP);
						if ( mandatoryRow > 0 )
						{
							String mandatoryComponentpackageName = getStringSafe(mandatoryComponents, dbComponentPackageColumn, mandatoryRow);
							if ( componentPackageName.equals(mandatoryComponentpackageName) )
							{
								// found a mandatory component
								continue;
							}
						}
					}

					boolean found = findComponent(dbComponents, dbComponentPackageColumn, componentPackageName, dbComponentColumn, componentName);
					if (found)
					{
						addComponentToResults(m_usedInBoth, type, componentPackageName, componentName);
					}
					else
					{
						addComponentToResults(m_usedOnPageNotDb, type, componentPackageName, componentName);
					}
				}
			}
		}

		private void CompareDbToPageComponents(Table pageComponents, String typeToCheck, Table dbComponents, Table mandatoryComponents, String dbComponentPackageColumn, String dbComponentColumn) throws OException
		{
			APM_Print("CompareDbToPageComponents");
			
			int numRows = dbComponents.getNumRows();
			for(int row = 1; row <= numRows; row++)
			{
				String componentName = getStringSafe(dbComponents, dbComponentColumn, row).trim();
				String componentPackageName = getStringSafe(dbComponents, dbComponentPackageColumn, row);
				
				// check whether its a mandatory component - if so - skip
				if ( mandatoryComponents != Util.NULL_TABLE)
				{
					int mandatoryRow = mandatoryComponents.findString(dbComponentColumn, componentName, SEARCH_ENUM.FIRST_IN_GROUP);
					if ( mandatoryRow > 0 )
					{
						String mandatoryComponentpackageName = getStringSafe(mandatoryComponents, dbComponentPackageColumn, mandatoryRow);
						if ( componentPackageName.equals(mandatoryComponentpackageName) )
						{
							// found a mandatory component
							continue;
						}
					}
				}
				
				boolean found = findComponent(pageComponents, PageParser.m_PACKAGE_NAME, componentPackageName, PageParser.m_NAME, componentName);
				if (!found)
				{
					addComponentToResults(m_usedInDbNotOnPage, typeToCheck, componentPackageName, componentName);
				}
			}
		}
	
		
		private boolean findComponent(Table tableComponents,  String componentPackageColumnName, String componentPackage, String componentColumnName, String componentValue) throws OException
		{
			Table searchResult = Table.tableNew();
			searchResult.addCol(componentPackageColumnName, COL_TYPE_ENUM.COL_STRING);
			searchResult.addCol(componentColumnName, COL_TYPE_ENUM.COL_STRING);
			
			searchResult.fillSetSourceTable(tableComponents);
			searchResult.fillAddMatchString(componentValue, componentColumnName, MATCH_CMP_ENUM.MATCH_EQ);
			searchResult.fillAddData(componentPackageColumnName, componentPackageColumnName);
			searchResult.fillAddData(componentColumnName, componentColumnName);
			searchResult.fill();

			boolean found = false;
			
			int numRows = searchResult.getNumRows();
			for(int row = 1; row <= numRows && found==false; row++)
			{
				String packageValue = getStringSafe(searchResult, componentPackageColumnName, row);
				
				if (packageValue.equals(componentPackage))
				{
					found = true;
				}
			}

			searchResult.destroy();					
			return found;
		}
		
		private void addComponentToResults(Table resultsTable, String type, String packageName, String component) throws OException
		{
			int newRow = resultsTable.addRow();
			resultsTable.setString(PageParser.m_TYPE, newRow, type);
			resultsTable.setString(PageParser.m_PACKAGE_NAME, newRow, packageName);
			resultsTable.setString(PageParser.m_NAME, newRow, component);
		}
	}
	
	class DbDatasetKeysByService
	{
		public final static String m_PACKAGE_NAME = "PackageName"; 
		public final static String m_TYPE = "Type"; 
		public final static String m_NAME = "Name"; 
		public final static String m_VALUE = "Value"; 
		public final static String m_PAGE_NAME = "PageName"; 
		public final static String m_SECTION = "Section"; 

		public final static String m_DATASET_TYPE_FILTER = "Dataset Type"; 
		public final static String m_PORTFOLIO_FILTER = "Portfolio"; 
		public final static String m_NAGAS_SERVICE_PROVIDER_FILTER = "NAGas Service Provider"; 
		public final static String m_SCENARIO_FILTER = "Scenario"; 
		public final static String m_COL_NUM = "ColumnNumber"; 
		public final static String m_ROW_NUM = "RowNumber"; 

		public final static String m_PAGEFILTER_TYPE = "PageFilter"; 
		public final static String m_FILTER_TYPE = "Filter"; 
		public final static String m_DATACOLUMN_TYPE = "DataColumn"; 
		
		public final static String m_DATASET_TYPE_COLNAME = "dataset_type_name"; 
		public final static String m_PACKAGE_NAME_COLNAME = "package"; 
		public final static String m_ENTITYGROUP_NAME_COLNAME = "entity_group_name"; 
		public final static String m_SCENARIO_NAME_COLNAME = "scenario_name"; 
		public final static String m_DATASET_TYPE_ID_COLNAME = "dataset_type_id"; 
		public final static String m_ENTITYGROUP_ID_COLNAME = "entity_group_id"; 
		public final static String m_SCENARIO_ID_COLNAME = "scenario_id"; 
		
		private Table m_keysInServicesWithRowCount;
		private Table m_keysInPages;
		private Table m_keysInServicesButNotInPages;
		private Table m_keysInPagesButNotInServices;
		private Table m_keysInServicesAndInPages;
		private Table m_keysInMoreThanOneService;
		
		public Table compareDbDatasetKeysVersusPageDatasetKeys(Table rawDataPageComponents) throws OException
		{
			APM_Print("compareDbDatasetKeysVersusPageDatasetKeys");
			
			Table dbDatasetKeys = getDatasetKeysForAllAPMServices();
			
			dbDatasetKeys.clearGroupBy();
			dbDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			dbDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			dbDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			dbDatasetKeys.groupBy();

			Table dbDatasetKeysWithRowCount = EnrichDbDatasetKeysWithRowCount(dbDatasetKeys);
			EnrichDbDataSetKeysWithBatchStatistics(dbDatasetKeysWithRowCount);
			m_keysInServicesWithRowCount = dbDatasetKeysWithRowCount;
			
			Table pageDatasetKeys = GetDatasetKeysPerPage(rawDataPageComponents);
			Table allPageDatasetKeys = GetAllDataSetKeysUsedByPages(pageDatasetKeys);
			
			Table expandedPageDatasetKeys = ExpandNotSetEntityGroups(allPageDatasetKeys, dbDatasetKeys);
			
			m_keysInPages = expandedPageDatasetKeys;
			
			m_keysInServicesButNotInPages = expandedPageDatasetKeys.cloneTable();
			m_keysInPagesButNotInServices = expandedPageDatasetKeys.cloneTable();
			m_keysInServicesAndInPages = expandedPageDatasetKeys.cloneTable();
			m_keysInMoreThanOneService = dbDatasetKeys.cloneTable();
			m_keysInServicesButNotInPages.setTableTitle("DSKs in Services only");
			m_keysInPagesButNotInServices.setTableTitle("DSKs in Pages only");
			m_keysInServicesAndInPages.setTableTitle("DSKs in both");
			m_keysInMoreThanOneService.setTableTitle("DSK duplicates in db");
			
			ComparePageKeysAgainstDbKeys(expandedPageDatasetKeys, dbDatasetKeys);
			CompareDbKeysAgainstPageKeys(dbDatasetKeys, expandedPageDatasetKeys);
			IdentifyKeysInMoreThanOneService(dbDatasetKeys);
						
			Table allDatasetKeyInfo = Table.tableNew();
			allDatasetKeyInfo.setTableTitle("DSK report");
			allDatasetKeyInfo.addCol("Service Keys", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addCol("Page Keys", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addCol("DSKs in both", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addCol("DSKs in Services only", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addCol("DSKs in Pages only", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addCol("DSK duplicates in db", COL_TYPE_ENUM.COL_TABLE);
			allDatasetKeyInfo.addRow();
			allDatasetKeyInfo.setTable("Service Keys", 1, m_keysInServicesWithRowCount);
			allDatasetKeyInfo.setTable("Page Keys", 1, m_keysInPages);
			allDatasetKeyInfo.setTable("DSKs in both", 1, m_keysInServicesAndInPages);
			allDatasetKeyInfo.setTable("DSKs in Services only", 1, m_keysInServicesButNotInPages);
			allDatasetKeyInfo.setTable("DSKs in Pages only", 1, m_keysInPagesButNotInServices);
			allDatasetKeyInfo.setTable("DSK duplicates in db", 1, m_keysInMoreThanOneService);
			
			APM_Print("FINISHED compareDbDatasetKeysVersusPageDatasetKeys");
			
			return allDatasetKeyInfo;
			
		}

		public Table GetKeysInServicesButNotInPages() throws OException
		{
			return m_keysInServicesButNotInPages;
		}
		
		public Table GetKeysInPagesButNotInServices() throws OException
		{
			return m_keysInPagesButNotInServices;
		}

		public Table GetKeysInServicesAndInPages() throws OException
		{
			return m_keysInServicesAndInPages;
		}

		public Table GetKeysInMoreThanOneService() throws OException
		{
			return m_keysInMoreThanOneService;
		}
		
		private Table EnrichDbDatasetKeysWithRowCount(Table dbDatasetKeys) throws OException
		{
			String what = "distinct a.entity_group_id, a.package, a.scenario_id, a.dataset_type_id, sum(blob_row_count) datacount";
			String from = "apm_dataset_control a, apm_data b";
			String where = "a.dataset_id = b.dataset_id and b.operation = 0" +
							"group by service_id, entity_group_id, package, scenario_id, dataset_type_id";
			
			Table dataCounts = Table.tableNew();
			DBaseTable.loadFromDbWithSQL(dataCounts, what, from, where);  

			// now join back onto dbDatasetKeys
			Table originalDatasetKeys = dbDatasetKeys.copyTable();
			originalDatasetKeys.select(dataCounts, "datacount", "package EQ $package AND entity_group_id EQ $entity_group_id AND scenario_id EQ $scenario_id AND dataset_type_id EQ $dataset_type_id");
			
			dataCounts.destroy();
			
			originalDatasetKeys.setColTitle("datacount", "NumOfDataRows");
			return originalDatasetKeys;
		}

		private void EnrichDbDataSetKeysWithBatchStatistics(Table originalDatasetKeys) throws OException
		{
			Table args = Table.tableNew();
			DBase.runProc("USER_apm_get_batch_stats", args);

			Table results = Table.tableNew();
			DBase.createTableOfQueryResults(results);
			
			String what = "entity_count, elapsed";
			String where = "service_name EQ $service_name AND entity_group_name EQ $entity_group_name AND package_name EQ $package AND msg_type EQ 1";
			originalDatasetKeys.select(results, what, where);
			
			originalDatasetKeys.setColTitle("entity_count", "NumOfEntities");
		}
		
		private void IdentifyKeysInMoreThanOneService(Table dbDatasetKeys) throws OException
		{
			APM_Print("IdentifyKeysInMoreThanOneService");
			
			dbDatasetKeys.clearGroupBy();
			dbDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			dbDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			dbDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			dbDatasetKeys.addGroupBy(m_ENTITYGROUP_NAME_COLNAME);
			dbDatasetKeys.groupBy();

			for (int loopCtr = 1; loopCtr < dbDatasetKeys.getNumRows(); loopCtr++)
			{
				String dbPackageName = getStringSafe(dbDatasetKeys, m_PACKAGE_NAME_COLNAME, loopCtr);
				String dbDataSetTypeName = getStringSafe(dbDatasetKeys, m_DATASET_TYPE_COLNAME, loopCtr);
				String dbScenarioName = getStringSafe(dbDatasetKeys, m_SCENARIO_NAME_COLNAME, loopCtr);
				String dbEntityGroupName = getStringSafe(dbDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, loopCtr);

				if ( dbPackageName == null || dbPackageName.length() == 0 || 
					 dbDataSetTypeName == null || dbDataSetTypeName.length() == 0 ||	
					 dbScenarioName == null || dbScenarioName.length() == 0 ||
					 dbEntityGroupName == null || dbEntityGroupName.length() == 0 )
						continue;
				
				String nextPackageName = getStringSafe(dbDatasetKeys, m_PACKAGE_NAME_COLNAME, loopCtr+1);
				String nextDataSetTypeName = getStringSafe(dbDatasetKeys, m_DATASET_TYPE_COLNAME, loopCtr+1);
				String nextScenarioName = getStringSafe(dbDatasetKeys, m_SCENARIO_NAME_COLNAME, loopCtr+1);
				String nextEntityGroupName = getStringSafe(dbDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, loopCtr+1);
				
				if ( dbPackageName.equals(nextPackageName) && dbDataSetTypeName.equals(nextDataSetTypeName) &&
						dbScenarioName.equals(nextScenarioName) &&  dbEntityGroupName.equals(nextEntityGroupName) )
				{
					// we have a duplicate row
					dbDatasetKeys.copyRowAdd(loopCtr, m_keysInMoreThanOneService);
					dbDatasetKeys.copyRowAdd(loopCtr+1, m_keysInMoreThanOneService);
				}
			}
			
		}
		
		private void ComparePageKeysAgainstDbKeys(Table expandedPageDatasetKeys, Table dbDatasetKeys)  throws OException
		{
			APM_Print("ComparePageKeysAgainstDbKeys");
			
			Table dbDatasetKeysClean = dbDatasetKeysWithoutDuplicates(dbDatasetKeys);
			
			// now compare keys used by pages against keys generated by db
			for (int loopCtr = 1; loopCtr <= expandedPageDatasetKeys.getNumRows(); loopCtr++)
			{
				String entityGroupName = getStringSafe(expandedPageDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, loopCtr);				
				String datasetType = getStringSafe(expandedPageDatasetKeys, m_DATASET_TYPE_COLNAME, loopCtr);
				String packageName = getStringSafe(expandedPageDatasetKeys, m_PACKAGE_NAME_COLNAME, loopCtr);
				String scenarioName = getStringSafe(expandedPageDatasetKeys, m_SCENARIO_NAME_COLNAME, loopCtr);
				int datasetTypeID = expandedPageDatasetKeys.getInt(m_DATASET_TYPE_ID_COLNAME, loopCtr);
				int scenarioID = expandedPageDatasetKeys.getInt(m_SCENARIO_ID_COLNAME, loopCtr);
				int entityGroupID = expandedPageDatasetKeys.getInt(m_ENTITYGROUP_ID_COLNAME, loopCtr);
				
				// we have an ALL row - cross reference what's being generated in db to see the actual keys being used
				int startRow = dbDatasetKeysClean.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.FIRST_IN_GROUP);
				int endRow = dbDatasetKeysClean.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.LAST_IN_GROUP);
				
				if ( startRow < 0 )
					continue; // this indicates a client side package such as prices...
				
				int newRow = -1;
				for (int packageRow = startRow; packageRow <= endRow; packageRow++)
				{
					String dbDataSetTypeName = getStringSafe(dbDatasetKeysClean, m_DATASET_TYPE_COLNAME, packageRow);
					String dbScenarioName = getStringSafe(dbDatasetKeysClean, m_SCENARIO_NAME_COLNAME, packageRow);
					int dbEntityGroupId = dbDatasetKeysClean.getInt(m_ENTITYGROUP_ID_COLNAME, packageRow);
					String dbEntityGroupName = getStringSafe(dbDatasetKeysClean, m_ENTITYGROUP_NAME_COLNAME, packageRow);
					
					if ( dbDataSetTypeName.equals(datasetType) && dbScenarioName.equals(scenarioName) && dbEntityGroupName.equals(entityGroupName))
					{
						//then this entityGroup is being used qnd it has been found - add a row for it
						newRow = m_keysInServicesAndInPages.addRow();
						m_keysInServicesAndInPages.setString(m_DATASET_TYPE_COLNAME, newRow, datasetType);
						m_keysInServicesAndInPages.setString(m_PACKAGE_NAME_COLNAME, newRow, packageName);
						m_keysInServicesAndInPages.setString(m_ENTITYGROUP_NAME_COLNAME, newRow, entityGroupName);
						m_keysInServicesAndInPages.setString(m_SCENARIO_NAME_COLNAME, newRow, scenarioName);
						m_keysInServicesAndInPages.setInt(m_DATASET_TYPE_ID_COLNAME, newRow, datasetTypeID);
						m_keysInServicesAndInPages.setInt(m_ENTITYGROUP_ID_COLNAME, newRow, dbEntityGroupId);
						m_keysInServicesAndInPages.setInt(m_SCENARIO_ID_COLNAME, newRow, scenarioID);
					}					
				}
				if ( newRow == -1 ) 
				{
					// not found in db....
					newRow = m_keysInPagesButNotInServices.addRow();
					m_keysInPagesButNotInServices.setString(m_DATASET_TYPE_COLNAME, newRow, datasetType);
					m_keysInPagesButNotInServices.setString(m_PACKAGE_NAME_COLNAME, newRow, packageName);
					m_keysInPagesButNotInServices.setString(m_ENTITYGROUP_NAME_COLNAME, newRow, entityGroupName);
					m_keysInPagesButNotInServices.setString(m_SCENARIO_NAME_COLNAME, newRow, scenarioName);
					m_keysInPagesButNotInServices.setInt(m_DATASET_TYPE_ID_COLNAME, newRow, datasetTypeID);
					m_keysInPagesButNotInServices.setInt(m_ENTITYGROUP_ID_COLNAME, newRow, entityGroupID);
					m_keysInPagesButNotInServices.setInt(m_SCENARIO_ID_COLNAME, newRow, scenarioID);					
				}
			}
			
		}

		private void CompareDbKeysAgainstPageKeys(Table dbDatasetKeys, Table expandedPageDatasetKeys)  throws OException
		{
			APM_Print("CompareDbKeysAgainstPageKeys");
			
			expandedPageDatasetKeys.clearGroupBy();
			expandedPageDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			expandedPageDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			expandedPageDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			expandedPageDatasetKeys.addGroupBy(m_ENTITYGROUP_NAME_COLNAME);
			expandedPageDatasetKeys.groupBy();
			
			// now compare keys used by pages against keys generated by db
			for (int loopCtr = 1; loopCtr <= dbDatasetKeys.getNumRows(); loopCtr++)
			{
				String entityGroupName = getStringSafe(dbDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, loopCtr);				
				String datasetType = getStringSafe(dbDatasetKeys, m_DATASET_TYPE_COLNAME, loopCtr);
				String packageName = getStringSafe(dbDatasetKeys, m_PACKAGE_NAME_COLNAME, loopCtr);
				String scenarioName = getStringSafe(dbDatasetKeys, m_SCENARIO_NAME_COLNAME, loopCtr);
				int datasetTypeID = dbDatasetKeys.getInt(m_DATASET_TYPE_ID_COLNAME, loopCtr);
				int scenarioID = dbDatasetKeys.getInt(m_SCENARIO_ID_COLNAME, loopCtr);
				int entityGroupID = dbDatasetKeys.getInt(m_ENTITYGROUP_ID_COLNAME, loopCtr);
				
				// we have an ALL row - cross reference whats being generated in db to see the actual keys being used
				int startRow = expandedPageDatasetKeys.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.FIRST_IN_GROUP);
				int endRow = expandedPageDatasetKeys.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.LAST_IN_GROUP);
								
				int newRow = -1;
				boolean found = false;
				if ( startRow > 0 )
				{
					for (int packageRow = startRow; packageRow <= endRow; packageRow++)
					{
						String pageDataSetTypeName = getStringSafe(expandedPageDatasetKeys, m_DATASET_TYPE_COLNAME, packageRow);
						String pageScenarioName = getStringSafe(expandedPageDatasetKeys, m_SCENARIO_NAME_COLNAME, packageRow);
						String pageEntityGroupName = getStringSafe(expandedPageDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, packageRow);
						
						if ( pageDataSetTypeName.equals(datasetType) && pageScenarioName.equals(scenarioName) && pageEntityGroupName.equals(entityGroupName))
						{
							found = true;
							break;
						}
					}
				}
				
				if ( found == false ) 
				{
					// not found in db....
					newRow = m_keysInServicesButNotInPages.addRow();
					m_keysInServicesButNotInPages.setString(m_DATASET_TYPE_COLNAME, newRow, datasetType);
					m_keysInServicesButNotInPages.setString(m_PACKAGE_NAME_COLNAME, newRow, packageName);
					m_keysInServicesButNotInPages.setString(m_ENTITYGROUP_NAME_COLNAME, newRow, entityGroupName);
					m_keysInServicesButNotInPages.setString(m_SCENARIO_NAME_COLNAME, newRow, scenarioName);
					m_keysInServicesButNotInPages.setInt(m_DATASET_TYPE_ID_COLNAME, newRow, datasetTypeID);
					m_keysInServicesButNotInPages.setInt(m_ENTITYGROUP_ID_COLNAME, newRow, entityGroupID);
					m_keysInServicesButNotInPages.setInt(m_SCENARIO_ID_COLNAME, newRow, scenarioID);					
				}
			}
			
		}
		
		private Table ExpandNotSetEntityGroups(Table allPageDatasetKeys, Table dbDatasetKeys)  throws OException
		{
			Table expandedPageDatasetKeys = Table.tableNew();
		
			// this fn deals with instances where the entityGroup filters (portfolio/NAGas service provider) are not set, so ALL entityGroups for a key are potentially viewed.
			// note that this could be inaccurate as only those entityGroups for which a user has priv will be viewed - but we have to assume all
			String what = "*";
			String where = m_ENTITYGROUP_NAME_COLNAME + " NE ALL";
			expandedPageDatasetKeys.select(allPageDatasetKeys, what, where);

			Table dbDatasetKeysClean = dbDatasetKeysWithoutDuplicates(dbDatasetKeys);		
			
			for (int loopCtr = 1; loopCtr <= allPageDatasetKeys.getNumRows(); loopCtr++)
			{
				String entityGroupValues = getStringSafe(allPageDatasetKeys, m_ENTITYGROUP_NAME_COLNAME, loopCtr);
				
				if (!entityGroupValues.equals("ALL"))
					continue;

				String datasetType = getStringSafe(allPageDatasetKeys, m_DATASET_TYPE_COLNAME, loopCtr);
				String packageName = getStringSafe(allPageDatasetKeys, m_PACKAGE_NAME_COLNAME, loopCtr);
				String scenarioName = getStringSafe(allPageDatasetKeys, m_SCENARIO_NAME_COLNAME, loopCtr);
				int datasetTypeID = allPageDatasetKeys.getInt(m_DATASET_TYPE_ID_COLNAME, loopCtr);
				int scenarioID = allPageDatasetKeys.getInt(m_SCENARIO_ID_COLNAME, loopCtr);
				
				// we have an ALL row - cross reference whats being generated in db to see the actual keys being used
				int startRow = dbDatasetKeysClean.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.FIRST_IN_GROUP);
				int endRow = dbDatasetKeysClean.findString(m_PACKAGE_NAME_COLNAME, packageName, SEARCH_ENUM.LAST_IN_GROUP);
				
				if ( startRow < 0 )
					continue; // this indicates a client side package such as prices...
				
				for (int packageRow = startRow; packageRow <= endRow; packageRow++)
				{
					String dbDataSetTypeName = getStringSafe(dbDatasetKeysClean, m_DATASET_TYPE_COLNAME, packageRow);
					String dbScenarioName = getStringSafe(dbDatasetKeysClean, m_SCENARIO_NAME_COLNAME, packageRow);
					int dbEntityGroupId = dbDatasetKeysClean.getInt(m_ENTITYGROUP_ID_COLNAME, packageRow);
					String dbEntityGroupName = getStringSafe(dbDatasetKeysClean, m_ENTITYGROUP_NAME_COLNAME, packageRow);
					
					if ( dbDataSetTypeName.equals(datasetType) && dbScenarioName.equals(scenarioName) )
					{
						//then this entityGroup is being used - add a row for it
						int expandedRow = expandedPageDatasetKeys.addRow();
						expandedPageDatasetKeys.setString(m_DATASET_TYPE_COLNAME, expandedRow, datasetType);
						expandedPageDatasetKeys.setString(m_PACKAGE_NAME_COLNAME, expandedRow, packageName);
						expandedPageDatasetKeys.setString(m_ENTITYGROUP_NAME_COLNAME, expandedRow, dbEntityGroupName);
						expandedPageDatasetKeys.setString(m_SCENARIO_NAME_COLNAME, expandedRow, scenarioName);
						expandedPageDatasetKeys.setInt(m_DATASET_TYPE_ID_COLNAME, expandedRow, datasetTypeID);
						expandedPageDatasetKeys.setInt(m_ENTITYGROUP_ID_COLNAME, expandedRow, dbEntityGroupId);
						expandedPageDatasetKeys.setInt(m_SCENARIO_ID_COLNAME, expandedRow, scenarioID);
					}
				}
			}
			
			Table expandedUniquePageDatasetKeys = Table.tableNew();
			expandedUniquePageDatasetKeys.select(expandedPageDatasetKeys, "DISTINCT, *", m_DATASET_TYPE_ID_COLNAME + " GT -1");
			
			expandedUniquePageDatasetKeys.setTableTitle("DSKs used by PAGES");
			expandedUniquePageDatasetKeys.clearGroupBy();
			expandedUniquePageDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			expandedUniquePageDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			expandedUniquePageDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			expandedUniquePageDatasetKeys.addGroupBy(m_ENTITYGROUP_NAME_COLNAME);
			expandedUniquePageDatasetKeys.groupBy();
			
			expandedPageDatasetKeys.destroy();
			return expandedUniquePageDatasetKeys;
		}

		private Table dbDatasetKeysWithoutDuplicates(Table allDbDatasetKeys) throws OException
		{
			Table dbDatasetKeys = Table.tableNew();
			
			String what  = "DISTINCT, " + m_DATASET_TYPE_COLNAME + "," + m_PACKAGE_NAME_COLNAME + "," + m_ENTITYGROUP_NAME_COLNAME + "," + 
			m_SCENARIO_NAME_COLNAME + ", " + m_DATASET_TYPE_ID_COLNAME + "," + m_ENTITYGROUP_ID_COLNAME + "," + m_SCENARIO_ID_COLNAME;    
			String where = m_DATASET_TYPE_ID_COLNAME + " GT -1";
			dbDatasetKeys.select( allDbDatasetKeys, what, where);
		      
			dbDatasetKeys.clearGroupBy();
			dbDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			dbDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			dbDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			dbDatasetKeys.groupBy();

			return dbDatasetKeys;
		}
		
		private Table GetAllDataSetKeysUsedByPages(Table pageDatasetKeys) throws OException
		{
			Table allPageDatasetKeys = Table.tableNew();
			String what  = "DISTINCT, " + m_DATASET_TYPE_COLNAME + "," + m_PACKAGE_NAME_COLNAME + "," + m_ENTITYGROUP_NAME_COLNAME + "," + 
							m_SCENARIO_NAME_COLNAME + ", " + m_DATASET_TYPE_ID_COLNAME + "," + m_ENTITYGROUP_ID_COLNAME + "," + m_SCENARIO_ID_COLNAME;    

			String where = m_DATASET_TYPE_ID_COLNAME + " GT -1";
			allPageDatasetKeys.select(pageDatasetKeys, what, where);  
			
			allPageDatasetKeys.setTableTitle("List of Dataset Keys consumed by PAGES");			
			allPageDatasetKeys.addGroupBy(m_DATASET_TYPE_COLNAME);
			allPageDatasetKeys.addGroupBy(m_PACKAGE_NAME_COLNAME);
			allPageDatasetKeys.addGroupBy(m_SCENARIO_NAME_COLNAME);
			allPageDatasetKeys.addGroupBy(m_ENTITYGROUP_NAME_COLNAME);
			allPageDatasetKeys.groupBy();
			
			return allPageDatasetKeys;
		}
		
		private Table GetColsandFiltersForPages(Table rawDataPageComponents) throws OException
		{
			Table filtersPerPage = Table.tableNew();
			String what  = "DISTINCT, " + m_PAGE_NAME + "," + m_COL_NUM + "," + m_ROW_NUM + ", " + m_TYPE + "," + m_PACKAGE_NAME + "," + m_NAME + "," + m_VALUE;    
			String where = m_TYPE + " EQ " + m_PAGEFILTER_TYPE; 
			filtersPerPage.select(rawDataPageComponents, what, where);  
			where = m_TYPE + " EQ " + m_FILTER_TYPE; 
			filtersPerPage.select(rawDataPageComponents, what, where);  
			where = m_TYPE + " EQ " + m_DATACOLUMN_TYPE; 
			filtersPerPage.select(rawDataPageComponents, what, where);  
			return filtersPerPage;
		}
		
        private Table GetKeyValuesForPages(Table filtersPerPage) throws OException
        {
	        // filter for the keys only
	        Table keyValuesPerPage = Table.tableNew();
	        keyValuesPerPage.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
	        keyValuesPerPage.addCol(m_COL_NUM, COL_TYPE_ENUM.COL_INT);
	        keyValuesPerPage.addCol(m_ROW_NUM, COL_TYPE_ENUM.COL_INT);
	        keyValuesPerPage.addCol(m_TYPE, COL_TYPE_ENUM.COL_STRING);
	        keyValuesPerPage.addCol(m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
	        keyValuesPerPage.addCol(m_NAME, COL_TYPE_ENUM.COL_STRING);
	        keyValuesPerPage.addCol(m_VALUE, COL_TYPE_ENUM.COL_STRING);
	        
	        keyValuesPerPage.fillSetSourceTable(filtersPerPage);
	        keyValuesPerPage.fillAddData(m_PAGE_NAME, m_PAGE_NAME);
	        keyValuesPerPage.fillAddData(m_COL_NUM, m_COL_NUM);
	        keyValuesPerPage.fillAddData(m_ROW_NUM, m_ROW_NUM);
	        keyValuesPerPage.fillAddData(m_TYPE, m_TYPE);
	        keyValuesPerPage.fillAddData(m_PACKAGE_NAME, m_PACKAGE_NAME);
	        keyValuesPerPage.fillAddData(m_NAME, m_NAME);
	        keyValuesPerPage.fillAddData(m_VALUE, m_VALUE);
	
	        // dataset type
	        keyValuesPerPage.fillAddMatchString(m_DATASET_TYPE_FILTER, m_NAME, MATCH_CMP_ENUM.MATCH_EQ);
	        keyValuesPerPage.fillDistinct();                                                
	
	        // portfolio (entity group key for deal services)
	        keyValuesPerPage.fillClearMatch();
	        keyValuesPerPage.fillAddMatchString(m_PORTFOLIO_FILTER, m_NAME, MATCH_CMP_ENUM.MATCH_EQ);
	        keyValuesPerPage.fillDistinct();                                                

	        // pipeline (entity group key for nom services)
	        keyValuesPerPage.fillClearMatch();
	        keyValuesPerPage.fillAddMatchString(m_NAGAS_SERVICE_PROVIDER_FILTER, m_NAME, MATCH_CMP_ENUM.MATCH_EQ);
	        keyValuesPerPage.fillDistinct();                                                
			
	        // scenario
	        keyValuesPerPage.fillClearMatch();
	        keyValuesPerPage.fillAddMatchString(m_SCENARIO_FILTER, m_NAME, MATCH_CMP_ENUM.MATCH_EQ);
	        keyValuesPerPage.fillDistinct();                                                
	
	        // now add the columns (they may be no keys specified for the columns - in which case we have to default
	        keyValuesPerPage.fillClearMatch();
	        keyValuesPerPage.fillAddMatchString(m_DATACOLUMN_TYPE, m_TYPE, MATCH_CMP_ENUM.MATCH_EQ);
	        keyValuesPerPage.fillDistinct();                                                
	        
	        // now sort correctly so we can loop to extract values
	        keyValuesPerPage.clearGroupBy();
	        keyValuesPerPage.addGroupBy(m_PAGE_NAME);
	        keyValuesPerPage.addGroupBy(m_COL_NUM);
	        keyValuesPerPage.addGroupBy(m_ROW_NUM);
	        keyValuesPerPage.addGroupBy(m_TYPE);
	        keyValuesPerPage.addGroupBy(m_PACKAGE_NAME);
	        keyValuesPerPage.addGroupBy(m_NAME);
	        keyValuesPerPage.groupBy();
	
	        return keyValuesPerPage;
        }
		
		private Table TransposeKeyValuesForPages(Table keyValuesPerPage) throws OException
		{
			// now transpose the values so we have 1 row per cell in the page
			Table transposedKeyValuesPerPage = Table.tableNew();
			transposedKeyValuesPerPage.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			transposedKeyValuesPerPage.addCol(m_COL_NUM, COL_TYPE_ENUM.COL_INT);
			transposedKeyValuesPerPage.addCol(m_ROW_NUM, COL_TYPE_ENUM.COL_INT);
			transposedKeyValuesPerPage.addCol(m_PACKAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			transposedKeyValuesPerPage.addCol("dataset_type_values", COL_TYPE_ENUM.COL_STRING);
			transposedKeyValuesPerPage.addCol("entity_group_name_values", COL_TYPE_ENUM.COL_STRING);
			transposedKeyValuesPerPage.addCol("scenario_name_values", COL_TYPE_ENUM.COL_STRING);
			
			String currentPageName = "";
			int currentColNum = -1;
			int currentRowNum = -1;
			int transposedRow = -1;			
			int numRows = keyValuesPerPage.getNumRows();
			for ( int loopCtr = 1; loopCtr <= numRows; loopCtr++)
			{
				String pageName = getStringSafe(keyValuesPerPage, m_PAGE_NAME, loopCtr);
				int rowNum = keyValuesPerPage.getInt(m_ROW_NUM, loopCtr);
				int colNum = keyValuesPerPage.getInt(m_COL_NUM, loopCtr);
				
				if ( !pageName.equals(currentPageName) || rowNum != currentRowNum || colNum != currentColNum )
				{
					// add a row to the expanded table
					transposedRow = transposedKeyValuesPerPage.addRow();
					transposedKeyValuesPerPage.setString(m_PAGE_NAME, transposedRow, pageName);
					transposedKeyValuesPerPage.setInt(m_COL_NUM, transposedRow, colNum);
					transposedKeyValuesPerPage.setInt(m_ROW_NUM, transposedRow, rowNum);
					transposedKeyValuesPerPage.setString(m_PACKAGE_NAME, transposedRow, getStringSafe(keyValuesPerPage, m_PACKAGE_NAME, loopCtr));
					currentPageName = pageName;
					currentRowNum = rowNum;
					currentColNum = colNum;
				}
				
				String value = getStringSafe(keyValuesPerPage, m_VALUE, loopCtr);
				
				String type = getStringSafe(keyValuesPerPage, m_TYPE, loopCtr);
				if ( type.equals(m_DATACOLUMN_TYPE) )
					continue;
				
				// make sure page level does not overwrite column/row level
				String name = getStringSafe(keyValuesPerPage, m_NAME, loopCtr);
				if (name.equals(m_DATASET_TYPE_FILTER) && value != null && value.length() > 0 )
				{
					String values = getStringSafe(transposedKeyValuesPerPage, "dataset_type_values", transposedRow);
					if ( values == null || (values.length() > 0 && type.equals(m_FILTER_TYPE)) || values.isEmpty())
						transposedKeyValuesPerPage.setString("dataset_type_values", transposedRow, value);
				}
				else if (name.equals(m_PORTFOLIO_FILTER) && value != null && value.length() > 0 )
				{
					String values = getStringSafe(transposedKeyValuesPerPage, "entity_group_name_values", transposedRow);
					if ( values == null || (values.length() > 0 && type.equals(m_FILTER_TYPE)) || values.isEmpty())
						transposedKeyValuesPerPage.setString("entity_group_name_values", transposedRow, value);					
				}
				else if (name.equals(m_NAGAS_SERVICE_PROVIDER_FILTER) && value != null && value.length() > 0 )
				{
					String values = getStringSafe(transposedKeyValuesPerPage, "entity_group_name_values", transposedRow);
					if ( values == null || (values.length() > 0 && type.equals(m_FILTER_TYPE)) || values.isEmpty())
						transposedKeyValuesPerPage.setString("entity_group_name_values", transposedRow, value);					
				}
				else if (name.equals(m_SCENARIO_FILTER) && value != null && value.length() > 0 )
				{
					String values = getStringSafe(transposedKeyValuesPerPage, "entity_group_name_values", transposedRow);
					if ( values == null || (values.length() > 0 && type.equals(m_FILTER_TYPE)) || values.isEmpty())
						transposedKeyValuesPerPage.setString("scenario_name_values", transposedRow, value);										
				}				
			}

			return transposedKeyValuesPerPage;
		}
		
		private Table ExpandKeyValuesPerPage(Table transposedKeyValuesPerPage)  throws OException
		{
			// now expand each row into individual keys
			Table expandedKeyValuesPerPage = Table.tableNew();
			expandedKeyValuesPerPage.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			expandedKeyValuesPerPage.addCol(m_COL_NUM, COL_TYPE_ENUM.COL_INT);
			expandedKeyValuesPerPage.addCol(m_ROW_NUM, COL_TYPE_ENUM.COL_INT);
			expandedKeyValuesPerPage.addCol(m_DATASET_TYPE_COLNAME, COL_TYPE_ENUM.COL_STRING);
			expandedKeyValuesPerPage.addCol(m_PACKAGE_NAME_COLNAME, COL_TYPE_ENUM.COL_STRING);
			expandedKeyValuesPerPage.addCol(m_ENTITYGROUP_NAME_COLNAME, COL_TYPE_ENUM.COL_STRING);
			expandedKeyValuesPerPage.addCol(m_SCENARIO_NAME_COLNAME, COL_TYPE_ENUM.COL_STRING);
			expandedKeyValuesPerPage.addCol(m_DATASET_TYPE_ID_COLNAME, COL_TYPE_ENUM.COL_INT);
			expandedKeyValuesPerPage.addCol(m_ENTITYGROUP_ID_COLNAME, COL_TYPE_ENUM.COL_INT);
			expandedKeyValuesPerPage.addCol(m_SCENARIO_ID_COLNAME, COL_TYPE_ENUM.COL_INT);
			
			// to expand this we need to loop around each of the key values and add rows
			int expandedRow;
			int numRows = transposedKeyValuesPerPage.getNumRows();
			for ( int loopCtr = 1; loopCtr <= numRows; loopCtr++)
			{
				String pageName = getStringSafe(transposedKeyValuesPerPage, m_PAGE_NAME, loopCtr);
				int rowNum = transposedKeyValuesPerPage.getInt(m_ROW_NUM, loopCtr);
				int colNum = transposedKeyValuesPerPage.getInt(m_COL_NUM, loopCtr);

				ArrayList<String> datasetTypeValues = new ArrayList<String>();
				ArrayList<Integer> datasetTypeIds = new ArrayList<Integer>();
				ParseValues(getStringSafe(transposedKeyValuesPerPage, "dataset_type_values", loopCtr), datasetTypeValues, datasetTypeIds);
				if ( datasetTypeValues.size() == 0)
				{
					datasetTypeValues.add("Default");
					datasetTypeIds.add(0);
				}
				// loop around values in dataset type
				for ( int datasetCtr = 0; datasetCtr < datasetTypeValues.size(); datasetCtr++)
				{
					ArrayList<String> entityGroupValues = new ArrayList<String>();
					ArrayList<Integer> entityGroupIds = new ArrayList<Integer>();
					ParseValues(getStringSafe(transposedKeyValuesPerPage, "entity_group_name_values", loopCtr), entityGroupValues, entityGroupIds);
					if ( entityGroupValues.size() == 0)
					{
						entityGroupValues.add("ALL");
						entityGroupIds.add(0);
					}
					
					// loop around values in entityGroups
					for ( int entityGroupCtr = 0; entityGroupCtr < entityGroupValues.size(); entityGroupCtr++)
					{
						ArrayList<String> scenarioValues = new ArrayList<String>();
						ArrayList<Integer> scenarioIds = new ArrayList<Integer>();
						ParseValues(getStringSafe(transposedKeyValuesPerPage, "scenario_name_values", loopCtr), scenarioValues, scenarioIds);
						if ( scenarioValues.size() == 0)
						{
							scenarioValues.add("Base");
							scenarioIds.add(1);
						}

						// loop around scenarios
						for ( int scenarioCtr = 0; scenarioCtr < scenarioValues.size(); scenarioCtr++)
						{
							// add a row to the expanded table
							expandedRow = expandedKeyValuesPerPage.addRow();
							expandedKeyValuesPerPage.setString(m_PAGE_NAME, expandedRow, pageName);
							expandedKeyValuesPerPage.setInt(m_COL_NUM, expandedRow, colNum);
							expandedKeyValuesPerPage.setInt(m_ROW_NUM, expandedRow, rowNum);
							expandedKeyValuesPerPage.setString(m_PACKAGE_NAME_COLNAME, expandedRow, getStringSafe(transposedKeyValuesPerPage, m_PACKAGE_NAME, loopCtr));
							expandedKeyValuesPerPage.setString(m_DATASET_TYPE_COLNAME, expandedRow, datasetTypeValues.get(datasetCtr));
							expandedKeyValuesPerPage.setString(m_ENTITYGROUP_NAME_COLNAME, expandedRow, entityGroupValues.get(entityGroupCtr));
							expandedKeyValuesPerPage.setString(m_SCENARIO_NAME_COLNAME, expandedRow, scenarioValues.get(scenarioCtr));
							expandedKeyValuesPerPage.setInt(m_DATASET_TYPE_ID_COLNAME, expandedRow, datasetTypeIds.get(datasetCtr));
							expandedKeyValuesPerPage.setInt(m_ENTITYGROUP_ID_COLNAME, expandedRow, entityGroupIds.get(entityGroupCtr));
							expandedKeyValuesPerPage.setInt(m_SCENARIO_ID_COLNAME, expandedRow, scenarioIds.get(scenarioCtr));
						}
					}
				}
			}
			
			expandedKeyValuesPerPage.setTableTitle("Dataset Keys By Page");
			
			return expandedKeyValuesPerPage;
		}
		
		private Table GetDatasetKeysPerPage(Table rawDataPageComponents) throws OException
		{
			APM_Print("GetDatasetKeysPerPage");
//			rawDataPageComponents.viewTable();
			
			Table filtersPerPage = GetColsandFiltersForPages(rawDataPageComponents);
			Table keyValuesPerPage = GetKeyValuesForPages(filtersPerPage);
//			filtersPerPage.viewTable();
//			keyValuesPerPage.viewTable();
			
			Table transposedKeyValuesPerPage = TransposeKeyValuesForPages(keyValuesPerPage);
//			transposedKeyValuesPerPage.viewTable();
			
			Table expandedKeyValuesPerPage = ExpandKeyValuesPerPage(transposedKeyValuesPerPage);
//			expandedKeyValuesPerPage.viewTable();
			
			filtersPerPage.destroy();
			keyValuesPerPage.destroy();
			transposedKeyValuesPerPage.destroy();
			return expandedKeyValuesPerPage;			
		}
		
		private void ParseValues(String pipeDelimitedValues, ArrayList<String> values, ArrayList<Integer> ids) throws OException
		{
			if ( pipeDelimitedValues == null || pipeDelimitedValues.length() == 0)
				return;
			
			String[] splits = pipeDelimitedValues.split("\\|");
			
			int numberOfElements = splits.length / 2;
			
			if ( numberOfElements > 0 )
			{
				for ( int loopCtr = 0; loopCtr < splits.length; loopCtr++)
				{
					if ( loopCtr % 2 == 0 )
						ids.add(Integer.parseInt(splits[loopCtr]));
					else
						values.add(splits[loopCtr]);
				}
			}
		}

		public Table getDatasetKeysForAllAPMServices() throws OException
		{
			APM_Print("getDatasetKeysForAllAPMServices");
			
		   Table serviceList = GetListOfAllAPMServices();
		   Table allDatasetTypes = GetListOfAllDatasetTypes();
		   Table allScenarios = GetListOfAllScenarios();
	
		   Table services_table = Table.tableNew();
	
		   for ( int row = 1; row <= serviceList.getNumRows(); row++)
		   {
		      String serviceName = getStringSafe(serviceList, "name", row);
		      
		      Table pfield_tbl = null;
		      pfield_tbl = Services.getServiceMethodProperties(serviceName, "ApmService");
		      if ( pfield_tbl == null) // must be a nom service
		         pfield_tbl = Services.getServiceMethodProperties(serviceName, "ApmNomService");
	
		      // could be there under new name or old name (not both)
		      Table entityGroupTable = Util.NULL_TABLE;
		      if (pfield_tbl.unsortedFindString("pfield_name", "portfolio_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
		    	  entityGroupTable = Services.getMselPicklistTable( pfield_tbl, "portfolio_id"); 
		      else if (pfield_tbl.unsortedFindString("pfield_name", "internal_portfolio", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
		    	  entityGroupTable = Services.getMselPicklistTable( pfield_tbl, "internal_portfolio"); 
		      else if (pfield_tbl.unsortedFindString("pfield_name", "pipeline_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
		    	  entityGroupTable = Services.getMselPicklistTable( pfield_tbl, "pipeline_id"); 
 
		      if (entityGroupTable == null)
		      {
                  throw new OException("Service " + serviceName + " has no entity groups (portfolios/service providers) selected.");
		      }
		      
		      Table entityGroupList = entityGroupTable.copyTable();
		      
		      // get simulation name - checking field type as can differ between endur cuts
		      int simulationRow = pfield_tbl.unsortedFindString("pfield_name","simulation_name", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		      int simulationNameType = pfield_tbl.getInt("pfield_type", simulationRow);
	
		      String simulationName;
		      if (simulationNameType == 1005 ) // PFIELD_TYPE_REF_TABLE_SSEL_UNIQUE
		    	  simulationName = Services.getPicklistString(pfield_tbl, "simulation_name");
		      else
		    	  simulationName = Services.getStringValue(pfield_tbl, "simulation_name");
	
		      // For old Endur cuts, the dataset type field is not present in the service properties
		      // Check before retrieving, else the console gets an ugly error
		      String datasetType = "";
		      if (pfield_tbl.unsortedFindString("pfield_name", "dataset_type_id", SEARCH_CASE_ENUM.CASE_SENSITIVE) > 0)
		    	  datasetType = Services.getPicklistString(pfield_tbl, "dataset_type_id");
	
		      entityGroupList.addCol( "service_name", COL_TYPE_ENUM.COL_STRING);
		      entityGroupList.addCol( "sim_name", COL_TYPE_ENUM.COL_STRING);
		      entityGroupList.addCol( "package", COL_TYPE_ENUM.COL_STRING);
		      entityGroupList.addCol( "dataset_type_id", COL_TYPE_ENUM.COL_INT);
		      entityGroupList.addCol( "dataset_type_name", COL_TYPE_ENUM.COL_STRING);
		      entityGroupList.addCol( "scenario_id", COL_TYPE_ENUM.COL_INT);
		      entityGroupList.addCol( "scenario_name", COL_TYPE_ENUM.COL_STRING);
	
		      // set the script name, if we're got a package in the service properties look up from there, 
		      if (pfield_tbl.unsortedFindString("pfield_name", "package_name", SEARCH_CASE_ENUM.CASE_SENSITIVE) >0)
		      {
		    	  int packageRow = pfield_tbl.unsortedFindString( "pfield_name","package_name", SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		    	  int packageNameType = pfield_tbl.getInt("pfield_type",packageRow);
	
			     if (packageNameType == 1008 ) // PFIELD_TYPE_USER_MSEL_UNIQUE
			     {
			        Table package_name_tbl = Services.getMselPicklistTable( pfield_tbl, "package_name"); 
			        // The 'where' condition is there to force a proper matrix multiplication of all 
			        // entity groups against all packages - at this point, scenario is zero (not set yet)
			        // and package ID is non-negative
			        entityGroupList.select(package_name_tbl, "value(package)", "id GE $scenario_id");
			     }
			     else
			     {
			        String package_name = Services.getPicklistString( pfield_tbl, "package_name"); 
			        entityGroupList.setColValString( "package", package_name);
			     }
		      }
	
		      /* set the defn name */
		      entityGroupList.setColValString( "service_name", serviceName);
	
		      /* set the simulation name */
		      entityGroupList.setColValString( "sim_name", simulationName);
	
		      // Retrieve the scenario names from the simulation, if it exists
		      Table simDetails;
		      Table scenarioDef;
		      if ( !simulationName.equals("None") && ((simDetails = Sim.loadSimulation(simulationName)) != Util.NULL_TABLE) &&
		      	   ((scenarioDef = simDetails.getTable( "scenario_def", 1)) != Util.NULL_TABLE) )
		      {
			     // The 'where' condition will always apply, since scenario is initialized to zero 
			     // within the entity groups table - this gives us a multiplication of tables
		    	  entityGroupList.select(scenarioDef, "scenario_name", "scenario_id GT $scenario_id");
	
			     // Scenario ID comes from apm_scenario_list, not the simulation definition
		    	  entityGroupList.select(allScenarios, "scenario_id", "scenario_name EQ $scenario_name");
	
			     simDetails.destroy();
		      }
		      else
		      {
		    	  // If there is no simulation defined, set the values to "Base" scenario
		    	  entityGroupList.setColValInt("scenario_id", 1);
		    	  entityGroupList.setColValString( "scenario_name", "Base");
		      }
	
		      // Set dataset type and label
		      int datasetNameCol = allDatasetTypes.getColNum( "apm_dataset_name");
		      if (datasetNameCol < 1)
		    	  datasetNameCol = allDatasetTypes.getColNum("name");
		      
		      int datasetIdCol = allDatasetTypes.getColNum("apm_dataset_id");
		      if (datasetIdCol < 1 )
		    	  datasetIdCol = allDatasetTypes.getColNum( "dataset_type_id");
	
		      int datasetTypeRow = allDatasetTypes.unsortedFindString(datasetNameCol, datasetType, SEARCH_CASE_ENUM.CASE_INSENSITIVE);
		      if (datasetTypeRow > 0)
		      {
		    	  entityGroupList.setColValInt( "dataset_type_id", allDatasetTypes.getInt(datasetIdCol, datasetTypeRow));
		    	  entityGroupList.setColValString( "dataset_type_name", datasetType );
		      }
		      else
		      {
		    	  entityGroupList.setColValInt( "dataset_type_id", 0);
		    	  entityGroupList.setColValString( "dataset_type_name", "Default");
		      }
	
		      /* copy from the entityGroupList into the services table */
		      services_table.select( entityGroupList, "service_name, dataset_type_name, package, value(entity_group_name), scenario_name, dataset_type_id, id(entity_group_id), scenario_id", "dataset_type_id GT -1");
	
		      pfield_tbl.destroy();
		      entityGroupList.destroy();	
		   }
			   
		   allScenarios.destroy();    
		   allDatasetTypes.destroy();    
			
		   services_table.setTableTitle("DSKs generated");
		   services_table.addGroupBy("service_name");
		   services_table.addGroupBy("dataset_type_id");
		   services_table.addGroupBy("entity_group_name");
		   services_table.addGroupBy("package");
		   services_table.addGroupBy("scenario_id");
		   services_table.groupBy();
		   
		   return services_table;
		}
		
		private Table GetListOfAllAPMServices() throws OException
		{
			Table APMServices = Table.tableNew();
			
			String what  = "name";    
			String from  = "job_cfg";    
			String where = "service_group_type in (33,46) and type = 0"; 
			DBaseTable.loadFromDbWithSQL(APMServices, what, from, where);  

			return APMServices;
		}

		private Table GetListOfAllDatasetTypes() throws OException
		{
			Table datasetTypes = Table.tableNew();
			
			String what  = "*";    
			String from  = "apm_dataset_type";    
			String where = "1 = 1"; 
			DBaseTable.loadFromDbWithSQL(datasetTypes, what, from, where);  

			return datasetTypes;
		}

		private Table GetListOfAllScenarios() throws OException
		{
			Table scenarios = Table.tableNew();
			
			String what  = "*";    
			String from  = "apm_scenario_list";    
			String where = "1 = 1"; 
			DBaseTable.loadFromDbWithSQL(scenarios, what, from, where);  

			return scenarios;
		}
				
	}
	
	class UserStatistics
	{
		static final String m_USERNAME = "username";
		static final String m_PAGE_NAME = "pageName";
		static final String m_LAST_ONLINE = "pageLastOnline";
		static final String m_LAST_ONLINE_DATE_TIME = "last_update";

		public Table generate(int activeInLastDays) throws OException
		{
			APM_Print("GenerateUserPageStatistics");

			ODateTime oldestActiveUserDate = oldestActiveUserDate(activeInLastDays); 
			Table userPageHistory = generateUserPageHistory(oldestActiveUserDate);
			
			Table userReport = generateUserReport(oldestActiveUserDate, userPageHistory);
			
			oldestActiveUserDate.destroy();
			return userReport;			
		}

		private Table generateUserPageHistory(ODateTime oldestActiveUserDate) throws OException
		{
			final String USER_PAGE_DETAILS = "filename";

			Table tfeFileSystem = getCompleteTable("tfe_file_system");
			Table personnel = getCompleteTable("personnel");

			Table userPageHistory = Table.tableNew();
			userPageHistory.setTableTitle("User page full history");
			userPageHistory.addCol(m_USERNAME, COL_TYPE_ENUM.COL_STRING);
			userPageHistory.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);
			userPageHistory.addCol(m_LAST_ONLINE, COL_TYPE_ENUM.COL_STRING);
			
			int numUserPages = tfeFileSystem.getNumRows();
			for (int userRow = 1; userRow <= numUserPages; userRow++)
			{
				ODateTime lastOnline = getLastOnlineDateTime(tfeFileSystem, userRow); 
				
				if (lastOnline.getDate() >= oldestActiveUserDate.getDate())
				{
					String userPageDetails = getStringSafe(tfeFileSystem, USER_PAGE_DETAILS, userRow);
					parseUserPageDetails(tfeFileSystem, personnel, userPageDetails, userPageHistory, userRow);
					
				}
				lastOnline.destroy();
			}

			return userPageHistory;
		}
		
		private Table getCompleteTable(String tableName) throws OException
		{
			final String ALL_COLUMNS = "*";
			final String GET_ALL_ROWS = "1 = 1";
			
			Table userDetails = Table.tableNew();
			
			String what  = ALL_COLUMNS;    
			String from  = tableName;    
			String where = GET_ALL_ROWS; 
			DBaseTable.loadFromDbWithSQL(userDetails, what, from, where);  

			return userDetails;
		}

		private ODateTime oldestActiveUserDate(int nubmerOfDaysActivity) throws OException
		{
			ODateTime todayInMarketManager = ODateTime.dtNew();
			todayInMarketManager.setDate(OCalendar.today());
			
			ODateTime oldestActiveDate = ODateTime.dtNew(); 
			int hoursToSubtract = 0 - (nubmerOfDaysActivity * 24);
			todayInMarketManager.addHoursToDateTime(oldestActiveDate, hoursToSubtract);

			todayInMarketManager.destroy();

			return oldestActiveDate; 
		}
	
		private ODateTime getLastOnlineDateTime(Table tfeFileSystem, int rowNumber) throws OException
		{
			int rowJulienDate = tfeFileSystem.getInt(m_LAST_ONLINE_DATE_TIME, rowNumber);
			ODateTime lastOnline = ODateTime.dtNew();
			lastOnline.setDate(rowJulienDate);
			
			return lastOnline;
		}

		private void parseUserPageDetails(Table tfeFileSystem, Table personnel, String userPageDetails, Table userPageHistory, int userPageHistoryRowNumber) throws OException
		{
			final String APM_V2_SEPARATOR = "::";
			
			int apmV2SeparatorPosition = userPageDetails.indexOf(APM_V2_SEPARATOR);
			if (apmV2SeparatorPosition > 0)
			{
				String userNameAndPage = userPageDetails.substring(apmV2SeparatorPosition + APM_V2_SEPARATOR.length());

				String userName = validUserName(personnel, userNameAndPage);
				int userNameLength = userName.length();

				if (userNameLength > 0)
				{
					String pageName = userNameAndPage.substring(userNameLength + 1);
					
					int newRow = userPageHistory.addRow();
					userPageHistory.setString(m_USERNAME, newRow, userName);
					userPageHistory.setString(m_PAGE_NAME, newRow, pageName);
					int lastUpdate = tfeFileSystem.getInt(m_LAST_ONLINE_DATE_TIME, userPageHistoryRowNumber);
					userPageHistory.setString(m_LAST_ONLINE, newRow, OCalendar.formatJdForDbAccess(lastUpdate));
				}
			}
		}

		private String validUserName(Table personnel, String userNameAndPage) throws OException
		{
			final String PERSONNEL_NAME = "name";

			String validUsername = "";
			
			int numPersonnel = personnel.getNumRows();
			for (int personnelRow = 1; personnelRow <= numPersonnel && validUsername.length() == 0; personnelRow++)
			{
				String userName = getStringSafe(personnel, PERSONNEL_NAME, personnelRow);
				
				int personnelNameLength = userName.length();

                if (personnelNameLength <= userNameAndPage.length())
                {
                    String userNameAndPagePrefix = userNameAndPage.substring(0, personnelNameLength); 

      				if (userNameAndPagePrefix.equals(userName))
    				{
    					validUsername = userName;
    				}
                }
			}
			
			return validUsername;
		}
				
		private int countOfDistinctUsers(Table userPageHistory) throws OException
		{
			Table users = distinctUsers(userPageHistory);
			int userCount = users.getNumRows();
			users.destroy();
			
			return userCount;
		}

		private Table distinctUsers(Table userPageHistory) throws OException
		{
			final String BLANK_NAME = "";
			
			Table distinctUsers = Table.tableNew();
			distinctUsers.addCol(m_USERNAME, COL_TYPE_ENUM.COL_STRING);
			distinctUsers.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);

			distinctUsers.fillSetSourceTable(userPageHistory);
			distinctUsers.fillAddData(m_USERNAME, m_USERNAME);
			//The following line will return all user names.
			distinctUsers.fillAddMatchString(BLANK_NAME, m_USERNAME, MATCH_CMP_ENUM.MATCH_NE);
			distinctUsers.fillDistinct();

			return distinctUsers;
		}
		
		private Table generateUserReport(ODateTime oldestActiveUserDate, Table userPageHistory) throws OException
		{
			final String NUMBER_OF_USERS = "numberOfUsers";
			final String PAGE_COUNT_BY_USER = "pageCountByUser";
			final String FULL_PAGE_HISTORY = "fullPageHistory";
			final int TIME_FORMAT_LENGTH = 8;
			
			Table userStatistics = Table.tableNew();
			userStatistics.setTableTitle("User statistics");
			userStatistics.addCol(NUMBER_OF_USERS, COL_TYPE_ENUM.COL_INT);
			userStatistics.addCol(PAGE_COUNT_BY_USER, COL_TYPE_ENUM.COL_TABLE);
			userStatistics.addCol(FULL_PAGE_HISTORY, COL_TYPE_ENUM.COL_TABLE);
			
			String numberOfUsersTitle = oldestActiveUserDate.formatForDbAccess();
			numberOfUsersTitle = numberOfUsersTitle.substring(0, numberOfUsersTitle.length() - TIME_FORMAT_LENGTH);
			userStatistics.setColTitle(NUMBER_OF_USERS, "Active Users since: " + numberOfUsersTitle);

			Table howManyPagesByUser = pagesByUser(userPageHistory);
			int numberOfUsers = countOfDistinctUsers(userPageHistory);
			
			int newRow = userStatistics.addRow();
			userStatistics.setInt(NUMBER_OF_USERS, newRow, numberOfUsers);
			userStatistics.setTable(PAGE_COUNT_BY_USER, newRow, howManyPagesByUser);
			userStatistics.setTable(FULL_PAGE_HISTORY, newRow, userPageHistory);
			
			return userStatistics;
		}
		
		private Table pagesByUser(Table userPageHistory) throws OException
		{
			final String PAGE_COUNT = "pageCount";
			final String PAGE_NAMES = "pageNames";
			
			Table pagesByUser = Table.tableNew();
			pagesByUser.setTableTitle("Pages by user");
			pagesByUser.addCol(m_USERNAME, COL_TYPE_ENUM.COL_STRING);
			pagesByUser.addCol(PAGE_COUNT, COL_TYPE_ENUM.COL_INT);
			pagesByUser.addCol(PAGE_NAMES, COL_TYPE_ENUM.COL_TABLE);

			Table distinctUsers = distinctUsers(userPageHistory);

			int numUsers = distinctUsers.getNumRows();
			for (int userRow = 1; userRow <= numUsers; userRow++)
			{
				String username = getStringSafe(distinctUsers, m_USERNAME, userRow);
				
				Table userPages = Table.tableNew();
				userPages.setTableTitle("Pages for user");
				userPages.addCol(m_USERNAME, COL_TYPE_ENUM.COL_STRING);
				userPages.addCol(m_PAGE_NAME, COL_TYPE_ENUM.COL_STRING);

				userPages.fillSetSourceTable(userPageHistory);
				userPages.fillAddData(m_USERNAME, m_USERNAME);
				userPages.fillAddData(m_PAGE_NAME, m_PAGE_NAME);
				userPages.fillAddMatchString(username, m_USERNAME, MATCH_CMP_ENUM.MATCH_EQ);
				userPages.fillDistinct();
				
				int numberOfPages = userPages.getNumRows();
				
				int newRow = pagesByUser.addRow();
				pagesByUser.setString(m_USERNAME, newRow, username);
				pagesByUser.setInt(PAGE_COUNT, newRow, numberOfPages);
				pagesByUser.setTable(PAGE_NAMES, newRow, userPages);
			}
			
			return pagesByUser;
		}

	}
	
	private void APM_Print(String sProcessingMessage)
	{
		
		try
		{		
			String sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + sProcessingMessage + "\n";
			OConsole.oprint(sMsg);
		} 
		catch (OException e) {}
	}
	
	private String getStringSafe(Table valuesTable, String columnName, int rowNumber) throws OException
	{
		String value = valuesTable.getString(columnName, rowNumber);
		
		if (value == null)
		{
			value = "";
		}
		
		value = value.trim();
		
		return value;
	}

    private void PrintToExcel(Table output, String filename) throws OException
    {
          for (int iCol = 1; iCol<=output.getNumCols(); iCol++)
          {
                int colType = output.getColType(iCol);
                if (colType == COL_TYPE_ENUM.COL_TABLE.toInt())
                {
                      for (int iRow = 1; iRow<=output.getNumRows(); iRow++)
                      {
                            Table tbl = output.getTable(iCol, iRow);
                            PrintToExcel(tbl, filename);
                      }
                }
          }
          
          String worksheetTitle = output.getTableTitle();
          if (worksheetTitle == null)
                worksheetTitle = "";
          if (worksheetTitle.length() > 29)
                worksheetTitle = worksheetTitle.substring(0,27);
          
          worksheetTitle = worksheetTitle + "-" + sheetcounter++;
          
          APM_Print(worksheetTitle);
          String error = "";
          int ret = output.excelSave(filename, worksheetTitle, "A1", 0);
          try {
                Thread.sleep(500);
          } catch (InterruptedException e) {
                e.printStackTrace();
                error = e.getMessage();
          }
          if (ret == 0)
                throw new OException("failed to save Excel file: " + error);
          
          APM_Print(String.valueOf(ret));
    }
	
}

