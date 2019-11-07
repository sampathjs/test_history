package com.olf.jm.forwardrate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.market.EnumBmo;
import com.olf.openrisk.staticdata.Currency;
import com.olf.openrisk.staticdata.EnumReferenceObject;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;

@ScriptCategory({ EnumScriptCategory.Generic })
public class ForwardRate extends AbstractGenericScript {

	private static SimpleDateFormat SDF1 = new SimpleDateFormat("dd-MMM-yyyy");
	private int days = 0;
	private Date Spot_Date = null;
	private Double Spot_Price = 0.0;
	private Date Value_Date = null;
	private Double Value_Price = 0.0;
	private Currency CURRENCY = null;
	private Currency METAL = null;
	private Double rate = 0.0;

	public Table execute(Session session, ConstTable table) {

		Table parameters = table.getTable("PluginParameters", 0);
		setParameters(session, parameters);
		getPrices(session);
		Table output = session.getTableFactory().createTable();
		output = buildOutput(session, output);
		return output;

	}

	private Table buildOutput(Session session, Table output) {
		String[] col_names = { "Spot_Date", "Spot_Price", "Value_Date",
				"Value_Price", "Days", "Metal","Currency", "Rate" };
		EnumColType[] col_types = { EnumColType.String, EnumColType.Double,
				EnumColType.String, EnumColType.Double, EnumColType.Int,
				EnumColType.String, EnumColType.String, EnumColType.Double };
		
		output.addColumns(col_names, col_types);

		output.addRow();
		output.setValue("Spot_Date", 0,
				new SimpleDateFormat("dd-MMM-yyyy").format(Spot_Date));
		output.setValue("Spot_Price", 0, Spot_Price);
		output.setValue("Value_Date", 0,
				new SimpleDateFormat("dd-MMM-yyyy").format(Value_Date));
		output.setValue("Value_Price", 0, Value_Price);
		output.setValue("Days", 0, days);
		output.setValue("Metal", 0, METAL.getName());
		output.setValue("Currency", 0, CURRENCY.getName());
		output.setValue("Rate", 0, rate);
		return output;
	}

	private void setParameters(Session session, Table parameters) {
		int rowId = parameters.find(parameters.getColumnId("parameter_name"),
				"DATE_DIF", 0);
		if (rowId >= 0) {
			days = Integer.parseInt(parameters.getString("parameter_value",
					rowId))-2;
		}
		rowId = parameters.find(parameters.getColumnId("parameter_name"),
				"VALUE_DATE", 0);
		if (rowId >= 0) {
			try {
				Value_Date = SDF1.parse(parameters.getString("parameter_value",
						rowId));
			} catch (ParseException e) {
				session.getDebug().printLine("Fwd_Rate Error: " + e + ":\n");
			}
		}
		rowId = parameters.find(parameters.getColumnId("parameter_name"),
				"SPOT_DATE", 0);
		if (rowId >= 0) {
			try {
				Spot_Date = SDF1.parse(parameters.getString("parameter_value",
						rowId));
			} catch (ParseException e) {
				session.getDebug().printLine("Fwd_Rate Error: " + e + ":\n");
			}
		}
		rowId = parameters.find(parameters.getColumnId("parameter_name"),
				"METAL", 0);
		if (rowId >= 0) {

			METAL = (Currency) session.getStaticDataFactory()
					.getReferenceObject(EnumReferenceObject.Currency,
							parameters.getString("parameter_value", rowId));
		}
		
		rowId = parameters.find(parameters.getColumnId("parameter_name"),
				"CURRENCY", 0);
		if (rowId >= 0) {

			CURRENCY = (Currency) session.getStaticDataFactory()
					.getReferenceObject(EnumReferenceObject.Currency,
							parameters.getString("parameter_value", rowId));
		}

	}

	private void getPrices(Session session) {
		
		Value_Price = session.getMarket().getFXRate(METAL, CURRENCY, Value_Date,
				EnumBmo.Mid);
		Spot_Price = session.getMarket().getFXSpotRate(METAL, CURRENCY, Spot_Date,
				EnumBmo.Mid);

		rate = 100 * ((Value_Price / Spot_Price) - 1) * 360 / days;
	}

}
