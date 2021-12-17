if exists (select * from sysobjects where name='USER_jm_group')
BEGIN
	INSERT INTO dbo.USER_jm_group VALUES('CA Europe',1);
	INSERT INTO dbo.USER_jm_group VALUES('CA US',1);
	INSERT INTO dbo.USER_jm_group VALUES('R&C Europe',1);
	INSERT INTO dbo.USER_jm_group VALUES('R&C US',1);
	INSERT INTO dbo.USER_jm_group VALUES('NM UK',1);
	INSERT INTO dbo.USER_jm_group VALUES('NM US',1);
END
GO