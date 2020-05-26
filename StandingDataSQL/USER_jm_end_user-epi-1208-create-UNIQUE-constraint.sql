

IF  EXISTS (SELECT * FROM dbo.sysobjects WHERE id = OBJECT_ID(N'[UC_EndUser]') AND xtype = 'UQ')
BEGIN
ALTER TABLE USER_jm_end_user DROP CONSTRAINT [UC_EndUser]
END

GO

ALTER TABLE USER_jm_end_user ADD CONSTRAINT UC_EndUser UNIQUE (jm_group_company, end_user_customer);

GO