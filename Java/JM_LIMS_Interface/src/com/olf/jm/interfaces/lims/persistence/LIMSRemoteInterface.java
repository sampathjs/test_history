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
 */

/**
 * Class containing all functionality to perform the basic LIMS interface tasks done on remote systems.
 * @author jwaechter
 * @version 1.0
 */
public class LIMSRemoteInterface {
	private final Session session;
	private final LIMSServer server;
	private final ConnectionExternal sampleServer;
	private final ConnectionExternal resultServer;

	public LIMSRemoteInterface (final Session session, final LIMSServer server) {
		this.session = session;
		this.server = server;
		this.sampleServer = getConnectionExternalSample(server);
		this.resultServer = getConnectionExternalResult(server);
	}

	public Table loadSampleIDsFromLims (String batchId, Map<String, String> productsToTests) {
		String finalSql = getSampleQuery(batchId, productsToTests.keySet());
		Object[][] sampleResult = sampleServer.query(finalSql);
		if (sampleResult != null && sampleResult.length != 0) {
			Table remoteSqlResult = session.getTableFactory().createTable(sampleResult);
			guardedSetColNameAndTitle(remoteSqlResult, 0, "SAMPLE_NUMBER", "Sample Number");
			guardedSetColNameAndTitle(remoteSqlResult, 1, "PRODUCT", "Product");
			guardedSetColNameAndTitle(remoteSqlResult, 2, "JM_BATCH_ID", "Batch ID"); 
			remoteSqlResult.addColumn("ANALYSIS", EnumColType.String);
			for (TableRow row : remoteSqlResult.getRows()) {
				String product = row.getString ("PRODUCT");
				String analysis = productsToTests.get(product);
				remoteSqlResult.setString("ANALYSIS", row.getNumber(), analysis);
			}
			return remoteSqlResult;
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
		String finalQuery = getResultQuery (sample, analysis);
		MeasuresWithSource mws = new MeasuresWithSource (batchNum, purity, brand);
		Object[][] rawMeasures=resultServer.query(finalQuery);
		for (Object[] rawMeasure : rawMeasures) {
			String name=((String)rawMeasure[0]).toUpperCase(); // one or two letters from chemical period table.

			MeasurementUnits unit=MeasurementUnits.valueOf((String)rawMeasure[1]);
			String formattedEntry= (String)rawMeasure[2];
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
}