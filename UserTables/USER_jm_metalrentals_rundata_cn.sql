------------------------------------------------------------------------------
-- Account Mapping Table
------------------------------------------------------------------------------

IF OBJECT_ID('dbo.USER_jm_metalrentals_rundata_cn' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_metalrentals_rundata_cn] 

GO

create table USER_jm_metalrentals_rundata_cn
(
month_stamp varchar(255),
month_stamp_str varchar(255),
account_name varchar(255),
account_id int,
metal_description varchar(255),
status varchar(255),
deal_tracking_num int,
message varchar(255),
last_update datetime,
long_name varchar(255),
addr1 varchar(255),
addr2 varchar(255),
addr_reference_name varchar(255),
irs_terminal_num varchar(255),
city varchar(255),
county_id varchar(255),
mail_code varchar(255),
state_id varchar(255),
country varchar(255),
fax varchar(255),
phone varchar(255),
tax_id varchar(255),
party_id int,
short_name varchar(255),
addr11 varchar(255),
addr21 varchar(255),
addr_reference_name1 varchar(255),
irs_terminal_num1 varchar(255),
city1 varchar(255),
county_id1 varchar(255),
mail_code1 varchar(255),
state_id1 varchar(255),
country1 varchar(255),
fax1 varchar(255),
phone1 varchar(255),
tax_id1 varchar(255),
account_number varchar(255),
statement_date varchar(255),
reporting_unit varchar(255),
preferred_currency varchar(255),
metal varchar(255),
avg_balance float,
avg_price float,
net_interest_rate_new float,
gross_interest_rate_new float,
value float,
long_name1 varchar(255),
addr12 varchar(255),
addr22 varchar(255),
addr_reference_name2 varchar(255),
irs_terminal_num2 varchar(255),
city2 varchar(255),
county_id2 varchar(255),
mail_code2 varchar(255),
state_id2 varchar(255),
country2 varchar(255),
fax2 varchar(255),
phone2 varchar(255),
description varchar(255),
address_type_id varchar(255),
metal_interest_rate float,
avg_cny_rate float
)


grant select, insert, update, delete on USER_jm_metalrentals_rundata_cn to olf_user
grant select, insert, update, delete on USER_jm_metalrentals_rundata_cn to olf_user_manual

grant select on USER_jm_metalrentals_rundata_cn to olf_readonly

SELECT * FROM USER_jm_metalrentals_rundata_cn 

SELECT COUNT(*) FROM USER_jm_metalrentals_rundata_cn 