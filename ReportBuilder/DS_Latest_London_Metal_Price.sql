select
index_id, price 
from
 idx_historical_prices i
 where 
i.ref_source = 20011
and i.reset_date =  (select max(h2.reset_date) from idx_historical_prices h2 where h2.index_id in (1020058, 1020060, 1020061, 1020065, 1020069, 1020077)  and h2.ref_source = 20011)
and i.index_id in (1020058, 1020060, 1020061, 1020065, 1020069, 1020077)
