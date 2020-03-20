Deployment
* Generate the JAR file with all the dependencies:
    * Command: .\mvnw.cmd clean package
    * File path: target\limits-reporting-1.0-jar-with-dependencies.jar 
* Copy JAR and update AB_CLASSPATH in the config file
* Import OpenComponents scripts under "src\main\java\com\matthey\pmm\limits\reporting" into Endur, with project name "LimitsReporting" 
* Create tables using "sql\usertable.sql"
* Run CMM - the following entries should be included
    * Functional Groups
        * EOD Limits Reporting - Dealing
        * EOD Limits Reporting - Lease
        * EOD Limits Reporting - Liquidity
        * EOD Limits Reporting - Summary
    * Queries
        * Limits Reporting - Lease Deals
    * Report Builder Report
        * PMM Closing Position by Metal and BU
    * Tasks
        * Limits Reporting EOD
        * Limits Reporting Intraday
        * Limits Reporting Summary
    * User Tables
        * USER_limits_reporting_account
        * USER_limits_reporting_balance
        * USER_limits_reporting_dealing
        * USER_limits_reporting_lease
        * USER_limits_reporting_liquidity
    * TPM
        * Global EOD
    * Workflow
        * Intraday Desk Limits
        * Weekly Summary
