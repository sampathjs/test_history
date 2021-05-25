CREATE TABLE dbo.USER_gmm_user
(
    personnel_id INT,
    password     VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_user TO olf_user
GRANT SELECT ON dbo.USER_gmm_user TO olf_readonly

CREATE TABLE dbo.USER_gmm_user_group
(
    personnel_id INT,
    jm_group     VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_user_group TO olf_user
GRANT SELECT ON dbo.USER_gmm_user_group TO olf_readonly

CREATE TABLE dbo.USER_gmm_forecast
(
    [group]             VARCHAR(255),
    balance_date        VARCHAR(255),
    metal               VARCHAR(255),
    [user]              VARCHAR(255),
    company_code        VARCHAR(255),
    unit                VARCHAR(255),
    comments            VARCHAR(255),
    deliverable         INT,
    customer            VARCHAR(255),
    current_balance     INT,
    shipment_volume     INT,
    shipment_window     INT,
    basis_of_assumption VARCHAR(255),
    create_time         VARCHAR(255)
)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_gmm_forecast TO olf_user
GRANT SELECT ON dbo.USER_gmm_forecast TO olf_readonly
