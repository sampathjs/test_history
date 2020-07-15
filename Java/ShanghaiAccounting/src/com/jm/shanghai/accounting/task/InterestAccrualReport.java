package com.jm.shanghai.accounting.task;

import com.olf.openjvs.OException;
import com.olf.openjvs.ReportBuilder;

public class InterestAccrualReport {

	final private String region;
	final private String regionAbbrev;

	public InterestAccrualReport(String region) {
		this.region = region;
		this.regionAbbrev = toAbbrev(region);
	}

	public void runReport() {
		try {
			ReportBuilder reportBuilder = ReportBuilder
					.createNew("Endur Accounting Feed - General Ledger Extract for Metal Interest");
			reportBuilder.setParameter("ALL", "QUERY_NAME", "Metal Interest Query DV_" + regionAbbrev);
			reportBuilder.setParameter("ALL", "regional_segregation", region);
			reportBuilder.setParameter("ALL", "REPORTING_PATH",
					"\\reports\\Non-SAP\\JDE-XML-Extracts\\" + regionAbbrev);
			reportBuilder.runReportOutput("JDE");
			reportBuilder.runReportOutput("DB");
		} catch (OException e) {
			throw new RuntimeException(e);
		}
	}

	private static String toAbbrev(String region) {
		switch (region) {
		case "United Kingdom":
			return "UK";
		case "United States":
			return "US";
		case "Hong Kong":
			return "HK";
		}
		throw new RuntimeException("invalid region: " + region);
	}
}
