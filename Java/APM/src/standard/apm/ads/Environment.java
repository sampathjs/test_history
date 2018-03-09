package standard.apm.ads;

import java.util.Map;
import java.util.ArrayList;

/**
 * 
 * @author mknight
 * 
 * This class provides information and checking about the ADS environment.
 *
 */
public class Environment
{
	/**
	 * This checks that the CLASSPATH and AB_CLASSPATH do not contain ADS jar files.
	 * 
	 * @throws ADSException
	 */
	public static void CheckClassPaths() throws ADSException
	{
		// Get the environment variables.
		
        Map<String, String> environmentVariables = System.getenv();
        ArrayList<String> pathErrors = new ArrayList<String>();
        ArrayList<String> abPathErrors = new ArrayList<String>();
        String classpath;
        String abClasspath;
        
        // Check CLASSPATH.
        
        classpath = environmentVariables.get("CLASSPATH");
        
        if( classpath != null )
        {
            classpath = classpath.trim();
        	
        	pathErrors.addAll(ScanPath(classpath));
        }
        
        // Check AB_CLASSPATH.
        
        abClasspath = environmentVariables.get("AB_CLASSPATH");
        
        if( abClasspath != null )
        {
        	abClasspath = abClasspath.trim();
        	
        	abPathErrors.addAll(ScanPath(abClasspath));
        }
        
        // Build up the error message, if required.
        
        StringBuilder errorMessage = new StringBuilder("ADS jar files are not permitted on the classpaths. The entries must be removed from the paths before ADS will function correctly." + SEPARATOR);
        boolean error = false;
        
        if( pathErrors.size() > 0 )
        {
        	// Show the errors on the CLASSPATH.
        	
        	errorMessage.append( CLASSPATH_ERROR_MESSAGE );
        	
        	for( String jarName : pathErrors )
        	{
        		errorMessage.append( jarName );
        		errorMessage.append( SEPARATOR );
        	}
        	
        	// Show the CLASSPATH.
        	
        	errorMessage.append( CLASSPATH_LISTING_MESSAGE );
        	errorMessage.append( classpath );
    		errorMessage.append( SEPARATOR );
    		
        	error = true;
        }
        
        if( abPathErrors.size() > 0 )
        {
        	errorMessage.append( AB_CLASSPATH_ERROR_MESSAGE );
        	
        	for( String jarName : abPathErrors )
        	{
        		errorMessage.append( jarName );
        		errorMessage.append( SEPARATOR );
        	}
        	
        	// Show the AB_CLASSPATH.
        	
        	errorMessage.append( AB_CLASSPATH_LISTING_MESSAGE );
        	errorMessage.append( abClasspath );
    		errorMessage.append( SEPARATOR );
    		        	
        	error = true;
        }
        
        // Throw the exception, if required.
        
        if( error )
        {
        	throw new ADSException( errorMessage.toString() );
        }
	}
	
	/**
	 * This checks an environment path for known ADS jar files.
	 * 
	 * @param path	The path to scan.
	 * @return		A list of the errors, or an empty list otherwise.
	 */
	private static ArrayList<String> ScanPath( String path )
	{
		ArrayList<String> errors = new ArrayList<String>();
		
		if( path == null )
		{
			return errors;
		}

		String[] items = path.split(";");
		
		for( String item : items )
		{
			String current = item.trim();
			
			for( String adsJarName : ADS_JARS )
			{
				if( current.equals(adsJarName)
						|| current.endsWith("/" + adsJarName)
						|| current.endsWith("\\" + adsJarName))
				{
					errors.add(adsJarName);
					
					break;
				}
			}
		}
		return errors;
	}
	    
	private static final String SEPARATOR = "\n";
	private static final String ERROR_MESSAGE = " must have the following entries removed:" + SEPARATOR;
	private static final String CLASSPATH_ERROR_MESSAGE = "CLASSPATH" + ERROR_MESSAGE;
	private static final String AB_CLASSPATH_ERROR_MESSAGE = "AB_CLASSPATH" + ERROR_MESSAGE;
	private static final String PATH_LISTING_MESSAGE = "Here is the current value for ";	
	private static final String CLASSPATH_LISTING_MESSAGE = PATH_LISTING_MESSAGE + "CLASSPATH:" + SEPARATOR;
	private static final String AB_CLASSPATH_LISTING_MESSAGE = PATH_LISTING_MESSAGE + "AB_CLASSPATH:" + SEPARATOR;
    
    /**
     * A list of the known ADS jars.
     */
	private static final String[] ADS_JARS =
	{
		"ads_core.jar",
		"ads_endur.jar",
		"ads_coherence.jar",
		"ads_apm.jar",
		"ads_apm_common.jar",
		"ads_rtpe.jar",
		"ads_examples.jar",
		"ads_examples_security.jar",
		"ads_jdbc.jar",
		"ads_utils.jar",
		"ads_oscar.jar",
	};	
}
