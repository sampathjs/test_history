begin tran
IF OBJECT_ID('dbo.USER_bunit_ins_defaultdate ' , 'U') IS NOT NULL
  DROP TABLE [dbo].[USER_bunit_ins_defaultdate ]
GO

CREATE TABLE USER_bunit_ins_defaultdate 
(
	internal_bunit varchar(255),
	toolset varchar(255),
	type_name varchar(255),
	value varchar(255)
)
go
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_bunit_ins_defaultdate  TO olf_user--, olf_user_manual;
go

go
insert into USER_bunit_ins_defaultdate values ('TANAKA KIKINZOKU KOGYO KK - BU','ComSwap','Cash Payment Term','7d')
go

select * from USER_bunit_ins_defaultdate

go
commit tran