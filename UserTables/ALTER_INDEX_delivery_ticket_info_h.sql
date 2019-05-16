ALTER INDEX deliv_tkt_info_h_idx 
ON delivery_ticket_info_h
REBUILD WITH (DATA_COMPRESSION=PAGE, FILLFACTOR=100) -- 8 mins 33
GO

