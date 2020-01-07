/* Released with version 09-Jan-2014_V14_0_6 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

import com.olf.openjvs.QueryRequest;
import com.olf.openjvs.Table;

public class APM_ExecuteQuery
{
	private APM_Utils m_APMUtils;
	
    public APM_ExecuteQuery() {
		m_APMUtils = new APM_Utils();
    }

    public QueryRequest createQueryIdFromMainArgt(int iMode, Table tAPMArgumentTable, Table argt) throws OException
    {
   	 return createQueryIdFromMainArgt(iMode, tAPMArgumentTable, argt, -1);
    }
    
    public QueryRequest createQueryIdFromMainArgt(int iMode, Table tAPMArgumentTable, Table argt, int iPortfolio) throws OException
    {
   	int iRetVal = 1;
		QueryRequest qreq = null; 
   	 
		String query_name = "None";
		if ( argt.getColNum("query_name") > 0 )
			query_name = argt.getString("query_name", 1);
		
		try
		{
		   if (iRetVal != 0) {
		      Table selected_criteria = argt.getTable( "selected_criteria", 1);
		      qreq = createQueryRequest(iMode, tAPMArgumentTable, query_name, selected_criteria, iPortfolio);
		   }
		} 
		catch(Exception t) 
		{
		   iRetVal = 0;
		   m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Exception while creating and executing query");
		   String message = m_APMUtils.getStackTrace(t);
		   m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
		}
		
		return qreq;
	 }
        
    private QueryRequest createQueryRequest(int iMode, Table tAPMArgumentTable, String query_name, Table tSelectedCriteria, int portfolioId) throws OException
    {
		int iRetVal = 1;
		QueryRequest qreq = null; 
		
		// //////////////////////////////////////////////////////////////////////////////////
		//
		// Get a query matching all deal(s) to be processed
		//
		// //////////////////////////////////////////////////////////////////////////////////
		if (iMode != m_APMUtils.cModeDealDoNothing) {
				boolean queryFromSavedQuery = false;

				if(Str.equal(query_name, "None") == 1)
				{
				   qreq = QueryRequest.queryRequestNew();

				   /* Initialize the query request object for transaction queries */
				   qreq.initializeData( 2 /* TRANSACTION_QUERY_TRANSACTION_GROUP_ID */);

				   /* Add a page to the query request object */
				   qreq.addpage(0);
				}
				else
				{
				   try
				   {
				      /* Load named query into query request structure */
				      /* Do not use the new load by name fn - will cause scripts to fail to compile in earlier V11 */
				      /* qreq = QueryRequest.loadSavedQueryByName(query_name); */

				      // try to find cached table view.
				      Table tSavedQueryTable = Table.getCachedTable("saved_queries");
				      int saved_query_id = 0;
				      if (Table.isTableValid(tSavedQueryTable) == 1) 
				      {
				         int queryRow = tSavedQueryTable.findString(2, query_name, SEARCH_ENUM.FIRST_IN_GROUP);
				         if ( queryRow > 0 )
				            saved_query_id = tSavedQueryTable.getInt(1, queryRow);
				         else
				            tSavedQueryTable.destroy(); // destroy the table if we cannot find it - forces a reload in case its a new query name
				      }

				      if (Table.isTableValid(tSavedQueryTable) == 0 || saved_query_id == 0) 
				      {
				      	// get map of query name to ID from database
				      	tSavedQueryTable = Table.tableNew("saved_queries");
				      	iRetVal = m_APMUtils.APM_TABLE_LoadFromDbWithSQL(tAPMArgumentTable, tSavedQueryTable, "dn.client_data_id, dn.node_name", "dir_node dn inner join qry_groups qg on (qg.id_number = dn.category)", "dn.node_type = 3");
				      	tSavedQueryTable.sortCol("node_name");
				      	if (iRetVal != 0)
				      		Table.cacheTable("saved_queries", tSavedQueryTable);
				      }

				      // no need to find it again if its already been found
				      if (saved_query_id == 0 )
				      {
				      	int queryRow = tSavedQueryTable.findString(2, query_name, SEARCH_ENUM.FIRST_IN_GROUP);
				      	if ( queryRow > 0 )
				      		saved_query_id = tSavedQueryTable.getInt(1, queryRow);
				      }

				      if ( saved_query_id > 0 )
				      {
				         m_APMUtils.APM_PrintMessage(tAPMArgumentTable, "------- LOADING NAMED QUERY: " + query_name + "  --------");				      	
				         qreq = QueryRequest.loadSavedQuery(saved_query_id);
				         queryFromSavedQuery = true;
				      }
				      else
				      {
				         iRetVal = 0;
				         m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Could not find named query: " + query_name);
				      } 
				   } 
				   catch(Exception t) 
				   {
				      iRetVal = 0;
				      m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Exception while loading named query: " + query_name);
				      String message = m_APMUtils.getStackTrace(t);
				      m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
				   }
				}

				try
				{
				   if (iRetVal != 0) {
				         executeQuery(iMode, tAPMArgumentTable, tSelectedCriteria, qreq, queryFromSavedQuery, portfolioId);
				   }
				} 
				catch(Exception t) 
				{
				   iRetVal = 0;
				   m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Exception while creating and executing query");
				   String message = m_APMUtils.getStackTrace(t);
				   m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, message+ "\n");
				}
		}
		
		return qreq;
    }

    QueryRequest executeQuery(int iMode, Table tAPMArgumentTable, Table tSelectedCriteria, QueryRequest qreq, boolean queryFromSavedQuery, int portfolioId)  throws OException
	 {
	   String field_name;
	   String field_label;
	   int i;
	   int j;
	   int new_row;
	   Table field_values;
	   Table filter_table;
	   String value;
	   
	   if(iMode == m_APMUtils.cModeBatch)
	   {
	      int additionCriteriaColNum = tSelectedCriteria.getColNum("additional_criteria");
	      int numPagesAdded = 0;

      	String criteriaMapColName =  "filter_map";
	      if ( tSelectedCriteria.getColNum("criteria_map") > 0 )
	      	criteriaMapColName =  "criteria_map";
	      
	      for(i = 1; i <= tSelectedCriteria.getNumRows(); i++)
	      {
	         field_name = tSelectedCriteria.getString(criteriaMapColName, i);
	         	         
	         // now see if there is any additional criteria for suspended units
	         // note that this can only be external_bunit & external_lentity fields
	         // anything else is unsupported - it has to be hard coded
	         Table additionalIncludeCriteria = null;
	         Table additionalExcludeCriteria = null;
	         if ( additionCriteriaColNum > 0 )
	         {
	         	Table additionalCriteria = tSelectedCriteria.getTable(additionCriteriaColNum, i);
	         	if ( additionalCriteria != Util.NULL_TABLE && additionalCriteria != null)
	         	{
	         		additionalIncludeCriteria = additionalCriteria.getTable("include", 1);
	         		additionalExcludeCriteria = additionalCriteria.getTable("exclude", 1);
	         		
	         		// exclude criteria should never be set so warn if it is (Oleg added it for the future - don't know why)
	         		if ( additionalExcludeCriteria != Util.NULL_TABLE && additionalExcludeCriteria != null)
	        		    	m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Exclude criteria populated in batch criteria !!  Currently unsupported.");

	         		// include criteria could be populated for suspended counterpartiers (external_bunit & external_lentity)
	         		// anything else is unsupported as we don't know how to apply the additional criteria
	         		if ( additionalIncludeCriteria != Util.NULL_TABLE && additionalIncludeCriteria != null)
	         		{
	         			if ( Str.equal("external_bunit", field_name) == 0 && Str.equal("external_lentity", field_name) == 0)
		        		    	m_APMUtils.APM_PrintErrorMessage(tAPMArgumentTable, "Include criteria populated for unsupported criteria: " + field_name);
	         			else if ( queryFromSavedQuery == true)
	         			{
	         				// not currently supported as we do not know how many pages are in the saved query
		        		    	m_APMUtils.APM_PrintAndLogErrorMessage(iMode, tAPMArgumentTable, "Configuration Error !! Setting Suspended " + field_name + " criteria on the APM service is not supported due to saved query set on service. Please remove query or suspended criteria.");	         				
	         			}
	         			else
	         			{
		         			// add an exclude page for this criteria
		         			// note that QREQ returns deals booked agaisnt suspended counterparties by default
		         			// so we have to do an exclude page to get rid of all suspended units NOT selected
		         			// ugly - but only way
		         			qreq.addpage(1);
		         			numPagesAdded++;
		         			
		         			// now do specific bits for bunits
		         			if ( Str.equal("external_bunit", field_name) == 1)
		         			{
			         			// set the status to suspended only
		         				field_label = "Status";
		      	            qreq.setFieldValueString(numPagesAdded, "External Unit", field_label, "Suspended");
	
		      	            // if none selected then exclude all suspended counterparties, otherwise only exclude the ones selected
		      	            if ( additionalIncludeCriteria.unsortedFindString("value", "None", SEARCH_CASE_ENUM.CASE_INSENSITIVE) < 1)
		      	            {
			      	            field_label = "Short Name";
			      	            
				         			// set up the field values table
				      	         field_values = qreq.getFieldValueTable(numPagesAdded, "External Unit", field_label);
				      	         int numValues = additionalIncludeCriteria.getNumRows();
				      	         field_values.addNumRows(numValues);
				      	         for (j = 1; j <= numValues; j++)
				      	         	field_values.setString(1, j, "!" + additionalIncludeCriteria.getString("value", j));
				      	         
				      	         // set the values
			      	            qreq.setFieldValueTable(numPagesAdded, "External Unit", field_label, field_values);
				      	         field_values.destroy();
		      	            }
		         			}
		         			
		         			// now do specific bits for lentities
		         			if ( Str.equal("external_lentity", field_name) == 1)
		         			{
		         				field_label = "Status";
		      	            qreq.setFieldValueString(numPagesAdded, "External Entity", field_label, "Suspended");
	
		      	            // if none selected then exclude all suspended counterparties, otherwise only exclude the ones selected
		      	            if ( additionalIncludeCriteria.unsortedFindString("value", "None", SEARCH_CASE_ENUM.CASE_INSENSITIVE) < 1)
		      	            {
			      	            field_label = "Short Name";
		
			      	            // set up the field values table
				      	         field_values = qreq.getFieldValueTable(numPagesAdded, "External Entity", field_label);
				      	         int numValues = additionalIncludeCriteria.getNumRows();
				      	         field_values.addNumRows(numValues);
				      	         for (j = 1; j <= numValues; j++)
				      	         	field_values.setString(1, j, "!" + additionalIncludeCriteria.getString("value", j));
				      	         
				      	         // set the values
			      	            qreq.setFieldValueTable(numPagesAdded, "External Entity", field_label, field_values);
				      	         field_values.destroy();
		      	            }
		         			}
	         			}
	         		}
	         	}
	         }
	         
	         // now populate the original page
	      	String criteriaTableColName =  "filter_table";
		      if ( tSelectedCriteria.getColNum("criteria_table") > 0 )
		      	criteriaTableColName =  "criteria_table";
	         
	         Table orig_filter_table = tSelectedCriteria.getTable(criteriaTableColName, i);
	         if ( orig_filter_table == Util.NULL_TABLE || orig_filter_table == null)
	         	continue;
	         
	         filter_table = tSelectedCriteria.getTable(criteriaTableColName, i).copyTable();
	         
	         // if a portfolio specified then override the selection if we are on the internal_portfolio row
	         if ( field_name.equals("internal_portfolio") && portfolioId > 0 )
	         {
	         	filter_table.clearRows();
	         	filter_table.addRow();
	         	filter_table.setString("value", 1,  Table.formatRefInt(portfolioId, SHM_USR_TABLES_ENUM.PORTFOLIO_TABLE));
	         }
	         
	         filter_table.addCol( "intersect_flag", COL_TYPE_ENUM.COL_INT);   /* Safe to add a column to a table copy */
	         
	         field_label = qreq.getFieldLabelFromFieldName("ab_tran", field_name);

	         field_values = qreq.getFieldValueTable(0, "Transaction", field_label);
	         field_values.addCol( "intersect_flag", COL_TYPE_ENUM.COL_INT); /* We own field_values table so it is save to add a column to this table as well. */

	         if(filter_table.getNumRows() > 0)
	         {
	            /* For select criteria fields which have intersecting values between saved  query
	               and property filters,  need to ensure that only intersecting criteria is used. */
	            if(field_values.getNumRows() > 0)
	            {
	               /* First get rid of the values present in saved query but not in property filters. */
	               field_values.setColValInt("intersect_flag", 1);
	               filter_table.select(field_values, "intersect_flag", "label EQ $value");
	               filter_table.deleteWhereValue("intersect_flag", 0);

	               /* Now get rid of the values present in property filters but not in saved query. */
	               field_values.setColValInt("intersect_flag", 0);
	               filter_table.setColValInt("intersect_flag", 1);
	               field_values.select(filter_table, "intersect_flag", "value EQ $label");
	               field_values.deleteWhereValue("intersect_flag", 0);
	            }

	            for(j = filter_table.getNumRows(); j>0; j--)
	            {
	               value = filter_table.getString("value", j);
                  if(field_values.unsortedFindString("label", value, SEARCH_CASE_ENUM.CASE_SENSITIVE) <= 0)
                  { 
                     new_row = field_values.addRow();
                     field_values.setString("label", new_row, value);
                  }	               
	            }
	            qreq.setFieldValueTable(0, "Transaction", field_label, field_values);

	         }
	         int valueCount = field_values.getNumRows();
	         field_values.destroy();
	         filter_table.destroy();
	         if ( field_name.equals("internal_portfolio") && portfolioId > 0 && valueCount == 0)
    		 {
	        	 // no portfolios left on the query - which actually means that there are no deals for this portfolio to run against
	        	 return qreq; // no query ID as it has not been executed.
    		 }
	      }
	   }
	   else   /* INCREMENTAL */
	   {
	   	// note that we don't have to worry about extra criteria for suspended as that is dealt with via the ops service defn

	   	Table deals_table = tAPMArgumentTable.getTable("Filtered Deal Info", 1);	      
	      int tran_num;
	      
	      field_values = qreq.getFieldValueTable(0, "Transaction", "Tran Num");
	      
	      for(i = 1; i <= deals_table.getNumRows(); i++)
	      {
	         /* You can filter on specific deals if you need (incremental case with no saved query specified) */
	         tran_num = deals_table.getInt("tran_num", i);
	         new_row = field_values.addRow();
	         field_values.setString("label", new_row, Str.intToStr(tran_num));
	      }

	      qreq.setFieldValueTable(0, "Transaction", "Tran Num", field_values);
	   }  
	   
	   /* Execute query */
	   qreq.executeQuery();
      	   
	   return qreq;		
	}
	
}
