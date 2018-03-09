//****************************************************************************
//*                                                                          *
//*              Copyright 2014 OpenLink Financial, Inc.                     *
//*                                                                          *
//*                        ALL RIGHTS RESERVED                               *
//*                                                                          *
//****************************************************************************

// This class implements the process of analysing the statistics of incremental updates.
// It generate analysis tables and their corresponding CSV files.

package standard.apm.utilities;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.olf.openjvs.DBase;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Table;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.enums.*;

public class APM_IncUpdates_Analysis_Main implements IScript {
	
	@Override
	public void execute(IContainerContext context) throws OException
	{
		Table mListOfServices;
		ODateTime mStartDateTime;
		ODateTime mEndDateTime;
		int mBucketSize;
		int mServerUsageBucketSize;
		String mDirectory;
		String mAnalysisReportFile;
		String mShowAnalysisTableResultsFlag;
		String mCSVfile;
		String mSlaCSVfile;
		String mDurationColumn;
		
		Table argt = context.getArgumentsTable();
		
		if (argt.getNumRows() > 0)
		{
			mListOfServices = argt.getTable("apm_services", 1);
			mStartDateTime = argt.getDateTime("start_date_time", 1);
			mEndDateTime = argt.getDateTime("end_date_time", 1);
			mBucketSize = argt.getInt("bucket_size", 1);
			mDirectory = argt.getString("directory", 1);
			mAnalysisReportFile = argt.getString("analysis_report_file", 1);
			mShowAnalysisTableResultsFlag = argt.getString("show_final_table", 1);
			mCSVfile = argt.getString("csv_filename", 1);
			mSlaCSVfile = argt.getString("sla_filename", 1);	
			mDurationColumn = "total_queue_duration";
			
			if (argt.getColNum("server_usage_bucket_size") > 0)
				mServerUsageBucketSize = argt.getInt("server_usage_bucket_size", 1);
			else
				mServerUsageBucketSize = 60;
			APM_IncUpdates_Analysis_Args_Validator validator = new APM_IncUpdates_Analysis_Args_Validator();
			validator.ValidateArguments(mListOfServices, mCSVfile, mStartDateTime, mEndDateTime, mBucketSize, mDirectory, mAnalysisReportFile, mShowAnalysisTableResultsFlag);
			
			try
			{
				if(!validator.Succeeded())
					throw new OException( validator.ValidationReport());
				
				int endurMajorVarsion = GetEndurVersion();
				
				// To test the old version like TRAILSTONE, we need to uncomment the following line.
				//endurMajorVarsion = 15; 
				
				StatsColumnsNames statsColumnsName = new StatsColumnsNames(endurMajorVarsion);
				
				IncUpdatesStatisticsAnalyser incUpdatesStatisticsAnalyser = new IncUpdatesStatisticsAnalyser(statsColumnsName, mListOfServices, 
						mStartDateTime, mEndDateTime, mBucketSize, mServerUsageBucketSize, mShowAnalysisTableResultsFlag, 
						mDirectory, mAnalysisReportFile, mCSVfile, mSlaCSVfile, mDurationColumn);
					
				incUpdatesStatisticsAnalyser.LoadIncUpdatesStatistics();
				incUpdatesStatisticsAnalyser.GenerateIncUpdatesStatisticsBuckets();
				incUpdatesStatisticsAnalyser.GenerateIncUpdatesServerUsageBuckets();
				incUpdatesStatisticsAnalyser.GenerateIncUpdatesStatisticsAnalysis();
				incUpdatesStatisticsAnalyser.GenerateIncUpdatesStatisticsFiles();
				incUpdatesStatisticsAnalyser.GenerateIncUpdatesStatisticsSLAAnalysis();
		   	}
			catch (OException e)
			{
		      	 e.printStackTrace();
		    }
		}
	}
	
	
	private int GetEndurVersion() throws OException
	{
		int endurMajorVersion = 14;
		
		Table context = Table.tableNew();
		context =	Ref.getInfo();
		
		if (context.getNumRows() > 0)
		{
			Table versionTable = context.getTable("version", 1);
			endurMajorVersion = versionTable.getInt("major_version", 1);
		}
		
		return endurMajorVersion;
	}
	
	public class StatsColumnsNames
	{
		public String GetUpdatesStoredProcName;
		public String GetEntityInsertTimeColumnName;
		public String GetEntityNumberColumnName;
		public String GetEntityVersionColumnName;
		
		public StatsColumnsNames(int version)
		{
			boolean isMajorVersion = version >= 14;
			
			GetUpdatesStoredProcName = isMajorVersion ? "USER_apm_get_updates_by_date" : "USER_tfe_dealtimings_date";
			
			GetEntityInsertTimeColumnName = isMajorVersion ? "entity_insert_time" : "deal_insert_time";
			
			GetEntityNumberColumnName = isMajorVersion ? "secondary_entity_num" : "tran_num";
			
			GetEntityVersionColumnName = isMajorVersion ? "entity_version" : "version_num";
		}
	}
	
	//////////////////////////////////////////////////////////////
	// This class defines the main stages of the analysis process.
	//////////////////////////////////////////////////////////////
	
	public class IncUpdatesStatisticsAnalyser
	{
		private Table mListOfServices;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		private int mbucketSize;
		private int mServerUsageBucketSize;
		private String mShowAnalysisTableResultsFlag;
		private String mDirectory;
		private String mAnalysisFileName;
		private String mCSVfile;
		private Table mSlaValues;
		private String mDurationColumnName;
		
		private Table mIncUpdatesStatisticsMergedTable;
		private ArrayList<IncUpdatesStatisticsBucket> mIncUpdatesStatisticsBuckets;
		private Table mIncUpdatesStatisticsBucketsAnalysis;

		private ArrayList<IncUpdatesServerUsageBucket> mIncUpdatesServerUsageBuckets;
		private Table mIncUpdatesServerUsageBucketsAnalysis;
		
		private ArrayList<Table> mIncUpdatesStatisticsServicesTables;
		private Table mIncUpdatesStatisticsServicesAnalysis;
		
		private StatsColumnsNames mStatsColumnsNames; 
		
		public IncUpdatesStatisticsAnalyser(StatsColumnsNames statsColumnsNames, Table listOfServices, ODateTime startDateTime, 
											ODateTime endDateTime, int bucketSize, int serverUsageBucketSize,
											String showAnalysisTableResultsFlag,
											String directory,
											String analysisFileName,
											String CSVfile,
											String slaCSVfile,
											String durationColumnName) throws OException
		{
			mStatsColumnsNames = statsColumnsNames;
			mListOfServices = listOfServices;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
			mbucketSize = bucketSize;
			mServerUsageBucketSize = serverUsageBucketSize;
			mShowAnalysisTableResultsFlag = showAnalysisTableResultsFlag;
			mDirectory = directory;
			mAnalysisFileName = analysisFileName;
			mCSVfile = CSVfile;
			mSlaValues = LoadSlaValuesFromFile(slaCSVfile);
			mDurationColumnName = durationColumnName;
			
			mIncUpdatesStatisticsMergedTable = Table.tableNew();
			mIncUpdatesStatisticsBucketsAnalysis = Table.tableNew();
			mIncUpdatesServerUsageBucketsAnalysis = Table.tableNew();
			mIncUpdatesStatisticsServicesTables = new ArrayList<Table>();
		}
		
		
		// Loads and merges all Inc updates statistics from running APM services or
		// the statistics CSV file if selected. 
		public void LoadIncUpdatesStatistics() throws OException
		{	
			String[] apmServicesNames;
			Table incUpdateStatistics = Table.tableNew();
			
			String insertTimeColumName = mStatsColumnsNames.GetEntityInsertTimeColumnName;
			
			if (mCSVfile.equals("NONE") == false)
			{
				incUpdateStatistics = LoadIncUpdateStatisticsFromFile();
				apmServicesNames = ReloadServicesFromFile(incUpdateStatistics);
			}
			else
			{
				apmServicesNames = mListOfServices.getString("ted_str_value", 1).split(",");
			}
			
			int numberOfApmServices = apmServicesNames.length;
			
			for (int row = 0; row < numberOfApmServices; row++)
			{
				String apmServiceName = apmServicesNames[row].trim();
				
				IncUpdatesStatisticsServiceLoader incUpdatesStatisticsServiceLoader = new IncUpdatesStatisticsServiceLoader(mStatsColumnsNames, apmServiceName, mStartDateTime, mEndDateTime);

				Table tmp_incUpdatesTable;
				if (mCSVfile.equals("NONE"))
					tmp_incUpdatesTable = incUpdatesStatisticsServiceLoader.LoadIncUpdatesStatistics();
				else
					tmp_incUpdatesTable = incUpdatesStatisticsServiceLoader.LoadIncUpdatesStatisticsFromCSVTable(incUpdateStatistics);

				if(tmp_incUpdatesTable.getNumRows() > 0)
				{
					mIncUpdatesStatisticsServicesTables.add(tmp_incUpdatesTable);
				
					if(mIncUpdatesStatisticsMergedTable.getNumRows() == 0)
					{
						mIncUpdatesStatisticsMergedTable = tmp_incUpdatesTable.cloneTable();
						tmp_incUpdatesTable.copyRowAddAll(mIncUpdatesStatisticsMergedTable);
					}
					else
						tmp_incUpdatesTable.copyRowAddAll(mIncUpdatesStatisticsMergedTable);
				}
			}
			if(mIncUpdatesStatisticsMergedTable.getNumRows() > 0)
				mIncUpdatesStatisticsMergedTable.sortCol(mIncUpdatesStatisticsMergedTable.getColNum(insertTimeColumName));
		}
		
		// Loads Inc updates statistics from CSV File.
		public Table LoadIncUpdateStatisticsFromFile() throws OException
		{
			Table incUpdatesTable = Table.tableNew("CSVStatistics");
			
			String dealInsertTimeColumnName = mStatsColumnsNames.GetEntityInsertTimeColumnName;
			String entityNumberColumnName = mStatsColumnsNames.GetEntityNumberColumnName;
			String entityVersionColumnName = mStatsColumnsNames.GetEntityVersionColumnName;
			
			incUpdatesTable.addCol(dealInsertTimeColumnName, COL_TYPE_ENUM.COL_DATE_TIME);
			incUpdatesTable.addCol(entityNumberColumnName, COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol(entityVersionColumnName, COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("package_name", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("queue_insert_lag", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("queue_process_lag", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("total_queue_duration", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("job_duration", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("data_updated_time", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("hostname", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("module_name", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("pid", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("personnel_id", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("portfolio_id", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("tran_status", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("update_type", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("queue_insert_time", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("start_proc_time", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("job_complete_time", COL_TYPE_ENUM.COL_STRING);
			incUpdatesTable.addCol("monitor_id", COL_TYPE_ENUM.COL_INT);
			incUpdatesTable.addCol("op_services_run_id", COL_TYPE_ENUM.COL_INT);

			incUpdatesTable.inputFromCSVFile(mCSVfile);
			
			if (mShowAnalysisTableResultsFlag.equals("YES"))
				incUpdatesTable.viewTable();

			return incUpdatesTable;
		}

		// Generates a list of APM service names used in the given statistics table. 
		public String[] ReloadServicesFromFile(Table incUpdateStatistics) throws OException
		{
			String[] apmServicesNames;
			
			Table servicesNamesFromFile = Table.tableNew();
			servicesNamesFromFile.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
			
			incUpdateStatistics.copyColDistinct("service_name", servicesNamesFromFile, "service_name");
			
			int numberOfApmServices = servicesNamesFromFile.getNumRows();
			
			apmServicesNames = new String[numberOfApmServices];
			
			for(int row = 0; row < numberOfApmServices; row++)
				apmServicesNames[row] = servicesNamesFromFile.getString("service_name", row+1);
			
			return apmServicesNames;
		}
		
		// Loads services SLA values from CSV file into a table 
		public Table LoadSlaValuesFromFile(String slaFileName) throws OException
		{
			Table slaTable = Table.tableNew("SLA Values");
			
			if(slaFileName.intern() != "")
			{
				slaTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
				slaTable.addCol("sla_value", COL_TYPE_ENUM.COL_INT);
				
				slaTable.inputFromCSVFile(slaFileName);
			}
			return slaTable;
		}
		
		// Generate a list of Inc updates statistics buckets using the merged statistics table.
		public void GenerateIncUpdatesStatisticsBuckets() throws OException
		{
			IncUpdatesStatisticsBucketing incUpdatesBucketting = new IncUpdatesStatisticsBucketing(mIncUpdatesStatisticsMergedTable, mStartDateTime, mEndDateTime, mbucketSize);
			incUpdatesBucketting.GenerateIncUpdatesBuckets();
			mIncUpdatesStatisticsBuckets = incUpdatesBucketting.IncUpdatesStatisticsBuckets();
		}

		// Generate a list of Inc updates statistics buckets using the merged statistics table.
		public void GenerateIncUpdatesServerUsageBuckets() throws OException
		{
			IncUpdatesServerUsageBucketing incUpdatesBucketting = new IncUpdatesServerUsageBucketing(mIncUpdatesStatisticsMergedTable, mStartDateTime, mEndDateTime, mServerUsageBucketSize);
			incUpdatesBucketting.GenerateIncUpdatesBuckets();
			mIncUpdatesServerUsageBuckets = incUpdatesBucketting.IncUpdatesServerUsageBuckets();
		}
		
		// Generate the analysis reports tables for the list of buckets and the list of APM Services.
		public void GenerateIncUpdatesStatisticsAnalysis() throws OException
		{
			IncUpdatesStatisticsAnalysis incUpdatesStatisticsAnalysis = new IncUpdatesStatisticsAnalysis(mStatsColumnsNames);
			
			mIncUpdatesStatisticsBucketsAnalysis = incUpdatesStatisticsAnalysis.GenerateAnalysisForBuckets(mIncUpdatesStatisticsBuckets);
			
			mIncUpdatesStatisticsServicesAnalysis = incUpdatesStatisticsAnalysis.GenerateAnalysisForServices(mIncUpdatesStatisticsServicesTables);

			mIncUpdatesServerUsageBucketsAnalysis = incUpdatesStatisticsAnalysis.GenerateAnalysisForServerUsage(mIncUpdatesServerUsageBuckets);
		}
		
		// Runs the SLA analysis on the list of buckets and APM Services.
		public void GenerateIncUpdatesStatisticsSLAAnalysis() throws OException
		{
			IncUpdatesStatisticsAnalysis incUpdatesStatisticsAnalysis = new IncUpdatesStatisticsAnalysis(mStatsColumnsNames);
			
			incUpdatesStatisticsAnalysis.GenerateSLAAnalysisForBuckets(mIncUpdatesStatisticsBucketsAnalysis, mIncUpdatesStatisticsBuckets, mSlaValues, mDurationColumnName);
			
			incUpdatesStatisticsAnalysis.GenerateSLAAnalysisForServices(mIncUpdatesStatisticsServicesAnalysis, mIncUpdatesStatisticsServicesTables, mSlaValues, mDurationColumnName);
			
			if (mShowAnalysisTableResultsFlag.equals("YES"))
			{
				mIncUpdatesStatisticsBucketsAnalysis.viewTable();
				mIncUpdatesStatisticsServicesAnalysis.viewTable();
				mIncUpdatesServerUsageBucketsAnalysis.viewTable();
			}
		}
		
		// Sends the analysis reports tables to CSV files .
		public void GenerateIncUpdatesStatisticsFiles() throws OException
		{
			String analysisFullFilePath = GetAnalysisFilePath();
			
			String currentTimeStamp = GetTimeStampOfCurrentTime();
			
			String startDateTimeStr = GetTimeStampFromODateTime(mStartDateTime);
			
			String endDateTimeStr = GetTimeStampFromODateTime(mEndDateTime);
			
			String bucketsAnalysisFileName = analysisFullFilePath + "_Buckets_" + startDateTimeStr + "_" + endDateTimeStr + "_" + currentTimeStamp +  ".csv";
			SendAnalysisToFile(mIncUpdatesStatisticsBucketsAnalysis, bucketsAnalysisFileName);
			
			String servicesAnalysisFileName = analysisFullFilePath + "_Services_" + startDateTimeStr + "_" + endDateTimeStr + "_" + currentTimeStamp + ".csv";
			SendAnalysisToFile(mIncUpdatesStatisticsServicesAnalysis, servicesAnalysisFileName);

			String serverUsageAnalysisFileName = analysisFullFilePath + "_Server_Usage_" + startDateTimeStr + "_" + endDateTimeStr + "_" + currentTimeStamp +  ".csv";
			SendAnalysisToFile(mIncUpdatesServerUsageBucketsAnalysis, serverUsageAnalysisFileName);
		}
		
		// Returns the full file path to send analysis to
		public String GetAnalysisFilePath()
		{
			char lastChar = mDirectory.charAt(mDirectory.length()-1);
			if(lastChar == '\\' || lastChar == '/')
				return mDirectory + mAnalysisFileName;
			else
				return mDirectory + "\\" + mAnalysisFileName;
		}
		
		// Returns the list of Inc updates statistics buckets
		public ArrayList<IncUpdatesStatisticsBucket> GetIncUpdatesStatisticsBuckets()
		{
			if (mIncUpdatesStatisticsBuckets != null)
				return mIncUpdatesStatisticsBuckets;
			
			return null;
		}
		
		// Returns the list of Inc updates statistics for each APM service
		public ArrayList<Table> GetIncUpdatesStatisticsServicesTables()
		{
			if (mIncUpdatesStatisticsServicesTables != null)
				return mIncUpdatesStatisticsServicesTables;
			
			return null;
		}
		
		// Returns the SLA values table
		public Table GetSlaValues()
		{
			return mSlaValues;
		}
		
		// Returns a string representing the current date and time stamp in a specific format,
		// to be used in the report CSV file name.
		public String GetTimeStampOfCurrentTime() throws OException
		{
			int current_date = OCalendar.getServerDate();
			String currentDateAsString = OCalendar.formatJd(current_date, DATE_FORMAT.DATE_FORMAT_MINIMAL);
			
			Calendar currentTime = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
			String currentTimeStamp = sdf.format(currentTime.getTime());
			
			return currentDateAsString + "_" + currentTimeStamp;
		}
		
		// Returns a string representing a given DateTime in a specific format,
		// to be used in the report CSV file name.
		public String GetTimeStampFromODateTime(ODateTime oDateTime) throws OException
		{
			
			int jDate = oDateTime.getDate();
			int jTime = oDateTime.getTime();
			
			int hour = jTime / 3600;
			int minutes = (jTime % 3600) / 60;
			int seconds = jTime % 60;
			
			String dateAsString = OCalendar.formatJd(jDate, DATE_FORMAT.DATE_FORMAT_MINIMAL);
			
			String timeAsString = TwoDigitString(hour) + TwoDigitString(minutes) + TwoDigitString(seconds);
			
			return dateAsString + "_" + timeAsString;
		}
		
		// returns two digit format given a number, 
		// to format time when constructing the file name
		private String TwoDigitString(int number) 
		{
		    if (number == 0) {
		        return "00";
		    }

		    if (number / 10 == 0) {
		        return "0" + number;
		    }

		    return String.valueOf(number);
		}
		
		// Writes the analysis table to the given file and saves the file.
		public void SendAnalysisToFile(Table analysisTable, String fileName) throws OException
		{
			analysisTable.showColNames();
			analysisTable.hideRowNames();
			analysisTable.noFormatPrint();
			analysisTable.commaSep();
			analysisTable.printTableToFile(fileName);
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////
	// This class implements the functionality that loads the Inc updates 
	// statistics from the database or a CSV file.
	///////////////////////////////////////////////////////////////////////
	
	class IncUpdatesStatisticsServiceLoader
	{
		private String mApmServiceName;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		private StatsColumnsNames mStatsColumnsName;
		
		public IncUpdatesStatisticsServiceLoader(StatsColumnsNames statsColumnsNames, String apmServiceName, ODateTime startDateTime, ODateTime endDateTime)throws OException
		{
			mApmServiceName = apmServiceName;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
			
			mStatsColumnsName = statsColumnsNames;
		}
		
		// Runs the stored procedure that returns a table of Inc updates Statistics
		public Table LoadIncUpdatesStatistics()throws OException
		{
			Table argTable = Table.tableNew("args");
			Table incUpdatesTable = Table.tableNew(mApmServiceName);
			
			argTable.addCol("defn_name", COL_TYPE_ENUM.COL_STRING);
			argTable.addCol("start_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
			argTable.addCol("end_date_time", COL_TYPE_ENUM.COL_DATE_TIME);
			int row = argTable.addRow();
			argTable.setString("defn_name", row, mApmServiceName);
			argTable.setDateTime("start_date_time", row, mStartDateTime);
			argTable.setDateTime("end_date_time", row, mEndDateTime);
			
			String getUpdatesStroredProcName = mStatsColumnsName.GetUpdatesStoredProcName ;
			
			int iRetVal = DBase.runProc(getUpdatesStroredProcName, argTable);
			
			if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				return Table.tableNew();
			else
			{
				iRetVal = DBase.createTableOfQueryResults(incUpdatesTable);
				return incUpdatesTable;
			}
		}

		// Runs the stored procedure that returns a table of Inc updates Statistics
		public Table LoadBatchesStatistics()throws OException
		{
			Table argTable = Table.tableNew("args");
			Table batchesTable = Table.tableNew(mApmServiceName);
			
			argTable.addRow();
			
			int iRetVal = DBase.runProc("USER_apm_get_batch_stats", argTable);
			
			if(iRetVal != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.toInt())
				return Table.tableNew();
			else
			{
				iRetVal = DBase.createTableOfQueryResults(batchesTable);
				batchesTable.viewTable();
				return batchesTable;
			}
		}
		
		// Read the statistics CSB file and construct a corresponding table.
		public Table LoadIncUpdatesStatisticsFromCSVTable(Table incUpdateStatistics) throws OException
		{
			Table incUpdatesForService = Table.tableNew();
			incUpdatesForService = incUpdateStatistics.cloneTable();
			incUpdatesForService.setTableName(mApmServiceName);
			int numberOfRows = incUpdateStatistics.getNumRows();
			for (int row = 1; row <= numberOfRows; row++)
			{
				String insertTime = mStatsColumnsName.GetEntityInsertTimeColumnName;
				
				ODateTime insertDateTime = incUpdateStatistics.getDateTime(insertTime, row);
				
				if (AfterStartTime(insertDateTime) && BeforeEndTime(insertDateTime))
				{
					String service_name = incUpdateStatistics.getString("service_name", row);
					if (service_name.equalsIgnoreCase(mApmServiceName))
					{
						incUpdateStatistics.copyRowAdd(row, incUpdatesForService);
					}
				}
			}
			
			return incUpdatesForService;
		}
		
		// Checks the given date time is later than mStartDateTime,
		// used to work out if the given entry should be included in the bucket.
		public boolean AfterStartTime(ODateTime insertDateTime) throws OException
		{
			boolean isAfterStartTime = false;
			if (mStartDateTime.getDate() < insertDateTime.getDate())
			{
				isAfterStartTime = true;
			}
			else if (mStartDateTime.getDate() == insertDateTime.getDate())
			{
				if (mStartDateTime.getTime() <= insertDateTime.getTime())
				{
					isAfterStartTime = true;
				}
			}
			return isAfterStartTime;
		}
		
		// Checks the given date time is earlier than mEndDateTime,
		// used to work out if the given entry should be included in the bucket.
		public boolean BeforeEndTime(ODateTime insertDateTime) throws OException
		{
			boolean isBeforeEndTime = false;
			if (mEndDateTime.getDate() > insertDateTime.getDate())
			{
				isBeforeEndTime = true;
			}
			else if (mEndDateTime.getDate() == insertDateTime.getDate())
			{
				if (mEndDateTime.getTime() >= insertDateTime.getTime())
				{
					isBeforeEndTime = true;
				}
			}
			return isBeforeEndTime;
		}
	}
	
	
	///////////////////////////////////////////////////////////////////////
	// This class implements the functionality that generate the buckets. 
	///////////////////////////////////////////////////////////////////////
	
	class IncUpdatesStatisticsBucketing
	{
		private Table mIncUpdatesTable;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		private int mBucketSize;
		private ArrayList<IncUpdatesStatisticsBucket> mIncUpdatesStatisticsBuckets;
		
		public IncUpdatesStatisticsBucketing(Table incUpdatesTable, ODateTime startDateTime, ODateTime endDateTime, int bucketSize)
		{
			mIncUpdatesTable = incUpdatesTable;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
			mBucketSize = bucketSize;
			mIncUpdatesStatisticsBuckets = new ArrayList<IncUpdatesStatisticsBucket>();
		}
		
		// Generate the list of buckets from the merged table of Inc updates statistics.
		public void GenerateIncUpdatesBuckets() throws OException
		{
			ODateTime bucketStartDateTime;
			ODateTime bucketEndDateTime;
			
			int numRows = mIncUpdatesTable.getNumRows();
			int foundBucketEndDateRow = 0;
			int foundBucketStartDateRow = 0;
	
			bucketStartDateTime = mStartDateTime;
			
			if(numRows > 0)
			{
				while(!IsBucketingCompeted(bucketStartDateTime))
				{
					Table bucketTable;
					
					// Work out the start row of the next bucket range
					foundBucketStartDateRow = mIncUpdatesTable.findDateTime(1, bucketStartDateTime, SEARCH_ENUM.FIRST_IN_GROUP);
					
					// Work out the last row of the next bucket range
					bucketEndDateTime = ODateTime.dtNew();
					bucketStartDateTime.addSecondsToDateTime(bucketEndDateTime, mBucketSize-1);
					foundBucketEndDateRow = mIncUpdatesTable.findDateTime(1, bucketEndDateTime, SEARCH_ENUM.LAST_IN_GROUP);
					
					// if date range has no corresponding entries then return empty table
					if (foundBucketStartDateRow == -1 && foundBucketEndDateRow == -1)
					{
						bucketTable = mIncUpdatesTable.cloneTable();
					}
					
					// Get the corresponding entries within the date range
					else 
					{
						foundBucketStartDateRow = Math.abs(foundBucketStartDateRow);
						foundBucketEndDateRow = Math.abs(foundBucketEndDateRow);
						if(foundBucketEndDateRow >= numRows)
						{
							foundBucketEndDateRow = numRows;
						}
						else
							foundBucketEndDateRow = GetActualBucketEndDateRow(bucketEndDateTime, foundBucketEndDateRow);
					
						// Extract the bucket and add it to the list of buckets
						bucketTable = mIncUpdatesTable.cloneTable();
						mIncUpdatesTable.copyRowAddRange(foundBucketStartDateRow, foundBucketEndDateRow, bucketTable);
						//bucketTable.viewTable();
					}
					
					ODateTime snapBucketEndDateTime = ODateTime.dtNew();
					snapBucketEndDateTime.setDateTime(bucketEndDateTime.getDate(), bucketEndDateTime.getTime());
					
					IncUpdatesStatisticsBucket incUpdatesStatisticsBucket = new IncUpdatesStatisticsBucket(bucketTable, bucketStartDateTime, snapBucketEndDateTime);
					mIncUpdatesStatisticsBuckets.add(incUpdatesStatisticsBucket);
					
					bucketStartDateTime = bucketEndDateTime;
					bucketStartDateTime.addSecondsToDateTime(bucketEndDateTime, 1);
				}
			}
		}
		
		public ArrayList<IncUpdatesStatisticsBucket> IncUpdatesStatisticsBuckets()
		{
			return mIncUpdatesStatisticsBuckets;
		}
		
		private int GetActualBucketEndDateRow(ODateTime bucketEndDateTime, int bucketEndDateTimeRow) throws OException
		{
			ODateTime actualTime = mIncUpdatesTable.getDateTime(1, bucketEndDateTimeRow);
			
			if (actualTime.getDate() > bucketEndDateTime.getDate())
				return --bucketEndDateTimeRow;
			
			if (actualTime.getTime() > bucketEndDateTime.getTime())
				return --bucketEndDateTimeRow;
			
			return bucketEndDateTimeRow;
		}
		
		private boolean IsBucketingCompeted(ODateTime bucketStartDateTime) throws OException
		{
			if(bucketStartDateTime.getDate() > mEndDateTime.getDate())
				return true;
			
			if(bucketStartDateTime.getDate() == mEndDateTime.getDate() && 
					bucketStartDateTime.getTime() > mEndDateTime.getTime())
				return true;
			
			return false;
		}
	}
	

	///////////////////////////////////////////////////////////////////////
	// This class implements the functionality that generate the buckets. 
	///////////////////////////////////////////////////////////////////////
	
	class IncUpdatesServerUsageBucketing
	{
		private Table mIncUpdatesTable;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		private int mBucketSize;
		private ArrayList<IncUpdatesServerUsageBucket> mIncUpdatesServerUsageBuckets;
		
		public IncUpdatesServerUsageBucketing(Table incUpdatesTable, ODateTime startDateTime, ODateTime endDateTime, int bucketSize)
		{
			mIncUpdatesTable = incUpdatesTable;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
			mBucketSize = bucketSize;
			mIncUpdatesServerUsageBuckets = new ArrayList<IncUpdatesServerUsageBucket>();
		}
		
		// Generate the list of buckets from the merged table of Inc updates statistics.
		public void GenerateIncUpdatesBuckets() throws OException
		{
			ODateTime bucketStartDateTime;
			ODateTime bucketEndDateTime;
			
			int numRows = mIncUpdatesTable.getNumRows();
			int foundBucketEndDateRow = 0;
			int foundBucketStartDateRow = 0;
	
			bucketStartDateTime = mStartDateTime;
			
			if(numRows > 0)
			{
				while(!IsBucketingCompeted(bucketStartDateTime))
				{
					Table bucketTable;
					
					// Work out the start row of the next bucket range
					foundBucketStartDateRow = mIncUpdatesTable.findDateTime(1, bucketStartDateTime, SEARCH_ENUM.FIRST_IN_GROUP);
					
					// Work out the last row of the next bucket range
					bucketEndDateTime = ODateTime.dtNew();
					bucketStartDateTime.addSecondsToDateTime(bucketEndDateTime, mBucketSize-1);
					foundBucketEndDateRow = mIncUpdatesTable.findDateTime(1, bucketEndDateTime, SEARCH_ENUM.LAST_IN_GROUP);
					
					// if date range has no corresponding entries then return empty table
					if (foundBucketStartDateRow == -1 && foundBucketEndDateRow == -1)
					{
						bucketTable = mIncUpdatesTable.cloneTable();
					}
					
					// Get the corresponding entries within the date range
					else 
					{
						foundBucketStartDateRow = Math.abs(foundBucketStartDateRow);
						foundBucketEndDateRow = Math.abs(foundBucketEndDateRow);
						if(foundBucketEndDateRow >= numRows)
						{
							foundBucketEndDateRow = numRows;
						}
						else
							foundBucketEndDateRow = GetActualBucketEndDateRow(bucketEndDateTime, foundBucketEndDateRow);
					
						// Extract the bucket and add it to the list of buckets
						bucketTable = mIncUpdatesTable.cloneTable();
						mIncUpdatesTable.copyRowAddRange(foundBucketStartDateRow, foundBucketEndDateRow, bucketTable);
					}
					
					ODateTime snapBucketEndDateTime = ODateTime.dtNew();
					snapBucketEndDateTime.setDateTime(bucketEndDateTime.getDate(), bucketEndDateTime.getTime());
					
					IncUpdatesServerUsageBucket incUpdatesServerUsageBucket = new IncUpdatesServerUsageBucket(bucketTable, bucketStartDateTime, snapBucketEndDateTime);
					mIncUpdatesServerUsageBuckets.add(incUpdatesServerUsageBucket);
					
					bucketStartDateTime = bucketEndDateTime;
					bucketStartDateTime.addSecondsToDateTime(bucketEndDateTime, 1);
				}
			}
		}
		
		public ArrayList<IncUpdatesServerUsageBucket> IncUpdatesServerUsageBuckets()
		{
			return mIncUpdatesServerUsageBuckets;
		}
		
		private int GetActualBucketEndDateRow(ODateTime bucketEndDateTime, int bucketEndDateTimeRow) throws OException
		{
			ODateTime actualTime = mIncUpdatesTable.getDateTime(1, bucketEndDateTimeRow);
			
			if (actualTime.getDate() > bucketEndDateTime.getDate())
				return --bucketEndDateTimeRow;
			
			if (actualTime.getTime() > bucketEndDateTime.getTime())
				return --bucketEndDateTimeRow;
			
			return bucketEndDateTimeRow;
		}
		
		private boolean IsBucketingCompeted(ODateTime bucketStartDateTime) throws OException
		{
			if(bucketStartDateTime.getDate() > mEndDateTime.getDate())
				return true;
			
			if(bucketStartDateTime.getDate() == mEndDateTime.getDate() && 
					bucketStartDateTime.getTime() > mEndDateTime.getTime())
				return true;
			
			return false;
		}
	}
	
	///////////////////////////////////////////////////////////////////////
	// This class represents a Bucket of Inc updates statistics  
	///////////////////////////////////////////////////////////////////////
	
	class IncUpdatesStatisticsBucket
	{
		private Table mBucket;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		
		public IncUpdatesStatisticsBucket(Table bucket, ODateTime startDateTime, ODateTime endDateTime)
		{
			mBucket = bucket;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
		}
		
		public Table GetTable()
		{
			return mBucket;
		}
		
		public ODateTime GetStartDateTime()
		{
			return mStartDateTime;
		}
		
		public ODateTime GetEndDateTime()
		{
			return mEndDateTime;
		}
	}

	///////////////////////////////////////////////////////////////////////
	// This class represents a Bucket of Inc updates statistics  
	///////////////////////////////////////////////////////////////////////
	
	class IncUpdatesServerUsageBucket
	{
		private Table mBucket;
		private ODateTime mStartDateTime;
		private ODateTime mEndDateTime;
		
		public IncUpdatesServerUsageBucket(Table bucket, ODateTime startDateTime, ODateTime endDateTime)
		{
			mBucket = bucket;
			mStartDateTime = startDateTime;
			mEndDateTime = endDateTime;
		}
		
		public Table GetTable()
		{
			return mBucket;
		}
		
		public ODateTime GetStartDateTime()
		{
			return mStartDateTime;
		}
		
		public ODateTime GetEndDateTime()
		{
			return mEndDateTime;
		}
	}
	
	
	//////////////////////////////////////////////////////////////////////////
	// This class implements the functionality that generate the full analysis
	// upon the list of Inc updates statistics buckets and per all APM services 
	//////////////////////////////////////////////////////////////////////////
	
	class IncUpdatesStatisticsAnalysis
	{
		private StatsColumnsNames mStatsColumnsNames;
		
		public IncUpdatesStatisticsAnalysis(StatsColumnsNames statsColumnsNames)
		{
			mStatsColumnsNames = statsColumnsNames;
		}
		
		// Given a list of statistics buckets it generate and returns a table of analysis results 
		public Table GenerateAnalysisForBuckets(ArrayList<IncUpdatesStatisticsBucket> incUpdatesStatisticsBuckets) throws OException
		{
			Table analysisResults = CreateAnalysisForBucketsTable();
			
			String entityNumberColumnName = mStatsColumnsNames.GetEntityNumberColumnName;
			String entityVersionColumnName = mStatsColumnsNames.GetEntityVersionColumnName;
			
			for(int i = 0; i < incUpdatesStatisticsBuckets.size(); i++)
			{
				BucketAnalyser bucketAnalyser = new BucketAnalyser();
				IncUpdatesStatisticsBucket bucket = incUpdatesStatisticsBuckets.get(i);
				Table bucketTable = bucket.GetTable();
				
				ODateTime bucketStartInsertTime = bucket.GetStartDateTime();
				ODateTime bucketEndInsertTime = bucket.GetEndDateTime();
				int numberOfDeals = bucketAnalyser.NumberOfDeals(bucketTable,  entityNumberColumnName, entityVersionColumnName);
				int numberOfEntries = bucketAnalyser.NumberOfEntries(bucketTable);
				double averageDuration = bucketAnalyser.Average(bucketTable, "total_queue_duration");
				int maxDuration = bucketAnalyser.Max(bucketTable, "total_queue_duration");
				double averageInsertLag = bucketAnalyser.Average(bucketTable, "queue_insert_lag");
				int maxInsertLag = bucketAnalyser.Max(bucketTable, "queue_insert_lag");
				
				int row = analysisResults.addRow();
				
				analysisResults.setDateTime("bucket_start_time", row, bucketStartInsertTime);
				analysisResults.setDateTime("bucket_end_time", row, bucketEndInsertTime);
				analysisResults.setInt("number_of_deals", row, numberOfDeals);
				analysisResults.setInt("number_of_entries", row, numberOfEntries);
				analysisResults.setDouble("avg_duration", row, averageDuration);
				analysisResults.setInt("max_duration", row, maxDuration);
				analysisResults.setDouble("avg_queue_insert_lag", row, averageInsertLag);
				analysisResults.setInt("max_queue_insert_lag", row, maxInsertLag);
			}
			
			return analysisResults;
		}
		
		// Given a list of statistics tables it generates and returns a table of analysis results 
		public Table GenerateAnalysisForServices(ArrayList<Table> incUpdatesStatisticsServices) throws OException
		{
			BucketAnalyser bucketAnalyser = new BucketAnalyser();
			Table analysisResults = CreateAnalysisForServicesTable();
			
			String entityNumberColumnName = mStatsColumnsNames.GetEntityNumberColumnName;
			String entityVersionColumnName = mStatsColumnsNames.GetEntityVersionColumnName;
			
			for(int i = 0; i < incUpdatesStatisticsServices.size(); i++)
			{
				Table serviceTable = incUpdatesStatisticsServices.get(i);
				
				String apmServiceName = serviceTable.getTableName();
				int numberOfDeals = bucketAnalyser.NumberOfDeals(serviceTable, entityNumberColumnName, entityVersionColumnName);
				int numberOfEntries = bucketAnalyser.NumberOfEntries(serviceTable);
				double averageDuration = bucketAnalyser.Average(serviceTable, "total_queue_duration");
				int maxDuration = bucketAnalyser.Max(serviceTable, "total_queue_duration");
				double averageInsertLag = bucketAnalyser.Average(serviceTable, "queue_insert_lag");
				int maxInsertLag = bucketAnalyser.Max(serviceTable, "queue_insert_lag");
				
				int row = analysisResults.addRow();
				
				analysisResults.setString("apm_service_name", row, apmServiceName);
				analysisResults.setInt("number_of_deals", row, numberOfDeals);
				analysisResults.setInt("number_of_entries", row, numberOfEntries);
				analysisResults.setDouble("avg_duration", row, averageDuration);
				analysisResults.setInt("max_duration", row, maxDuration);
				analysisResults.setDouble("avg_queue_insert_lag", row, averageInsertLag);
				analysisResults.setInt("max_queue_insert_lag", row, maxInsertLag);
			}
			
			return analysisResults;
		}
		
		// Generate the server usage stats to show the server and engine usage
		public Table GenerateAnalysisForServerUsage(ArrayList<IncUpdatesServerUsageBucket> incUpdatesServerUsageBuckets) throws OException
		{
			Table analysisResults = CreateAnalysisForServerUsageTable();
			
			//Get a table of servers for this bucket
			Table hostTable = Table.tableNew();
			hostTable.addCol("hostname", COL_TYPE_ENUM.COL_STRING);
			//Need to use PID as that is unique
			Table engineTable = Table.tableNew();
			engineTable.addCol("pid", COL_TYPE_ENUM.COL_INT);
			
			for(int i = 0; i < incUpdatesServerUsageBuckets.size(); i++)
			{
				BucketAnalyser bucketAnalyser = new BucketAnalyser();
				IncUpdatesServerUsageBucket bucket = incUpdatesServerUsageBuckets.get(i);
				Table bucketTable = bucket.GetTable();
				
				ODateTime bucketStartInsertTime = bucket.GetStartDateTime();
				ODateTime bucketEndInsertTime = bucket.GetEndDateTime();
				int numberOfEntries = bucketAnalyser.NumberOfEntries(bucketTable);
				int numberOfEngines = bucketAnalyser.NumberOfEngines(bucketTable);
				int numberOfServers = bucketAnalyser.NumberOfServers(bucketTable);
				int row = analysisResults.addRow();
				
				analysisResults.setDateTime("bucket_start_time", row, bucketStartInsertTime);
				analysisResults.setDateTime("bucket_end_time", row, bucketEndInsertTime);
				analysisResults.setInt("number_of_entries", row, numberOfEntries);
				analysisResults.setInt("number_of_engines", row, numberOfEngines);
				analysisResults.setInt("number_of_servers", row, numberOfServers);
				
				hostTable.clearRows();
				bucketTable.copyColDistinctRange("hostname", hostTable, "hostname", 1, bucketTable.getNumRows());
				
				for (int hostRow = 1; hostRow <= hostTable.getNumRows(); hostRow++)
				{
					//Add server name as a column
					String serverName = hostTable.getString("hostname", hostRow);
					if (analysisResults.getColNum(serverName) < 1)
					{
						analysisResults.addCol(serverName, COL_TYPE_ENUM.COL_INT);
					}
					
					//Now count the number of engines used on that server in this bucket
					engineTable.clearRows();
					engineTable.select(bucketTable, "DISTINCT, pid", "hostname EQ "+serverName);
					analysisResults.setInt(serverName, row, engineTable.getNumRows());
				}
			}
			
			return analysisResults;
		}
		
		
		// Create a table to store the analysis for the list of statistics bucket. 
		public Table CreateAnalysisForBucketsTable() throws OException
		{
			Table analysisTable = Table.tableNew("Inc Updates Statistics Analysis");
			
			analysisTable.addCol("bucket_start_time", COL_TYPE_ENUM.COL_DATE_TIME);
			analysisTable.addCol("bucket_end_time", COL_TYPE_ENUM.COL_DATE_TIME);
			analysisTable.addCol("number_of_deals", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("number_of_entries", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("avg_duration", COL_TYPE_ENUM.COL_DOUBLE);
			analysisTable.addCol("max_duration", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("avg_queue_insert_lag", COL_TYPE_ENUM.COL_DOUBLE);
			analysisTable.addCol("max_queue_insert_lag", COL_TYPE_ENUM.COL_INT);
			
			return analysisTable;
		}
		
		// Create a table to store the analysis for the list of statistics Tables.
		public Table CreateAnalysisForServicesTable() throws OException
		{
			Table analysisTable = Table.tableNew("Services Statistics Analysis");
			
			analysisTable.addCol("apm_service_name", COL_TYPE_ENUM.COL_STRING);
			analysisTable.addCol("number_of_deals", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("number_of_entries", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("avg_duration", COL_TYPE_ENUM.COL_DOUBLE);
			analysisTable.addCol("max_duration", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("avg_queue_insert_lag", COL_TYPE_ENUM.COL_DOUBLE);
			analysisTable.addCol("max_queue_insert_lag", COL_TYPE_ENUM.COL_INT);
			
			return analysisTable;
		}
		
		// Create a table to store the analysis for server usage
		public Table CreateAnalysisForServerUsageTable() throws OException
		{
			Table analysisTable = Table.tableNew("Server Usage Analysis");
			
			analysisTable.addCol("bucket_start_time", COL_TYPE_ENUM.COL_DATE_TIME);
			analysisTable.addCol("bucket_end_time", COL_TYPE_ENUM.COL_DATE_TIME);
			analysisTable.addCol("number_of_entries", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("number_of_engines", COL_TYPE_ENUM.COL_INT);
			analysisTable.addCol("number_of_servers", COL_TYPE_ENUM.COL_INT);
			
			return analysisTable;
		}
		
		// Given a list of statistic buckets it generates SLA analysis and adds results to existing analysis reports.
		
		// Given a list of statistic buckets it generates SLA analysis and adds results to existing analysis reports.
		public void GenerateSLAAnalysisForBuckets(Table analysisResults, 
													ArrayList<IncUpdatesStatisticsBucket> incUpdatesStatisticsBuckets,
													Table slaValues,
													String durationColumn) throws OException
		{
			BucketAnalyser bucketAnalyser = new BucketAnalyser();
			
			if (slaValues.getNumRows() > 0)
			{
				analysisResults.addCol("number_of_entries_exceeding_sla", COL_TYPE_ENUM.COL_INT);
				analysisResults.addCol("entries_exceeding_sla_table", COL_TYPE_ENUM.COL_TABLE);
			
				for(int i = 0; i < incUpdatesStatisticsBuckets.size(); i++)
				{
					Table bucketTable = incUpdatesStatisticsBuckets.get(i).GetTable();
					
					Table entriesExceedingSla = bucketAnalyser.GetSlaAnalysis(bucketTable, slaValues, durationColumn);
					
					int numberOfEntriesExceedingSla = Integer.parseInt(entriesExceedingSla.getTableName());
					if(numberOfEntriesExceedingSla > 0)
					{
						analysisResults.setInt("number_of_entries_exceeding_sla", i+1, numberOfEntriesExceedingSla);
						String bucketStartDateTime = incUpdatesStatisticsBuckets.get(i).GetStartDateTime().toString();
						String bucketEndDateTime = incUpdatesStatisticsBuckets.get(i).GetStartDateTime().toString();
						entriesExceedingSla.setTableName("Bucket: " + bucketStartDateTime + "-" + bucketEndDateTime);
						analysisResults.setTable("entries_exceeding_sla_table", i+1, entriesExceedingSla);
					}
				}
			}
		}
		
		// Given a list of statistics tables it generates SLA analysis and adds results to existing analysis reports.
		public void GenerateSLAAnalysisForServices(Table analysisResults, 
													ArrayList<Table> incUpdatesStatisticsServices,
													Table slaValues,
													String durationColumn) throws OException
		{
			BucketAnalyser bucketAnalyser = new BucketAnalyser();
			
			if (slaValues.getNumRows() > 0)
			{
				analysisResults.addCol("sla_value", COL_TYPE_ENUM.COL_INT);
				analysisResults.addCol("number_of_entries_exceeding_sla", COL_TYPE_ENUM.COL_INT);
				analysisResults.addCol("entries_exceeding_sla_table", COL_TYPE_ENUM.COL_TABLE);
				
				for(int i = 0; i < incUpdatesStatisticsServices.size(); i++)
				{
					Table serviceTable = incUpdatesStatisticsServices.get(i);
					
					String apmServiceName = serviceTable.getTableName();
					
					int slaRow = slaValues.findString("service_name", apmServiceName, SEARCH_ENUM.FIRST_IN_GROUP);
					
					if(slaRow > 0)
					{
						int slaValue = slaValues.getInt("sla_value", slaRow);
						Table entriesExceedingSla = bucketAnalyser.GetSlaAnalysis(serviceTable, slaValue, durationColumn);
						
						int row = analysisResults.findString("apm_service_name", apmServiceName, SEARCH_ENUM.FIRST_IN_GROUP);
						analysisResults.setInt("sla_value", row, slaValue);
						
						int numberOfEntriesExceedingSla = entriesExceedingSla.getNumRows();
						if(numberOfEntriesExceedingSla > 0)
						{
							analysisResults.setInt("number_of_entries_exceeding_sla", row, numberOfEntriesExceedingSla);
							
							entriesExceedingSla.setTableName(apmServiceName + " Exceeding Sla");
							
							analysisResults.setTable("entries_exceeding_sla_table", row, entriesExceedingSla);
						}
					}
				}
			}
		}
	}

	
	//////////////////////////////////////////////////////////////////////////
	// This class implements the functionality that deals with the bucket analysis 
	// calculations given a table of Inc updates statistics.
	//////////////////////////////////////////////////////////////////////////
	
	class BucketAnalyser
	{
		public BucketAnalyser()
		{
		}
		
		// Returns the first deal insert time in the table
		public ODateTime BucketStartDealInsterTime(Table table) throws OException
		{
			ODateTime startInsertDealTime = ODateTime.dtNew();
			if(table.getNumRows() > 0)
				startInsertDealTime = table.getDateTime("deal_insert_time", 1);
			
			return startInsertDealTime;
		}
		
		// Returns the number of entries in the table
		public int NumberOfEntries(Table table) throws OException
		{
			return table.getNumRows();
		}

		// Returns the number of engines in the table
		public int NumberOfEngines(Table table) throws OException
		{
			//This is the number of unique PID values
			Table PIDTable = Table.tableNew();
			PIDTable.addCol("pid", COL_TYPE_ENUM.COL_INT);
			table.copyColDistinctRange("pid", PIDTable, "pid", 1, table.getNumRows());
			return PIDTable.getNumRows();
		}
		
		// Returns the number of servers in the table
		public int NumberOfServers(Table table) throws OException
		{
			//This is the number of unique host name values
			Table hostTable = Table.tableNew();
			hostTable.addCol("hostname", COL_TYPE_ENUM.COL_STRING);
			table.copyColDistinctRange("hostname", hostTable, "hostname", 1, table.getNumRows());
			return hostTable.getNumRows();
		}

		// Returns the number of deals in the table
		public int NumberOfDeals(Table table, String entityNumberColumnName, String entityVersionColumnName) throws OException
		{
			ArrayList<String> dealEntries = new ArrayList<String>();
			
			for(int row = 1; row <= table.getNumRows(); row++)
			{
				String dealEntry = table.getInt(entityNumberColumnName, row) + "." + table.getInt(entityVersionColumnName, row);
				if(!dealEntries.contains(dealEntry))
					dealEntries.add(dealEntry);	
			}
			
			return dealEntries.size();
		}
		
		// Returns the average of the values of the give column in the table
		public double Average(Table table, String ColumnName) throws OException
		{
			double result = 0;
			int numberOfRows = table.getNumRows();
			
			if(numberOfRows > 0)
			{
				double total = table.sumRangeInt(ColumnName, 1, numberOfRows);
				result = total/numberOfRows;
			}
			return result;
		}
		
		// Returns the maximum of the values of the give column in the table
		public int Max(Table table, String ColumnName) throws OException
		{
			int numberOfRows = table.getNumRows();
			int max = 0;
			if(numberOfRows > 0)
			{
				for(int row = 1; row <= numberOfRows; row++)
				{
					int total = table.getInt(ColumnName, row);
					if (total > max)
						max = total;
				}
			}
			return max;
		}
		
		// Applied to APM services analysis Report, 
		// Returns a table of raw entries from a table of Inc updates statistics that exceeds SLA value.
		public Table GetSlaAnalysis(Table table, int slaValue, String durationColumn) throws OException
		{
			Table entriesExceedingSla = Table.tableNew();
			entriesExceedingSla = table.cloneTable();
			
			for(int row = 1; row <= table.getNumRows(); row++)
			{
				int duration = table.getInt(durationColumn, row);
				if (duration > slaValue)
					table.copyRowAdd(row, entriesExceedingSla);
			}
			
			return entriesExceedingSla;
		}
		
		// Applied to Buckets analysis Report, 
		// Returns a table of service name, SLA value and raw entries from a table of Inc updates statistics that exceeds SLA value.
		public Table GetSlaAnalysis(Table table, Table slaValues, String durationColumn) throws OException
		{
			Table slaAnalysisTable = CreateSlaAnalysisTableForBuckets();
			
			ArrayList<Table> entriesExceedingSlaPerService = new ArrayList<Table>();
			
			int numberOfEntriesExceedingSla = 0;
			
			for(int row = 1; row <= table.getNumRows(); row++)
			{
				String apmServiceName = table.getString("service_name", row);
				int slaRow = slaValues.findString("service_name", apmServiceName, SEARCH_ENUM.FIRST_IN_GROUP);
				int slaValue = slaValues.getInt("sla_value", slaRow);
				
				int duration = table.getInt(durationColumn, row);
				if (duration > slaValue)
				{
					int entriesExceedingSlaIndex = findEntriesExceedingSlaTable(entriesExceedingSlaPerService, apmServiceName);
					if (entriesExceedingSlaIndex == -1)
					{
						Table entriesExceedingSla = Table.tableNew();
						entriesExceedingSla = table.cloneTable();
						entriesExceedingSla.setTableName(apmServiceName);
						entriesExceedingSlaPerService.add(entriesExceedingSla);
						entriesExceedingSlaIndex = entriesExceedingSlaPerService.size()-1;
					}
					table.copyRowAdd(row, entriesExceedingSlaPerService.get(entriesExceedingSlaIndex));
					numberOfEntriesExceedingSla++;
				}
			}
			
			slaAnalysisTable.setTableName(String.valueOf(numberOfEntriesExceedingSla));
			
			if(entriesExceedingSlaPerService.size() > 0)
			{
				for(int i = 0; i < entriesExceedingSlaPerService.size(); i++)
				{
					Table entriesExceedingSla = entriesExceedingSlaPerService.get(i);
					String serviceName = entriesExceedingSla.getTableName();
					int slaRow = slaValues.findString("service_name", entriesExceedingSla.getTableName().intern(), SEARCH_ENUM.FIRST_IN_GROUP);
					int slaValue = slaValues.getInt("sla_value", slaRow);
					
					slaAnalysisTable.addRow();
					
					int NumberOfentriesExceedingSla = entriesExceedingSla.getNumRows();
					entriesExceedingSla.setTableName(serviceName + " Exceeding Sla");
					
					slaAnalysisTable.setString("service_name", i+1, serviceName);
					slaAnalysisTable.setInt("sla_value", i+1, slaValue);
					slaAnalysisTable.setInt("number_of_entries_exceeding_sla", i+1, NumberOfentriesExceedingSla);
					slaAnalysisTable.setTable("entries_exceeding_sla_table", i+1, entriesExceedingSla);
				}
			}
			
			return slaAnalysisTable;
		}
		
		private int findEntriesExceedingSlaTable(ArrayList<Table> entriesExceedingSlaPerService, String apmServiceName) throws OException
		{
			int size = entriesExceedingSlaPerService.size();
			if(size > 0)
			{
				for(int i = 0; i < size; i++)
				{
					String tableName = entriesExceedingSlaPerService.get(i).getTableName().intern();
					if(tableName.equals(apmServiceName))
						return i;
				}
				return -1;
			}
			else
				return -1;
		}
		
		// Creates and returns an empty table to be used for Buckets SLA analysis.
		private Table CreateSlaAnalysisTableForBuckets() throws OException
		{
			Table slaAnalysisTable = Table.tableNew("slaAnalysisTable");
			
			slaAnalysisTable.addCol("service_name", COL_TYPE_ENUM.COL_STRING);
			slaAnalysisTable.addCol("sla_value", COL_TYPE_ENUM.COL_INT);
			slaAnalysisTable.addCol("number_of_entries_exceeding_sla", COL_TYPE_ENUM.COL_INT);
			slaAnalysisTable.addCol("entries_exceeding_sla_table", COL_TYPE_ENUM.COL_TABLE);
			
			return slaAnalysisTable;
		}
	}
}


