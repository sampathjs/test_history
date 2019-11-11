/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.include;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.olf.openjvs.OException;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.Util;
import com.olf.openjvs.OCalendar;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.enums.COL_TYPE_ENUM;
import com.olf.openjvs.enums.SEARCH_CASE_ENUM;
import com.olf.openjvs.enums.DATE_FORMAT;

/// Singleton access point for ConsoleCapture to ensure access across multiple use cases.
public class ConsoleCaptureWrapper extends ConsoleCapture{

	/// The arguments stored for this console capture.  Horrible mechanism for passing information around, breaks 
	/// encapsulation and strong type checking, needs to be changed in future OO version of APM server code.
	private static Table m_arguments = null;
	
	/// Check whether the API required is available or not.
	private boolean m_apiAvailable = false;

	/// Whether the console logging is  turned on.
	private boolean m_loggingOff = false;
	
	/// Object which started the console capture.  Only this object should be able to close the capture, although others might force a flush.
	private Object m_creator = null;
	
	/// Private static instance of the ConsoleCaptureInstance class.
	private static ConsoleCaptureWrapper s_instance = null;
	
	/// The lock to the object to ensure that there will be only one!
	private static Lock s_lock = new ReentrantLock();
	
        private static APM_Utils s_apmUtils = new APM_Utils();

	/// Singleton instance to control access to the console capture.
	public static ConsoleCaptureWrapper getInstance(Table tAPMArgumentTable) throws OException {			
		try {
		   m_arguments = tAPMArgumentTable;

			// Ensure that we really have only one instance created.
			s_lock.lock();		
			if ( s_instance == null ) {							
				s_instance = new ConsoleCaptureWrapper();	
				
				// The API is available in tfe_interface_api version 61+.									
				s_instance.m_apiAvailable = true;

				s_instance.setLogToDb( s_apmUtils.useDbForServerLogging(m_arguments) );
				s_instance.GetEnvironmentVariables();
			}
		} finally {
			// Unlock the instance.
			s_lock.unlock();
		}
		
		return ( s_instance );		
	}
	
	/// Flush the console capture to some target with basic parameters.
	public void flush() throws OException, IOException {

		/// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return;
		
		String jobName = "unknown";
		String packageName = "unknown"; 
		int serviceId = -1;		
		int entityGroupId = -1;
		int scenarioId = -1;
		int datasetTypeId = -1;
		int secondaryEntityNum = -1;
		int entityVersion = -1;
		int opsRunId = -1;
		
		if ( Table.isTableValid(m_arguments) != 0 && m_arguments.getNumRows() > 0 ) {
			
			serviceId = getIntValue( m_arguments, "service_id" );
			jobName = getStringValue( m_arguments, "Job Name");
			packageName = getStringValue( m_arguments, "Package Name");			
			entityGroupId = getIntValue( m_arguments, "Current Entity Group Id" );
			scenarioId = getIntValue( m_arguments, "Current Scenario");
			datasetTypeId = getIntValue( m_arguments, "dataset_type_id" );
			secondaryEntityNum = getIntValue( m_arguments, "Current Secondary Entity Num" );
			entityVersion = getIntValue( m_arguments, "Current Entity Version" );
			opsRunId = getIntValue( m_arguments, "op_services_run_id" );							
		}
		
		flush( serviceId, jobName, packageName, entityGroupId, scenarioId, datasetTypeId, secondaryEntityNum, entityVersion, opsRunId );				
	}

	/// Private method which gets a string parameter from the table.
	private String getStringValue( Table table, String columnName ) throws OException {
		String value = "unknown";
		
		if ( Table.isTableValid( table ) == 0 || table.getNumRows() != 1 )
			return ( value );
		
		int col = table.getColNum(columnName);

		if ( col > 0 && table.getColType( col ) == COL_TYPE_ENUM.COL_STRING.toInt() )
			value = table.getString( col, 1 );
		
		if ( value == null || value.isEmpty() )
			return ( "unknown" );
		
		return ( value );
	}
	
	/// Private method which gets an int parameter from the table
	private int getIntValue( Table table, String columnName ) throws OException {
		int value = -1;
		
		if ( Table.isTableValid( table ) == 0 || table.getNumRows() != 1 )
			return ( value );
		
		int col = table.getColNum(columnName);
		
		if ( col > 0 && table.getColType( col ) == COL_TYPE_ENUM.COL_INT.toInt() )
			value = table.getInt( col, 1 );
		
		return ( value );
	}
	
	/// Open a console capture session, if we already have a session this will do nothing.
	public void open( Object creator ) throws OException {
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return;		
		m_creator = creator;
		super.open();	
	}
	
	/// Close the wrapper which will turn off the console capture.
	public void close( Object creator ) throws OException {
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return;
		
		// If not the creator, we can't close the console capture.
		if ( m_creator == null || creator != m_creator )
			return;
		
		super.close();
	}

	/// Check whether we have started a capture event.
	public boolean isOpen() {		
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return( false );
		return ( super.isOpen() );
	}
	
	/// Get the previous error, or return an empty string if no error encountered.
	public String getError() {
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )				
			return ( "" );
		return ( super.getError() );
	}
	
	/// Set whether we are to log the output of the console capture to the database.
	public void setLogToDb( boolean logToDb ) {
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return;
		super.setLogToDb(logToDb);
	}
	
	/// Dump the contents of the console capture to a string.
	public String toString() {
		// No api available.
		if ( !m_apiAvailable || m_loggingOff )
			return ( "" );
		return ( super.toString() );
	}
	
	/// Private constructor for the instance.  Singleton usage only.
	private ConsoleCaptureWrapper() throws OException {
		super( false );	
	}	

	/// Get the environment variables that are needed for this run... 
	private void GetEnvironmentVariables() throws OException
	{

		if ( Str.isEmpty(Util.getEnv("AB_APM_DISABLE_CONSOLE_LOGGING")) != 1 )
		{
			String consoleLoggingOff = Util.getEnv("AB_APM_DISABLE_CONSOLE_LOGGING");
		
			try
			{
				m_loggingOff = Boolean.parseBoolean(consoleLoggingOff.trim());
				String warningMessage = "AB_APM_DISABLE_CONSOLE_LOGGING set !!  Console logging is off = " + m_loggingOff;								
				String sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + warningMessage + "\n";
				OConsole.oprint(sMsg);				
			}
			catch (Exception e)
			{
				String warningMessage = "AB_APM_DISABLE_CONSOLE_LOGGING environment variable has bad format =" + consoleLoggingOff;								
				String sMsg = OCalendar.formatDateInt(OCalendar.getServerDate(), DATE_FORMAT.DATE_FORMAT_DMLY_NOSLASH) + " " + Util.timeGetServerTimeHMS() + ":" + warningMessage + "\n";
				OConsole.oprint(sMsg);				
			}	
		}		
	}
	
}
