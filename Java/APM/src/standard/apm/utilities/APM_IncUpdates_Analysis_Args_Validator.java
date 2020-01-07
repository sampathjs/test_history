// ****************************************************************************
// *                                                                          *
// *              Copyright 2014 OpenLink Financial, Inc.                     *
// *                                                                          *
// *                        ALL RIGHTS RESERVED                               *
// *                                                                          *
// ****************************************************************************

// This class implements the validation process of the parameters that passed to the 
// main script which analyse the statistics of incremental updates.

package standard.apm.utilities;


import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.ODateTime;

public class APM_IncUpdates_Analysis_Args_Validator {

	private String mValidationReport = "valid";
	
	public void ValidateArguments(Table listOfAPMServices,
									String csvFile,
									ODateTime startDateTime,
									ODateTime endDateTime,
									int bucketSize,
									String analysisReportDirectory,
									String analysisReportFileName,
									String showAnalysisTables) throws OException
	{
		if(csvFile.equals("NONE") == false && csvFile.equals("") == true)
			mValidationReport = "WARNING: CSV File name is missing !!";
				
		if(listOfAPMServices.getNumRows() == 0)
			if(csvFile.equals("NONE") == true)
			{
				mValidationReport = "WARNING: Missing APM services and CSV File name !!";
			}
		
		if(!IsDateTimeRangeValid(startDateTime, endDateTime))
		{
			mValidationReport = "WARNING: End date time is earlier than Start date time !!";
		}
	}
	
	private boolean IsDateTimeRangeValid(ODateTime startDateTime, ODateTime endDateTime) throws OException
	{
		if(endDateTime.getDate() > startDateTime.getDate())
			return true;
		
		if(endDateTime.getDate() == startDateTime.getDate() && 
				endDateTime.getTime() > startDateTime.getTime())
			return true;
		
		return false;
	}
	
	public boolean Succeeded()
	{
		if(mValidationReport == "valid")
			return true;
		return false;
	}
	
	public String ValidationReport()
	{
		return mValidationReport;
	}
	

}
