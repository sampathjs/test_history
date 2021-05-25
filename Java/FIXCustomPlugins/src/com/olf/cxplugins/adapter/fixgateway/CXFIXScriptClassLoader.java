package com.olf.cxplugins.adapter.fixgateway; 

/*
      Name       : CXFIXScriptClassLoader.java

      Description: The script provides a framework for the user to choose which BizInclude class files
       			   should be loaded to perform standard FIX scripts functions

                  User should reference this script as part of INCLUDE script to invoke
                  any built-in functions listed here.

      Release    : Original 6/1/2007
 */

public class CXFIXScriptClassLoader
{ 	
	public CXFIXScriptClassLoader() {  
	}
	
	public static String getClassName() {
		return "com.olf.cxplugins.adapter.fixgateway.CXFIXScriptClassLoader";
	}
	
    /*-------------------------------------------------------------------------------        
    Name:             getCXFIXScriptClassLoader
    Description:      This function will return com.olf.cxplugins.adapter.fixgateway.CXFIXScriptClassLoader 
    				  if it can be loaded.

    Release    :	  Original 5/18/2011
    -------------------------------------------------------------------------------*/        
	public static CXFIXScriptClassLoader getCXFIXScriptClassLoader(String className) {
		
		try {
			Class<?> c = Class.forName(className);     			
			return (CXFIXScriptClassLoader)c.newInstance();
		} catch (Exception ex) {}
		
		return null;
	}
	
	/*-------------------------------------------------------------------------------
	   Name       : getFIXBizIncludeClass

	   Description: The script provides a framework for the user to load appropriate CXFIXBizInclude class file
				   which implements ICXFIXCustomBizInclude interface.  The class was used by 
				   "CXFIXTradePreProcess", "CXFIXTradePostProcess", "CXFIXOutput"
				   and "CXFIXGeneration"

	   			    TODO section should be modified to load required ICXFIXCustomBizInclude class.

	   Release    : Original 6/1/2007
	-------------------------------------------------------------------------------*/ 
	public ICXFIXCustomBizInclude getFIXBizIncludeClass() {

		/* TODO - Add custom logic here */
		return new CXFIXCustomBizInclude();
		/* END TODO */
	}

	/*-------------------------------------------------------------------------------
	   Name       : getFIXPushIncludeClass

	   Description: The script provides a framework for the user to load appropriate FIXPushInclude class file
				   which implements IFIXCustomPushInclude interface.  The class was used by "FIXSTDPushTrade".

	   			    TODO section should be modified to load required IFIXCustomPushInclude class.

	   Release    : Original 6/1/2007
	-------------------------------------------------------------------------------*/ 
	public IFIXCustomPushInclude getFIXPushIncludeClass() {

		/* TODO - Add custom logic here */
		return new FIXCustomPushInclude();
		/* END TODO */
	} 

	/*-------------------------------------------------------------------------------
	   Name       : getFIXProcessFIXIncludeClass

	   Description: The script provides a framework for the user to load appropriate FIXProcessFIXInclude class file
				   which implements IFIXCustomProcessFIXInclude interface.  The class was used to process incoming FIX message.

	   			    TODO section should be modified to load required IFIXCustomProcessFIXInclude class.

	   Release    : Original 6/1/2007
	-------------------------------------------------------------------------------*/    
	public IFIXCustomProcessFIXInclude getFIXProcessFIXIncludeClass() {

		/* TODO - Add custom logic here */
		return new FIXCustomProcessFIXIncludeDistributor();
		/* END TODO */
	} 
}
