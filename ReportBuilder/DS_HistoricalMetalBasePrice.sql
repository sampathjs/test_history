--tabletitle HistoricalMetalBasePrice "Historical Metal Base Price"
--argument $$reset_date_start$$ "" "Reset Date Start"
--argument $$reset_date_end$$ "" "Reset Date End"

SELECT ihp.index_id, ihp.reset_date, ihp.ref_source, ccy.id_number as metal, idx.currency, ihp.price
FROM   currency                   ccy
JOIN   idx_gpt_def                igd ON (igd.gpt_name = ccy.name)
JOIN   idx_def                    idx ON (idx.index_version_id = igd.index_version_id)
JOIN   USER_jm_base_price_fixings ubf ON (ubf.index_id = idx.index_id AND ubf.src_gpt_id = igd.gpt_id)
JOIN   idx_historical_prices      ihp ON (ihp.index_id = ubf.target_index_id)
JOIN   ref_source                 rs  ON (rs.id_number = ihp.ref_source)
WHERE  idx.db_status = 1
AND    idx.index_name = 'JM_Base_Price'
AND    rs.name NOT LIKE 'LBMA %'
AND    rs.name NOT LIKE 'LPPM %'
AND    ihp.reset_date between '$$reset_date_start$$' and '$$reset_date_end$$'
UNION
SELECT ihp.index_id, ihp.reset_date, 0 AS ref_source, ccy.id_number as metal, idx.currency, AVG(ihp.price) AS price
FROM   currency                   ccy
JOIN   idx_gpt_def                igd ON (igd.gpt_name = ccy.name)
JOIN   idx_def                    idx ON (idx.index_version_id = igd.index_version_id)
JOIN   USER_jm_base_price_fixings ubf ON (ubf.index_id = idx.index_id AND ubf.src_gpt_id = igd.gpt_id)
JOIN   idx_historical_prices      ihp ON (ihp.index_id = ubf.target_index_id)
JOIN   ref_source                 rs  ON (rs.id_number = ihp.ref_source)
WHERE  idx.db_status = 1
AND    idx.index_name <> 'JM_Base_Price'
AND    rs.name IN ('LBMA AM', 'LBMA PM', 'LBMA Silver')
AND    ihp.reset_date between '$$reset_date_start$$' and '$$reset_date_end$$'
GROUP  BY ihp.index_id, ihp.reset_date, ccy.id_number, idx.currency
