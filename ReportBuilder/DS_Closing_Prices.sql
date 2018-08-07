--tabletitle closing_price "Closing Price"

Select idx.index_id, idx.price, idx.last_update, c.id_number 
From idx_historical_prices idx INNER JOIN (Select MAX(last_update) As last_update, index_id From idx_historical_prices Group By index_id) As idx2
On idx.index_id = idx2.index_id And idx.last_update = idx2.last_update
INNER JOIN currency c On c.spot_index = idx.index_id
Where c.precious_metal = 1
Order By idx.last_update Desc