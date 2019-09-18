if exists
(
      select *
      from sysobjects
      where name = 'USER_purge_tpm_disconnected_variables' and type = 'P'
)
begin
      drop proc USER_purge_tpm_disconnected_variables
end
go

CREATE procedure [dbo].[USER_purge_tpm_disconnected_variables]
	
as
declare @err int, @rcount int
	delete from 
		jbpm_variableinstance 
	where 
		not exists ( select p.id_ from jbpm_processinstance p where p.id_ = processinstance_)
   execute @err = shrink_table 'jbpm_byteblock'
	if( @@error != 0 or @err != 0 )
	begin
		raiserror ('25002:Error Running Nested Stored Procedure USER_purge_tpm_disconnected_variables', 16, -1 )
		return -1
	end

go

GRANT EXEC ON USER_purge_tpm_disconnected_variables TO olf_user;
GRANT EXEC ON USER_purge_tpm_disconnected_variables TO olf_user_manual;      

go