select i.index_id, max(h.price) AS price
from idx_historical_prices h
, idx_parent_link p
, idx_def i
where i.db_status = 1
and p.index_version_id = i.index_version_id
and h.index_id = p.parent_index_id
and h.last_update = (select max(h2.last_update) from idx_historical_prices h2 where h2.index_id = h.index_id)
group by i.index_id