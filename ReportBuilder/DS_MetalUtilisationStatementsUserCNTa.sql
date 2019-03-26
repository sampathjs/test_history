--tabletitle MetalUtilStatementsUCN "Metal Utilisation Statements from user table"
--argument $$account_name$$ "" "Account Name"

SELECT 
    rd.month_stamp
  ,rd.month_stamp_str
  ,rd.account_name
  ,rd.account_name account_id 
  ,rd.metal_description
  ,rd.status
  ,rd.deal_tracking_num
  ,rd.message
  ,rd.last_update
  ,rd.long_name
  ,rd.addr1
  ,rd.addr2
  ,rd.addr_reference_name
  ,rd.irs_terminal_num
  ,rd.city
  ,rd.county_id
  ,rd.mail_code
  ,rd.state_id
  ,rd.country
  ,rd.fax
  ,rd.phone
  ,rd.tax_id
  ,rd.party_id
  ,rd.short_name
  ,rd.addr11
  ,rd.addr21
  ,rd.addr_reference_name1
  ,rd.irs_terminal_num1
  ,rd.city1
  ,rd.county_id1
  ,rd.mail_code1
  ,rd.state_id1
  ,rd.country1
  ,rd.fax1
  ,rd.phone1
  ,rd.tax_id1
  ,rd.account_number
  ,rd.statement_date
  ,rd.reporting_unit
  ,rd.preferred_currency
  ,rd.metal
  ,rd.avg_balance
  ,rd.avg_price
  ,rd.metal_interest_rate
  ,rd.value
  ,rd.long_name1
  ,rd.addr12
  ,rd.addr22
  ,rd.addr_reference_name2
  ,rd.irs_terminal_num2
  ,rd.city2
  ,rd.net_interest_rate_new
  ,rd.gross_interest_rate_new
  ,avg_cny_rate
   , (CASE 
          WHEN (SELECT DATA_TYPE 
                      FROM INFORMATION_SCHEMA.COLUMNS
                      WHERE 
                                    TABLE_NAME = 'USER_jm_metalrentals_rundata_cn'
                           AND  COLUMN_NAME = 'county_id2')                                         = 'varchar' THEN rd.county_id2
          ELSE (SELECT county_name FROM ps_county ps WHERE ps.county_id = rd.county_id2)            
     END) county_id2 
  ,rd.mail_code2
   , (CASE 
          WHEN (SELECT DATA_TYPE 
                      FROM INFORMATION_SCHEMA.COLUMNS
                      WHERE 
                                    TABLE_NAME = 'USER_jm_metalrentals_rundata_cn'
                           AND  COLUMN_NAME = 'state_id2')                                         = 'varchar' THEN rd.state_id2
          ELSE (SELECT short_name FROM states st WHERE st.state_id = rd.state_id2)            
     END) state_id2  
  ,rd.country2
  ,rd.fax2
  ,rd.phone2
  ,rd.description
FROM USER_jm_metalrentals_rundata_cn rd
WHERE rd.account_name =  LTRIM(RTRIM('$$account_name$$'))
