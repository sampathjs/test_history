CREATE CLUSTERED INDEX USER_CL_apm_batch_timings_hist
ON apm_batch_timings_hist(start_time)
WITH (DATA_COMPRESSION=PAGE,FILLFACTOR=100) -- 3 mins 48
GO

ALTER INDEX apm_batch_timings_hist_idx 
ON apm_batch_timings_hist REBUILD WITH (DATA_COMPRESSION=PAGE)

