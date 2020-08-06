package com.matthey.openlink.utilities;

import java.util.Map;
import java.util.Properties;

import com.olf.openjvs.OConsole;
import com.olf.openjvs.OException;
import com.olf.jm.logging.Logging;
import com.openlink.util.constrepository.ConstRepository;

public class Repository {

	
	public static Properties getConfiguration(final String context, final String subContext, final Map<String, String> properties) {

		try {
		    Logging.init(Repository.class, "", "");
			ConstRepository implementationConstants = null;
			Properties config = new Properties();
			if (context != null){
				implementationConstants = new ConstRepository(context,subContext);
			}
			
			for (java.util.Map.Entry<String, String> property : properties.entrySet()) {
				if (context != null) {
					config.put( property.getKey(), implementationConstants.getStringValue( property.getKey(), property.getValue()));
				} else {
					config.put(property.getKey(), property.getValue());
				}
				OConsole.message(String.format("KEY: %s \t\t VALUE:%s\n",property.getKey(), config.getProperty(property.getKey())));
			}
	        return config;
		} catch (OException e) {
			Logging.error( "constant repository problem", e);
			throw new RuntimeException("constant repository problem:CAUSE>" + e.getLocalizedMessage(), e);
		}finally{
		    Logging.close();
		}
		

	}
    
    private Repository() {}
}
