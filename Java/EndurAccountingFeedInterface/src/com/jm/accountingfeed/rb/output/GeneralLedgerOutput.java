package com.jm.accountingfeed.rb.output;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableGeneralLedgerDataColumns;
import com.jm.accountingfeed.enums.BoundaryTableRefDataColumns;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.jaxbbindings.generalledger.ObjectFactory;
import com.jm.accountingfeed.jaxbbindings.generalledger.Trade;
import com.jm.accountingfeed.jaxbbindings.generalledger.Trades;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder output plugin for 'Reference data Extract' report.
 * @author SharmV03
 *
 */
public class GeneralLedgerOutput extends AccountingFeedOutput
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
	        ObjectFactory objectFactory = new ObjectFactory();
	        String elementValue;
	        xmlData = objectFactory.createTrades();
	        List<Trade> glDataList = ((Trades) xmlData).getTrade();

	        JAXBContext jaxbContext = JAXBContext.newInstance(Trade.class);
	        Marshaller marshaller = jaxbContext.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

	        tblOutputData.addCol("payload", COL_TYPE_ENUM.COL_CLOB);
	        int numberOfRowsInArgTblReportBuilderOutput = tblOutputData.getNumRows();

	        for (int i = 1; i <= numberOfRowsInArgTblReportBuilderOutput; i++)
	        {
	            Trade glData = objectFactory.createTrade();

	            elementValue = tblOutputData.getString("desk_location", i);
	            glData.setDeskLocation(elementValue);

	            elementValue = String.valueOf(tblOutputData.getInt("deal_num", i));
	            glData.setTradeRef(elementValue);

	            elementValue = Ref.getShortName(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, tblOutputData.getInt("tran_status", i));
	            glData.setTradeStatus(elementValue);

	            elementValue = String.valueOf(tblOutputData.getInt("extpartyid", i));
	            glData.setPmmAccount(elementValue);

	            elementValue = String.valueOf(tblOutputData.getInt("intpartyid", i));
	            glData.setIntPmmAccount(elementValue);

	            elementValue = Ref.getShortName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, tblOutputData.getInt("Portfolio", i)); 
	            glData.setPortfolio(elementValue);

	            elementValue = tblOutputData.getString("ins_type", i);
	            glData.setInsType(elementValue);

	            elementValue = tblOutputData.getString("trade_type", i);
	            glData.setTradeType(elementValue);

	            elementValue = Ref.getName(SHM_USR_TABLES_ENUM.FX_FLT_TABLE, tblOutputData.getInt("fixed_float", i));
	            glData.setFixedFloat(elementValue);

	            int reverseGL = tblOutputData.getInt("reversegl", i);
	            elementValue = (reverseGL == 1) ? "Y" : "N";
	            glData.setReverseGL(elementValue);

	            elementValue = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("trade_date", i));
	            glData.setTradeDate(elementValue);

	            elementValue = tblOutputData.getString("tradinglocation", i);
	            glData.setTradingLoc(elementValue);

	            elementValue = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblOutputData.getInt("from_currency", i)); 
	            glData.setFromCurrency(elementValue);

                elementValue = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblOutputData.getInt("to_currency", i)); 
	            glData.setToCurrency(elementValue);

	            elementValue = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("value_date", i));
	            glData.setValueDate(elementValue);

	            BigDecimal fromValue = Util.roundPosition(tblOutputData.getDouble("position_uom", i));
	            glData.setFromValue(fromValue);

	            BigDecimal toValue = Util.roundCashAmount(tblOutputData.getDouble("cash_amount", i));
	            glData.setToValue(toValue);

	            BigDecimal excRate = Util.roundDoubleTo6DPS(tblOutputData.getDouble("exchange_rate", i));
	            glData.setDealExcRate(excRate);

	            elementValue = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, tblOutputData.getInt("uom", i));
	            glData.setUnitOfMeasure(elementValue);

	            BigDecimal baseWeight = Util.roundPosition(tblOutputData.getDouble("position_toz", i));
	            glData.setBaseWeight(baseWeight);

	            BigDecimal spotEqPrice = Util.roundDoubleTo6DPS(tblOutputData.getDouble("spotequivprc", i));
	            glData.setSpotEquivPrc(spotEqPrice);

	            BigDecimal spotEqVal = Util.roundCashAmount(tblOutputData.getDouble("spotequivval", i));
	            glData.setSpotEquivVal(spotEqVal);

	            BigDecimal unitPrice = Util.roundDoubleTo6DPS(tblOutputData.getDouble("unitprice", i));
	            glData.setUnitPrice(unitPrice);

	            elementValue = tblOutputData.getString("reference", i);
	            glData.setCustomerRef(elementValue);

	            BigDecimal intRate = Util.roundCashAmount(tblOutputData.getDouble("interestrate", i));
	            glData.setInterestRate(intRate);

	            elementValue = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("paymentdate", i));
	            glData.setPaymentDate(elementValue);

	            elementValue = tblOutputData.getString("is_coverage", i);
	            glData.setIsCoverage(elementValue);
	            
	            elementValue = tblOutputData.getString("coverage_text", i);
	            glData.setCoverageText(elementValue);

	            StringWriter stringWriter = new StringWriter();
	            marshaller.marshal(glData, stringWriter);
	            String payLoad = stringWriter.toString();

	            tblOutputData.setClob("payload", i, payLoad);

	            glDataList.add(glData);
	        }
	    }
	    catch(OException oe)
	    {
	    	PluginLog.error("Failed to creating XML data Marshaller." + oe.getMessage());
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
     * Insert the Audit boundary table for General Ledger extract with the 'Trade' payload xml for every Deal in the Report output 
     */
	@Override
	public void extractAuditRecords() throws OException
	{
		Table tableToInsert = null;
		try
		{
			tableToInsert = Table.tableNew(ExtractionTableName.GENERAL_LEDGER.toString());
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
                int dealNum = tblOutputData.getInt("deal_num", row);
                int tranNum = tblOutputData.getInt("tran_num", row);
                int tranStatus = tblOutputData.getInt("tran_status", row);
                String payLoad = tblOutputData.getClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row);
                
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.DEAL_NUM.toString(), row, dealNum);
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.TRAN_NUM.toString(), row, tranNum);
                tableToInsert.setInt(BoundaryTableGeneralLedgerDataColumns.TRAN_STATUS.toString(), row, tranStatus);
                tableToInsert.setClob(BoundaryTableGeneralLedgerDataColumns.PAYLOAD.toString(), row, payLoad);   

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
		return Trades.class;
	}
}
