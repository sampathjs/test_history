DECLARE @roleName NVARCHAR(50)
DECLARE @sql NVARCHAR(MAX)
SELECT @roleName = name
    FROM sys.database_principals
    WHERE name = 'olf_user_manual'
       OR name = 'olf_user' AND type = 'R'

IF OBJECT_ID('USER_metal_rentals_borrowings', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_borrowings
CREATE TABLE USER_metal_rentals_borrowings
(
    value VARCHAR(255)
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_borrowings TO ' + @roleName
EXEC (@sql)

GRANT SELECT ON USER_metal_rentals_borrowings TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_rates', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_rates
CREATE TABLE USER_metal_rentals_rates
(
    run_time   VARCHAR(255),
    index_name VARCHAR(255),
    result     VARCHAR(255),
    run_user   VARCHAR(255),
    XPT        FLOAT,
    XPD        FLOAT,
    XRH        FLOAT,
    XAU        FLOAT,
    XAG        FLOAT,
    XIR        FLOAT,
    XOS        FLOAT,
    XRU        FLOAT
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_rates TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_rates TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_deal_booking', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_deal_booking
CREATE TABLE USER_metal_rentals_deal_booking
(
    currency           VARCHAR(255),
    internal_bu        VARCHAR(255),
    internal_portfolio VARCHAR(255),
    external_bu        VARCHAR(255),
    external_portfolio VARCHAR(255),
    cashflow_type      VARCHAR(255),
    settle_date        VARCHAR(255),
    statement_date     VARCHAR(255),
    position           FLOAT,
    fx_rate            FLOAT,
    inc_vat            INT
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_deal_booking TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_deal_booking TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_user', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_user
CREATE TABLE USER_metal_rentals_user
(
    personnel_id INT,
    password     VARCHAR(255)
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_user TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_user TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_statement', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_statement
CREATE TABLE USER_metal_rentals_statement
(
    run_time        VARCHAR(255),
    statement_month VARCHAR(255),
    result          VARCHAR(255),
    run_user        VARCHAR(255),
    party           VARCHAR(255),
    account_group   VARCHAR(255),
    statement_path  VARCHAR(255),
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_statement TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_statement TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_emailing', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_emailing
CREATE TABLE USER_metal_rentals_emailing
(
    run_time        VARCHAR(255),
    statement_month VARCHAR(255),
    result          VARCHAR(255),
    run_user        VARCHAR(255),
    party           VARCHAR(255),
    contact         VARCHAR(255),
    email           VARCHAR(255),
    statement_path  VARCHAR(255),
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_emailing TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_emailing TO olf_readonly

IF OBJECT_ID('USER_metal_rentals_booked_deal', 'U') IS NOT NULL DROP TABLE USER_metal_rentals_booked_deal
CREATE TABLE USER_metal_rentals_booked_deal
(
    run_time           VARCHAR(255),
    statement_month    VARCHAR(255),
    result             VARCHAR(255),
    run_user           VARCHAR(255),
    internal_bu        VARCHAR(255),
    internal_portfolio VARCHAR(255),
    external_bu        VARCHAR(255),
    external_portfolio VARCHAR(255),
    cashflow_type      VARCHAR(255),
    settle_date        VARCHAR(255),
    statement_date     VARCHAR(255),
    position           FLOAT,
    fx_rate            FLOAT,
    tran_num           VARCHAR(255),
)
SET @sql = 'GRANT SELECT, INSERT, UPDATE, DELETE ON USER_metal_rentals_booked_deal TO ' + @roleName
EXEC (@sql)
GRANT SELECT ON USER_metal_rentals_booked_deal TO olf_readonly
