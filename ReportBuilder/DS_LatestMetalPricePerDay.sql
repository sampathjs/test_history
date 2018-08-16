--tabletitle LatestMetalPricePerDay "Latest Metal Prices Per Day"
--argument $$start_date$$ "" "Dispatch Date Start"
--argument $$end_date$$ "" "Dispatch Date End"

SELECT i.index_id, dates.dispatch_date, max(h.price) AS price
FROM idx_historical_prices h
  INNER JOIN idx_parent_link p
     ON h.index_id = p.parent_index_id
  INNER JOIN idx_def i
     ON p.index_version_id = i.index_version_id
	   AND i.db_status = 1
  INNER JOIN (SELECT  DATEADD(DAY, nbr - 1, '$$start_date$$') AS dispatch_date
          FROM    ( SELECT    ROW_NUMBER() OVER ( ORDER BY c.object_id ) AS Nbr
                          FROM      sys.columns c
                        ) nbrs
          WHERE   nbr - 1 <= DATEDIFF(DAY, '$$start_date$$', '$$end_date$$')) dates 
     ON dates.dispatch_date >= '$$start_date$$' 
	   AND h.start_date = dates.dispatch_date 
GROUP BY i.index_id, dates.dispatch_date