/*
 * This script is specific to JM_GBL_Trading_Book APM page. 								   
 * 
 * History:
 * 2020-06-16	V1.0	-	Arjit  -	Initial Version
 * 
 **/

package com.matthey.apm.utilities;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.openlink.util.logging.PluginLog;

public class JMGBLTradingBookPage extends BasePage {
	
	@Override
	protected Table postSnapshotLogic() throws OException {
		Table tblCSVData = Util.NULL_TABLE;
		Table tblOutput = Util.NULL_TABLE;
		
		try {
			PluginLog.info("Applying post logic for page - " + getPageName());
			tblCSVData = Table.tableNew();
			tblCSVData.inputFromCSVFile(getCsvFile());

			tblOutput = initOutputTbl();
			populateOutputTbl(tblCSVData, tblOutput);
			PluginLog.info("Output table populated successfully for page - " + getPageName());

		} catch (OException oe) {
			PluginLog.error("Error in applying postSnapshot logic to input csv file, Message: " + oe.getMessage());
			throw oe;

		} finally {
			if (Table.isTableValid(tblCSVData) == 1) {
				tblCSVData.destroy();
			}
		}
		
		return tblOutput;
	}

	/**
	 * This method is used to populate output table (to be emailed to user) from the different maps generated.
	 * 
	 * @param tCSVData
	 * @param output
	 * @throws OException
	 */
	private void populateOutputTbl(Table tCSVData, Table output) throws OException {
		Map<String, String> mapPositions = retrieveColValues(PageConstants.COL_INDEX_POSITION, tCSVData);
		Map<String, String> mapPhyPositions = retrieveColValues(PageConstants.COL_INDEX_TOTAL_PHY_POS, tCSVData);
		Map<String, String> mapPriceHedge = retrieveColValues(PageConstants.COL_INDEX_PRICE_HEDGE, tCSVData);
		
		
		for (String key : mapPositions.keySet()) {
			String[] ccyAndBU = key.split("_");
			PluginLog.info("Inserting row values in output table for key : " + key);
			
			String position = mapPositions.get(key); //
			String totPhy = mapPhyPositions.get(key);//getTblData().getString("tot_physical_pos", intRow);
			String priceHedge = mapPriceHedge.get(key); //getTblData().getString("price_hedge", intRow);
			
			Double dblPos = (position == null || "".equals(position)) ? 0.0 : Double.parseDouble(position);
			Double dblTotPhy = (totPhy == null || "".equals(totPhy)) ? 0.0 : Double.parseDouble(totPhy);
			Double dblPriceHedge = (priceHedge == null || "".equals(priceHedge)) ? 0.0 : Double.parseDouble(priceHedge);

			int rowNum = output.addRow();
			output.setString("currency", rowNum, ccyAndBU[0]);
			output.setString("bunit", rowNum, ccyAndBU[1]);
			output.setDouble("position", rowNum, Math.round(dblPos));
			output.setDouble("tot_physical_pos", rowNum, Math.round(dblTotPhy));
			output.setDouble("price_hedge", rowNum, Math.round(dblPriceHedge));
		}
	}
	
	private Table initOutputTbl() throws OException {
		Table output;
		output = Table.tableNew();
		output.addCol("currency", COL_TYPE_ENUM.COL_STRING, "Currency");
		output.addCol("bunit", COL_TYPE_ENUM.COL_STRING, "Business Unit");
		output.addCol("position", COL_TYPE_ENUM.COL_DOUBLE, "Position");
		output.addCol("tot_physical_pos", COL_TYPE_ENUM.COL_DOUBLE, "Total Physical Position");
		output.addCol("price_hedge", COL_TYPE_ENUM.COL_DOUBLE, "Price Hedge");
		return output;
	}
	
	/**
	 * This method is used to retrieve column values like positions, physical positions etc from the snapshot 
	 * generated csv.
	 * 
	 * @param colNum
	 * @param tCSVData
	 * @return
	 * @throws OException
	 */
	private Map<String, String> retrieveColValues(int colNum, Table tCSVData) throws OException {
		Map<String, String> hashColValues = new HashMap<>();
		int rows = tCSVData.getNumRows();
		int startRow = 7; //First 7 rows corresponds to header rows from APM page in snapshot CSV, so ignoring them
		String key = null;
		
		for (int row = startRow + 1; row <= rows;) {
			String ccy = tCSVData.getString(1, row);
			key = ccy + "_All";
			hashColValues.put(key, tCSVData.getString(colNum, row));
			
			int tmpIdx = row + 1;
			while (tmpIdx <= rows && tCSVData.getString(1, tmpIdx).indexOf("JM") > -1) {
				key = ccy + "_" + tCSVData.getString(1, tmpIdx);
				hashColValues.put(key, tCSVData.getString(colNum, tmpIdx));
				tmpIdx++;
				row = tmpIdx;
			}
		}
		return hashColValues;
	}
	
	@Override
	protected String prepareTableData() throws OException {
		String htmlTable = "<table border=1>";
		htmlTable += "<tr>";
		htmlTable += "<th>Currency</th>";
		htmlTable += "<th>Business Unit</th>";
		htmlTable += "<th>Position</th>";
		htmlTable += "<th>Total Physical Position</th>";
		htmlTable += "<th>Price Hedge</th>";
		htmlTable += "</tr>";

		getTblData().group("currency, bunit");
		DecimalFormat formatter = new DecimalFormat("#,###");
		
		int rows = getTblData().getNumRows();
		for (int intRow = 1; intRow <= rows; intRow++) {
			String bunit = getTblData().getString("bunit", intRow);
			if (!"All".equalsIgnoreCase(bunit)) {
				continue;
			}
			
			Double dblPos = getTblData().getDouble("position", intRow);
			Double dblTotPhy = getTblData().getDouble("tot_physical_pos", intRow);
			Double dblPriceHedge = getTblData().getDouble("price_hedge", intRow);
			
			htmlTable += "<tr>";
			htmlTable += "<td>" + getTblData().getString("currency", intRow) + "</td>";
			htmlTable += "<td>" + getTblData().getString("bunit", intRow) + "</td>";
			htmlTable += "<td style=\"text-align: right\">" + formatter.format(dblPos) + "</td>";
			htmlTable += "<td style=\"text-align: right\">" + formatter.format(dblTotPhy) + "</td>";
			htmlTable += "<td style=\"text-align: right\">" + formatter.format(dblPriceHedge) + "</td>";
			htmlTable += "</tr>";
		}
		htmlTable += "</table>";
		return htmlTable;
	}

}
