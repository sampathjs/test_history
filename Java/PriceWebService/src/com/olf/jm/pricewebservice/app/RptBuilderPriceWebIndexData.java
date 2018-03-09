package com.olf.jm.pricewebservice.app;

import com.olf.jm.pricewebservice.persistence.DBHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.logging.PluginLog;
import com.openlink.util.misc.TableUtilities;

/**
 * ReportBuilder plugin to be used as DataSource.
 * This plugin contains a datasource that loads the closing prices for the JM base curve
 * based on 5 input parameters. There are two different output types: <br/> 
 * without pivot and with pivot. 
 * The report builder expects the following <b>mandatory</b> arguments:
 * <table>
 *   <tr>
 *     <th> Argument Name </th>
 *     <th> Argument Type </th>
 *     <th> Description </th>
 *   </tr>
 *   <tr>
 *     <td> IndexName </td>
 *     <td> String </td>
 *     <td> Name of the Index containing the prices to retrieve </td>
 *   </tr>
 *   <tr>
 *     <td> StartDate </td>
 *     <td> DateTime </td>
 *     <td> Start Date of the time span to retrieve data for </td>
 *   </tr>
 *   <tr>
 *     <td> EndDate </td>
 *     <td> DateTime </td>
 *     <td> End Date of the time span to retrieve data for </td>
 *   </tr>
 *   <tr>
 *     <td> DatasetType </td>
 *     <td> String </td>
 *     <td> Name of the dataset to retrieve data for e.g. JM London </td>
 *   </tr>
 *   <tr>
 *     <td> UsePivot </td>
 *     <td> String </td>
 *     <td> Either TRUE or FALSE. Controls output type </td>
 *   </tr>
 * </table> 
 * <br/>
 * The output columns <b>without</b> pivot are the following:
 * <table>
 *   <tr>
 *     <th> <b>Column Name</b> </th>
 *     <th> <b>Column Title</b> </th>
 *     <th> <b>Data Type</b> </th>
 *     <th> <b>Field Description</b> </th>
 *   </tr>
 *   <tr>
 *     <td> dataset_type_id </td>
 *     <td> Dataset Type </td>
 *     <td> Integer </td>
 *     <td> ID of the Dataset Type of the values retrieved </td>
 *   </tr>
 *   <tr>
 *     <td> datetime </td>
 *     <td> Date/Time </td>
 *     <td> Date/Time </td>
 *     <td> Date the market data are valid for </td>
 *   </tr>
 *   <tr>
 *     <td> group </td>
 *     <td> Group / GPT ID </td>
 *     <td> String </td>
 *     <td> Name of the Grid Point / Group </td>
 *   </tr>
 *   <tr>
 *     <td> index_id </td>
 *     <td> Index ID </td>
 *     <td> Integer </td>
 *     <td> Name of the Index </td>
 *   </tr>
 *   <tr>
 *     <td> refsource_id </td>
 *     <td> Refsource ID</td>
 *     <td> Integer </td>
 *     <td> ID of the reference source </td>
 *   </tr>
 *   <tr>
 *     <td> value </td>
 *     <td> Value </td>
 *     <td> Double </td>
 *     <td> Value of the Grid Point on date </td>
 *   </tr>
 * </table>
 * 
 * 
 * <br/>The columns <b>with</b> pivot: 
 * <table>
 *   <tr>
 *     <th> <b>Column Name</b> </th>
 *     <th> <b>Column Title</b> </th>
 *     <th> <b>Data Type</b> </th>
 *     <th> <b>Field Description</b> </th>
 *   </tr>
 *   <tr>
 *     <td> dataset_type_id </td>
 *     <td> Dataset Type </td>
 *     <td> Integer </td>
 *     <td> ID of the Dataset Type of the values retrieved </td>
 *   </tr>
 *   <tr>
 *     <td> datetime </td>
 *     <td> Date/Time </td>
 *     <td> Date/Time </td>
 *     <td> Date the market data are valid for </td>
 *   </tr>
 *   <tr>
 *     <td> index_id </td>
 *     <td> Index ID </td>
 *     <td> Integer </td>
 *     <td> Name of the Index </td>
 *   </tr>
 *   <tr>
 *     <td> refsource_id </td>
 *     <td> Refsource ID</td>
 *     <td> Integer </td>
 *     <td> ID of the reference source </td>
 *   </tr>
 *   <tr>
 *     <td> XIR </td>
 *     <td> XIR </td>
 *     <td> Double </td>
 *     <td> Price of Iridium / USD </td>
 *   </tr>
 *   <tr>
 *     <td> XOS </td>
 *     <td> XOS </td>
 *     <td> Double </td>
 *     <td> Price of Osmium / USD </td>
 *   </tr>
 *   <tr>
 *     <td> XPD </td>
 *     <td> XPD </td>
 *     <td> Double </td>
 *     <td> Price of Palladium / USD </td>
 *   </tr>
 *   <tr>
 *     <td> XPT </td>
 *     <td> XPT </td>
 *     <td> Double </td>
 *     <td> Price of Platinum / USD </td>
 *   </tr>
 *   <tr>
 *     <td> XRH </td>
 *     <td> XRH </td>
 *     <td> Double </td>
 *     <td> Price of Rhodium / USD </td>
 *   </tr>
 *   <tr>
 *     <td> XRU </td>
 *     <td> XRU </td>
 *     <td> Double </td>
 *     <td> Price of Ruthenium / USD </td>
 *   </tr>
 * </table>
 * 
 * @author jwaechter
 * @version 1.0
 */
public class RptBuilderPriceWebIndexData implements IScript{
	private boolean usePivot;
	private int startDate;
	private int endDate;
	private String indexName;
	private String dataType;

	@Override
	public void execute(IContainerContext context) throws OException {
		Table prices=null;
		Table pivot=null;
		try {
			init (context);
			Table returnt = context.getReturnTable();
			prices = DBHelper.retrievePrices(indexName, dataType, startDate, endDate);
			pivot = prices.pivot("group", "index_id,datetime", "value", " ");
			if (usePivot) {
				returnt.select(pivot, "*", "index_id GT 0");
			} else {
				returnt.select(prices, "*", "index_id GT 0");
			}
			returnt.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
			returnt.addCol("refsource_id", COL_TYPE_ENUM.COL_INT);
			int datasetId = Ref.getValue(SHM_USR_TABLES_ENUM.IDX_MARKET_DATA_TYPE_TABLE, dataType);
			int refSourceId = Ref.getValue(SHM_USR_TABLES_ENUM.REF_SOURCE_TABLE, dataType);
			returnt.setColValInt("dataset_type_id", datasetId);
			returnt.setColValInt("refsource_id", refSourceId);
		} catch (Throwable ex) {
			OConsole.oprint(ex.toString());
			PluginLog.error(ex.toString());
			throw ex;
		} finally {
			TableUtilities.destroy(prices);
			TableUtilities.destroy(pivot);
		}
	}

	private void init(IContainerContext context) throws OException {
		String abOutdir = Util.getEnv("AB_OUTDIR");
		ConstRepository constRepo = new ConstRepository(DBHelper.CONST_REPOSITORY_CONTEXT,
				DBHelper.CONST_REPOSITORY_SUBCONTEXT);
		String logLevel = constRepo.getStringValue("logLevel", "info"); 
		String logFile = constRepo.getStringValue("logFile", this.getClass().getSimpleName() + ".log");
		String logDir = constRepo.getStringValue("logDir", abOutdir);
		try {
			PluginLog.init(logLevel, logDir, logFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		Table argt = context.getArgumentsTable();
		if (argt.getNumRows() == 0 || argt.getColNum("PluginParameters") == 0) {
			throw new OException ("ARGT invalid: " + getClass().getName() + " requires to be run inside the ReportBuilder");
		}
		
		Table pluginParameters = argt.getTable("PluginParameters", 1);
		startDate = -1;
		endDate = -1;
		indexName = null;
		dataType = null;
		for (int row=pluginParameters.getNumRows(); row >= 1; row--) {
			String name = pluginParameters.getString("parameter_name", row).trim();
			String value = pluginParameters.getString("parameter_value", row).trim();
			switch (name) {
			case "StartDate":
				startDate = Integer.parseInt(value);
				break;
			case "EndDate":
				endDate = Integer.parseInt(value);
				break;
			case "IndexName":
				indexName = value;
				break;
			case "DatasetType":
				dataType = value;
				break;
			case "UsePivot":
				usePivot = Boolean.parseBoolean(value);
				break;
			}
		}
		if (startDate == -1) {
			throw new OException ("Parameter 'StartDate' is not provided. Setup 'StartDate' in ReportBuilder");
		}
		if (endDate == -1) {
			throw new OException ("Parameter 'EndDate' is not provided. Setup 'EndDate' in ReportBuilder");
		}
		if (indexName == null) {
			throw new OException ("Parameter 'IndexName' is not provided. Setup 'IndexName' in ReportBuilder");
		}
		if (dataType == null) {
			throw new OException ("Parameter 'DatasetType' is not provided. Setup 'DatesetType' in ReportBuilder");
		}
		
	}
}
