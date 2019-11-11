/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.include;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;

// Handles logging configuration. Split out the scripts to write into 
// a log file per cluster engine/script engine/process.  The configurator
// is a singleton object. Not thread-safe.
public class LogConfigurator {

	/// The singleton instance of the configurator.
	private static LogConfigurator s_instance = null;
	
	/// The lock to the object to ensure that there will be only one!
	private static Lock s_lock = new ReentrantLock();
	
	/// The stack of pushed filenames to store the configurator stack.
	private List<String> m_stack = new ArrayList<String>();
	
	/// The currently running service name.
	private String m_serviceName = "";
	
	/// The current path.
	private String m_path = "";
	
	/// Get the singleton instance of the object.
	public static LogConfigurator getInstance() {
		try {
			s_lock.lock();
			if ( s_instance == null )
				s_instance = new LogConfigurator();
		} catch( Throwable exception ) {
			s_lock.unlock();
		}
		return ( s_instance );
	}
	
	/// Private constructor for singleton instance.
	private LogConfigurator() {		
	}
	
	/// Set the path to create/look for a log from.
	public void setPath( String path ) {
		try {
			s_lock.lock();			
			m_path = path;				
			if ( !m_path.endsWith("\\") && !m_path.endsWith("/") )
				m_path += "/";
		} finally {
			s_lock.unlock();
		}	
	}
	
	/// Set the service name for our object.
	public void setServiceName( String serviceName ) {
		try {		
			// Wait until we can safely get the main lock
			s_lock.lock();
			m_serviceName = serviceName;
		} finally {
			s_lock.unlock();
		}								
	}
	
	/// Gets the filename with or without the directory path
	public String getFileName( boolean includeDir )  throws OException {
		// Create the filename for saving...				
		StringBuilder fileName = new StringBuilder();
				
		try {		
			// Wait until we can safely get the main lock
			s_lock.lock();		
			fileName.append( APM_Utils.sanitiseFilename(m_serviceName) );
			fileName.append( "_" );
			fileName.append(Ref.getProcessId());
			fileName.append(".log");
			
			if ( includeDir ) {
				return ( m_path + fileName.toString() );
			}
		} finally {
			s_lock.lock();
		}
		
		return ( fileName.toString() );		
	}
	
	/// Push the filename to the head of the list.
	public void push( String fileName ) {
		try {		
			// Wait until we can safely get the main lock
			s_lock.lock();
			m_stack.add(0, fileName);
		} finally {
			s_lock.unlock();
		}	
	}
	
	/// Pop the current head off the stack, otherwise return the current filename.
	public String pop( ) throws OException {
		try {		
			// Wait until we can safely get the main lock
			s_lock.lock();
			
			if ( m_stack.size() > 0 )
				return m_stack.remove(0);
			
			return ( getFileName( true ) );
		} finally {
			s_lock.unlock();
		}	
	}
	
	/// Peek what is at the top/front of the stack, otherwise returns the current filename.
	public String front( ) throws OException {
		try {		
			// Wait until we can safely get the main lock
			s_lock.lock();
			
			if ( m_stack.size() > 0 )
				return m_stack.get(0);
			
			return ( getFileName( true ) );
		} finally {
			s_lock.unlock();
		}	
	}
}
