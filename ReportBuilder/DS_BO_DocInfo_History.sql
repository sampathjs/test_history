--table title "BO Info Field History"


SELECT stl_info.document_num,  
             stl_info.type_id,
             stl_info.value, 
             stl_info.stldoc_hdr_hist_id,
             stl_info.last_update,
             stl_info.personnel_id

FROM stldoc_info_h stl_info
JOIN (
    SELECT i.document_num, i.type_id, max(i.stldoc_hdr_hist_id) hist_id , max(i.last_update) last_update
             FROM stldoc_info_h i
   JOIN stldoc_header_hist hist ON i.stldoc_hdr_hist_id=hist.stldoc_hdr_hist_id and hist.doc_status=4
                   GROUP BY i.document_num,i.type_id) latest ON stl_info.document_num = latest.document_num AND stl_info.stldoc_hdr_hist_id=latest.hist_id AND stl_info.last_update=latest.last_update AND stl_info.type_id=latest.type_id 

WHERE stl_info.type_id=20007