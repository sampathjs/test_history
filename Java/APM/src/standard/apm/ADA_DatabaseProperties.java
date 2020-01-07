/* Released with version 29-Aug-2019_V17_0_124 of APM */

package standard.apm;

import standard.include.APM_Utils;

import com.olf.openjvs.OException;
import com.olf.openjvs.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

public class ADA_DatabaseProperties
{
	private APM_Utils m_APMUtils;

	private String m_dbName;
	private String m_dbType;
	private String m_dbServer;
	private String m_dbUserName;
	private String m_dbPassword;

	public ADA_DatabaseProperties() {
		m_APMUtils = new APM_Utils();
	}

	public String getDbName()
	{
		return m_dbName;
	}
	
	public String getDbType()
	{
		return m_dbType;
	}
	
	public String getDbServer()
	{
		return m_dbServer;
	}
	
	public String getDbUserName()
	{
		return m_dbUserName;
	}
	
	public String getDbPassword()
	{
		return m_dbPassword;
	}

	public void loadConfiguration() throws OException
	{
		String configFile = Util.getEnv("AB_ADA_CONFIG_FILE");

		if (configFile == null || configFile.isEmpty()){
			configFile = Paths.get("").toAbsolutePath().toString() + "\\risk_analytics\\ada_config.properties";
		}
		
		m_APMUtils.APM_PrintMessage(null, "Using ADA Configuration File located at: " + configFile);
		
		InputStream input = null;

		try {

			input = new FileInputStream(configFile);

			Properties configProps = new Properties();
			configProps.load(input);

			m_dbName = getProperty(configProps, "dbName", "Database Name", configFile);
			m_dbType = getProperty(configProps, "dbType", "Database Type", configFile);
			m_dbUserName = getProperty(configProps, "dbUserName", "Database UserName", configFile);
			m_dbPassword = getProperty(configProps, "dbPassword", "Database Password", configFile);
			m_dbServer = getProperty(configProps, "dbServer", "Database Server", configFile);
		} catch (IOException e) {
			e.printStackTrace();
			
			throw new OException("An error occurred while reading the ADA properties file.");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String getProperty(Properties properties, String propertyName, String propertyDescription, String configFileName) throws OException
	{
		String propertyValue = properties.getProperty(propertyName);
		if (propertyValue == null || propertyValue.isEmpty()){
			RaiseException(propertyDescription, propertyName, configFileName);
		}
		return propertyValue;
	}
	
	private void RaiseException(String config, String configName, String configFile) throws OException {
		throw new OException("Invalid/Missing " + config + " provided in the properties file. Please make sure Config: " + configName + " is set to a valid value in "+ configFile + " file" );
	}
}
