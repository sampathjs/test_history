package com.jm.accountingfeed.udsr.ledgers;

import com.jm.accountingfeed.rb.datasource.salesledger.SalesLedgerDiagnostic;
import com.jm.accountingfeed.udsr.UdsrBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.PluginCategory;
import com.olf.openjvs.Query;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SCRIPT_CATEGORY_ENUM;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.openlink.util.logging.PluginLog;

@PluginCategory(SCRIPT_CATEGORY_ENUM.SCRIPT_CAT_SIM_RESULT)
public class SalesLedgerData extends UdsrBase
{

    @Override
    protected void calculate(IContainerContext context) throws OException 
    {
        PluginLog.info("Calculating Sales Ledger data");

        Table slExtractOutput = Util.NULL_TABLE;
        int tranTableQueryId = 0;
        try
        {
            tranTableQueryId = Query.tableQueryInsert(tblTransactions, "tran_num");        
            PluginLog.debug(" queryId " + tranTableQueryId);
            
            if (tranTableQueryId <= 0) 
            {
                PluginLog.debug("Unable to insert deal numbers in DB query_result table");
                Util.exitFail();            
            }
            SalesLedgerDiagnostic slExtract = new SalesLedgerDiagnostic();
            slExtract.setTranTableQueryId(tranTableQueryId);
            slExtractOutput = Table.tableNew();
            slExtract.setOutputFormat(slExtractOutput);
            slExtract.generateOutput(slExtractOutput);

            tblReturnt.select(tblTransactions, "deal_num", "deal_num GT -1");
            tblReturnt.select(slExtractOutput, "*", "deal_num EQ $deal_num");

        }
        finally
        {
            if(Table.isTableValid(slExtractOutput) == 1)
            {
                slExtractOutput.destroy();
            }
            if (tranTableQueryId > 0)
            {
                Query.clear(tranTableQueryId);
            }
        }

    }
    
    @Override
    protected void format(IContainerContext context) throws OException 
    {
        Table tblReturnt = context.getReturnTable();
        
        tblReturnt.setColFormatAsRef("endur_doc_status", SHM_USR_TABLES_ENUM.STLDOC_DOCUMENT_STATUS_TABLE);
        tblReturnt.setColFormatAsRef("cflow_type", SHM_USR_TABLES_ENUM.CFLOW_TYPE_TABLE);
        tblReturnt.setColFormatAsRef("ins_type", SHM_USR_TABLES_ENUM.INS_TYPE_TABLE);
        tblReturnt.setColFormatAsRef("buy_sell", SHM_USR_TABLES_ENUM.BUY_SELL_TABLE);
        tblReturnt.setColFormatAsRef("ins_sub_type", SHM_USR_TABLES_ENUM.INS_SUB_TYPE_TABLE);
        tblReturnt.setColFormatAsRef("external_bunit", SHM_USR_TABLES_ENUM.PARTY_TABLE);
        tblReturnt.setColFormatAsRef("internal_lentity", SHM_USR_TABLES_ENUM.PARTY_TABLE);
        tblReturnt.setColFormatAsRef("currency", SHM_USR_TABLES_ENUM.CURRENCY_TABLE);
        
        tblReturnt.setColFormatAsDate("last_doc_update_time");
        tblReturnt.setColFormatAsDate("invoice_date");
        tblReturnt.setColFormatAsDate("value_date");
        tblReturnt.setColFormatAsDate("payment_date");
        tblReturnt.setColFormatAsDate("event_date");
    }
}