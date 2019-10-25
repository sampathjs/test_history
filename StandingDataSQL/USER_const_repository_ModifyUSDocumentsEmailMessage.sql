begin tran

select count(*) from USER_const_repository

go

insert into dbo.USER_const_repository([context],[sub_context],[name],[type],[string_value]) values ('SCBO', 'DocsOutput', 'Do_Not_Reply_Email_Message_Text_US', 2, 'PLEASE DO NOT REPLY TO THIS AUTO GENERATED EMAIL. IF YOU HAVE ANY QUERIES, PLEASE RESPOND TO: PmmInstructions@jmusa.com')


go
select count(*) from USER_const_repository

go
commit tran

go

select * from USER_const_repository where [sub_context] like '%DocsOutput%'


