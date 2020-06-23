

IF NOT EXISTS(SELECT 1 FROM sys.columns 
          WHERE Name = N'sector'
          AND Object_ID = Object_ID(N'USER_jm_end_user'))
BEGIN
ALTER Table USER_jm_end_user ADD sector varchar(255);

END

IF NOT EXISTS(SELECT 1 FROM sys.columns 
          WHERE Name = N'sub_sector'
          AND Object_ID = Object_ID(N'USER_jm_end_user'))
BEGIN
ALTER Table USER_jm_end_user ADD sub_sector varchar(255);

END

IF NOT EXISTS(SELECT 1 FROM sys.columns 
          WHERE Name = N'source_type'
          AND Object_ID = Object_ID(N'USER_jm_end_user'))
BEGIN
ALTER Table USER_jm_end_user ADD source_type varchar(255);

END