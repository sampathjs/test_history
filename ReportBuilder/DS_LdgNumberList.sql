SELECT DISTINCT csh.ins_num, csh.param_seq_num, csh.schedule_id,
   CONVERT(VARCHAR(255), STUFF(
      (SELECT ',' + dti.info_value
         FROM delivery_ticket_info_types dtit
         JOIN delivery_ticket_info dti ON (dti.type_id = dtit.type_id)
         JOIN tsd_delivery_ticket tdt ON (tdt.id_number = dti.delivery_ticket_id)
         WHERE dtit.type_name = 'LGD Number'
         AND tdt.schedule_id = csh.schedule_id
         FOR XML PATH('')), 1, 1, '')) AS lgd_number
FROM comm_schedule_header csh;
