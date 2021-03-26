insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_Balance_Change_Alert','Total customer Metal Balance Alert',3,'NA',-20,0,'N','Y');
go
insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_Balance_Change_Alert','Customer Specific Metal Balance Alert',3,'NA',-20,0,'Y','N');
go
insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_SpotPrice_Change_Alert','Metal Spot Price Change',1,'Spot',0,10,'NA','Y');
go
insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_SpotPrice_Change_Alert','Metal Spot Price Change',10,'Spot',0,25,'NA','Y');
go
insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_LeasePrice_Change_Alert','Metal Spot Price Change',7,'1m',0,20,'NA','Y');
go
insert into [dbo].[USER_jm_metals_email_alert_rule]
(task_name,rule_description,monitoring_window,grid_point,threshold_lower_limit,threshold_upper_limit,customer_balance_reporting,active)
values
('Metals_LeasePrice_Change_Alert','Metal Spot Price Change',7,'3m',0,20,'NA','Y');
go

select * from [dbo].[USER_jm_metals_email_alert_rule]
go

SELECT COUNT(*) from [dbo].[USER_jm_metals_email_alert_rule]
go