package com.olf.recon.rb.datasource.endurinvoiceextract;

import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.recon.enums.EndurDocumentInfoField;
import com.olf.recon.enums.EndurDocumentStatus;

/**
 * Return invoices that have transitioned through a "Cancelled" document status.
 * 
 * Custom invoice numbers for these are stored in "Cancellation Doc Num" and "Cancellation VAT Num" doc info fields.
 */
public class CancelledInvoices extends AbstractEndurInvoiceExtract
{
	private Table tblApplicableInvoices;
	private Table tblJMDocumentNumbers;
	
	public CancelledInvoices(int windowStartDate, int windowEndDate,String region ,Table tblOutputStructure) throws OException 
	{
		super(windowStartDate, windowEndDate, region,tblOutputStructure);
		
		/* Filter invoices for only those that were sent to CP */
		tblApplicableInvoices = getAllApplicableInvoices().copyTable();
		filterDocumentStatus(tblApplicableInvoices, EndurDocumentStatus.CANCELLED.id());
		
		/* Get a list of custom JM doc numbers for invoices that have been cancelled */
		int docInfos[] = new int[] { 
				EndurDocumentInfoField.CANCELLATION_DOC_NUM.toInt(),
				EndurDocumentInfoField.CANCELLATION_VAT_NUM.toInt()
		};
		
		tblJMDocumentNumbers = getHistoricDocumentNumbers(tblApplicableInvoices, docInfos);
	}
	
	@Override
	protected Table generateMergedInvoiceData() throws OException 
	{
		return generateMergedInvoiceData(EndurDocumentInfoField.CANCELLATION_VAT_NUM.toInt());
	}
	
	@Override
	protected Table getCashInvoices() throws OException 
	{
		return getCashInvoices(
				tblApplicableInvoices, 
				tblJMDocumentNumbers, 
				EndurDocumentInfoField.CANCELLATION_DOC_NUM.toInt());
	}
	
	@Override
	protected Table getTaxInvoices() throws OException
	{
		return getTaxInvoices(
				tblApplicableInvoices, 
				tblJMDocumentNumbers, 
				EndurDocumentInfoField.CANCELLATION_DOC_NUM.toInt(), 
				EndurDocumentInfoField.CANCELLATION_VAT_NUM.toInt());
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