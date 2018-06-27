INSERT INTO USER_jm_sap_time_code_map(sap_time_code, endur_time_code, sap_inst_id)
SELECT 'LN' sap_time_code
            , 'JM London Opening' endur_time_code
            , 'BAB' sap_inst_id
EXCEPT
SELECT sap_time_code
            , endur_time_code
            , sap_inst_id
FROM USER_jm_sap_time_code_map 


UPDATE USER_jm_sap_time_code_map SET sap_time_code='LN' WHERE sap_time_code='AM' AND endur_time_code='JM London Opening' AND sap_inst_id='BAS';
