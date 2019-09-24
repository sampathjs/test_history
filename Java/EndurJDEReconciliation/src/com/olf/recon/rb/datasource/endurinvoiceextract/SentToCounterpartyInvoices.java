package com.olf.recon.rb.datasource.endurinvoiceextract;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.recon.enums.EndurDocumentInfoField;
import com.olf.recon.enums.EndurDocumentStatus;

/**
 * Return invoices that have transitioned through a "Sent 2 CP" document status.
 * 
 * Custom invoice numbers for these are stored in "Our Doc Num" and "VAT Invoice Doc Num" doc info fields.
 */
public class SentToCounterpartyInvoices extends AbstractEndurInvoiceExtract
{
	private Table tblApplicableInvoices;
	private Table tblJMDocumentNumbers;
	
	public SentToCounterpartyInvoices(int windowStartDate, int windowEndDate, String region,Table tblOutputStructure) throws OException 
	{
		super(windowStartDate, windowEndDate, region, tblOutputStructure);
		
		/* Filter invoices for only those that were sent to CP */
		tblApplicableInvoices = getAllApplicableInvoices().copyTable();
		filterDocumentStatus(tblApplicableInvoices, EndurDocumentStatus.SENT_TO_COUNTERPARTY.id());
		
		/* Get a list of custom JM doc numbers for invoices that have been sent to the counterparty */
		int docInfos[] = new int[] { 
				EndurDocumentInfoField.OUR_DOC_NUM.toInt(),
				EndurDocumentInfoField.VAT_INVOICE_DOC_NUM.toInt()
		};
		
		tblJMDocumentNumbers = getHistoricDocumentNumbers(tblApplicableInvoices, docInfos);
	}
	
	@Override
	protected Table generateMergedInvoiceData() throws OException 
	{
		return generateMergedInvoiceData(EndurDocumentInfoField.VAT_INVOICE_DOC_NUM.toInt());
	}
	
	@Override
	protected Table getCashInvoices() throws OException 
	{
		return getCashInvoices(
				tblApplicableInvoices, 
				tblJMDocumentNumbers, 
				EndurDocumentInfoField.OUR_DOC_NUM.toInt());
	}
	
	@Override
	protected Table getTaxInvoices() throws OException
	{
		return getTaxInvoices(
				tblApplicableInvoices, 
				tblJMDocumentNumbers, 
				EndurDocumentInfoField.OUR_DOC_NUM.toInt(), 
				EndurDocumentInfoField.VAT_INVOICE_DOC_NUM.toInt());
	}
	
	@Override
	protected void cleanup() throws OException 
	{
		super.cleanup();
		
		if (tblApplicableInvoices != null)
		{
			tblApplicableInvoices.destroy();
		}
		
		if (tblJMDocumentNumbers != null)
		{
			tblJMDocumentNumbers.destroy();
		}
	}
}
