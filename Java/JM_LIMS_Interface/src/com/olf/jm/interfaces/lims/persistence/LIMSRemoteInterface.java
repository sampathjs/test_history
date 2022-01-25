package com.olf.jm.interfaces.lims.persistence;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Map;
import java.util.Set;

import com.olf.jm.interfaces.lims.model.ConfigurationItem;
import com.olf.jm.interfaces.lims.model.LIMSServer;
import com.olf.jm.interfaces.lims.model.MeasureDetails;
import com.olf.jm.interfaces.lims.model.MeasureSource;
import com.olf.jm.interfaces.lims.model.MeasurementUnits;
import com.olf.jm.interfaces.lims.model.MeasuresWithSource;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import com.olf.openrisk.trading.EnumPlannedMeasureFieldId;

/*
 * History:
 * 2015-09-01	V1.0	jwaechter	-	initial version
 * 2021-12-15	V2.0	Prashanth   -   Remove login to query from Lims Database instead fetch from user tables		
 */

/**
 * Class containing all functionality to perform the basic LIMS interface tasks done on remote systems.
 * @author jwaechter
 * @version 1.0
 */
public class LIMSRemoteInterface {
	private final Session session;
	private final LIMSServer server;

	public LIMSRemoteInterface (final Session session, final LIMSServer server) {
		this.session = session;
		this.server = server;
	}

	public Table loadSampleIDsFromLims (String batchId, Map<String, String> productsToTests) {
		Table sampleResult = getSampleData( batchId, productsToTests.keySet());
		if (sampleResult != null && sampleResult.getRowCount()!= 0) {
			sampleResult.addColumn("analysis", EnumColType.String);
			for (TableRow row : sampleResult.getRows()) {
				String product = row.getString ("product");
				String analysis = productsToTests.get(product);
				sampleResult.setString("analysis", row.getNumber(), analysis);
			}
			return sampleResult;
		}
		return session.getTableFactory().createTable("No Results");
	}

	private void guardedSetColNameAndTitle(final Table remoteSqlResult, final int colNum, final String name, final String title) {
		if (remoteSqlResult.getColumnCount() >= colNum+1) {
			remoteSqlResult.setColumnName(colNum, name);
			remoteSqlResult.getFormatter().setColumnTitle(colNum, name);
		}
	}
	
	public MeasuresWithSource loadPlannedMeasureDetailsFromLims (String sample, 
			String batchNum, String analysis, String purity, String brand) {
		
		Table resultTable = getResultData(sample, analysis);
		MeasuresWithSource mws = new MeasuresWithSource (batchNum, purity, brand);
		for(TableRow row : resultTable.getRows()){
			// one or two letters from chemical period table.
			String name = ((String)row.getValue(resultTable.getColumnId("name"))).toUpperCase() ;
			MeasurementUnits unit=MeasurementUnits.valueOf((String)row.getValue(resultTable.getColumnId("units")));
			String formattedEntry= (String)row.getValue(resultTable.getColumnId("formatted_entry"));
			boolean isNumber = isNumeric (formattedEntry);
			double value = (isNumber)?Double.parseDouble(formattedEntry):0.0d;

			MeasureDetails md =  new MeasureDetails(name, sample);
			md.addDetail(EnumPlannedMeasureFieldId.Unit, unit.getEndurName());

			switch (unit) {
			case PPM_WW:
			case PPMMETAL:
				md.addDetail(EnumPlannedMeasureFieldId.UpperValue, formattedEntry);
				md.addDetail(EnumPlannedMeasureFieldId.LowerValue, formattedEntry);
				break;
			case PERCENTWW:
				if (isNumber) {
					md.addDetail(EnumPlannedMeasureFieldId.UpperValue, Double.toString(value*100));
					md.addDetail(EnumPlannedMeasureFieldId.LowerValue, Double.toString(value*100));
				} else {
					md.addDetail(EnumPlannedMeasureFieldId.UpperValue, formattedEntry);
					md.addDetail(EnumPlannedMeasureFieldId.LowerValue, formattedEntry);
				}
				break;
			}
			mws.addMeasure(md, MeasureSource.LIMS);
		}
		return mws;
	}

	public boolean isNumeric(String str)
	{
		NumberFormat formatter = DecimalFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}

	private ConnectionExternal getConnectionExternalResult (LIMSServer server) {
		switch (server) {
		case LIMS_UK:
			return new ConnectionExternal(ConfigurationItem.AS400_RESULT_SERVER_NAME_UK.getValue(), 
					ConfigurationItem.AS400_RESULT_DB_NAME_UK.getValue(), 
					ConfigurationItem.AS400_RESULT_USER_UK.getValue(),
					ConfigurationItem.AS400_RESULT_PASSWORD_UK.getValue());
		case LIMS_US:
			return new ConnectionExternal(ConfigurationItem.AS400_RESULT_SERVER_NAME_US.getValue(), 
					ConfigurationItem.AS400_RESULT_DB_NAME_US.getValue(), 
					ConfigurationItem.AS400_RESULT_USER_US.getValue(),
					ConfigurationItem.AS400_RESULT_PASSWORD_US.getValue());			
		}
		throw new UnsupportedOperationException ("Mapping of config items to result server not implemented for server " + server);
	}

	private ConnectionExternal getConnectionExternalSample (LIMSServer server) {
		switch (server) {
		case LIMS_UK:
			return new ConnectionExternal(ConfigurationItem.AS400_SAMPLE_SERVER_NAME_UK.getValue(), 
					ConfigurationItem.AS400_SAMPLE_DB_NAME_UK.getValue(), 
					ConfigurationItem.AS400_SAMPLE_USER_UK.getValue(),
					ConfigurationItem.AS400_SAMPLE_PASSWORD_UK.getValue());
		case LIMS_US:
			return new ConnectionExternal(ConfigurationItem.AS400_SAMPLE_SERVER_NAME_US.getValue(), 
					ConfigurationItem.AS400_SAMPLE_DB_NAME_US.getValue(), 
					ConfigurationItem.AS400_SAMPLE_USER_US.getValue(),
					ConfigurationItem.AS400_SAMPLE_PASSWORD_US.getValue());
		}
		throw new UnsupportedOperationException ("Mapping of config items to sample server not implemented for server " + server);
	}

	private String getResultQuery(String sample, String analysis) {
		switch (server) {
		case LIMS_UK:
			return String.format(ConfigurationItem.AS400_RESULT_QUERY_UK.getValue(), sample, analysis);
		case LIMS_US:
			return String.format(ConfigurationItem.AS400_RESULT_QUERY_US.getValue(), sample, analysis);
		}
		throw new UnsupportedOperationException("Mapping of result query to server " + server + " not supported.");
	}

	private String getSampleQuery(String batchId, Set<String> products) {
		StringBuilder prods = new StringBuilder ();
		boolean first=true;
		for (String product : products) {
			if(!first) {
				prods.append(", ");
			}
			prods.append("'");
			prods.append(product);
			prods.append("'");
			first=false;
		}
		String rawQuery=null;

		switch (server) {
		case LIMS_UK:
			rawQuery = ConfigurationItem.AS400_SAMPLE_QUERY_UK.getValue();
			break;
		case LIMS_US:
			rawQuery = ConfigurationItem.AS400_SAMPLE_QUERY_US.getValue();
			break;
		}
		return String.format(rawQuery, batchId, prods.toString());
	}
	
	private Table getResultData(String sample, String analysis) {

		String sql = "Select * from USER_JM_LIMS_RESULT \nWHERE sample_number = '" + sample + "'  AND analysis = '" + analysis + "'";
		Table resultUT = session.getTableFactory().createTable();
		resultUT = session.getIOFactory().runSQL(sql);
		return resultUT;
	}

	private Table getSampleData(String batchId, Set<String> products) {

		StringBuilder prods = new StringBuilder();
		boolean first = true;
		for (String product : products) {
			if (!first) {
				prods.append(", ");
			}
			prods.append("'");
			prods.append(product);
			prods.append("'");
			first = false;
		}
		String sql = "Select * from USER_JM_LIMS_SAMPLES \nWHERE UPPER(jm_batch_id) LIKE ('" + batchId + "') AND product IN ("
				+ prods.toString() + ")";
		Table sampleUT = session.getTableFactory().createTable();
		sampleUT = session.getIOFactory().runSQL(sql);
		return sampleUT;
	}

}