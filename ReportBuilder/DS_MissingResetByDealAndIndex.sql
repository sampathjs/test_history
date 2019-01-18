SELECT a.deal_tracking_num DealNum, CAST(r.reset_date AS date) ResetDate, CAST(r.ristart_date AS date) RFISDate, p.price DealResetPrice,
id.index_name IndexName, rf.name IndexRefSource, p.price AvailablePrice_Curve,
fid.index_name FxIndex, frf.name FxIndexRefSource, fxp.price AvailablePrice_FxCurve,
DATENAME(WEEKDAY,r.reset_Date) Weekday, 
(CASE WHEN r.reset_date IN (SELECT CAST(holiday_date AS date) FROM holiday_detail WHERE holiday_num = 20001 AND active= 1) THEN 'Y' ELSE 'N' END) Holiday, 
(CASE WHEN r.reset_date IN (SELECT CAST(holiday_date AS date) FROM holiday_detail WHERE holiday_num = 20001 AND active= 1) THEN (SELECT name FROM holiday_detail WHERE holiday_num = 20001 AND active= 1 AND holiday_date = r.reset_date) ELSE 'N' END) Holiday_Desc
FROM ab_tran a
    JOIN reset r ON (a.ins_num = r.ins_num AND r.block_end = 0)
    JOIN param_reset_header h ON (r.ins_num = h.ins_num AND r.param_seq_num = h.param_seq_num AND r.param_reset_header_seq_num = h.param_reset_header_seq_num)
    LEFT JOIN idx_historical_prices p ON ( p.index_id = h.proj_index AND h.ref_source = p.ref_source AND r.reset_date = p.reset_date AND r.ristart_date = p.start_date) 
    LEFT JOIN idx_historical_prices fxp ON (fxp.index_id = h.spot_idx AND h.spot_ref_source = fxp.ref_source AND r.reset_date = fxp.reset_date AND r.ristart_date = fxp.start_date) 
    JOIN idx_def id ON (id.index_id = h.proj_index AND id.db_status = 1 AND id.index_status = 2)
    LEFT JOIN idx_def fid ON (fid.index_id = h.spot_idx AND fid.db_status = 1 AND fid.index_status = 2)
    JOIN ref_source rf ON (rf.id_number = h.ref_source)
    JOIN ref_source frf ON (frf.id_number = h.spot_ref_source)
WHERE a.tran_status = 3 AND a.trade_flag = 1 AND (ISNULL(p.price,0) = 0 OR ISNULL(fxp.price,0) = 0)  AND a.ins_type IN (30201) 
AND r.reset_date BETWEEN '$$start_date$$' AND '$$end_date$$'
ORDER BY a.deal_tracking_num DESC, r.reset_date