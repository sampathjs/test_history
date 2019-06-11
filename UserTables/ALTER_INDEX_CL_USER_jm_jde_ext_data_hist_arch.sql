CREATE CLUSTERED INDEX CL_USER_jm_jde_ext_data_hist_arch
ON USER_jm_jde_ext_data_hist_arch(entry_date, entry_time)
WITH (DATA_COMPRESSION=PAGE) -- 38 seconds

