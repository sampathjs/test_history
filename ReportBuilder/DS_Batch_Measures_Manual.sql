--tabletitle batch_spec_measures "batch_spec_measures"
--argument $$batch_num$$ "" "batch_num"

select a.measurement_type, a.lower_value, a.value_modifier, a.unit, a.batch_num, a.brand_id, a.form_id, a.country_of_origin_id, a.measure_group_id, a.idx_subgroup_id  from 
(Select m.schedule_id as mscid, m.measurement_type, m.lower_value, m.unit, m.value_modifier, cb.batch_num, cb.brand_id, cb.form_id, cb.country_of_origin_id, csdc.measure_group_id, cb.idx_subgroup_id, RANK() over(partition by m.measurement_type order by m.last_update desc) AS rank 

FROM tsd_measure as m

INNER JOIN comm_schedule_header csh
ON m.schedule_id = csh.schedule_id
INNER JOIN comm_sched_delivery_cmotion csdc
ON csh.delivery_id = csdc.delivery_id
INNER JOIN comm_batch as cb
ON csdc.batch_id = cb.batch_id
WHERE  cb.batch_num ='$$batch_num$$' /*get batch NUM*/
AND csdc.activity_id=20003
AND m.tsd_delivery_ticket_id = 0
) AS a

WHERE a.rank = 1