IF OBJECT_ID('USER_limits_reporting_account', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_account
CREATE TABLE USER_limits_reporting_account
(
    account_name VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_account TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_account TO olf_readonly

IF OBJECT_ID('USER_limits_reporting_balance', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_balance
CREATE TABLE USER_limits_reporting_balance
(
    balance_line VARCHAR(255),
    purpose      VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_balance TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_balance TO olf_readonly

IF OBJECT_ID('USER_limits_reporting_liquidity', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_liquidity
CREATE TABLE USER_limits_reporting_liquidity
(
    metal         VARCHAR(255),
    lower_limit   INT,
    upper_limit   INT,
    max_liability INT
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_liquidity TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_liquidity TO olf_readonly

IF OBJECT_ID('USER_limits_reporting_dealing', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_dealing
CREATE TABLE USER_limits_reporting_dealing
(
    limit_type VARCHAR(255),
    desk       VARCHAR(255),
    metal      VARCHAR(255),
    limit      INT
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_dealing TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_dealing TO olf_readonly

IF OBJECT_ID('USER_limits_reporting_lease', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_lease
CREATE TABLE USER_limits_reporting_lease
(
    parameter VARCHAR(255),
    value     INT
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_lease TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_lease TO olf_readonly

IF OBJECT_ID('USER_limits_reporting_result', 'U') IS NOT NULL DROP TABLE USER_limits_reporting_result
CREATE TABLE USER_limits_reporting_result
(
    run_date                DATETIME,
    run_type                VARCHAR(255),
    desk                    VARCHAR(255),
    metal                   VARCHAR(255),
    liquidity_lower_limit   INT,
    liquidity_upper_limit   INT,
    liquidity_max_liability INT,
    position_limit          INT,
    breach                  INT,
    liquidity_breach_limit  VARCHAR(255),
    current_position        FLOAT,
    liquidity_diff          INT,
    breach_toz              FLOAT,
    breach_gbp              FLOAT,
    critical                INT,
    breach_dates            VARCHAR(255),
    update_time             VARCHAR(255),
)
GRANT SELECT, INSERT, UPDATE, DELETE ON USER_limits_reporting_result TO olf_user_manual
GRANT SELECT ON USER_limits_reporting_result TO olf_readonly
