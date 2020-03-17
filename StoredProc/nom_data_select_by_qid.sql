--
--This procedure is run from OL core in the dispatch screen
-- History 
-- Original version 
--
if exists
(
      select *
      from sysobjects
      where name = 'nom_data_select_by_qid' and type = 'P'
)
begin
      drop proc nom_data_select_by_qid
end
go

/****** Object:  StoredProcedure [dbo].[nom_data_select_by_qid]    Script Date: 17/03/2020 13:19:37 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER OFF
GO

create procedure nom_data_select_by_qid

	@qid	int

as

	select

		comm_schedule_delivery.*

	from

		comm_schedule_delivery,

                query_result

	where

		query_result.unique_id = @qid and 

		query_result.query_result = comm_schedule_delivery.delivery_id

	select

		comm_sched_deliv_contracts.*

	from

		comm_sched_deliv_contracts,

		query_result

	where

		query_result.unique_id = @qid and 

		query_result.query_result = comm_sched_deliv_contracts.delivery_id

	select

		comm_sched_deliv_comments.*

	from

		comm_sched_deliv_comments,

		query_result

	where

		query_result.unique_id = @qid and 

		query_result.query_result = comm_sched_deliv_comments.delivery_id

	select

		csdd.*

	from

		comm_sched_deliv_deal csdd 

		join query_result on query_result.query_result = csdd.delivery_id		

		left join ab_tran on csdd.deal_num = ab_tran.deal_tracking_num and ab_tran.current_flag = 1

	where

		query_result.unique_id = @qid

	select

		comm_sched_delivery_cmotion.*

	from

		comm_sched_delivery_cmotion,

		query_result

	where

		query_result.unique_id = @qid and 

		query_result.query_result = comm_sched_delivery_cmotion.delivery_id

	select

		nomination_carriage.*

	from

		nomination_carriage,

		query_result

	where

		query_result.unique_id = @qid and 

		query_result.query_result = nomination_carriage.delivery_id        

	select

		comm_schedule_delivery.delivery_id, nom_group.* 

	from 

		nom_group, comm_schedule_delivery, query_result

	where 

		query_result.unique_id = @qid and

		query_result.query_result = comm_schedule_delivery.delivery_id and

		nom_group.nom_group_id=comm_schedule_delivery.nom_group_id

	select

		comm_schedule_header.delivery_id, comm_schedule_header.ins_num, comm_schedule_header.param_seq_num, comm_schedule_header.volume_type, tsd_delivery_ticket.*

	from

		comm_sched_delivery_cmotion,

		comm_schedule_header,

		tsd_delivery_ticket,

		query_result,

		ab_tran

	where

		query_result.unique_id = @qid and 

		query_result.query_result = comm_sched_delivery_cmotion.delivery_id and

        comm_sched_delivery_cmotion.transport_class <> 7  /* Skip for TRANSPORT_CLASS_FUNGIBLE_PIPELINE (CPS) */  and

		comm_sched_delivery_cmotion.delivery_id = comm_schedule_header.delivery_id and

		comm_schedule_header.schedule_id = tsd_delivery_ticket.schedule_id and 

                ab_tran.base_ins_type <> 48011 and  /* comm_phys_batch */ 

		ab_tran.ins_num = comm_schedule_header.ins_num and 

		ab_tran.current_flag = 1

        select

                csh.delivery_id, csh.ins_num, csh.param_seq_num, abt.deal_tracking_num, abt.buy_sell, abt.ins_type,

                inp.pay_rec, csh.bav_flag, csh.location_id, csh.volume_type, csh.unit, csh.total_quantity,

                csh.day_start_date_time, csh.day_end_date_time, csh.total_gross_quantity, csh.calc_specific_gravity, csh.calc_air_vacuum_factor,

                csh.parcel_id, abt.int_trading_strategy, abt.ext_trading_strategy, csh.int_strategy_id, csh.ext_strategy_id, abt.internal_bunit, abt.external_bunit  

        from

                ab_tran abt,

                comm_sched_delivery_cmotion csdc,

                comm_schedule_header csh,

                ins_parameter inp,

                query_result qry

        where

                qry.unique_id     = @qid              and

                qry.query_result  = csdc.delivery_id  and

                csdc.delivery_id  = csh.delivery_id   and

                inp.ins_num       = csh.ins_num       and

                inp.param_seq_num = csh.param_seq_num and

                abt.base_ins_type <> 48011            and  /* comm_phys_batch */ 

                abt.ins_num       = csh.ins_num       and

                abt.current_flag  = 1

	select 

		csh.delivery_id, csh.ins_num, csh.param_seq_num, abt.deal_tracking_num, csh.profile_seq_num, isnull(dt.incoterms_id,0) incoterms_id , csh2.volume_type, csh2.total_quantity as unscheduled_quantity, phys_h.trip_status_id, phys_h.price_adj_level, 
		isnull(pd.range_type,0) range_type, phys_h.schedule_complete, pgd.parcel_group_fully_scheduled

        from

                ab_tran abt

                join query_result qr on (qr.unique_id = @qid)

                join comm_sched_delivery_cmotion csdc on (qr.query_result = csdc.delivery_id)

                join comm_schedule_header csh on (qr.query_result = csh.delivery_id and abt.ins_num = csh.ins_num and csh.bav_flag=1)

                left join delivery_term dt on (dt.ins_num = csh.ins_num and dt.param_seq_num = csh.param_seq_num)

                join comm_schedule_delivery csd on (qr.query_result  = csd.delivery_id)

                join phys_header phys_h on (phys_h.ins_num = abt.ins_num)

                left join comm_sched_deliv_deal csdd on (qr.query_result  = csdd.delivery_id)

                left join comm_schedule_header csh2 on (csh2.ins_num = csh.ins_num and csh2.param_seq_num = csh.param_seq_num and csh2.profile_seq_num = csh.profile_seq_num and

							csh2.volume_type = 5  and (csh.parcel_id = csh2.parcel_id or csh2.parcel_id = 0))

                left join parcel_data pd on (pd.parcel_id = csh.parcel_id and pd.ins_num = csh.ins_num)

				left join parcel_group_data pgd on (pgd.ins_num = csh.ins_num and pgd.param_seq_num = csh.param_seq_num and pgd.parcel_group_id = csh.parcel_group_id)

        where

                abt.base_ins_type <> 48011 and  /* comm_phys_batch */ 

                abt.current_flag = 1  and 

                (csdd.deal_num = abt.deal_tracking_num or csd.service_provider_deal_num = abt.deal_tracking_num) and

                csdc.transport_class <> 7  /* Skip for TRANSPORT_CLASS_FUNGIBLE_PIPELINE (CPS) */ 

        select

	        csh.delivery_id, de.*

	    from

                delivery_event de,

                comm_schedule_header csh,

                ab_tran,

                query_result qr

        where

                qr.unique_id = @qid and

                ab_tran.base_ins_type <> 48011 and  /* comm_phys_batch */ 

                csh.ins_num = ab_tran.ins_num and ab_tran.current_flag = 1 and csh.delivery_id = qr.query_result and csh.bav_flag = 1 and

                de.source_id = csh.delivery_id and de.source_type = 2   /* DELIVERY_ES_NOMINATION */ 

        union select  

                h.delivery_id, e.*

        from

                comm_schedule_header h,

                delivery_event e,

                tsd_delivery_ticket t,

                ab_tran ab,

                query_result qr

        where

                ab.base_ins_type <> 48011 and  /* comm_phys_batch */ 

                qr.unique_id = @qid and

                h.ins_num = ab.ins_num and ab.current_flag = 1 and h.delivery_id = qr.query_result and h.bav_flag = 1 and

                h.schedule_id = t.schedule_id and e.source_id = t.id_number and t.is_actual_ticket = 1 and e.source_type = 1  /* DELIVERY_ES_DELIVERY_TICKET */ 

        select

                csdc.delivery_id, 

                batch.*, 

                ab.tran_num, 

                csh.total_quantity as net_qty, 

                csh.total_gross_quantity as gross_qty, 

                ticket_count.num_of_tickets, 

                c_nom.delivery_id as c_delivery_id,

                c_nom.source_delivery_id as c_source_delivery_id,

                i_nom.delivery_id as i_delivery_id

        from   

               comm_batch batch, 

               query_result qr,

               ab_tran ab,

               comm_schedule_header csh,

               comm_sched_delivery_cmotion csdc 

             left join (select distinct comm_schedule_header.delivery_id, count (*) num_of_tickets 

                from ab_tran, comm_schedule_header, comm_schedule_delivery, tsd_delivery_ticket, comm_sched_delivery_cmotion 

                where comm_sched_delivery_cmotion.batch_id > 0 and

                      comm_sched_delivery_cmotion.delivery_id = comm_schedule_delivery.delivery_id and 

                      comm_schedule_header.schedule_id = tsd_delivery_ticket.schedule_id and

                      tsd_delivery_ticket.container_type_id <> 1 and

                      comm_schedule_header.bav_flag = 1 and 

                      ab_tran.deal_tracking_num = comm_schedule_delivery.service_provider_deal_num and

                      ab_tran.ins_num = comm_schedule_header.ins_num and 

                      ab_tran.current_flag = 1 and

                      comm_schedule_header.delivery_id = comm_schedule_delivery.delivery_id

                group by comm_schedule_header.delivery_id) ticket_count  on ticket_count.delivery_id = csdc.delivery_id

             left join (select comm_sched_delivery_cmotion.delivery_id, comm_sched_delivery_cmotion.source_delivery_id 

                 from comm_sched_delivery_cmotion  where batch_id > 0) c_nom

                 on c_nom.delivery_id = csdc.delivery_id

             left join (select comm_sched_delivery_cmotion.delivery_id

                     from comm_sched_delivery_cmotion 

                      where comm_sched_delivery_cmotion.batch_id > 0 and

                      0=(select count (*) from comm_sched_deliv_deal csdd2 where csdd2.delivery_id = comm_sched_delivery_cmotion.delivery_id and deal_num <> 6) and 

                      1=(select count (*) from comm_sched_deliv_deal csdd3 

                      where comm_sched_delivery_cmotion.source_delivery_id <> comm_sched_delivery_cmotion.delivery_id and 

                            comm_sched_delivery_cmotion.source_delivery_id <> 0 and 

                            csdd3.deal_num <> 6 and  /* NOM_SYSTEM_TRAN_PLACEHOLDER */  

                            csdd3.delivery_id = comm_sched_delivery_cmotion.source_delivery_id)) i_nom 

                 on i_nom.delivery_id = csdc.delivery_id 

	where

               csdc.batch_id > 0 and

               batch.batch_id = csdc.batch_id and

               csh.ins_num = batch.ins_num and

               csh.bav_flag = 1 and

               ab.ins_num = batch.ins_num and 

               qr.query_result = csdc.delivery_id and

               qr.unique_id = @qid

go

GRANT EXEC ON nom_data_select_by_qid TO olf_user;
GRANT EXEC ON nom_data_select_by_qid TO olf_user_manual;      

go
