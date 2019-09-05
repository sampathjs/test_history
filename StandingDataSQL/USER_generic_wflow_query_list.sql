


BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_generic_wflow_query_list' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_generic_wflow_query_list] 

GO

create Table [dbo].[USER_generic_wflow_query_list]
(
id int,
query_name varchar(200),
query varchar(255),
sequence int,
active tinyint,
exact_expected_rows int,
max_expected_rows int
);

grant select, insert, update, delete on USER_generic_wflow_query_list to olf_user
grant select on USER_generic_wflow_query_list to olf_readonly
grant select,insert,update,delete on USER_generic_wflow_query_list to olf_user_manual

COMMIT; 


INSERT INTO USER_generic_wflow_query_list VALUES(1, 'Accounts without account class set', 'SELECT a.account_id, a.account_name, a.account_number, a.account_status, a.account_class, a.last_update FROM account a WHERE a.account_class NOT IN (SELECT account_class_id FROM account_class) AND a.account_status  = 1', 1, 1, 0, 0)
GO

INSERT INTO USER_generic_wflow_query_list VALUES(2, 'Long Running TPM workflows', 
'SELECT br.op_services_run_id, br.instance_id, br.service_id, bd.bpm_name, bc.bpm_category_name, bs.name as bpm_status, p.name as submitter, br.start_time, br.item_num ,  LTRIM(DATEDIFF(MINUTE, 0, GETDATE() - br.start_time))  running_time', 1, 1, 0, 0)
Go

INSERT INTO USER_generic_wflow_query_list VALUES(3, 'Long Running TPM workflows', 
'FROM bpm_running br JOIN bpm_definition bd ON (br.bpm_definition_id = bd.id_number) JOIN bpm_category bc ON (br.category_id = bc.bpm_category_id) JOIN bpm_status bs ON (br.bpm_status = bs.id_number) JOIN personnel p ON (p.id_number = br.submitter_id)', 2, 1, 0, 0)
GO

INSERT INTO USER_generic_wflow_query_list VALUES(4, 'Long Running TPM workflows', 
'WHERE bd.bpm_name NOT IN (''Credit Management'', ''Risk Management'', ''Dispatch'') AND  LTRIM(DATEDIFF(MINUTE, 0, GETDATE() - br.start_time))  > 60', 3, 1, 0, 0)
GO


select * from USER_generic_wflow_query_list