BEGIN TRANSACTION
IF OBJECT_ID('dbo.USER_jm_transfercharges_criteria' , 'U') IS NOT NULL
	  DROP TABLE [dbo].[USER_jm_transfercharges_criteria]
GO
CREATE TABLE [dbo].[USER_jm_transfercharges_criteria](
	
	[internal_bunit] [varchar](255) NOT NULL,
	[external_bunit] [varchar](255) NOT NULL,
	[generate_charge] [varchar](255) NOT NULL,
	

)
CREATE UNIQUE INDEX transfercharges_index
ON [dbo].[USER_jm_transfercharges_criteria]
(
	[internal_bunit],
	[external_bunit],
	[generate_charge]
);


grant select on [USER_jm_transfercharges_criteria] to olf_readonly

grant select, insert, update, delete on [USER_jm_transfercharges_criteria] to olf_user, olf_readonly

GO

SET ANSI_PADDING OFF
GO


COMMIT
 