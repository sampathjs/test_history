/* Released with version 29-Oct-2015_V14_2_4 of APM */

package standard.include;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.olf.openjvs.Apm;
import com.olf.openjvs.OConsole;
import com.olf.openjvs.ODateTime;
import com.olf.openjvs.Ref;
import com.olf.openjvs.Str;
import com.olf.openjvs.Table;
import com.olf.openjvs.XString;
import com.olf.openjvs.OException;
import com.olf.openjvs.enums.COL_TYPE_ENUM;

/// 
public class ConsoleCapture {

	/// Handle to the capture object that we are creating.
	private int m_captureHandle = -1;

	/// The table containing the captured lines.
	private Table m_capture = null; 
	
	/// The error string in the console capture object.
	private String m_error = "";
	
	/// Whether we are flushing the console capture to db or file.
	private boolean m_toDb = false;
	
	/// The lock to the object to ensure that there will be only one!
	private Lock m_lock = new ReentrantLock();
	
	/// Offset to perform the operation to start the console capture
	private final int APM_BEGIN_CONSOLE_CAPTURE = 28;
	
	/// Offset to perform the operation to end console capture.
	private final int APM_END_CONSOLE_CAPTURE = 29;
	
	/// Offset to perform the operation to begin console capture.
	private final int APM_INSERT_CONSOLE_CAPTURE = 30;
	
	/// Column name for the console capture handle.
	private final String APM_CONSOLE_CAPTURE_HANDLE_FIELD_NAME = "console_capture_handle";
	
	/// Column name for the console capture output.
	private final String APM_CONSOLE_CAPTURE_OUTPUT_FIELD_NAME = "console_capture_output";
	
	/// Column name for the console capture log entry to write back to db.
	private final String APM_CONSOLE_CAPTURE_LOG_ENTRY_FIELD_NAME = "console_capture_log_entry";
	
	/// Default constructor, will attempt to start a console capture session.
	public ConsoleCapture( ) throws OException {
		// RAII, or as close to that as is possible in Java.  Attempt to start
		// the console capture and get a handle into the object.
		open();		
	}
	
	/// Protected constructor to be used by derived classes.
	protected ConsoleCapture( boolean doOpen ) throws OException {
		if ( doOpen )
			open();
	}
	
	/// Finalizer needed to ensure that we always stop a console capture session if started.
	protected void finalize() throws Throwable {
		try {
			// Make sure that our wrapper clears the console listener.
			close( false );      
			
			// Make sure that our stored tables are destroyed.
			if ( Table.isTableValid( m_capture ) != 0 )
				m_capture.destroy();
	    } finally {
	        super.finalize();
	    }
	}
	
	/// Open a console capture session, if we already have a session this will do nothing.
	protected void open() throws OException {
		
		XString errorString = Str.xstringNew();
		Table params = Table.tableNew( "params" );		
		
		try {		
			// Wait until we can safely get the main lock
			m_lock.lock();
			
			// Only create a connection if we have not already.
			if ( !isOpen() ) {			
				// Get the parameter table.
				int ok = Apm.performOperation( APM_BEGIN_CONSOLE_CAPTURE, 1, params, errorString);
			
				if ( ok != 0 ) {							
					// Try and get the handle now.
					ok = Apm.performOperation( APM_BEGIN_CONSOLE_CAPTURE, 0, params, errorString );
				
					if ( ok != 0 ) {
						m_captureHandle = params.getTable( "parameter_value", 1 ).getInt( APM_CONSOLE_CAPTURE_HANDLE_FIELD_NAME, 1 );				
					}						
				}
			
				if ( ok == 0 ) {
					// Some state is bad, throwing an explanation to be handled higher up.
					OException exception = new OException(errorString.toString());				
					exception.fillInStackTrace();
					throw exception;
				}		
			}
		} finally {
			// Final clean up.
			m_lock.unlock();
			params.destroy();
			Str.xstringDestroy(errorString);
		}		
	}
	
	/// Close the wrapper which will turn off the console capture.
	protected void close() throws OException {		
		// Close the console capture wrapper, don't store the output.
		close( false );		
		return;
	}	
	
	/// Check whether we have started a capture event.
	public boolean isOpen()
	{
		boolean open = false;
		
		try {
			m_lock.lock();
			open = ( m_captureHandle != -1 );
		} finally {
			m_lock.unlock();
		}
		
		return ( open );
	}
	
	/// Get the previous error, or return an empty string if no error encountered.
	public String getError() {
		return ( m_error );
	}
	
	/// Set whether we are to log the output of the console capture to the database.
	public void setLogToDb( boolean logToDb ) {
		try {
			m_lock.lock();
			m_toDb = logToDb;
		} finally {
			m_lock.unlock();
		}
	}
	
	/// Dump the contents of the console capture to a string.
	public String toString() {
		StringBuilder builder = new StringBuilder( "" );	
		
		try {
			// Do nothing until we have a lock on the thread.
			m_lock.lock();	
			
			// Clear the previous error if any.
			m_error = "";
			
			// Make sure that the object is closed.
			close( true );
			
			// If we have a captured object then dump this to a StringBuilder object to return.
			if ( m_capture != null ) {				
				int numRows = m_capture.getNumRows();

				for ( int row = 1; row <= numRows; row++) {							
					builder.append( m_capture.getString(1, row) );							
				}																			
			}					
		} catch (OException exception) {
			m_error = exception.toString();
		}		
		finally {
			m_lock.unlock();
		}
		
		return (builder.toString());
	}

	/// Flush the console capture to a range of destinations.
	protected void flush( int serviceId, String jobName, String packageName, int entityGroupId, int scenarioId, int datasetTypeId, int secondaryEntityNum, int entityVersion, int opsRunId ) throws OException, IOException {
		if ( m_toDb )
			toDatabase( serviceId, jobName, packageName, entityGroupId, scenarioId, datasetTypeId, secondaryEntityNum, entityVersion, opsRunId );
		else
			toFile();
	}

	/// Close the wrapper which may or may not store the console capture.
	private void close( boolean storeCapture ) throws OException {		
		
		XString errorString = Str.xstringNew();
		Table params = Table.tableNew("params");
		
		try {
			// Wait until we can safely get the main lock
			m_lock.lock();
		
			// Only attempt to close if we have previously started a console capture session.
			if ( isOpen() ) {
				/// Get the parameters for the capture event, and stop the capture.
				int ok = getEndCaptureParams( m_captureHandle, params, errorString );
					
				if ( ok != 0 ) {							
					ok = doEndCapture( storeCapture, params, errorString );
				}
			
				if ( ok == 0 ) {
					// Some state is bad, throwing an explanation to be handled higher up.
					OException exception = new OException(errorString.toString());				
					exception.fillInStackTrace();
					throw exception;
				}
				
				m_captureHandle = -1;
			}			
		} finally {			
			// Clean up.
			m_lock.unlock();			
			params.destroy();			
			Str.xstringDestroy(errorString);				
		}				
	}
	
	/// Writes out any captured data to the given path.
	private void toFile() throws OException, IOException {		
		try {
			m_lock.lock();	
			
			// Make sure that the object is closed.
			close( true );
			
			if ( m_capture != null ) {							
				
				// Write out where we are dumping this to.
			    OConsole.oprint( "Attempting to write a log file to " + LogConfigurator.getInstance().getFileName(true));
				
				// We have some capture, send it to our file output.			
				FileWriter file = new FileWriter( LogConfigurator.getInstance().getFileName( true ) );
				
				if ( file != null ) {				
					BufferedWriter writer = new BufferedWriter( file ); 
					
					if ( writer != null ) {	
						int numRows = m_capture.getNumRows();

						for ( int row = 1; row <= numRows; row++) {							
							writer.write( m_capture.getString(1, row) );							
						}															
						
						writer.flush();
						writer.close();
					}					
				}
				
				// We have used the capture object... no need to keep it now...
				m_capture.destroy();
				m_capture = null;
			}						
		} finally {
			m_lock.unlock();
		}
	}
	
	/// Write out any captured data to the database.
	private void toDatabase( int serviceId, String jobName, String packageName, int entityGroupId, int scenarioId, int datasetTypeId, int secondaryEntityNum, int entityVersion, int opsRunId ) throws OException {		
		Table params = null;
		Table capture = null;
		ODateTime dateTime = null;
		XString errorString = null;
		
		try {
			m_lock.lock();				
			errorString = Str.xstringNew();
			
			// Make sure that the object is closed.
			close( true );
			
			if ( m_capture != null ) {							
				params = Table.tableNew("params");
				
				// Attempt to get the params for the db write action.
				int ok = Apm.performOperation( APM_INSERT_CONSOLE_CAPTURE, 1, params, errorString);
			
				if ( ok == 0 ) {
					// Some state is bad, throwing an explanation to be handled higher up.
					OException exception = new OException(errorString.toString());				
					exception.fillInStackTrace();
					throw exception;
				}
				
				// create a new capture table.				
				
				capture = Table.tableNew("capture");
				capture.addCol("service_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("job_name", COL_TYPE_ENUM.COL_STRING);
				capture.addCol("process_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("package", COL_TYPE_ENUM.COL_STRING);
				capture.addCol("entity_group_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("dataset_type_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("scenario_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("secondary_entity_num", COL_TYPE_ENUM.COL_INT);
				capture.addCol("entity_version", COL_TYPE_ENUM.COL_INT);
				capture.addCol("ops_run_id", COL_TYPE_ENUM.COL_INT);
				capture.addCol("timestamp", COL_TYPE_ENUM.COL_DATE_TIME);
				capture.addCol("blob_data_length", COL_TYPE_ENUM.COL_INT);
				capture.addCol("blob_data", COL_TYPE_ENUM.COL_BYTE_ARRAY);
				
				byte[] array = toString().getBytes();
				dateTime = APM_Utils.getODateTime();
				capture.addRow();				
				capture.setInt("service_id", 1, serviceId);
				capture.setString("job_name", 1, jobName);
				capture.setInt("process_id", 1, Ref.getProcessId());
				capture.setString("package", 1, packageName);
				capture.setInt("entity_group_id", 1, entityGroupId);
				capture.setInt("dataset_type_id", 1, datasetTypeId);
				capture.setInt("scenario_id", 1, scenarioId);
				capture.setInt("secondary_entity_num", 1, secondaryEntityNum);				
				capture.setInt("entity_version", 1, entityVersion);
				capture.setInt("ops_run_id", 1, opsRunId );				
				capture.setDateTime(capture.getColNum("timestamp"), 1, dateTime);
				capture.setByteArray(capture.getColNum("blob_data"), 1, array);
				capture.compressColByteArray(capture.getColNum("blob_data"));
				
				// Get the compressed length of the array				
				byte[] compressedArray = capture.getByteArray(capture.getColNum("blob_data"), 1);				
				capture.setInt("blob_data_length", 1, compressedArray.length);
				
				if ( Table.isTableValid( params ) != 0 ){
					// Setup the parameters to return to db... should check that the setup is as expected...
					Table logs = params.getTable( "parameter_value", 1 );
					
					if ( Table.isTableValid( logs ) != 0 ){
						logs.setTable(APM_CONSOLE_CAPTURE_LOG_ENTRY_FIELD_NAME, 1, capture);						
					}
					
					// Insert the capture table... the dbase call should be 
					// generic, but the current apm call passes an argument table
					// around which is not good... move this code if we create a 
					// more generic 

					// Try and close listener and if required get the contents of the cyclic buffer.
					ok = Apm.performOperation( APM_INSERT_CONSOLE_CAPTURE, 0, params, errorString );
				
					if ( ok == 0 ) {	
						// Some state is bad, throwing an explanation to be handled higher up.
						OException exception = new OException(errorString.toString());				
						exception.fillInStackTrace();
						throw exception;
					}					
				}
				
				// We have used the capture object... no need to keep it now...
				m_capture.destroy();
				m_capture = null;
			}						
		} finally {			
			if ( errorString != null )
				Str.xstringDestroy(errorString);
			
			m_lock.unlock();
			
			// Only need to destroy the params table, as capture table now belongs to it.			
			if ( Table.isTableValid(params) != 0 )
				params.destroy();
			
			if ( dateTime != null )
				dateTime.destroy();							
		}							
	}			

	/// Get the parameters to send through to stop the console capture.
	private int getEndCaptureParams( int handle, Table params, XString errorString ) throws OException {
		
		// Attempt to get the params for the end capture action.
		int ok = Apm.performOperation( APM_END_CONSOLE_CAPTURE, 1, params, errorString);
	
		if ( ok == 0 ) {
			return ( ok );			
		}
		
		params.getTable( "parameter_value", 1 ).setInt( APM_CONSOLE_CAPTURE_HANDLE_FIELD_NAME, 1, handle );
		
		return ( ok );
	}
	
	/// Stop the capture operation and handle the capture return table.
	private int doEndCapture( boolean storeCapture, Table params, XString errorString ) throws OException {
		
		// Try and close listener and if required get the contents of the cyclic buffer.
		int ok = Apm.performOperation( APM_END_CONSOLE_CAPTURE, 0, params, errorString );
	
		if ( ok == 0 ) {	
			return ( ok );
		}
		
		if ( storeCapture ) {			
			if ( Table.isTableValid( m_capture ) != 0 )
				m_capture.destroy();
			
			m_capture = params.getTable( "parameter_value", 2 ).getTable( APM_CONSOLE_CAPTURE_OUTPUT_FIELD_NAME, 1 ).copyTable();		
		}
		
		return (ok);		
	}					
}
