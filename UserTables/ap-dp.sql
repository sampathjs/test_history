CREATE TABLE dbo.USER_jm_ap_dp_reporting
(
    run_date       VARCHAR(255),
    pricing_type   VARCHAR(255),
    deal_num       INT,
    customer       VARCHAR(255),
    deal_date      VARCHAR(255),
    expiry_date    VARCHAR(255),
    days_to_expiry INT,
    open_toz       FLOAT,
    last_update    DATETIME DEFAULT (GETDATE())
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_jm_ap_dp_reporting TO olf_user
GRANT SELECT ON dbo.USER_jm_ap_dp_reporting TO olf_readonly

CREATE TABLE dbo.USER_jm_ap_dp_interest
(
    customer_id   INT,
    interest_rate FLOAT,
    start_date    DATETIME,
    end_date      DATETIME
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_jm_ap_dp_interest TO olf_user
GRANT SELECT ON dbo.USER_jm_ap_dp_interest TO olf_readonly

CREATE TABLE dbo.USER_jm_ap_dp_interest_charges
(
    customer_id     INT,
    run_date        DATETIME,
    interest_charge FLOAT,
    effective       VARCHAR(255),
    last_update     DATETIME
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_jm_ap_dp_interest_charges TO olf_user
GRANT SELECT ON dbo.USER_jm_ap_dp_interest_charges TO olf_readonly

CREATE TABLE dbo.USER_jm_ap_dp_pricingwindow
(
    customer_id    INT,
    pricing_type   VARCHAR(255),
    metal_ccy_id   INT,
    pricing_window INT
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_jm_ap_dp_pricingwindow TO olf_user
GRANT SELECT ON dbo.USER_jm_ap_dp_pricingwindow TO olf_readonly
