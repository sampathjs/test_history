package com.jm.accountingfeed.rb.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableGeneralLedgerDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableRefDataColumns;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentAmountType;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentHeaderType;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentItemType;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentPostingRequestType;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentQuantityType;
import com.jm.accountingfeed.jaxbindings.shanghai.AccountingDocumentType;
import com.jm.accountingfeed.jaxbindings.shanghai.BusinessPartnerIDWithCategoryType;
import com.jm.accountingfeed.jaxbindings.shanghai.DocumentExchangeRateType;
import com.jm.accountingfeed.jaxbindings.shanghai.EnterpriseMessageHeaderType;
import com.jm.accountingfeed.jaxbindings.shanghai.MessageHeaderBusinessScopeType;
import com.jm.accountingfeed.jaxbindings.shanghai.MessageHeaderDataEntityType;
import com.jm.accountingfeed.jaxbindings.shanghai.MessageHeaderReceiverType;
import com.jm.accountingfeed.jaxbindings.shanghai.MessageHeaderSenderType;
import com.jm.accountingfeed.jaxbindings.shanghai.ObjectFactory;
import com.jm.accountingfeed.jaxbindings.shanghai.TaxDetailsType;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.constrepository.ConstRepository;
import com.openlink.util.constrepository.ConstantNameException;
import com.openlink.util.constrepository.ConstantTypeException;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder output plugin for 'Shanghai General Ledger' report.
 * @author jwaechter
 *
 */
public class ShanghaiGeneralLedgerOutput extends AccountingFeedOutput
{
    /**
     * This method reads the output data of 'General Ledger Extract' report.
     * Populates the class variable 'xmlData' with the 'Trades' xml
     */
	@Override
	public void cacheXMLData() throws OException
	{
	    try
	    {
			String region = reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString());
			tblOutputData.group("company_id, document_type, document_currency, document_reference, endur_doc_num, endur_doc_status, grouping_document, grouping_item");
	        ObjectFactory objectFactory = new ObjectFactory();
	        String elementValue;
	        xmlData = objectFactory.createAccountingDocumentPostingRequestType();
	        AccountingDocumentPostingRequestType doc = (AccountingDocumentPostingRequestType) xmlData;

	        EnterpriseMessageHeaderType header =  objectFactory.createEnterpriseMessageHeaderType();
	        fillMessageHeader(objectFactory, header);
	        doc.setMessageHeader(header);
	        
	        List<AccountingDocumentType> accountingDocs = doc.getAccountingDocument();
	       
	        // possibly change class to  AccountingDocumentType.class? 
	        JAXBContext jaxbContext = JAXBContext.newInstance(AccountingDocumentType.class);
	        Marshaller marshaller = jaxbContext.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

	        tblOutputData.addCol("payload", COL_TYPE_ENUM.COL_CLOB);
	        int numberOfRowsInArgTblReportBuilderOutput = tblOutputData.getNumRows();

	        Set<Integer> rowsToDelete = new TreeSet<>();
	        for (int i = 1; i <= numberOfRowsInArgTblReportBuilderOutput; i++)
	        {
	        	AccountingDocumentType glData = objectFactory.createAccountingDocumentType();
	        	
	        	AccountingDocumentHeaderType accHeader = objectFactory.createAccountingDocumentHeaderType();
	        	glData.setHeader(accHeader);
	        	
	            createAccountingDocumentHeader(objectFactory, i, accHeader);
	            List<AccountingDocumentItemType> items = glData.getItem();
	            
	            int rowNumLastOfItemGroup =  getLastRowNumOfItemGroup (i, numberOfRowsInArgTblReportBuilderOutput);
	            int rowNumFirstOfItemGroup = i;
	            for (int itemRowNum = i; itemRowNum < rowNumLastOfItemGroup; itemRowNum++) {
	            	int lastTaxGroupRow = sumTaxItemsBasedOnGrouping(itemRowNum, rowNumLastOfItemGroup);
	            	for (int k = itemRowNum+1; k < lastTaxGroupRow; k++) {
	            		rowsToDelete.add(k);
	            	}
	            	itemRowNum = lastTaxGroupRow;
	            	if (itemRowNum > i) {
	            		rowsToDelete.add(itemRowNum);
	            	}
		            AccountingDocumentItemType item = 
		            		objectFactory.createAccountingDocumentItemType();
		            
		            elementValue = tblOutputData.getString("item_category", itemRowNum);
		            item.setCategory(elementValue);
		            
		    		elementValue = tblOutputData.getString("item_material", i);
		    		item.setMaterial(elementValue);

		            AccountingDocumentAmountType itemDocumentCurrencyAmount = objectFactory.createAccountingDocumentAmountType();
		            itemDocumentCurrencyAmount.setValue(round(new BigDecimal(Double.toString(tblOutputData.getDouble("item_document_currency_amount", itemRowNum))), 2, true));
		            elementValue = tblOutputData.getString("item_currency_code", itemRowNum);
		            itemDocumentCurrencyAmount.setCurrencyCode(elementValue);
		            item.setDocumentCurrencyAmount(itemDocumentCurrencyAmount);
		            
		            elementValue = tblOutputData.getString("item_debit_credit_indicator", itemRowNum);
		            item.setDebitCreditIndicator(elementValue);
		            
		            BusinessPartnerIDWithCategoryType businessPartner = objectFactory.createBusinessPartnerIDWithCategoryType();
		            elementValue = tblOutputData.getString("item_businesspartner_id", itemRowNum);
		            businessPartner.setID(elementValue);
		            elementValue = tblOutputData.getString("item_businesspartner_category", itemRowNum);
		            businessPartner.setCategory(elementValue);
		            item.setBusinessPartner(businessPartner);

		            AccountingDocumentQuantityType quantity = objectFactory.createAccountingDocumentQuantityType();
					
		            quantity.setValue(round(new BigDecimal(Double.toString(tblOutputData.getDouble("item_quantity", itemRowNum))), 3, true));
		            elementValue = tblOutputData.getString("item_quantity_unit_code", itemRowNum);
		            quantity.setUnitCode(elementValue);
		            item.setQuantity(quantity);

		            elementValue = tblOutputData.getString("item_general_ledger_account", itemRowNum);
		        	if (elementValue != null && elementValue.trim().length() > 0) {
			            item.setGeneralLedgerAccount(elementValue);		        		
		        	}
		            
		            elementValue = tblOutputData.getString("item_value_date", itemRowNum);
		            item.setValueDate(elementValue);
		            
		            elementValue = tblOutputData.getString("item_baseline_date", itemRowNum);
		            item.setBaselineDate(elementValue);
		            
		            elementValue = tblOutputData.getString("item_assignment", itemRowNum);
		            item.setAssignment(elementValue);
		            
		            elementValue = tblOutputData.getString("item_profit_centre_id", itemRowNum);
		            item.setProfitCentreID(elementValue);		            
		            
		            TaxDetailsType taxDetails = objectFactory.createTaxDetailsType();
		            elementValue = tblOutputData.getString("item_tax_details_tax_code", itemRowNum);
		            taxDetails.setTaxCode(elementValue);
		            
		            double tddcba = tblOutputData.getDouble("item_tax_details_document_currency_base_amount", itemRowNum);
		            if (tddcba != 0.0) {
			            AccountingDocumentAmountType taxDetailsDocumentCurrencyBaseAmount = objectFactory.createAccountingDocumentAmountType();
			            taxDetailsDocumentCurrencyBaseAmount.setValue(round (new BigDecimal(Double.toString(tddcba)), 2, false));
			            elementValue = tblOutputData.getString("item_tax_details_document_currency_base_amount_currency_code", itemRowNum);
			            taxDetailsDocumentCurrencyBaseAmount.setCurrencyCode(elementValue);
			            taxDetails.setDocumentCurrencyBaseAmount(taxDetailsDocumentCurrencyBaseAmount);		            	
		            }
		            double tddcta = tblOutputData.getDouble("item_tax_details_document_currency_tax_amount", itemRowNum);
		            if (tddcta != 0.0) {
			            AccountingDocumentAmountType taxDetailsDocumentCurrencyTaxAmount = objectFactory.createAccountingDocumentAmountType();
			            taxDetailsDocumentCurrencyTaxAmount.setValue(round (new BigDecimal(Double.toString(tddcta)), 2, false));
			            taxDetailsDocumentCurrencyTaxAmount.setCurrencyCode(elementValue);
			            elementValue = tblOutputData.getString("item_tax_details_document_currency_tax_amount_currency_code", itemRowNum);
			            taxDetails.setDocumentCurrencyTaxAmount(taxDetailsDocumentCurrencyTaxAmount);
		            }
		            
		    		elementValue = tblOutputData.getString("itemreferencekeytwo", i);
		    		if (elementValue != null && elementValue.trim().length() > 0) {
		    			item.setReferenceKeyTwo(elementValue);
		    		}
		    		
		    		elementValue = tblOutputData.getString("itemnote", i);
		    		if (elementValue != null && elementValue.trim().length() > 0) {
		    			item.setNote(elementValue);
		    		}

		            item.setTaxDetails(taxDetails);
		            items.add(item);
	            }
	            i  = rowNumLastOfItemGroup-1;
	            StringWriter stringWriter = new StringWriter();
	            marshaller.marshal(glData, stringWriter);
	            String payLoad = stringWriter.toString();

	            tblOutputData.setClob("payload", rowNumFirstOfItemGroup, payLoad);

	            accountingDocs.add(glData);
	        }
	        // remove table rows of group members
	        for (int i=numberOfRowsInArgTblReportBuilderOutput; i > 0;i--) {
	        	if (rowsToDelete.contains(i)) {
	        		tblOutputData.delRow(i);
	        	}
	        }
	    }
	    catch(OException oe)
	    {
	    	PluginLog.error("Failed to create XML data Marshaller." + oe.getMessage());
	    	Util.printStackTrace( oe );
            throw new AccountingFeedRuntimeException("Error whilst cache'ing XML extract data", oe);
	    } 
	    catch (JAXBException je) 
	    {
	        PluginLog.error("Failed to initialize Marshaller." + je.getMessage());
	        Util.printStackTrace( je );
	        throw new AccountingFeedRuntimeException("Error whilst cache'ing XML extract data", je);
	    }
	}

	/**
	 * Sums up the fields "ItemDocumentCurrencyAmount", "ItemTaxDetailsDocumentCurrencyBaseAmount" and "ItemTaxDetailsDocumentCurrencyTaxAmount".
	 * Also sets the ccy code fields for the amounts on all grouped rows.
	 * @param itemRowNum
	 * @param rowNumLastOfItemGroup
	 * @return
	 * @throws OException 
	 */
	private int sumTaxItemsBasedOnGrouping(int itemRowNum,
			int rowNumLastOfItemGroup) throws OException {
		if (itemRowNum+1 == rowNumLastOfItemGroup ) {
			return itemRowNum;
		}
		double sumDocCcyAmount = 0.0;
		double sumDocCcyBaseAmount = 0.0;
		double sumDocCcyTaxAmount = 0.0;
		String docCcyAmountCcyCode = "";
		String docCcyBaseAmountCcyCode = "";
		String docCcyTaxAmountCcyCode = "";
		int grouping = tblOutputData.getInt("grouping_item", itemRowNum);
		int endurDocNum = tblOutputData.getInt("endur_doc_num", itemRowNum);
		int endurDocStatus = tblOutputData.getInt("endur_doc_status", itemRowNum);
		int lastOfTaxGroup = itemRowNum;
		for (int i = itemRowNum; i < rowNumLastOfItemGroup; i++) {
			int groupingNextRow = tblOutputData.getInt("grouping_item", i);
			int endurDocNumNextRow = tblOutputData.getInt("endur_doc_num", i);
			int endurDocStatusNextRow = tblOutputData.getInt("endur_doc_status", i);

			if (groupingNextRow != grouping || endurDocNumNextRow != endurDocNum || endurDocStatusNextRow != endurDocStatus) {
				break;
			} else {
				lastOfTaxGroup = i;
			}
			double docCcyAmount = tblOutputData.getDouble("item_document_currency_amount", i);
			double docCcyBaseAmount = tblOutputData.getDouble("item_tax_details_document_currency_base_amount", i);
            double docCcyTaxAmount = tblOutputData.getDouble("item_tax_details_document_currency_tax_amount", i);
            String docCcyAmountCcyCode2 = tblOutputData.getString("item_currency_code", i);
            String docCcyBaseAmountCcyCode2 = tblOutputData.getString("item_tax_details_document_currency_base_amount_currency_code", i);
            String docCcyTaxAmountCcyCode2 = tblOutputData.getString("item_tax_details_document_currency_tax_amount_currency_code", i);
            if (docCcyAmountCcyCode2 != null && docCcyAmountCcyCode2.trim().length() > 0) {
            	docCcyAmountCcyCode = docCcyAmountCcyCode2;
            }
            if (docCcyBaseAmountCcyCode2 != null && docCcyBaseAmountCcyCode2.trim().length() > 0) {
            	docCcyBaseAmountCcyCode = docCcyBaseAmountCcyCode2;
            }
            if (docCcyTaxAmountCcyCode2 != null && docCcyTaxAmountCcyCode2.trim().length() > 0) {
            	docCcyTaxAmountCcyCode = docCcyTaxAmountCcyCode2;
            }
            sumDocCcyAmount += docCcyAmount;
            sumDocCcyBaseAmount += docCcyBaseAmount;
            sumDocCcyTaxAmount += docCcyTaxAmount;
		}
		if (lastOfTaxGroup == itemRowNum) {
			return itemRowNum;
		}
		tblOutputData.setDouble("item_document_currency_amount", lastOfTaxGroup, sumDocCcyAmount) ;
		tblOutputData.setDouble("item_tax_details_document_currency_base_amount", lastOfTaxGroup, sumDocCcyBaseAmount);
		tblOutputData.setDouble("item_tax_details_document_currency_tax_amount", lastOfTaxGroup, sumDocCcyTaxAmount);
		tblOutputData.setString("item_currency_code", lastOfTaxGroup, docCcyAmountCcyCode);
		tblOutputData.setString("item_tax_details_document_currency_base_amount_currency_code", lastOfTaxGroup, docCcyBaseAmountCcyCode);
		tblOutputData.setString("item_tax_details_document_currency_tax_amount_currency_code", lastOfTaxGroup, docCcyTaxAmountCcyCode);
		return lastOfTaxGroup;
	}

	private int getLastRowNumOfItemGroup(int startRowNum, int numberOfRowsInArgTblReportBuilderOutput) throws OException {
		int grouping = tblOutputData.getInt("grouping_document", startRowNum);
		String companyId = tblOutputData.getString("company_id", startRowNum);
		String documentType = tblOutputData.getString("document_type", startRowNum);
		String documentCurrency = tblOutputData.getString("document_currency", startRowNum);
		int documentReference = tblOutputData.getInt("document_reference", startRowNum);
		int endurDocNum = tblOutputData.getInt("endur_doc_num", startRowNum);
		// used to count how many rows of the same deal are being found at the end of the 
		int endurDocStatus = tblOutputData.getInt("endur_doc_status", startRowNum);
		// list of rows
		int equalsCounter = 0;
		
		for (int i = startRowNum+1; i <= numberOfRowsInArgTblReportBuilderOutput; i++) {
			int groupingOther = tblOutputData.getInt("grouping_document", i);
			String companyIdOtherRow = tblOutputData.getString("company_id", i);
			String documentTypeOtherRow = tblOutputData.getString("document_type", i);
			String documentCurrencyOtherRow = tblOutputData.getString("document_currency", i);
			int documentReferenceOtherRow = tblOutputData.getInt("document_reference", i);
			int endurDocNumOtherRow = tblOutputData.getInt("endur_doc_num", i);
			int endurDocStatusOtherRow = tblOutputData.getInt("endur_doc_status", i);
			equalsCounter++;
			if (!companyIdOtherRow.equals(companyId)
				|| !documentTypeOtherRow.equals(documentType)
				|| !documentCurrencyOtherRow.equals(documentCurrency)
				|| documentReferenceOtherRow != documentReference
				|| endurDocNumOtherRow != endurDocNum 
				|| endurDocStatusOtherRow != endurDocStatus
				|| groupingOther != grouping) {
				return i;
			}
		}
		return startRowNum+equalsCounter+1;
	}

	private void createAccountingDocumentHeader(ObjectFactory objectFactory, int i,
			AccountingDocumentHeaderType accHeader) throws OException {
		String elementValue;
		
		elementValue = tblOutputData.getString("reference_id", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setReferenceID(elementValue);
		}
		
		elementValue = tblOutputData.getString("company_id", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setCompanyID(elementValue);
		}

		elementValue = tblOutputData.getString("document_type", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setDocumentType(elementValue);
		}

		elementValue = tblOutputData.getString("user_id", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setUserID(elementValue);
		}
		
		elementValue = tblOutputData.getString("document_currency", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setDocumentCurrency(elementValue);
		}

		DocumentExchangeRateType exchRate = objectFactory.createDocumentExchangeRateType();
		elementValue = tblOutputData.getString("exchange_rate", i);
		if (elementValue.trim().length() > 0) {
			BigDecimal bd = new BigDecimal(elementValue);
			bd = round(bd, 5, true);
			exchRate.setValue(bd);
			elementValue = tblOutputData.getString("exchange_rate_type", i);
			exchRate.setType(elementValue);
			accHeader.setExchangeRate(exchRate);
		} else {
			// do nothing, as we are not outputting fx rates of 1 (field not mandatory)
			// set to 1 if exchange rate becomes mandatory again.
		}
		
		elementValue = tblOutputData.getString("posting_date", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setPostingDate(elementValue);
		}
		
		elementValue = tblOutputData.getString("document_date", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setDocumentDate(elementValue);
		}
		
		if (tblOutputData.getInt("document_reference", i) >= 0) {
			elementValue = Integer.toString(tblOutputData.getInt("document_reference", i));
			accHeader.setDocumentReference(elementValue);			
		}
		
		elementValue = tblOutputData.getString("referencekeyone", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setReferenceKeyOne(elementValue);			
		}

		elementValue = tblOutputData.getString("referencekeytwo", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setReferenceKeyTwo(elementValue);
		}
		
		elementValue = tblOutputData.getString("note", i);
		if (elementValue != null && elementValue.trim().length() > 0) {
			accHeader.setNote(elementValue);
		}
	}

	private void fillMessageHeader(ObjectFactory objectFactory, EnterpriseMessageHeaderType header) throws OException {
		if (tblOutputData.getNumRows() <= 0) {
			return;
		}
		
		String elementValue = tblOutputData.getString("message_header_exchange_id", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			header.setMessageExchangeID(elementValue);
		}

		elementValue = tblOutputData.getString("message_header_acknowledgement_request", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			header.setAcknowledgementRequest(elementValue);
		}
		
		elementValue = tblOutputData.getString("message_header_creation_datetime", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			header.setCreationDateTime(elementValue);
		}
		MessageHeaderBusinessScopeType headerScopeType = objectFactory.createMessageHeaderBusinessScopeType();

		elementValue = tblOutputData.getString("message_header_business_scope_integration_flow_id", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			headerScopeType.setIntegrationFlowID(elementValue);
		}

		MessageHeaderDataEntityType det = objectFactory.createMessageHeaderDataEntityType();
		elementValue = tblOutputData.getString("message_header_data_entity_type", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			det.setType(elementValue);
		}
		headerScopeType.setDataEntity(det);
		if (headerScopeType.getDataEntity() != null 
				|| headerScopeType.getIntegrationFlowID() != null 
				|| headerScopeType.getTaskID() != null) {
			header.setBusinessScope(headerScopeType);			
		}
		
		elementValue = tblOutputData.getString("message_header_sender_logical_id", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			MessageHeaderSenderType mhst = objectFactory.createMessageHeaderSenderType();
			mhst.setLogicalID(elementValue);
			header.setSender(mhst);
		}
		
		elementValue = tblOutputData.getString("message_header_receiver_logical_id", 1);
		if (elementValue != null && elementValue.trim().length() > 0) {
			MessageHeaderReceiverType mhrt = objectFactory.createMessageHeaderReceiverType();
			mhrt.setLogicalID(elementValue);
			header.getReceiver().add(mhrt);
		}
	}

    /**
     * Insert the Audit boundary table for General Ledger extract with the 'Trade' payload xml for every Deal in the Report output 
     */
	@Override
	public void extractAuditRecords() throws OException
	{
		Table tableToInsert = null;
		try
		{
			tableToInsert = Table.tableNew(reportParameter.getStringValue("boundary_table"));
			int numRows = tblOutputData.getNumRows();
			ODateTime timeIn = ODateTime.getServerCurrentDateTime();
			
			String auditRecordStatusString = null; 
			
			DBUserTable.structure(tableToInsert);
			
			tableToInsert.addNumRows(numRows);
			
			tableToInsert.setColValInt(BoundaryTableRefDataColumns.EXTRACTION_ID.toString(), getExtractionId());
			tableToInsert.setColValString(BoundaryTableRefDataColumns.REGION.toString(), reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()));
			tableToInsert.setColValDateTime(BoundaryTableRefDataColumns.TIME_IN.toString(), timeIn);
			
            for (int row = 1; row <= numRows; row++)			
            {
                int dealNum = tblOutputData.getInt("deal_tracking_num", row);
                int tranNum = tblOutputData.getInt("tran_num", row);
                int tranStatus = tblOutputData.getInt("tran_status", row);
                String payLoad = tblOutputData.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row);             
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.DEAL_NUM.toString(), row, dealNum);
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.TRAN_NUM.toString(), row, tranNum);
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.TRAN_STATUS.toString(), row, tranStatus);
                tableToInsert.setClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row, payLoad);
			}
            for (int row = numRows; row > 0; row--) {
                String clob = tableToInsert.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row);
                if (clob == null || clob.trim().length() == 0) {
                	tableToInsert.delRow(row);
                }
            }
			if(null == errorDetails || errorDetails.isEmpty())
			{
				auditRecordStatusString = AuditRecordStatus.NEW.toString();
			}
			else
			{
				auditRecordStatusString = AuditRecordStatus.ERROR.toString();
				tableToInsert.setColValString(BoundaryTableRefDataColumns.ERROR_MSG.toString(), errorDetails);
			}
			tableToInsert.setColValString(BoundaryTableRefDataColumns.PROCESS_STATUS.toString(), auditRecordStatusString);
            tableToInsert.clearGroupBy();
            tableToInsert.addGroupBy(BoundaryTableGeneralLedgerDataColumns.DEAL_NUM.toString());
            tableToInsert.groupBy();
			int retval = DBUserTable.insert(tableToInsert);
			if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
			    PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
			}
			tableToInsert.destroy();
		}
		catch (OException oException)
		{
			String message = "Exception occurred while extracting records.\n" + oException.getMessage();
			PluginLog.error(message);
			Util.printStackTrace(oException);
			throw oException;
		}
		finally
		{
			if (tableToInsert != null)
			{
				tableToInsert.destroy();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.jm.accountingfeed.rb.output.AccountingFeedOutput#getRootClass()
	 */
	@Override
	public Class<?> getRootClass()
	{
		return AccountingDocumentPostingRequestType.class;
	}
	
	@Override
	protected void generateXMLOutputFile() throws OException, JAXBException	{
		super.generateXMLOutputFile();
		ConstRepository constRep = new ConstRepository("Accounting", "Shanghai");
		String value = "";
		try {
			value = constRep.getStringValue("ShowOutputFileInTableViewer", "false");
		} catch (ConstantTypeException | ConstantNameException | OException e) {
			throw new RuntimeException ("Error retrieving value from Constants Repository", e);
		} 
		if (Boolean.parseBoolean(value)) {
			showOutputFileContentInTableViewer();			
		}
	}

	protected void showOutputFileContentInTableViewer() throws OException {
		boolean isSuccessful;

		String targetDirectoryPathString = reportParameter.getStringValue(ReportBuilderParameter.TARGET_DIR.toString());
		String targetFileName = reportParameter.getStringValue(ReportBuilderParameter.TARGET_FILENAME.toString());

		PluginLog.info("Started generating XML output file " + targetFileName + " under directory " + targetDirectoryPathString);
		
		targetDirectoryPathString = targetDirectoryPathString.trim();

		File targetDirectory = new File(targetDirectoryPathString);
		File file = new File(targetDirectoryPathString + "/" + targetFileName);

		if (!targetDirectory.exists())
		{
			PluginLog.info("Directory doesn't exist: " + targetDirectory);
			isSuccessful = targetDirectory.mkdirs();

			if (isSuccessful)
			{
				PluginLog.debug("Directories created successfully. Path:" + targetDirectoryPathString);
			}
			else
			{
				String message = "Directories not created.Path: " + targetDirectoryPathString;
				PluginLog.error(message);
				throw new AccountingFeedRuntimeException(message);
			}
		}
		Table debugTable = Table.tableNew("Content of output file '" + file.getName() + "'");
		debugTable.addCol("textline", COL_TYPE_ENUM.COL_STRING);
		
		
		try (InputStream is = new FileInputStream(file);
			BufferedReader buf = new BufferedReader(new InputStreamReader(is));){
			String line = buf.readLine();  
			while(line != null){
				int rowNum = debugTable.addRow();
				debugTable.setString("textline", rowNum, line);
				line = buf.readLine();
			}
			debugTable.viewTable();
		} catch (IOException e) {
			PluginLog.info("Error: creating debug table failed: " + e);
		}
	}
	
	public static BigDecimal round(BigDecimal d, int scale, boolean roundUp) {
		  int mode = (roundUp) ? BigDecimal.ROUND_HALF_UP : BigDecimal.ROUND_HALF_DOWN;
		  return d.setScale(scale, mode);
	}
}