/**
 * Project:  D377 - JM-Invoices
 * 
 * Description:
 * - This script extends the logic of a standard solution by specific acting on Invoices and
 *   Credit Notes and on so called VAT Only Invoices (in case of differing Tax currency).
 * 
 * Assumptions:
 * - Gen Data and Xml Data are provided and to be handled
 * - The standard solutions handles Gen Data and Xml Data in similar manner;
 *   this custom solution will analyze Gen Data only but handle both Gen Data and Xml Data
 * 
 * Revision History:
 *  29.10.15  jbonetzk  	initial version
 *  19.04.16  jwaechter		- moved tax doc num generation into method "applyVatDocNumbering"
 *                          - added logic to generate vat doc num in case described in JM-330
 *  12.05.16  jwaechter		- disable change for JM-330 (row 47/48). 
 * 							  This will be worked through again at a later point in time and within a Change Request.
 *  28.07.16  jwaechter	    - added synchronisation of the document number to USER_consecutive_number
 *  17.05.17  jwaechter	    - added writing back of cancellation document number to the GEN data,
 *                            not just the XML as before.
 *  04.02.19  jneufert      - add condition for China to use status '1 Waiting' 
 *  25.03.20  YadavP03  	- memory leaks, remove console prints & formatting changes
 *  
 */
package com.openlink.jm.bo;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.jm.sc.bo.util.BOInvoiceUtil;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.StlDoc;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.consecutivenumber.model.ConsecutiveNumberException;
import com.openlink.util.consecutivenumber.persistence.ConsecutiveNumber;
import com.openlink.util.constrepository.ConstRepository;
import com.olf.jm.logging.Logging;
import com.openlink.util.misc.TableUtilities;

@com.olf.openjvs.PluginCategory(com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_STLDOC_GENERATE)
@com.olf.openjvs.ScriptAttributes(allowNativeExceptions=false)
/** @author jbonetzky@olf.com, jneufert@olf.com */
public class JM_GEN_DocNumbering extends com.openlink.sc.bo.docnums.OLI_GEN_DocNumbering {
	
	// names of BO Doc Info types
	private final String STLDOC_INFO_TYPE_VATINVDOCNUM = "VAT Invoice Doc Num";
	private final String GEN_DATA_TABLE = "*SourceEventData";
	private String strVatInvNum = null;
	// names of specific data fields in Gen/Xml Data
	private static final String
		 GEN_DATA_OURDOCNUM     	= "olfStlDocInfo"+"_OurDocNum"
		,GEN_DATA_VATINVDOCNUM  	= "olfStlDocInfo"+"_VATInvoiceDocNum"
		,GEN_DATA_CANCELVATNUM  	= "olfStlDocInfo"+"_CancelVATNum"
		,GEN_DATA_CANCELDOCNUM  	= "olfStlDocInfo"+"_CancelDocNum"
		,GEN_DATA_PYMTCCY       	= "olfSetCcy"
		,GEN_DATA_TAXCCY        	= "olfTaxCcy"
		,GEN_DATA_PYMTTOTALDBL  	= "olfPymtTotalDbl"
		,GEN_DATA_PYMTTOTALTAX  	= "olfPymtTotalTax"
		,GEN_DATA_INS_TYPE	    	= "olfInsType"
		,GEN_DATA_CURRENCY	    	= "olfCurrency"
		,GEN_DATA_FILENAME_DOC_NUM 	= "olf_Filename_DocNum"
		,GEN_DATA_DOC_NUM_VAT_NUM  = "olfTable_DocNumVATNum"
	  //  ,GEN_DATA_CUST_PREF_CCY = "olfCppCcy"
		,GEN_DATA_CUST_PREF_CCY = "olfSetCcy"      //ignore the CppCcy or assume it's always the same as the settle currency
	;

	private Table _tblDocNumCfg = null;
	double _dblPymtTotal = 0D;

	public void execute(IContainerContext context) throws OException {
		
		_constRepo = new ConstRepository("BackOffice", "OLI-DocNumbering");
		initLog ();
		
		Table argt = context.getArgumentsTable();
		Table genData = argt.getTable("doc_table", argt.unsortedFindString("col_name", GEN_DATA_TABLE, SEARCH_CASE_ENUM.CASE_INSENSITIVE));

		// pre-check availability of destination info field
		String strVatInvDocNum  = getCurrentValue(argt, GEN_DATA_VATINVDOCNUM);
		
		// get current Our Doc Num for later comparing purposes
		String strOurDocNumCurr = getCurrentValue(argt, GEN_DATA_OURDOCNUM);
		
		// additional criteria for conditions used in 'applyCustomConditions'
		_dblPymtTotal = getCurrentValueDbl(argt, GEN_DATA_PYMTTOTALDBL);
		
		//OConsole.print("\n" + GEN_DATA_OURDOCNUM + ":= " + strOurDocNumCurr + " " + GEN_DATA_VATINVDOCNUM + ":= " + strVatInvDocNum + " " + GEN_DATA_PYMTTOTALDBL + " :=" + _dblPymtTotal);
		Logging.info(String.format("%s:= %s, %s:= %s, %s:= %s", GEN_DATA_OURDOCNUM, strOurDocNumCurr, GEN_DATA_VATINVDOCNUM, strVatInvDocNum, GEN_DATA_PYMTTOTALDBL, String.valueOf(_dblPymtTotal)));

		try 		{
			// cleanup of _tblDocNumCfg as possibly initialized thru 'applyCustomConditions'
			super.execute(context); // required: call standard logic
			
			// solution will act only in case standard solution acted
			String strOurDocNumNew = getCurrentValue(argt, GEN_DATA_OURDOCNUM);
			String newXmlData = updateField(argt, getXmlData(), GEN_DATA_FILENAME_DOC_NUM, strOurDocNumNew);
			setXmlData(newXmlData);
			
			if (BOInvoiceUtil.isVATInvoiceApplicable(argt) && (strVatInvDocNum == null || strVatInvDocNum.isEmpty())) {
				Logging.info("Generating VAT Inv Doc Num for document with Our Doc Num:" + strOurDocNumCurr);

				//applyVatDocNumbering(argt, strOurDocNumNew);

				strVatInvNum = applyVatDocNumbering(argt, strOurDocNumNew);
				//(SR 255688 )Below function updates concatenated values as "DocNum_VatNum in XML Data which will be used as File name."
				applyDocNumVatNum(argt,strOurDocNumNew,strVatInvNum); 
				
			}else{
				applyDocNumVatNum(argt,strOurDocNumNew,strVatInvNum); 
			}
			
			int toDocStatus = genData.getInt("next_doc_status", 1);
			if (strOurDocNumNew.equalsIgnoreCase(strOurDocNumCurr) == true && toDocStatus != 4) {
				Logging.info("No action required - '"+GEN_DATA_OURDOCNUM+"' remains: "+strOurDocNumCurr);
				return;
			}
			// If we are processing a cancellation, then we need to invert the doc types invoice=credit note, credit note=invoice
			// And we also need to generate new documents numbers for revised doc types.
			CancellationDocAndVatNums cancelDocAndVatNums = new CancellationDocAndVatNums();
			cancelDocAndVatNums.getCancellationDocNumCfg(argt, strVatInvDocNum);
			
			/*String insType = getCurrentValue(argt, GEN_DATA_INS_TYPE);
			String ccy = getCurrentValue (argt, GEN_DATA_CURRENCY);
			String ccyCpt = getCurrentValue (argt, GEN_DATA_CUST_PREF_CCY);
			if (   insType != null && insType.equalsIgnoreCase("Cash") && ccy != null && ccy.equalsIgnoreCase("GBP") && ccyCpt != null && !ccyCpt.equalsIgnoreCase("GBP") ){
				Logging.info(String.format("Inside applyVAT doc number logic - InsType: %s, DataCcy(olfCurrency): %s, CustPrefCcy(olfSetCcy): %s", insType, ccy, ccyCpt));
				applyVatDocNumbering(argt, strOurDocNumNew);
				return;
			}
			
			// solution will act only if Payment and Tax currency differ
			String strPymtCcy = getCurrentValue(argt, GEN_DATA_PYMTCCY);
			String strTaxCcy  = getCurrentValue(argt, GEN_DATA_TAXCCY);
			if (strPymtCcy.equalsIgnoreCase(strTaxCcy)) {
				Logging.info(String.format("No action required (for applying VAT) - Pymt Ccy (value: %s) equals Tax Ccy (value: %s)", strPymtCcy, strTaxCcy));
				return;
			}

			// solution will act only if Tax Currency = GBP
			if (!"GBP".equalsIgnoreCase(strTaxCcy)) {
				Logging.info(String.format("No action required (for applying VAT) - Tax Ccy (value: %s) is not GBP", strTaxCcy));
				return;
			}

			// solution will act only if Tax Amount <> 0
			String strPymtTotalTax = getCurrentValue(argt, GEN_DATA_PYMTTOTALTAX);
			double dblPymtTotalTaxAbs = Str.strToDouble(strPymtTotalTax.replaceAll("[-()]*", ""));
			if (dblPymtTotalTaxAbs < 0.00001) {
				Logging.info(String.format("No action required (for applying VAT) - Tax Amount (value: %s) equals zero", strPymtTotalTax));
				return;
			}

			applyVatDocNumbering(argt, strOurDocNumNew);*/
			
		} catch (OException oe) {
			Logging.error(String.format("Error occurred in JM_GEN_DocNumbering script, error_message- %s",  oe.getMessage()));
			throw oe;
			
		} finally { 
			Logging.close();
			if (_tblDocNumCfg != null) {
				_tblDocNumCfg.destroy(); 
			}
		}
	}

	private void applyDocNumVatNum(Table argt, String strOurDocNumNew, String strVatInvNum)throws OException {
		String strDocNumVatNum = null;
		if(strVatInvNum == null || strVatInvNum.trim().isEmpty() ){
			strDocNumVatNum = strOurDocNumNew;
		}else{
			strDocNumVatNum = strOurDocNumNew+"_"+strVatInvNum;;
		}
		String newXmlData = updateField(argt, getXmlData(), GEN_DATA_DOC_NUM_VAT_NUM, strDocNumVatNum);
		setXmlData(newXmlData);				

	}

	private String applyVatDocNumbering(Table argt, String strOurDocNumNew) throws OException {
		String strVatInvDocNum;
		Logging.info(String.format("Inside applyVatDocNumbering method for OurDocNum: %s ...", strOurDocNumNew));
		// reached this point, action by this custom solution may be required
		VatInvOnlyNumbering vatInvNumbering = new VatInvOnlyNumbering();

		// solution will act only in case of DocNumbering-SubType 'Invoice' (1)
		if (_tblDocNumCfg == null){
			_tblDocNumCfg = vatInvNumbering.getDocNumCfg(argt, 1); // 'Invoice' only
		}
		if (_tblDocNumCfg.getNumRows() <= 0) {
			Logging.info("No action required - document doesn't suite to config");
			return null;
		}
		
		if (_tblDocNumCfg.getNumRows() > 1) {
			Logging.debug(_tblDocNumCfg.exportCSVString()+ "Is ambiguous:");
			Logging.error("Failed to deal with ambiguous configuration");
			throw new OException("Failed to deal with ambiguous configuration");
		}

		// apply new number
		boolean isPreview = isPreview(argt);
		strVatInvDocNum = isPreview ? vatInvNumbering.fakeNextDocNum(strOurDocNumNew) : getNextDocNumber(_tblDocNumCfg, isPreview);
				
		String xmlData = getXmlData();
		xmlData = vatInvNumbering.updateField(argt, xmlData, GEN_DATA_VATINVDOCNUM, strVatInvDocNum);
		setXmlData(xmlData);
		Logging.info(String.format("%s field (value: %s) successfully updated in xmlData for OurDocNum field value: %s", STLDOC_INFO_TYPE_VATINVDOCNUM, strVatInvDocNum, strOurDocNumNew));

		if (!isPreview){
			vatInvNumbering.updateDB(_tblDocNumCfg, STLDOC_INFO_TYPE_VATINVDOCNUM, strVatInvDocNum);
		}

		Logging.info(GEN_DATA_VATINVDOCNUM+": "+strVatInvDocNum);
		Logging.info(String.format("Exiting applyVatDocNumbering method for OurDocNum: %s ...", strOurDocNumNew));
		return strVatInvDocNum;
	}

	private String getCurrentValue(Table argt, String name) throws OException {
		
		Logging.info("Retrieving value for '"+name+"' from Gen Data ...");
		int row = argt.unsortedFindString("col_name", name, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (row <= 0){
			throw new OException("Failed to retrieve value for '"+name+"' from Gen Data");
		}
		String val = argt.getString("col_data", row);
		val = val == null ? "" : val.trim();
		Logging.info("Retrieved value for '"+name+"' from Gen Data: "+val);
		return val;
	}


	private double getCurrentValueDbl(Table argt, String name) throws OException{
		
		Logging.info("Retrieving value for '"+name+"' from Gen Data ...");
		int row = argt.unsortedFindString("col_name", name, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		if (row <= 0){ 
			throw new OException("Failed to retrieve value for '"+name+"' from Gen Data");
		}
		double val = argt.getDouble("DoubleData", row);
		Logging.info("Retrieved value for '"+name+"' from Gen Data: "+val);
		return val;
	}
	
	private String updateField(Table argt, String xmlData, String genDataField, String targetValue) throws OException{
		
		if (argt == null) {
			return xmlData;
		}

		int row = argt.unsortedFindString("col_name", genDataField, SEARCH_CASE_ENUM.CASE_SENSITIVE);
		//update the table resulting data
		if (row > -1) {
			argt.setString("col_data", row, targetValue);	
		}

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
		
		if(posValueStart>0 && posClosingTag>0) {
			return builder.substring(posValueStart, posClosingTag);
		} else {
			return "";
		}
	}
	
	/**
	 * DocNumbering solution provides an entry point 
	 * for applying additional custom conditions.<p/>
	 * <b>Note:</b> the provided <code>tbl</code> has 
	 * at least one row and must have exact one row 
	 * at the end of this method
	 * @param tbl adjustable execution specific data
	 * @throws OException
	 */
	protected void applyCustomConditions(Table tbl) throws OException  {
		Logging.info("starts - applyCustomConditions method");
		Logging.debug(tbl.exportCSVString());

//		tbl.viewTable();
		Table tblConditions = null;
		try {
			tblConditions = Conditions.get();
//			tblConditions.viewTable();

			// overwrite current buy/sell flag based on Total Payment value
			tbl.setColValInt("buy_sell", _dblPymtTotal < 0 ? 0 : 1); // buy:sell

			// prepare flag for adjustment
			tblConditions.addCols("I(is_selected)");
			tblConditions.setColValInt("is_selected", 1);

			tbl.addCols("I(is_selected)");
			tbl.setColValInt("is_selected", 0);

			// apply condition rules
			tbl.select(tblConditions, "is_selected", "sub_type EQ $sub_type AND doc_status EQ $next_doc_status AND buy_sell EQ $buy_sell");
			tbl.select(tblConditions, "is_selected", "sub_type EQ $sub_type AND sub_type EQ 0");// strictly required to support unspecific sub type
//			tbl.viewTable();

			tbl.deleteWhereValue("is_selected", 0);
			tbl.delCol("is_selected");

			// keep a copy for later use regarding VAT Only Invoices
			_tblDocNumCfg = tbl.cloneTable();
			_tblDocNumCfg.select(tbl, "*", "sub_type EQ 1"); // '1': sub type Invoice only
		}  finally { 
			if (tblConditions != null) {
				tblConditions.destroy(); 
			}
		}

		Logging.debug(tbl.exportCSVString());
		Logging.info("done - applyCustomConditions method");
	}

	private static class Conditions {
		
		static Table get() throws OException {
			Table tbl = Table.tableNew("conditions");
			// column 'sub_type_name' is not used but for referential purposes on view
			tbl.addCols("I(doc_status)I(buy_sell)I(sub_type)S(sub_type_name)");
			// column formatting is not required but for referential purposes on view
			tbl.setColFormatAsRef(1, SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
			tbl.setColFormatAsRef(2, SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);

			// default: no specific sub type logic - providing this is strictly required
			tbl.addRowsWithValues("-1,-1,0,(none)");

			// Invoice: Generated and Sell -or- Cancelled and Buy
			tbl.addRowsWithValues("5,1,1,(Invoice) , 4,0,1,(Invoice)");
			// Credit Note: Generated and Buy -or- Cancelled and Sell
			tbl.addRowsWithValues("5,0,2,(Credit Note) , 4,1,2,(Credit Note)");

//jneufert: new conditions for China
			// Waiting Invoice: Waiting and Sell 
			tbl.addRowsWithValues("6,1,1,(Invoice)");
			// Waiting Credit Note: Waiting and Buy
			tbl.addRowsWithValues("6,0,2,(Credit Note)");

			return tbl;
		}
	}

	private class VatInvOnlyNumbering {
		// table pointer aliases, don't destroy!
		private Table argt = null, argtEventData = null; 

		// returns a non-freeable table; either the event table or a null table
		private Table getEventData(Table argt) throws OException {
			if (this.argt == null) this.argt = argt;
			int row = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			if (row <= 0){
				throw new OException("Failed to retrieve event data from arguments table");
			}
			return argt.getTable("doc_table", row);
		}

		Table getDocNumCfg(Table argt, int subType) throws OException {
			
			if (this.argt == null) {
				this.argt = argt;
			}
			if (argtEventData == null) {
				argtEventData = getEventData(argt);
			}

			int iIntLE   = argtEventData.getInt("doc_type", 1)
			   ,iDocType = argtEventData.getInt("internal_lentity", 1)
			   ;

			String sql
				= "select *"
				+ " from USER_bo_doc_numbering"
				+ " where doc_type_id="+iDocType+" and our_le_id="+iIntLE
				+ " and sub_type="+subType
				;
			Table tbl = Table.tableNew();
			DBaseTable.execISql(tbl, sql);

			// no rows in configuration - nothing to do
			if (tbl.getNumRows() <= 0){
				return tbl;
			}

			tbl.addCol("next_doc_status", COL_TYPE_ENUM.fromInt(argtEventData.getColType("next_doc_status")));
			tbl.addCol("buy_sell", COL_TYPE_ENUM.fromInt(argtEventData.getColType("buy_sell")));
			// column 'buy_sell' is also used flag-like: '-1' = no match, to be deleted afterwards
			tbl.setColValInt("buy_sell", -1);

			tbl.select(argtEventData,
					"next_doc_status,buy_sell",
					"doc_type EQ "+iDocType+" AND internal_lentity EQ "+iIntLE);
			tbl.deleteWhereValue("buy_sell", -1);
			tbl.makeTableUnique();

			applyCustomConditions(tbl);

			return tbl;
		}

		String fakeNextDocNum(String vatInvDocnum) throws OException {
			
			try {
				return ""+(Long.parseLong(vatInvDocnum)+1);
			}  catch (NumberFormatException e) { 
				throw new OException("NumberFormatException: " + vatInvDocnum); 
			}
		}

		String updateField(Table argt, String xmlData, String gEN_DATA_VATINVDOCNUM, String vatInvDocNum) throws OException {
			
			if (this.argt == null) {
				this.argt = argt;
			}

			int row = argt.unsortedFindString("col_name", gEN_DATA_VATINVDOCNUM, SEARCH_CASE_ENUM.CASE_SENSITIVE);
			//argt.setString("col_date", row, vatInvDocNum);
			argt.setString("col_data", row, vatInvDocNum);

			StringBuilder builder = new StringBuilder(xmlData);

			String field, value;
			int posValueStart, posClosingTag, lengthBefore;
			{
				field = gEN_DATA_VATINVDOCNUM;
				value = vatInvDocNum;

				posValueStart = -1;
				while ((posValueStart=builder.indexOf("<"+field+" ", posValueStart))>=0) {
					
					lengthBefore = builder.length();
					if (builder.indexOf("/", posValueStart)<builder.indexOf(">", posValueStart)) {
						// value is empty
						posValueStart = builder.indexOf("/", posValueStart);
						builder.replace(posValueStart, posValueStart+1, ">"+value+"</"+field);
					} else {
						posValueStart = builder.indexOf(">", posValueStart) + 1;
						posClosingTag = builder.indexOf("<", posValueStart);
						builder.replace(posValueStart, posClosingTag, value);
					}
					posValueStart += builder.length()-lengthBefore;	  
				}
			}
			return builder.toString();
		}

		void updateDB(Table config, String sTLDOC_INFO_TYPE_VATINVDOCNUM, String strVatInvDocNum) throws OException {
			Table tblDocNumbering = null;
			Logging.info(String.format("Inside updateDB method to update USER_bo_doc_numbering..."));
			try {
				// update user table
				tblDocNumbering = Table.tableNew("USER_bo_doc_numbering");
				DBUserTable.structure(tblDocNumbering);
				
				config.copyRowAddAllByColName(tblDocNumbering);
				tblDocNumbering.setString("last_number", 1, ""+strVatInvDocNum);
				tblDocNumbering.setString("reset_number_to", 1, "");
				tblDocNumbering.group("doc_type_id, our_le_id, sub_type");
				int retCode = DBUserTable.update(tblDocNumbering);
				if (retCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
                    String message = String.format("Failed to update value in USER_bo_doc_numbering, error message: %s", (DBUserTable.dbRetrieveErrorInfo (retCode, "DBUserTable.update() failed")));
                    Logging.error(message);
					throw new OException(message);
				}
				
				ConsecutiveNumber cn;
				try {
					cn = new ConsecutiveNumber("OLI_DocNumbering");
				}  catch (ConsecutiveNumberException e) { 
					Logging.error(e.getMessage());
					throw new OException(e.getMessage()); 
				}
				
				String item = ""+tblDocNumbering.getInt("our_le_id", 1) + "_"+tblDocNumbering.getInt("doc_type_id", 1);
				if (tblDocNumbering.getColNum("sub_type") > 0)
					item += "_"+tblDocNumbering.getInt("sub_type", 1);
				
				try  {
					cn.resetItem(item, Long.parseLong(strVatInvDocNum)+1);
				} catch (ConsecutiveNumberException e) {
					Logging.error(e.getMessage());
					throw new OException(e.getMessage()); 
				}

				Logging.info(String.format("USER_bo_doc_numbering table successfully updated with new value: %d", Long.parseLong(strVatInvDocNum)+1));
				//tblDocNumbering.destroy();
				// update info field
				if (argtEventData == null){
					argtEventData = getEventData(argt);
				}
				
				int document_num = argtEventData.getInt("document_num", 1);
				retCode = StlDoc.saveInfoValue(document_num, sTLDOC_INFO_TYPE_VATINVDOCNUM, strVatInvDocNum);
				if (retCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					String message = String.format("Error in saving doc info: %s field (value: %s) for document: %s", sTLDOC_INFO_TYPE_VATINVDOCNUM, strVatInvDocNum, document_num);
					Logging.error(message);
					throw new OException(message);
				}
				Logging.info(String.format("%s field successfully updated with value: %s for document: %d", sTLDOC_INFO_TYPE_VATINVDOCNUM, strVatInvDocNum, document_num));
				Logging.info(String.format("Exiting updateDB method to update USER_bo_doc_numbering..."));
				
			} finally {
				if (tblDocNumbering != null) {
					tblDocNumbering.destroy();
				}
			}
		}
	}
	
	/**
	 * @description If we are processing a cancellation, we need to generate the opposite doc numbers
	 * @author jonesg02
	 *
	 */
	private class CancellationDocAndVatNums {
		// table pointer aliases, don't destroy!
		private Table argt = null, argtEventData = null; 

		// returns a non-freeable table; either the event table or a null table
		private Table getEventData(Table argt) throws OException {
			if (this.argt == null) this.argt = argt;
			int row = argt.unsortedFindString("col_name", "*SourceEventData", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			if (row <= 0)
				throw new OException("Failed to retrieve event data from arguments table");
			return argt.getTable("doc_table", row);
		}
		
		/**
		 * @description	Generates the new document numbers if this is a cancellation
		 * @param 		argt
		 * @param 		strVatInvDocNum
		 * @return		results
		 * @throws 		OException
		 */
		void getCancellationDocNumCfg(Table argt, String strVatInvDocNum) throws OException {
			Logging.info(String.format("Inside getCancellationDocNumCfg method for VAT Invoice Num (strVatInvDocNum: %s)...", strVatInvDocNum));
			final String DOC_STATUS_CANCELLED = "Cancelled";
			if (this.argt == null) this.argt = argt;
			if (argtEventData == null) argtEventData = getEventData(argt);
			String sql = null;
			
			// Only do this if its a cancellation
			if (!DOC_STATUS_CANCELLED.equals(Ref.getName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, argtEventData.getInt("next_doc_status", 1)))){
				Logging.info(String.format("Exiting getCancellationDocNumCfg method for VATInvoiceNum (strVatInvDocNum: %s) as next_doc_status is not %s...", strVatInvDocNum, DOC_STATUS_CANCELLED));
				return;
			}
			
			int docNumIncrement = 1;
			boolean vatApplicable = false;
			if (strVatInvDocNum.length() > 0) {
				Logging.info(String.format("vatApplicable=true for strVatInvDocNum: %s", strVatInvDocNum));
				vatApplicable = true;
				docNumIncrement = 2;
			}
			
			int legalEntity = argtEventData.getInt("internal_lentity", 1),
				docType 	= argtEventData.getInt("doc_type", 1);
			Table tbl = Util.NULL_TABLE;

			try {
				// Credit note or invoice
				if (_dblPymtTotal > 0.00) {
					sql	= "select max(last_number)+" +docNumIncrement+" as last_number, doc_type_id, our_le_id, sub_type \n"
						+ "from USER_bo_doc_numbering \n"
						+ "where doc_type_id="+docType+" \n"
						+ "and our_le_id="+legalEntity + "\n"
						+ "and sub_type = 2 \n" // Credit Note?
						+ "group by  doc_type_id, our_le_id, sub_type";
				} else if (_dblPymtTotal < 0.00) {
					sql	= "select max(last_number)+" +docNumIncrement+" as last_number, doc_type_id, our_le_id, sub_type \n"
						+ "from USER_bo_doc_numbering \n"
						+ "where doc_type_id="+docType+" \n"
						+ "and our_le_id="+legalEntity + "\n"
						+ "and sub_type = 1 \n"	// Invoices
						+ "group by doc_type_id, our_le_id, sub_type";
				}

				Logging.debug(String.format("Executing SQL query: %s", sql));
				tbl = Table.tableNew();
				DBaseTable.execISql(tbl, sql);

				// no rows in configuration - nothing to do
				if (tbl.getNumRows() <= 0) {
					//tbl.dispose();
					Logging.info(String.format("Exiting getCancellationDocNumCfg method for VATInvoiceNum (strVatInvDocNum: %s) as SQL query returned 0 rows...", strVatInvDocNum));
					return;
				}

				// Update user table and apply numbering
				updateDB(tbl, vatApplicable);
				//tbl.dispose();
				Logging.info(String.format("Exiting getCancellationDocNumCfg method for VATInvoiceNum (strVatInvDocNum: %s)...", strVatInvDocNum));
			} finally {
				if (Table.isTableValid(tbl) == 1) {
					tbl.destroy();
				}
			}
		}
		
		/**
		 * @description Update the document numbers if the reversal is being generated. 
		 * 				Do not save the values if the doc is being previewed!
		 * @param 		results
		 * @param 		vatApplicable 
		 * @throws 		OException
		 * @throws IOException 
		 * @throws SAXException 
		 * @throws ParserConfigurationException 
		 */
		void updateDB(Table results, boolean vatApplicable) throws OException {	
			
			int cancelledDocNum;
			if (argtEventData == null){
				argtEventData = getEventData(argt);
			}
			int docNum = argtEventData.getInt("document_num", 1);
			Logging.info(String.format("Inside updateDB method for document: %d", docNum));
			int existingCancelDocNum = getExistingCancelDocNum(docNum);
			
			if (existingCancelDocNum == 0) {
				Table docNumbers = null;
				
				try {
					// update user table doc values	
					docNumbers = Table.tableNew("USER_bo_doc_numbering");
					cancelledDocNum = results.getInt("last_number", 1);
					results.convertColToString(results.getColNum("last_number"));		
					DBUserTable.structure(docNumbers);
					results.copyRowAddAllByColName(docNumbers);

					docNumbers.setString("reset_number_to", 1, "");
					docNumbers.group("doc_type_id, our_le_id, sub_type");
					if (!isPreview(argt)) {
						int retCode = DBUserTable.update(docNumbers);
						if (retCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
							//docNumbers.destroy();
							String message = String.format("Failed to update value in USER_bo_doc_numbering, error message: %s", (DBUserTable.dbRetrieveErrorInfo (retCode, "DBUserTable.update() failed")));
			                Logging.error(message);
							throw new OException(message);
						}
					}
					
					ConsecutiveNumber cn;
					try {
						cn = new ConsecutiveNumber("OLI_DocNumbering");
					} catch (ConsecutiveNumberException e) { 
						Logging.error(e.getMessage());
						throw new OException(e.getMessage()); 
					}
					
					String item = ""+docNumbers.getInt("our_le_id", 1) + "_"+docNumbers.getInt("doc_type_id", 1);
					if (docNumbers.getColNum("sub_type") > 0)
						item += "_"+docNumbers.getInt("sub_type", 1);
					
					try {
						cn.resetItem(item, cancelledDocNum+1);
					} catch (ConsecutiveNumberException e) { 
						Logging.error(e.getMessage());
						throw new OException(e.getMessage()); 
					}
					Logging.info(String.format("USER_bo_doc_numbering table successfully updated with new value: %d", cancelledDocNum+1));
					//docNumbers.destroy();
				} finally {
					if (docNumbers != null) {
						docNumbers.destroy();
					}
				}
			} else {
				cancelledDocNum = existingCancelDocNum;
			}
			
			// Update the cancellation numbers saved in the XML. 
			String strCancelledDocNum = Integer.toString(cancelledDocNum);	
			String strCancelledVatNum = "";
			if (vatApplicable == true) {
				strCancelledDocNum = Integer.toString(cancelledDocNum-1);
				strCancelledVatNum = Integer.toString(cancelledDocNum);
			}		

			/*String strOurDocNum = getCurrentValue(argt, GEN_DATA_OURDOCNUM);
			String strFileNameDocNum = strOurDocNum + "_" + strCancelledDocNum;
			updateXML("olfStlDocInfo_CancelDocNum", strCancelledDocNum);*/
			
			Table processData = argt.getTable("process_data", 1);
			Table userData = processData.getTable("user_data", 1);
			int row = userData.unsortedFindString("col_name", "olfStlDocInfo_CancelDocNum", SEARCH_CASE_ENUM.CASE_SENSITIVE);
			userData.setString("col_data", row, strCancelledDocNum);

			updateXML(GEN_DATA_CANCELDOCNUM, strCancelledDocNum);
						
			// Set the values in process_data
			processData.setString("stldoc_info_type_20007", 1, strCancelledDocNum);
			
			int retCode = StlDoc.saveInfoValue(docNum, "Cancellation Doc Num", strCancelledDocNum);
			if (retCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
				String message = String.format("Error in saving Cancellation Doc Num field value (%s) for document: %s", strCancelledDocNum, docNum);
				Logging.error(message);
				throw new OException(message);
			}
			Logging.info(String.format("Cancellation Doc Num field (value: %s) successfully saved & updated in xmlData for document: %d", strCancelledDocNum, docNum));
			
			// Update is applicable.
			if (vatApplicable) {
				updateXML(GEN_DATA_CANCELVATNUM, strCancelledVatNum);
				processData.setString("stldoc_info_type_20008", 1, strCancelledVatNum);
				
				int vatRow = userData.unsortedFindString("col_name", "olfStlDocInfo_CancelVATNum", SEARCH_CASE_ENUM.CASE_SENSITIVE);
				userData.setString("col_data", vatRow, strCancelledVatNum);
				
				retCode = StlDoc.saveInfoValue(docNum, "Cancellation VAT Num", strCancelledVatNum);
				if (retCode != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					String message = String.format("Error in saving Cancellation VAT Num field value (%s) for document: %s", strCancelledVatNum, docNum);
					Logging.error(message);
					throw new OException(message);
				}
				Logging.info(String.format("Cancellation VAT Num field (value: %s) successfully saved & updated in xmlData for document: %d", strCancelledVatNum, docNum));
			}
			Logging.info(String.format("Exiting updateDB method for document: %d", docNum));
		}

		private int getExistingCancelDocNum(int docNum) throws OException {
			String cancelDocNumQuery = 
					"\n SELECT ISNULL(info.value, '0') AS cancel_doc_num"
				+	"\n FROM stldoc_info_types type"
				+ 	"\n LEFT OUTER JOIN stldoc_info info"
				+   "\n  ON info.type_id = type.type_id"
				+   "\n  AND info.document_num = " + docNum
				+   "\n WHERE type.type_name = 'Cancellation Doc Num'";
			Table cancelDocNumInDb = null;
			
			try {
				Logging.debug(String.format("Executing SQL query: %s", cancelDocNumQuery));
				cancelDocNumInDb = Table.tableNew("Existing cancellation doc num for doc" + docNum);
				int ret = DBaseTable.execISql(cancelDocNumInDb, cancelDocNumQuery);
				
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt()) {
					String message = DBUserTable.dbRetrieveErrorInfo(ret, "Error executing SQL " + 
							cancelDocNumQuery);
					throw new RuntimeException (message);
				}
				String cancelDocNum = cancelDocNumInDb.getString("cancel_doc_num", 1);
				return Integer.parseInt(cancelDocNum);
			} finally {
				cancelDocNumInDb = TableUtilities.destroy(cancelDocNumInDb);
			}
		}
		
		/**
		 * @description Set the value within the XML object
		 * @param 		field
		 * @param 		value
		 * @throws 		OException
		 */
		void updateXML(String field, String value) throws OException {
			String xmlData = getXmlData();
			VatInvOnlyNumbering cancellation = new VatInvOnlyNumbering();
			xmlData = cancellation.updateField(argt, xmlData, field, value);
			setXmlData(xmlData);
		}
	}
	
	private void initLog() {
		try {
			Logging.init(this.getClass(), _constRepo.getContext(), _constRepo.getSubcontext());
		} catch (Exception e) {
			// do something
		}
	}
}

