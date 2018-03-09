package com.jm.accountingfeed.rb.output;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableRefDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableSalesLedgerDataColumns;
import com.jm.accountingfeed.enums.EndurDocumentStatus;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.jaxbbindings.salesledger.ObjectFactory;
import com.jm.accountingfeed.jaxbbindings.salesledger.Trade;
import com.jm.accountingfeed.jaxbbindings.salesledger.Trades;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder output plugin for 'Sales Ledger Extract' report.
 * 
 */
public class SalesLedgerOutput extends AccountingFeedOutput
{
    /**
     * This method reads the output data of 'Sales Ledger Extract' report.
     * Populates the class variable 'xmlData' with the 'Trades' xml
     */
	@Override
	public void cacheXMLData() throws OException
	{
		try
		{
			ObjectFactory objectFactory = new ObjectFactory();
			xmlData = objectFactory.createTrades();
			List<Trade> tradeList = ((Trades) xmlData).getTrade();

			int numRows = tblOutputData.getNumRows();
			for (int row = 1; row <= numRows; row++)
			{
				Trade trade = objectFactory.createTrade();

				String deskLocation = tblOutputData.getString("desk_location", row);
				trade.setDeskLocation(deskLocation);

				String dealNum = String.valueOf(tblOutputData.getInt("deal_num", row));
				trade.setTradeRef(dealNum);

				String invoiceNumber = String.valueOf(tblOutputData.getInt("invoice_number", row));
				trade.setInvoiceNumber(invoiceNumber);

				String docNum = String.valueOf(tblOutputData.getInt("endur_doc_num", row));
				trade.setEndurDocNo(docNum);

				String lastDocUpdate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("last_doc_update_time", row));
				trade.setLastDocUpdate(lastDocUpdate);

				String stldocHistoryId = String.valueOf(tblOutputData.getInt("stldoc_hdr_hist_id", row));
				trade.setStldocHistId(stldocHistoryId);

				String endurDocStatus = Ref.getShortName(SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE, tblOutputData.getInt("endur_doc_status", row));
				trade.setDocStatus(endurDocStatus);

				String cFlowType = Ref.getShortName(SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE, tblOutputData.getInt("cflow_type", row));
				trade.setBillingType(cFlowType);

				String internalPortfolio = Ref.getShortName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, tblOutputData.getInt("internal_portfolio", row));
				trade.setPortfolio(internalPortfolio);

				String internalPmmAccount = String.valueOf(tblOutputData.getInt("internal_bunit", row));
				trade.setIntPmmAccount(internalPmmAccount);

				String pmmAccount = String.valueOf(tblOutputData.getInt("external_bunit", row));
				trade.setPmmAccount(pmmAccount);

				String insType = Ref.getShortName(SHM_USR_TABLES_ENUM.INS_TYPE_TABLE, tblOutputData.getInt("ins_type", row));
				trade.setInsType(insType);
				
				String buySell = Ref.getShortName(SHM_USR_TABLES_ENUM.BUY_SELL_TABLE, tblOutputData.getInt("buy_sell", row));
				trade.setBuySell(buySell);

				String tradeType = tblOutputData.getString("trade_type", row);
				trade.setTradeType(tradeType);

				String tradeDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("trade_date", row));
				trade.setTradeDate(tradeDate);

				String reference = tblOutputData.getString("reference", row);
				trade.setCustomerRef(reference);

				String fixedFloat = Ref.getShortName(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, tblOutputData.getInt("fixed_float", row));
				trade.setFixedFloat(fixedFloat);

				String paymentDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("payment_date", row));
				trade.setPaymentDate(paymentDate);

				String invoiceDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("invoice_date", row));
				trade.setInvoiceDate(invoiceDate);

				String valueDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("value_date", row));
				trade.setValueDate(valueDate);

				String tradingLocation = tblOutputData.getString("trading_location", row);
				trade.setTradingLoc(tradingLocation);

				String fromCurrency = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblOutputData.getInt("from_currency", row));
				trade.setFromCurrency(fromCurrency);

				String uom = Ref.getShortName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, tblOutputData.getInt("uom", row));
				trade.setUnitOfMeasure(uom);

				String toCurrecny = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblOutputData.getInt("to_currency", row));
				trade.setToCurrency(toCurrecny);

				BigDecimal dealExchangeRate = Util.roundDoubleTo6DPS(tblOutputData.getDouble("deal_exchange_rate", row));
				trade.setDealExcRate(dealExchangeRate);

				BigDecimal positionUom = Util.roundPosition(tblOutputData.getDouble("position_uom", row));
				trade.setFromValue(positionUom);

				BigDecimal settleAmount = Util.roundCashAmount(tblOutputData.getDouble("settle_amount", row));
				trade.setToValue(settleAmount);

				BigDecimal positionToz = Util.roundPosition(tblOutputData.getDouble("position_toz", row));
				trade.setBaseWeight(positionToz);

				BigDecimal taxDealCurrency = Util.roundCashAmount(tblOutputData.getDouble("tax_in_deal_ccy", row));
				trade.setTaxDealCur(taxDealCurrency);

				String taxCurrency = tblOutputData.getString("tax_ccy", row);
				trade.setTaxCurrency(taxCurrency);

				BigDecimal taxExchangeRate = Util.roundDoubleTo6DPS(tblOutputData.getDouble("tax_exchange_rate", row));
				trade.setTaxExcRate(taxExchangeRate);

				BigDecimal taxReportingCurrency = Util.roundCashAmount(tblOutputData.getDouble("tax_in_tax_ccy", row));
				trade.setTaxRptCur(taxReportingCurrency);

				String taxCode = tblOutputData.getString("tax_code", row);
				trade.setTaxCode(taxCode);

				BigDecimal spotEquivPrice = Util.roundDoubleTo6DPS(tblOutputData.getDouble("spot_equivalent_price", row));
				trade.setSpotEquivPrc(spotEquivPrice);

				BigDecimal spotEquivValue = Util.roundCashAmount(tblOutputData.getDouble("spot_equivalent_value", row));
				trade.setSpotEquivVal(spotEquivValue);

				BigDecimal unitPrice = Util.roundDoubleTo6DPS(tblOutputData.getDouble("unit_price", row));
				trade.setUnitPrice(unitPrice);

				double interestRatOnLeaseeAsDouble = tblOutputData.getDouble("interest_rate_on_lease", row);
				BigDecimal interestRate = Util.roundCashAmount(interestRatOnLeaseeAsDouble);
				trade.setInterestRate(interestRate);

				String isCoverage = tblOutputData.getString("is_coverage", row);
				trade.setIsCoverage(isCoverage);
				
				String coverageText = tblOutputData.getString("coverage_text", row);
				trade.setCoverageText(coverageText);

				tradeList.add(trade);
			}
		} 
		catch (Exception e)
		{
			Util.printStackTrace(e);
			throw new AccountingFeedRuntimeException("Error whilst cache'ing XML extract data", e);
		}
	}

    /**
     * Sales ledger records have deal granularity.
     * This method inserts records in the boundary table at Endur doc number granularity by appending payload of multiple deals into one.
     */
	@Override
	public void extractAuditRecords() throws OException
	{
		Table tableToInsert = null;

		try
		{
			tableToInsert = Table.tableNew(ExtractionTableName.SALES_LEDGER.toString());

			int ret = DBUserTable.structure(tableToInsert);
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new AccountingFeedRuntimeException("Unable to get structure of: " + ExtractionTableName.SALES_LEDGER.toString());
			}

			int numRows = tblOutputData.getNumRows();

			/* API indicates a group by must be performed for a CLOB column set */
			tableToInsert.clearGroupBy();
			tableToInsert.addGroupBy("endur_doc_num");
			tableToInsert.groupBy();

			/* Map to store endurDocNum and table row # */
			HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
            /* Map to store docNum and SL Payload */
            HashMap<String, Trades> docNumPayload = getPayload();
            PluginLog.info("Created Map of DocNumber to all Trades of doc. Output rows=" + numRows + ", Num of EndurDocs=" + docNumPayload.size());
			
            JAXBContext jaxbContext = JAXBContext.newInstance(Trades.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
			for (int row = 1; row <= numRows; row++)
			{
				int endurDocNum = tblOutputData.getInt("endur_doc_num", row);
				int endurDocStatus = tblOutputData.getInt("endur_doc_status", row);
				String payLoad = "";
                Trades slTrades = docNumPayload.get(String.valueOf(endurDocNum));
                if(slTrades != null)
                {
                    StringWriter writer = new StringWriter();                  
                    marshaller.marshal(slTrades, writer);
                    payLoad = writer.toString();
                }
                else
                {
                    PluginLog.warn("Trades Payload not found for endurDocNum " + endurDocNum);
                }               
				
				boolean docAlreadyExtracted = map.containsKey(endurDocNum);
				
				if (!docAlreadyExtracted)
				{
					int newRow = tableToInsert.addRow();
					map.put(endurDocNum, newRow);
				}

				int rowToSet = map.get(endurDocNum);
				
				if (docAlreadyExtracted)
				{
					int existingDocStatus = tableToInsert.getInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), rowToSet);
					/* 
					 * If this is a cancelled document, set the doc_status to cancelled (this takes priority over a document
					 * that has been previously "Sent" 
					 */
					int newDocStatus = (endurDocStatus == EndurDocumentStatus.CANCELLED.id()) ? endurDocStatus : existingDocStatus;					
					tableToInsert.setInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), rowToSet, newDocStatus);
				}
				else
				{
					tableToInsert.setInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_NUM.toString(), rowToSet, endurDocNum);
					tableToInsert.setInt(BoundaryTableSalesLedgerDataColumns.ENDUR_DOC_STATUS.toString(), rowToSet, endurDocStatus);
					tableToInsert.setClob(BoundaryTableSalesLedgerDataColumns.PAYLOAD.toString(), rowToSet, payLoad);	
				}
			}
			
			/* Insert a new boundary table record in status "N" */
			if (tableToInsert.getNumRows() > 0)
			{
				int extractionId = getExtractionId();
				ODateTime timeIn = ODateTime.getServerCurrentDateTime();

				tableToInsert.setColValInt(BoundaryTableRefDataColumns.EXTRACTION_ID.toString(), extractionId);
				tableToInsert.setColValString(BoundaryTableRefDataColumns.REGION.toString(), reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()));
				tableToInsert.setColValDateTime(BoundaryTableRefDataColumns.TIME_IN.toString(), timeIn);

				String auditRecordStatusString = null; 
				if (null == errorDetails || errorDetails.isEmpty())
				{
					auditRecordStatusString = AuditRecordStatus.NEW.toString();
				}
				else
				{
					auditRecordStatusString = AuditRecordStatus.ERROR.toString();

					tableToInsert.setColValString(BoundaryTableRefDataColumns.ERROR_MSG.toString(), errorDetails);
				}

				tableToInsert.setColValString(BoundaryTableRefDataColumns.PROCESS_STATUS.toString(), auditRecordStatusString);

				ret = DBUserTable.insert(tableToInsert);
				if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				{
					throw new AccountingFeedRuntimeException("Unable to insert record into: " + ExtractionTableName.SALES_LEDGER.toString());
				}				
			}
		}
		catch (Exception exception)
		{
			String message = "Exception occurred while extracting records.\n" + exception.getMessage();
			PluginLog.error(message);
			PluginLog.info(message);
			Util.printStackTrace(exception);
			throw new AccountingFeedRuntimeException(message, exception);
		}
		finally
		{
			if (tableToInsert != null)
			{
				tableToInsert.destroy();
			}
		}
	}

    /**
     * Reads the Trades of the xmlData.
     * Create a Map of Endur Doc Num to all Trades of doc 
     * @return
     */
    private HashMap<String, Trades> getPayload()
    {
        HashMap<String, Trades> docNumPayload = new HashMap<>();
        List<Trade> tradeList = ((Trades) xmlData).getTrade();
        
        ObjectFactory objectFactory = new ObjectFactory();

        for(Trade trade: tradeList)
        {
            String endurDocNum = trade.getEndurDocNo();
            Trades payload;
            if(docNumPayload.containsKey(endurDocNum))
            {
                payload = docNumPayload.get(endurDocNum);   
            }
            else
            {
                payload = objectFactory.createTrades();
                docNumPayload.put(endurDocNum, payload);
            }
            payload.getTrade().add(trade);
        }        
        return docNumPayload;       
    }
    
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.jm.accountingfeed.rb.output.AccountingFeedOutput#getRootClass()
	 */
	@Override
	public Class<?> getRootClass()
	{
		return Trades.class;
	}
}
