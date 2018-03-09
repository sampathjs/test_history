/****** DMS Configuration v3.0  ******/
/****** Who 	  Date  	    ******/
/****** C Badcock 20/03/2018    ******/


BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_dg_doc_listing_queries' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_dg_doc_listing_queries] 
  

create Table [dbo].USER_dg_doc_listing_queries 
( 
query_name varchar(255) , 
widget_id int , 
widget_name varchar(255) , 
widget_type int , 
widget_value varchar(255)  
); 


grant select, insert, update, delete on [dbo].[USER_dg_doc_listing_queries] to olf_user, olf_user_manual

grant select on [dbo].[USER_dg_doc_listing_queries] to olf_readonly

 
commit; 