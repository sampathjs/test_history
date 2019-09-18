package com.jm.accountingfeed.rb.output;

import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.jm.accountingfeed.enums.AuditRecordStatus;
import com.jm.accountingfeed.enums.BoundaryTableRefDataColumns;
import com.jm.accountingfeed.enums.ExtractionTableName;
import com.jm.accountingfeed.enums.ReportBuilderParameter;
import com.jm.accountingfeed.exception.AccountingFeedRuntimeException;
import com.jm.accountingfeed.jaxbbindings.reference.Account;
import com.jm.accountingfeed.jaxbbindings.reference.Accounts;
import com.jm.accountingfeed.jaxbbindings.reference.ObjectFactory;
import com.jm.accountingfeed.util.Util;
import com.olf.openjvs.DBUserTable;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.openlink.util.logging.PluginLog;

/**
 * Report builder output plugin for 'Reference data Extract' report.
 * @author SharmV03
 *
 */
public class ReferenceDataExtractOutput extends AccountingFeedOutput 
{

	/**
	 * This method reads the output data of 'Reference data Extract' report.
	 * Populates the class variable 'xmlData' with the 'Accounts' xml
	 */
    @Override
	public void cacheXMLData() throws OException 
	{
	    try
	    {
	        ObjectFactory objectFactory = new ObjectFactory();
	        xmlData = objectFactory.createAccounts();
	        List<Account> referenceDataList = ((Accounts) xmlData).getAccount();

	        JAXBContext jaxbContext = JAXBContext.newInstance(Accounts.class);
	        Marshaller marshaller = jaxbContext.createMarshaller();
	        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

	        tblOutputData.addCol("payload", COL_TYPE_ENUM.COL_CLOB);
	        int numRows = tblOutputData.getNumRows();

	        for (int i = 1; i <= numRows; i++)
	        {
	            Account referenceData = objectFactory.createAccount();

	            String elementValue = tblOutputData.getString("desk_location", i);
	            referenceData.setDeskLocation(elementValue);

	            int elementValueInt = tblOutputData.getInt("party_id", i);
	            referenceData.setPartyId(String.valueOf(elementValueInt));

	            elementValueInt = tblOutputData.getInt("party_code_jde", i);
	            referenceData.setPmmAccount(elementValueInt);

	            elementValue = tblOutputData.getString("bu_name", i);
	            referenceData.setMetAcName(elementValue);

	            elementValue = tblOutputData.getString("bu_code", i);
	            referenceData.setBusinessUnit(elementValue);

	            elementValue = tblOutputData.getString("country_code", i);
	            referenceData.setCountry(elementValue);

	            elementValue = tblOutputData.getString("region", i);
	            referenceData.setRegion(elementValue);

	            elementValue = tblOutputData.getString("le_code", i);
	            referenceData.setLglEntity(elementValue);

	            elementValue = tblOutputData.getString("group_member", i);
	            referenceData.setGroup(elementValue);

	            elementValue = tblOutputData.getString("sap_gt_acct", i);
	            referenceData.setJmAccNo(elementValue);

	            elementValue = tblOutputData.getString("lppm_member", i);
	            referenceData.setLppmMbr(elementValue);

	            elementValue = tblOutputData.getString("le_name", i);
	            referenceData.setLglEntityDesc(elementValue);

	            elementValue = tblOutputData.getString("lbma_member", i);
	            referenceData.setLbmaMbr(elementValue);

	            elementValue = tblOutputData.getString("bu_name1", i);
	            referenceData.setBusUnitDesc(elementValue);
	            
	            StringWriter stringWriter = new StringWriter();
	            marshaller.marshal(referenceData, stringWriter);
	            String payLoad = stringWriter.toString();
	            
	            tblOutputData.setClob("payload", i, payLoad);

	            referenceDataList.add(referenceData);
	        }
	    }
	    catch (JAXBException je)
	    {
            PluginLog.error("Failed to initialize Marshaller." + je.getMessage());
            Util.printStackTrace(je);
            throw new AccountingFeedRuntimeException("Error whilst cache'ing XML extract data", je);
	    }
	}
	
	/**
	 * Insert the Audit boundary table for Reference Data extract with the 'Account' payload xml for every Party in the Report output 
	 */
    @Override
	public void extractAuditRecords() throws OException
	{
		Table tableToInsert = null;
		try
		{
			tableToInsert = Table.tableNew(ExtractionTableName.REF_DATA_EXTRACT.toString());
			int numRows = tblOutputData.getNumRows();
			ODateTime timeIn = ODateTime.getServerCurrentDateTime();
			
			String auditRecordStatusString = null; 
			
			int ret = DBUserTable.structure(tableToInsert);
			
			if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
			{
				throw new AccountingFeedRuntimeException("Unable to get structure of: " + ExtractionTableName.REF_DATA_EXTRACT.toString());
			}
			tableToInsert.clearGroupBy();
			tableToInsert.addGroupBy(BoundaryTableRefDataColumns.PARTY_ID.toString());
			tableToInsert.addGroupBy(BoundaryTableRefDataColumns.EXTRACTION_ID.toString());
			tableToInsert.groupByFormatted();
			
			tableToInsert.addNumRows(numRows);
			
			tableToInsert.setColValInt(BoundaryTableRefDataColumns.EXTRACTION_ID.toString(), getExtractionId());
			tableToInsert.setColValString(BoundaryTableRefDataColumns.REGION.toString(), reportParameter.getStringValue(ReportBuilderParameter.REGIONAL_SEGREGATION.toString()));
			tableToInsert.setColValDateTime(BoundaryTableRefDataColumns.TIME_IN.toString(), timeIn);
						
            for (int i = 1; i <= numRows; i++)
            {
			    int partyId = tblOutputData.getInt("party_id", i);
                String payLoad = tblOutputData.getClob("payload", i);

			    tableToInsert.setInt(BoundaryTableRefDataColumns.PARTY_ID.toString(), i, partyId);
				tableToInsert.setClob(BoundaryTableRefDataColumns.PAYLOAD.toString(), i, payLoad);
			}
			if(null == errorDetails || errorDetails.isEmpty())
			{
				auditRecordStatusString = AuditRecordStatus.PROCESSED.toString();
			}
			else
			{
				auditRecordStatusString = AuditRecordStatus.ERROR.toString();
				tableToInsert.setColValString(BoundaryTableRefDataColumns.ERROR_MSG.toString(), errorDetails);
			}
			tableToInsert.setColValString(BoundaryTableRefDataColumns.PROCESS_STATUS.toString(), auditRecordStatusString);
			
			DBUserTable.insert(tableToInsert);
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
			if(Table.isTableValid(tableToInsert) == 1)
			{
				tableToInsert.destroy();
			}
		}
	}
	
	@Override
	public Class<?> getRootClass()
	{
		return Accounts.class;
	}
}
