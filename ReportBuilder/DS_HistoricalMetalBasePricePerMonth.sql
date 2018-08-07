--tabletitle HistoricalMetalBasePrice "Historical Metal Base PricePerMonth"
--argument $$reset_date_start$$ "" "Reset Date Start"
--argument $$reset_date_end$$ "" "Reset Date End"

SELECT ubf.target_index_id AS index_id, ISNULL(ihp.ref_source, 0) AS ref_source, ccy.id_number as metal, idx.currency, AVG(ihp.price) AS monthly_price
FROM   currency                   ccy
JOIN   idx_gpt_def                igd ON (igd.gpt_name = ccy.name)
JOIN   idx_def                    idx ON (idx.index_version_id = igd.index_version_id)
INNER JOIN USER_jm_base_price_fixings ubf ON (ubf.index_id = idx.index_id AND ubf.src_gpt_id = igd.gpt_id)
LEFT OUTER JOIN (SELECT  DATEADD(DAY, nbr - 1, '$$reset_date_start$$') AS reset_date
          FROM    ( SELECT    ROW_NUMBER() OVER ( ORDER BY c.object_id ) AS Nbr
                          FROM      sys.columns c
                        ) nbrs
          WHERE   nbr - 1 <= DATEDIFF(DAY, '$$reset_date_start$$', '$$reset_date_end$$')) dates ON dates.reset_date >= '$$reset_date_start$$'
LEFT OUTER JOIN   idx_historical_prices      ihp ON (ihp.index_id = ubf.target_index_id  AND FORMAT(dates.reset_date, 'yyyyMMdd') = FORMAT(ihp.reset_date,'yyyyMMdd')) 
WHERE  idx.db_status = 1
AND    idx.index_name = 'JM_Base_Price'
AND ISNULL(ihp.ref_source, 0) IN (0, 20011, 20012, 20013, 20008)
AND idx.currency = (SELECT  MIN (idx2.currency) 
      FROM   currency                   ccy2
      JOIN   idx_gpt_def                igd2 ON (igd2.gpt_name = ccy2.name)
      JOIN   idx_def                    idx2 ON (idx2.index_version_id = igd2.index_version_id)
      WHERE  idx2.db_status = 1
         AND    idx2.index_name = 'JM_Base_Price'
)
GROUP  BY  ubf.target_index_id, ccy.id_number, idx.currency, ISNULL(ihp.ref_source, 0)

UNION
(SELECT  ubf.target_index_id AS index_id, 0 AS ref_source, ccy.id_number as metal, idx.currency, AVG(ihp.price) AS monthly_price
FROM   currency                   ccy
JOIN   idx_gpt_def                igd ON (igd.gpt_name = ccy.name)
JOIN   idx_def                    idx ON (idx.index_version_id = igd.index_version_id)
JOIN   USER_jm_base_price_fixings ubf ON (ubf.index_id = idx.index_id AND ubf.src_gpt_id = igd.gpt_id)
INNER JOIN (SELECT  DATEADD(DAY, nbr - 1, '$$reset_date_start$$') AS reset_date
          FROM    ( SELECT    ROW_NUMBER() OVER ( ORDER BY c.object_id ) AS Nbr
                          FROM      sys.columns c
                        ) nbrs
          WHERE   nbr - 1 <= DATEDIFF(DAY, '$$reset_date_start$$', '$$reset_date_end$$')) dates ON dates.reset_date >= '$$reset_date_start$$'
LEFT OUTER JOIN idx_historical_prices      ihp ON (ihp.index_id = ubf.target_index_id  AND FORMAT(dates.reset_date, 'yyyyMMdd') = FORMAT(ihp.reset_date,'yyyyMMdd'))
WHERE  idx.db_status = 1 
AND    idx.index_name <> 'JM_Base_Price'
AND ISNULL(ihp.ref_source, 0) IN (0, 20002, 20007, 20006, 20022)
AND idx.currency = (SELECT MIN (idx3.currency) 
   FROM   currency                     ccy3
      JOIN   idx_gpt_def                igd3 ON (igd3.gpt_name = ccy3.name)
      JOIN   idx_def                       idx3 ON (idx3.index_version_id = igd3.index_version_id)
   WHERE  idx3.db_status = 1
      AND    idx3.index_name <> 'JM_Base_Price'
)
GROUP  BY  ubf.target_index_id, ccy.id_number, idx.currency)
-- Adding USD to USD conversion rates as they are not explicitly saved but required in subsequent join
UNION
(SELECT 0 as index_id, 20013 as ref_source, 0 as metal, 0 as currency, 1.0 as monthly_price)  -- JM HK Closing
UNION
(SELECT 0 as index_id, 20012 as ref_source, 0 as metal, 0 as currency, 1.0 as monthly_price) -- JM HK Opening
UNION
(SELECT 0 as index_id, 20011 as ref_source, 0 as metal, 0 as currency, 1.0 as monthly_price) -- JM London Opening
UNION
(SELECT 0 as index_id, 20008  as ref_source, 0 as metal, 0 as currency, 1.0 as monthly_price) -- JM NY Opening
