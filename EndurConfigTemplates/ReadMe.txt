Rel	- Description of change
005 - V17 Baseline
006 - freemarker-2.3.30.jar, guava-20.0.jar, joda-time-2.10.5.jar, logback-classic-1.2.3.jar, logback-core-1.1.11.jar, slf4j-api-1.7.30.jar, mail-1.5.0-b01.jar for Compliance Reporting
007 - Removing AB_CLASSPATH references to logger.jar and alertbroker_shared_v14.jar
007_MetalRentals - New variables for MetalRentals included
008 - reinstated reference for alertbroker_shared_v14.jar
009 - move \x64\bin\connex\alertbroker\jars\alertbroker_shared_v14.jar to COMMON_JARS_06_20200605
010 - Put Helpdir on %ENDUR_DRIVE_LETTER% and re-enabled memory dumps.
010_MetalRentals + EndurConnector - Updates for latest version of metal rentals against V17 Endur
011 - Support for jsoup-1.13.1.jar
012 - Removed the parameter to create a mini dump file when script engines crash from AB_JVM_PARAMETERS.  Changed the Spread Log file name to be 'Spread_<EnvName>.log'
013 - Updates to the common_jars classpath entries to allow wildcards (Yi Hu, Jan 2021)