BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_dg_quick_query' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_dg_quick_query] 

GO

create Table [dbo].USER_dg_quick_query 
( 
query_id int , 
query_name varchar(255) , 
attribute_id int , 
attribute_value_int int , 
attribute_value_dbl double precision , 
attribute_value_str varchar(255)
); 


grant select, insert, update, delete on [dbo].[USER_dg_quick_query] to olf_user, olf_user_manual

grant select on [dbo].[USER_dg_quick_query] to olf_readonly

 
COMMIT; 

