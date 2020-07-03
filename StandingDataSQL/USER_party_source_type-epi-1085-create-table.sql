
-- create table 'USER_party_source_type'

BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_party_source_type' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_party_source_type] 

GO

create Table [dbo].[USER_party_source_type]
(
source_type varchar(255)
);

grant select, insert, update, delete on USER_party_source_type to olf_user
grant select on USER_party_source_type to olf_readonly
grant select,insert,update,delete on USER_party_source_type to olf_user_manual

COMMIT; 