package com.jm.sc.bo.util;

import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.jm.logging.Logging;

public class BOInvoiceUtil {
	
	/**
	 * Method to check if VAT Invoice is applicable for the document or not.
	 * 
	 * @param tblGenData
	 * @return
	 * @throws OException
	 */
	public static boolean isVATInvoiceApplicable(Table tblGenData) throws OException {
		String insType = getValueFromGenData(tblGenData, "olfInsType");
		String ccy = getValueFromGenData(tblGenData, "olfCurrency");
		String ccyCpt = getValueFromGenData(tblGenData, "olfSetCcy");
		
		if (insType != null && insType.equalsIgnoreCase("Cash") 
				&& ccy != null && ccy.equalsIgnoreCase("GBP") 
				&& ccyCpt != null && !ccyCpt.equalsIgnoreCase("GBP") ){
			Logging.info(String.format("Inside applyVAT doc number logic - InsType: %s, DataCcy(olfCurrency): %s, CustPrefCcy(olfSetCcy): %s", insType, ccy, ccyCpt));
			return true;
		}
		
		// solution will act only if Payment and Tax currency differ
		String strPymtCcy = ccyCpt;
		String strTaxCcy  = getValueFromGenData(tblGenData, "olfTaxCcy");
		
		if (strPymtCcy.equalsIgnoreCase(strTaxCcy)) {
			Logging.info(String.format("No action required (for applying VAT) - Pymt Ccy (value: %s) equals Tax Ccy (value: %s)", strPymtCcy, strTaxCcy));
			return false;
		}

		// solution will act only if Tax Currency = GBP
		if (!"GBP".equalsIgnoreCase(strTaxCcy)) {
			Logging.info(String.format("No action required (for applying VAT) - Tax Ccy (value: %s) is not GBP", strTaxCcy));
			return false;
		}

		// solution will act only if Tax Amount <> 0
		String strPymtTotalTax = getValueFromGenData(tblGenData, "olfPymtTotalTax");
		double dblPymtTotalTaxAbs = Str.strToDouble(strPymtTotalTax.replaceAll("[-()]*", ""));
		if (dblPymtTotalTaxAbs < 0.00001) {
			Logging.info(String.format("No action required (for applying VAT) - Tax Amount (value: %s) equals zero", strPymtTotalTax));
			return false;
		}

		return true;
	}
	
	public static String getValueFromGenData(Table docData, String name) throws OException {
		Logging.info("Retrieving value for '" + name + "' from Gen Data ...");
		int row = docData.unsortedFindString("col_name", name, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (row <= 0) {
			throw new OException("Failed to retrieve value for '" + name + "' from Gen Data");
		}
		String val = docData.getString("col_data", row);
		val = val == null ? "" : val.trim();
		
		Logging.info("Retrieved value for '" + name + "' from Gen Data: " + val);
		return val;
	}
}
