/* Released with version 05-Feb-2020_V17_0_126 of APM */


package standard.apm.ads;


/**
 ***************************************************************************
 *
 * Copyright 2008 Open Link Financial, Inc. *
 *
 * ALL RIGHTS RESERVED *
 *
 * ***************************************************************************
 *
 * @author rbarr
 *
 *         Description: Factory is responsible for constructing and returning the correct implementations of an interface.
 *
 *         This is used to break the dependency between ADS and APM, and to provide the correct implementations for different Endur versions where necessary
 */
public class Factory {

   private static ADSInterface adsImpl;

   private static IStatusLogger statusLoggingImpl;

   private static IGatherServiceStatus gatherServiceStatusImpl;

   private static String m_classpathErrorMessage = "A potential classpath conflict has occurred.\n"
	   + "Check that the ADS jar files (including ads_core.jar and olf_ads.jar)\n"
	   + "are not present on the CLASSPATH or AB_CLASSPATH.\n";

   /**
    * Instantiate the ADSInterface implementation if it does not already exist. Exceptions are thrown to the calling class to be handled
    *
    * @throws ADSException
    * @throws ClassNotFoundException
    * @throws IllegalAccessException
    * @throws InstantiationException
    */
   public static ADSInterface getADSImplementation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ADSException {
      if (adsImpl == null) {
    	  try {
    		  adsImpl = (ADSInterface)Class.forName("com.olf.ads.apm.openjvs.APMHandler").newInstance();
    	  } catch( java.lang.ExceptionInInitializerError ex ) {
    		  throw new ADSException(m_classpathErrorMessage, ex);
    	  }
      }
      return adsImpl;
   }

   /**
    * Instantiate the ADSDatasetStatusLogging implementation if it does not already exist. Exceptions are thrown to the calling class to be handled
    *
    * @throws ADSException
    * @throws ClassNotFoundException
    * @throws IllegalAccessException
    * @throws InstantiationException
    */
   public static IStatusLogger getStatusLoggingImplementation() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ADSException {
      if (statusLoggingImpl == null) {
    	  try {
    		  statusLoggingImpl = (IStatusLogger) Class.forName("com.olf.ads.apm.datasetstatus.DatasetStatusLogging").newInstance();
    	  } catch( java.lang.ExceptionInInitializerError ex ) {
    		  throw new ADSException(m_classpathErrorMessage, ex);
    	  }
      }
      return statusLoggingImpl;
   }

   /**
    * instantiates the ADS implementation via reflection if it does not exist already Exceptions are thrown to the calling class to be handled
    *
    * @throws ADSException
    * @throws ClassNotFoundException
    * @throws IllegalAccessException
    * @throws InstantiationException
    * @throws Exception
    */
   public static IGatherServiceStatus getGatherServiceStatusImpl() throws InstantiationException, IllegalAccessException, ClassNotFoundException, ADSException {
      if (gatherServiceStatusImpl == null) {
    	  try {
    		  gatherServiceStatusImpl = (IGatherServiceStatus) Class.forName("com.olf.ads.apm.gatherservicestatus.GatherServiceStatusImpl").newInstance();
    	  } catch( java.lang.ExceptionInInitializerError ex ) {
    		  throw new ADSException(m_classpathErrorMessage, ex);
    	  }
      }
      return gatherServiceStatusImpl;
   }
}
