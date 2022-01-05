if not exists (select * from sysobjects where name='USER_gmm_user')
CREATE TABLE dbo.USER_gmm_user
(
    personnel_id INT,
    password     VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_user TO olf_user
GRANT SELECT ON dbo.USER_gmm_user TO olf_readonly
GO 

if not exists (select * from sysobjects where name='USER_gmm_user_group')
CREATE TABLE dbo.USER_gmm_user_group
(
    personnel_id INT,
    jm_group     VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_user_group TO olf_user
GRANT SELECT ON dbo.USER_gmm_user_group TO olf_readonly
GO

if not exists (select * from sysobjects where name='USER_gmm_forecast')
CREATE TABLE dbo.USER_gmm_forecast
(
    group_name          VARCHAR(255),
    balance_date        VARCHAR(255),
    metal               VARCHAR(255),
    user_name           VARCHAR(255),
    company_code        VARCHAR(255),
    unit                VARCHAR(255),
    comments            VARCHAR(255),
    deliverable         FLOAT,
    customer            VARCHAR(255),
    current_balance     FLOAT,
    -- shipment_volume     FLOAT,
    in_use              FLOAT,
    shipment_window     INT,
    basis_of_assumption VARCHAR(255),
    create_time         VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_forecast TO olf_user
GRANT SELECT ON dbo.USER_gmm_forecast TO olf_readonly
GO

if not exists (select * from sysobjects where name='USER_jm_group')
CREATE TABLE dbo.USER_jm_group
(
    jm_group_name     VARCHAR(255),
    active INT
    
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_jm_group TO olf_user
GRANT SELECT ON dbo.USER_jm_group TO olf_readonly
GO
