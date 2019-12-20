;WITH doc_num_cte 

AS  ( -- Relevant doc nums with tran status, doc status and Int bunit
        SELECT DISTINCT stl.document_num,
                         stl.tran_status,
                         stl.internal_bunit,
						 p.short_name int_bu_name,
                         stlh.doc_status,
                         stld.doc_status_desc
        FROM stldoc_details stl
            JOIN stldoc_header stlh ON (stl.document_num = stlh.document_num AND stlh.doc_type = 1) -- Invoices only
            JOIN stldoc_document_status stld ON (stlh.doc_status = stld.doc_status)
            JOIN stldoc_header_hist stlhi ON (stl.document_num = stlhi.document_num AND stlhi.doc_status = 5)     -- consider only those documents that ever had an entry of status "1 Generated"
			JOIN party p ON (stl.internal_bunit = p.party_id)
        WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
	), 

doc_info_cte 

AS ( -- count of actual docs generated

	SELECT stli.document_num, COUNT(*) docnum_count_actual
         FROM   stldoc_info stli
                JOIN stldoc_header stlh ON (stli.document_num = stlh.document_num AND stlh.doc_type = 1) -- Invoices only
				JOIN (SELECT  DISTINCT document_num, internal_bunit FROM stldoc_details stld 
				      WHERE last_update > (SELECT prev_business_date - 1 FROM configuration) AND DATEDIFF( HOUR, last_update, sysdatetime()) > 1 ) stld 
				ON (stlh.document_num = stld.document_num AND stlh.doc_type = 1 AND stld.internal_bunit <> 20755)
         WHERE  stli.type_id IN ( 20003, 20005, 20007, 20008 ) 
         GROUP  BY stli.document_num
		 
		 UNION
		 
	SELECT stli.document_num, COUNT(*) docnum_count_actual
         FROM   stldoc_info stli
                JOIN stldoc_header stlh ON (stli.document_num = stlh.document_num AND stlh.doc_type = 1) -- Invoices only
				JOIN (SELECT DISTINCT document_num, internal_bunit FROM stldoc_details stld 
				      WHERE last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, last_update, sysdatetime()) > 1) stld
				ON (stlh.document_num = stld.document_num AND stlh.doc_type = 1 AND stld.internal_bunit = 20755)
         WHERE  stli.type_id IN ( 20003, 20007 ) 
         GROUP  BY stli.document_num
    ), 

doc_cash_vat_count_cte
	
AS( -- check for doc nums having Cash and VAT/Manual VAT records
	
	SELECT stl.document_num, COUNT(*) cash_vat_count
         FROM  stldoc_details stl
               JOIN stldoc_header stlh 
				 ON stl.document_num = stlh.document_num
                    AND stlh.doc_type = 1 -- Invoices only 
                    AND stl.event_type = 14 -- Cash Settlement
                    AND stl.ins_type IN ( 27001 ) -- Cash
                    AND stl.cflow_type IN ( 2009, 2018 ) -- VAT, Manual VAT
         WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
         GROUP  BY stl.document_num
	), 

doc_cash_prem_count_cte
     
AS ( -- check for doc nums having Cash and any Premium Charge records
	
	SELECT stl.document_num, COUNT(*) cash_prem_count
         FROM   stldoc_details stl
                JOIN stldoc_header stlh
                  ON stl.document_num = stlh.document_num
                     AND stlh.doc_type = 1 -- Invoices only 
                     AND stl.event_type = 14 -- Cash Settlement
                     AND stl.ins_type IN ( 27001 ) -- Cash
                     AND stl.cflow_type IN ( 2034, 2035, 2036, 2037, 2038 ) --'Premium Charge%'
         WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
         GROUP  BY stl.document_num
	),  
	
doc_uk_std_tax_cte 
	
AS ( -- check for doc nums having UK Std Tax records
    
	SELECT stl.document_num, COUNT(*) uk_std_tax_count
         FROM   stldoc_details stl
                JOIN stldoc_header stlh ON (stl.document_num = stlh.document_num AND stlh.doc_type = 1) -- Invoices only 
                JOIN ab_tran_event_info ei ON (stl.event_num = ei.event_num AND ei.type_id = 20002 AND ei.value = 'UK Std Tax' AND stl.delivery_ccy = 52) --GBP
         WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
         GROUP  BY stl.document_num
		 
   ),
     
doc_sa_std_tax_cte 
     
AS ( -- check for doc nums having SA Std Tax records

    SELECT stl.document_num, COUNT(*) sa_std_tax_count
         FROM   stldoc_details stl
                JOIN stldoc_header stlh ON (stl.document_num = stlh.document_num AND stlh.doc_type = 1) -- Invoices only 
                JOIN ab_tran_event_info ei ON (stl.event_num = ei.event_num AND ei.type_id = 20002 AND ei.value = 'SA Std Tax')
         WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
         GROUP  BY stl.document_num
		 
   ),
     
doc_tax_event_count_cte 
     
AS ( -- Tax Settlement records and their tax rates

     SELECT   stl.document_num,stl.cflow_type,
	          COUNT(*)tax_event_count,
              MAX(stl.para_position) max_tax_rate,
			  MIN(stl.para_position) min_tax_rate,
			  SUM(stl.para_position) sum_tax_rate
         FROM stldoc_details stl
                JOIN stldoc_header stlh 
				     ON stl.document_num = stlh.document_num
                     AND stlh.doc_type = 1 -- Invoices only 
                     AND stl.event_type = 98 -- Tax Settlement
         WHERE  stl.last_update > (SELECT prev_business_date - 1 FROM   configuration) AND DATEDIFF( HOUR, stl.last_update, sysdatetime()) > 1
         GROUP  BY stl.document_num,stl.cflow_type
    ),
     
all_doc_data_cte
     
AS (-- collating all the information required in one table 
	 SELECT docnum.document_num, docnum.tran_status, docnum.doc_status,docnum.internal_bunit,docnum.int_bu_name,doccashvat.cash_vat_count,
            doccashprem.cash_prem_count,docuktax.uk_std_tax_count,docsatax.sa_std_tax_count,tax_event_count,-- Document is considered having Tax event only if there are tax rates set across events which don't sum up to 0
            CASE WHEN (tax_event_count IS NOT NULL AND sum_tax_rate IS NOT NULL AND sum_tax_rate <> 0 ) THEN 1 ELSE 0 END 
			has_tax_event,max_tax_rate,min_tax_rate,docnum_count_actual
         FROM   doc_num_cte docnum
                LEFT JOIN doc_cash_vat_count_cte doccashvat ON (docnum.document_num = doccashvat.document_num)
                LEFT JOIN doc_cash_prem_count_cte doccashprem ON (docnum.document_num = doccashprem.document_num)
                LEFT JOIN doc_tax_event_count_cte doctax ON (docnum.document_num = doctax.document_num)
                LEFT JOIN doc_uk_std_tax_cte docuktax ON (docnum.document_num = docuktax.document_num)
                LEFT JOIN doc_sa_std_tax_cte docsatax ON (docnum.document_num = docsatax.document_num)
                LEFT JOIN doc_info_cte docinfo ON (docnum.document_num = docinfo.document_num)
	),
		
doc_num_with_counts
		
AS ( -- expected count of document
     SELECT *,
			   CASE
				 WHEN internal_bunit = 20755 AND doc_status NOT IN (4, 24) THEN 1  -- For CN and not cancelled       
				 WHEN internal_bunit = 20755 AND doc_status  IN (4, 24) THEN 2 -- For CN and cancelled
				 WHEN doc_status NOT IN (4, 24)  AND uk_std_tax_count > 0 THEN 1 -- Not cancelled and uk tax event
				 WHEN doc_status IN (4, 24) AND uk_std_tax_count > 0 THEN 2 -- Cancelled and uk tax event
				 WHEN doc_status NOT IN (4, 24) AND sa_std_tax_count > 0 THEN 1 -- Not cancelled and sa tax event
				 WHEN doc_status IN (4, 24) AND sa_std_tax_count > 0 THEN 2 -- Cancelled and sa tax event
				 WHEN doc_status NOT IN (4, 24) AND has_tax_event = 0 THEN 1 -- Not cancelled and no tax events
				 WHEN doc_status NOT IN (4, 24) AND has_tax_event = 1 THEN 2 -- Not cancelled and tax events
				 WHEN doc_status IN (4, 24) AND has_tax_event = 0 THEN 2 -- Cancelled and no tax events
				 WHEN doc_status IN (4, 24) AND has_tax_event = 1 THEN 4 -- Cancelled and tax events
				 WHEN cash_vat_count > 0 THEN 1 -- For Ctype Vat there will be only 1 document
				 WHEN cash_prem_count > 0 THEN 1  -- For Ctype premium  there will be only 1 document
				 ELSE -1
			   END docnum_count_expected
		FROM   all_doc_data_cte
   ),
		
doc_num_for_display
		      
AS( -- displaying the actual and expected document count along with document number and internal business unit 
     SELECT document_num, int_bu_name, docnum_count_actual, docnum_count_expected
	 FROM doc_num_with_counts
	 WHERE ISNULL(docnum_count_actual,-100) <> docnum_count_expected  --replaced null with negative value so that null comparison does not fail
  )     
		   
SELECT document_num , int_bu_name 
FROM doc_num_for_display
WHERE ISNULL(docnum_count_actual,-100) <> docnum_count_expected 
		   
	   
