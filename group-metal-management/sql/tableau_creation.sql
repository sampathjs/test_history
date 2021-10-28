if not exists (select * from sysobjects where name='USER_tableau_grp_cust_balances')
CREATE TABLE dbo.USER_tableau_grp_cust_balances
(
    
    balance_date                VARCHAR(10) NULL,
    metal                       VARCHAR(255) NOT NULL,
    deliverable_metal           FLOAT NULL,
    group_name                  VARCHAR(255) NOT NULL,
    company_code                VARCHAR(255) NULL,
    customer                    VARCHAR(255) NOT NULL,
    current_balance             FLOAT NULL,
    in_use                      FLOAT NULL,
    basis_of_assumption         VARCHAR(255) NULL,
    comments                    VARCHAR(255) NULL,
    unit                        VARCHAR(255) NULL,
    deliverable_metal_toz       FLOAT NULL,
    current_balance_toz         FLOAT NULL,
    in_use_toz                  FLOAT NULL,
    customer_excess_metal_toz   FLOAT NULL,
    last_update                 VARCHAR(255) NULL,
    personnel_id                INT NULL

)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_tableau_grp_cust_balances TO olf_user
GRANT SELECT ON dbo.USER_tableau_grp_cust_balances TO olf_readonly
GO 


if not exists (select * from sysobjects where name='USER_tableau_grp_cust_balances_h')
CREATE TABLE dbo.USER_tableau_grp_cust_balances_h
(
    
    balance_date                VARCHAR(10) NULL,
    metal                       VARCHAR(255) NOT NULL,
    deliverable_metal           FLOAT NULL,
    group_name                  VARCHAR(255) NOT NULL,
    company_code                VARCHAR(255) NULL,
    customer                    VARCHAR(255) NOT NULL,
    current_balance             FLOAT NULL,
    in_use                      FLOAT NULL,
    basis_of_assumption         VARCHAR(255) NULL,
    comments                    VARCHAR(255) NULL,
    unit                        VARCHAR(255) NULL,
    deliverable_metal_toz       FLOAT NULL,
    current_balance_toz         FLOAT NULL,
    in_use_toz                  FLOAT NULL,
    customer_excess_metal_toz   FLOAT NULL,
    last_update                 VARCHAR(255) NULL,
    personnel_id                INT NULL

)
GRANT SELECT, INSERT, UPDATE, DELETE ON dbo.USER_tableau_grp_cust_balances_h TO olf_user
GRANT SELECT ON dbo.USER_tableau_grp_cust_balances_h TO olf_readonly
GO 
