USE [END_UNI01]
GO
/****** Object:  StoredProcedure [dbo].[USER_update_jm_ledger_extraction]    Script Date: 19/03/2018 11:44:32 ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
ALTER procedure [dbo].[USER_update_jm_ledger_extraction]
          @extraction_id                 int,
          @new_extraction_end_time      datetime,
          @new_num_rows_extracted        int
as
BEGIN

DECLARE @error_number VARCHAR(MAX), @error_message VARCHAR(MAX), @error_procedure VARCHAR(MAX),@error_line INT = 0

BEGIN TRANSACTION
       BEGIN TRY
      update dbo.user_jm_ledger_extraction
      SET
        user_jm_ledger_extraction.extraction_end_time=@new_extraction_end_time,
              user_jm_ledger_extraction.num_rows_extracted=@new_num_rows_extracted
       where user_jm_ledger_extraction.extraction_id=@extraction_id
	   END TRY
       BEGIN CATCH
              SELECT @error_number = ERROR_NUMBER(), @error_procedure=ERROR_PROCEDURE(), @error_line=ERROR_LINE(), @error_message=ERROR_MESSAGE()
			  SET @error_message='error_number='+@error_number
				+' error_severity='+CONVERT(VARCHAR(MAX),ERROR_SEVERITY())
				+' error_state=' + CONVERT(VARCHAR(MAX),ERROR_STATE())
				+' error_procedure='+@error_procedure
				+' error_line='+CONVERT(VARCHAR(MAX),@error_line)
				+' error_message='+@error_message
              DECLARE @loggingOn INT = NULL
              DECLARE @is_variable_exists INT = 1
              
              SELECT @loggingOn=int_value FROM USER_const_repository WHERE context='Server side logging' AND sub_context='Error logging' AND name='isLoggingOn'
              
              IF (@loggingOn = 1 OR @loggingOn IS NULL)
              BEGIN
              INSERT INTO USER_JM_Components_Log (UserName,
                           Component,
                           error_logs) VALUES(SUSER_NAME()
                                                ,'AccountingFeedOutput'
                                                ,@error_message);
              END
       END CATCH
COMMIT TRAN


RETURN 0

END
