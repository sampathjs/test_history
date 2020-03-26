------------------------------------------------------------------
--- UPDATE THE .abacus_database_admin so that the stored proc ol_delete_file_object can run that clears up dir node
---------------------------------------------------------------------------

 INSERT INTO dbo.abacus_database_admin
	 VALUES ('ol_delete_file_object','clean up dir_node table') 