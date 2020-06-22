update user_jm_acc_mapping_config 
set endur_doc_status ='7;8;12;16;17;18;24'
where mode = 'SL' 
AND event_type in('Cash Settlement','Tax Settlement')
AND tran_status = 'Validated' 
AND endur_doc_status !='*' 
AND int_bu IN('JM PMM UK;JM PM LTD','JM PMM UK','JM PM LTD')
