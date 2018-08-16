--tabletitle jm_is_transaction_metal "Is Transaction Metal"
--join tran_num TO ab_tran.tran_num

SELECT at.tran_num
, SIGN(SUM(CASE WHEN ig.name='Base Metal' THEN 1 WHEN i.name='PREC-EXCH-FUT' THEN 1 ELSE cur.precious_metal end))  isPreciousMetal

FROM ab_tran at
JOIN instruments i ON i.name in ('FX', 'METAL-SWAP', 'METAL-B-SWAP','PREC-EXCH-FUT') AND i.id_number=at.base_ins_type
JOIN ins_parameter ip ON at.ins_num=ip.ins_num
JOIN trans_status ts ON at.tran_status=ts.trans_status_id AND  ts.name in ('Validated', 'Cancelled') 
LEFT JOIN currency cur ON ip.currency =cur.id_number
LEFT JOIN idx_group ig ON at.idx_group=ig.id_number  
WHERE at.current_flag=1
-- AND at.base_ins_type in (26001,30201,30202,32007) -- FX instrument & Swap type
group by at.tran_num,at.ins_type

