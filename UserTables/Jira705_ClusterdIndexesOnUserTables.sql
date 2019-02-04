-- Supplied by S Hindmarsh to improve efficiency for some of our user tables

CREATE CLUSTERED INDEX CL_user_jm_jde_extract_data ON user_jm_jde_extract_data(deal_num) 

CREATE CLUSTERED INDEX CL_User_jm_jde_extract_data_hist ON user_jm_jde_extract_data_hist(deal_num, entry_date, entry_time) WITH (DATA_COMPRESSION=PAGE)

CREATE CLUSTERED INDEX CL_User_alert_log on user_alert_log(log_id)
