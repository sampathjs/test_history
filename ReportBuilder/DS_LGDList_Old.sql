SELECT DISTINCT csh.ins_num, csh.param_seq_num, csh.schedule_id, batch2.batch_num,  batch2.form_id,
       CONVERT(VARCHAR(255),  STUFF(
      (SELECT ',' + dti.info_value
         FROM delivery_ticket_info_types dtit
         JOIN delivery_ticket_info dti ON (dti.type_id = dtit.type_id)
         JOIN tsd_delivery_ticket tdt ON tdt.id_number = dti.delivery_ticket_id  AND tdt.schedule_id IN 
            ( SELECT csh2.schedule_id FROM comm_schedule_header csh2  
               JOIN csd_cmotion_view cmv ON cmv.delivery_id = csh2.delivery_id 
               JOIN comm_batch batch ON batch.batch_id = cmv.batch_id AND batch2.batch_id = batch.batch_id AND batch.batch_num = batch2.batch_num AND batch.form_id = batch2.form_id  AND batch.country_of_origin_id = batch2.country_of_origin_id
               JOIN comm_sched_delivery_cmotion csdc ON csdc.batch_id = batch.batch_id 
               JOIN measure_group mg ON mg.id_number = csdc.measure_group_id AND mg.id_number = mg2.id_number
               WHERE csh.ins_num = csh2.ins_num
            )
         JOIN ab_tran ab ON ab.ins_num = csh.ins_num AND ab.current_flag = 1
                 AND ab.tran_num IN (SELECT ab2.tran_num FROM ab_tran ab2  WHERE ab2.current_flag = 1 AND ab2.ins_type = 48010 AND ab2.buy_sell = 1)
                  WHERE dtit.type_name = 'LGD Number'
         FOR XML PATH('')), 1, 1, '')
) AS lgd_number
FROM comm_schedule_header csh
JOIN csd_cmotion_view cmv2 ON cmv2.delivery_id = csh.delivery_id
JOIN comm_batch batch2 ON batch2.batch_id = cmv2.batch_id
JOIN comm_sched_delivery_cmotion csdc2 ON csdc2.batch_id = batch2.batch_id 
JOIN measure_group mg2 ON mg2.id_number = csdc2.measure_group_id
