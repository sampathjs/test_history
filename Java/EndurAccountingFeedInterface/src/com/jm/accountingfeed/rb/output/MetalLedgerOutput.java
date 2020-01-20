package com.jm.accountingfeed.rb.output;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableMetalLedgerDataColumns;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.PartyRegion;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.jaxbbindings.metalledger.ObjectFactory;
import com.jm.accountingfeed.jaxbbindings.metalledger.Trade;
import com.jm.accountingfeed.jaxbbindings.metalledger.Trades;
import com.jm.accountingfeed.util.Constants;
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
 * Report builder output plugin for 'Metal Ledger Extract' report.
 * 
 * Version		Updated By			Date		Ticket#			Description
 * -----------------------------------------------------------------------------------
 * 	1.1			Paras Yadav		10-Jan-2020		 P1722			Removed double destroy of tableToInsert
 */
public class MetalLedgerOutput extends AccountingFeedOutput 
{

    /**
     * This method reads the output data of 'Metal Ledger Extract' report.
     * Populates the class variable 'xmlData' with the 'Trades' xml
     */
    @Override
    public void cacheXMLData() throws OException 
    {
        try
        {
    		String region = reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString());

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
                
                String tranStatus = Ref.getShortName(SHM_USR_TABLES_ENUM.TRANS_STATUS_TABLE, tblOutputData.getInt("tran_status", row));
                trade.setTradeStatus(tranStatus);

                String legPhy = String.valueOf(tblOutputData.getInt("deal_leg_phy", row));
                trade.setLeg(legPhy);

                String portfolio = Ref.getShortName(SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE, tblOutputData.getInt("internal_portfolio", row)); 
                trade.setPortfolio(portfolio);

                String insType = tblOutputData.getString("ins_type", row);
                trade.setInsType(insType);

                String extPartyId = String.valueOf(tblOutputData.getInt("external_bunit", row));
                trade.setPmmAccount(extPartyId);

                String intPartyId = String.valueOf(tblOutputData.getInt("internal_bunit", row));
                trade.setIntPmmAccount(intPartyId);

                String tradeType = tblOutputData.getString("trade_type", row);
                trade.setTradeType(tradeType);

                String tradeDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("trade_date", row));
                trade.setTradeDate(tradeDate);

                String tradeLoc = tblOutputData.getString("trading_location", row);
                trade.setTradingLoc(tradeLoc);

                String fromCurrency = Ref.getName(SHM_USR_TABLES_ENUM.CURRENCY_TABLE, tblOutputData.getInt("from_currency", row)); 
                trade.setFromCurrency(fromCurrency);

                BigDecimal fromValue = Util.roundPosition(tblOutputData.getDouble("position_uom", row));
                trade.setFromValue(fromValue);

                String uom = Ref.getName(SHM_USR_TABLES_ENUM.IDX_UNIT_TABLE, tblOutputData.getInt("uom", row));
                trade.setUnitOfMeasure(uom);

                BigDecimal baseWeight = Util.roundPosition(tblOutputData.getDouble("position_toz", row));
                trade.setBaseWeight(baseWeight);

                String ref = tblOutputData.getString("reference", row);
                trade.setCustomerRef(ref);

                String site = Ref.getName(SHM_USR_TABLES_ENUM.FACILITY_TABLE, tblOutputData.getInt("site", row));
                trade.setSite(site);
                               
                if(!insType.equalsIgnoreCase(Constants.CASH_TYPE) || PartyRegion.HK.toString().equalsIgnoreCase(region)) 
                {
                    String Loc = tblOutputData.getString("location", row);
                    trade.setLocation(Loc);
                }
                else
                {
                	trade.setLocation("");
                }                
                String form = Ref.getName(SHM_USR_TABLES_ENUM.COMMODITY_FORM_TABLE, tblOutputData.getInt("form", row));
                trade.setForm(form);
                
                String purity = Ref.getName(SHM_USR_TABLES_ENUM.MEASURE_GROUP_TABLE, tblOutputData.getInt("purity", row));
                trade.setPurity(purity);

	            String valueDate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("value_date", row));
                trade.setValueDate(valueDate);
                
                String returndate = getJDEFormattedStringFromEndurDate(tblOutputData.getDateTime("returndate", row));
                trade.setReturndate(returndate);

                if (PartyRegion.HK.toString().equalsIgnoreCase(region))
	            {
	            	/* Pricing type is only applicable for Hong Kong */
	            	String elementValue = tblOutputData.getString("pricing_type", row);
	            	trade.setPricingType(elementValue);	
	            }
	            
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
     * Metal ledger records have deal leg granularity.
     * This method inserts records in the boundary table at deal granularity by appending payload of multiple legs into one.
     */
    @Override
    public void extractAuditRecords() throws OException 
    {
    	Table tableToInsert = null;
    	 
        try
        {
        	tableToInsert = Table.tableNew(ExtractionTableName.METALS_LEDGER.toString());
        	
            int numRows = tblOutputData.getNumRows();
            ODateTime timeIn = ODateTime.getServerCurrentDateTime();
            
            DBUserTable.structure(tableToInsert);            
            
            ArrayList<Integer> extractedDeals = new ArrayList<Integer>();
            /* Map to store dealNum and ML Payload */
            HashMap<String, Trades> dealNumPayload = getPayload();
            PluginLog.info("Created Map of DealNumber to all Trades of deal . Output rows=" + numRows + ", Num of Deals=" + dealNumPayload.size());
            
            JAXBContext jaxbContext = JAXBContext.newInstance(Trades.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            for (int row = 1; row <= numRows; row++)            
            {
                int dealNum = tblOutputData.getInt("deal_num", row);
                int tranNum = tblOutputData.getInt("tran_num", row);
                int tranStatus = tblOutputData.getInt("tran_status", row);

                if(extractedDeals.contains(dealNum))
                {
                    //Break out of the loop if the deal is already extracted
                    continue;
                }                
                extractedDeals.add(dealNum);
                
                int rowToSet = tableToInsert.addRow();
                tableToInsert.setInt(BoundaryTableMetalLedgerDataColumns.DEAL_NUM.toString(), rowToSet, dealNum);
                tableToInsert.setInt(BoundaryTableMetalLedgerDataColumns.TRAN_NUM.toString(), rowToSet, tranNum);
                tableToInsert.setInt(BoundaryTableMetalLedgerDataColumns.TRAN_STATUS.toString(), rowToSet, tranStatus);
                Trades glTrades = dealNumPayload.get(String.valueOf(dealNum));
                if(glTrades != null)
                {
                    StringWriter payLoad = new StringWriter();                  
                    marshaller.marshal(glTrades, payLoad);
                    tableToInsert.setClob(BoundaryTableMetalLedgerDataColumns.PAYLOAD.toString(), rowToSet, payLoad.toString());
                }
                else
                {
                    PluginLog.warn("Trades Payload not found for deal " + dealNum);
                }               
            }
            String auditRecordStatusString = null; 
            if(null == errorDetails || errorDetails.isEmpty())
            {
                auditRecordStatusString = AuditRecordStatus.NEW.toString();
            }
            else
            {
                auditRecordStatusString = AuditRecordStatus.ERROR.toString();
                tableToInsert.setColValString(BoundaryTableMetalLedgerDataColumns.ERROR_MSG.toString(), errorDetails);
            }
            tableToInsert.setColValString(BoundaryTableMetalLedgerDataColumns.PROCESS_STATUS.toString(), auditRecordStatusString);
            tableToInsert.setColValInt(BoundaryTableMetalLedgerDataColumns.EXTRACTION_ID.toString(), getExtractionId());
            tableToInsert.setColValString(BoundaryTableMetalLedgerDataColumns.REGION.toString(), reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()));
            tableToInsert.setColValDateTime(BoundaryTableMetalLedgerDataColumns.TIME_IN.toString(), timeIn);
            
            tableToInsert.clearGroupBy();
            tableToInsert.addGroupBy(BoundaryTableMetalLedgerDataColumns.DEAL_NUM.toString());
            tableToInsert.groupBy();
            int retval = DBUserTable.insert(tableToInsert);
            if (retval != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
            {
                PluginLog.error(DBUserTable.dbRetrieveErrorInfo(retval, "DBUserTable.insert() failed"));
            }
           
        }
        catch (OException oException)
        {
            String message = "Exception occurred while extracting records.\n" + oException.getMessage();
            PluginLog.error(message);
            Util.printStackTrace(oException);
            throw oException;
        } catch (JAXBException e) 
        {
        	Util.printStackTrace(e);
            throw new AccountingFeedRuntimeException("Exception occurred while extracting records.\n" + e.getMessage());
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
     * Create a Map of DealNumber to all Trades of deal 
     * @return
     */
    private HashMap<String, Trades> getPayload()
    {
        HashMap<String, Trades> dealNumPayload = new HashMap<>();
        List<Trade> tradeList = ((Trades) xmlData).getTrade();
        
        ObjectFactory objectFactory = new ObjectFactory();

        for(Trade trade: tradeList)
        {
            String dealNum = trade.getTradeRef();
            Trades payload;
            if(dealNumPayload.containsKey(dealNum))
            {
                payload = dealNumPayload.get(dealNum);   
            }
            else
            {
                payload = objectFactory.createTrades();
                dealNumPayload.put(dealNum, payload);
            }
            payload.getTrade().add(trade);
        }        
        
        return dealNumPayload;       
    }
    
    @Override
    public Class<?> getRootClass() 
    {
        return Trades.class;
    }
}
