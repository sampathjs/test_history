package com.jm.accountingfeed.rb.datasource.salesledger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.jm.accountingfeed.enums.JDEStatus;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;

public class SalesLedgerDiagnostic extends SalesLedgerExtract 
{

    public SalesLedgerDiagnostic() throws OException
    {
        this.returnt = Table.tableNew();
    }
    
    @Override
    public void setOutputFormat(Table output) throws OException 
    {
        super.setOutputFormat(output);
    }

    @Override
    public Table generateOutput(Table output) throws OException 
    {
        return super.generateOutput(output);
    }
    
    @Override
    public HashSet<Integer> getIncludedInternalLentities() throws OException
    {
        return new HashSet<Integer>();
    }

    @Override
    public HashSet<Integer> getExcludedCounterparties() throws OException
    {
        return new HashSet<Integer>();
    }
    
    @Override
    protected void filterRefData(Table tblData) throws OException
    {
        
    }
    
    @Override
    protected List<JDEStatus> getApplicableJdeStatus()
    {
        /* Query all JDE statuses for diagnostic purposes */
        List<JDEStatus> jdeStatus = new ArrayList<JDEStatus>();
        JDEStatus[] statusArray = JDEStatus.values();
        for (JDEStatus status : statusArray)
        {
            jdeStatus.add(status);
        }
        return jdeStatus;
    }
    
    @Override
    public void setTranTableQueryId(int tranTableQueryId) 
    {
        this.tranTableQueryId = tranTableQueryId;
    }   

}
