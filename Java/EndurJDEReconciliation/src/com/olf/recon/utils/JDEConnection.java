package com.olf.recon.utils;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.olf.recon.exception.ReconciliationRuntimeException;
import com.openlink.util.logging.PluginLog;

/**
 * This class encapsulates the functionality around opening a connection to an external AS400 database.
 */
public class JDEConnection 
{
	private static final String JDBC_DRIVER_CLASS_AS400 = "com.ibm.as400.access.AS400JDBCDriver";
	private final String serverName;
	private final String dbName;
	private final String user;
	private final String password;
	private Connection conn;

	public JDEConnection(final String serverName, final String dbName, final String user, final String password) 
	{
		this.serverName = serverName;
		this.dbName = dbName;
		this.user = user;
		this.password = password;
	}
	
	/**
	 * Prepare a new stored procedure call given the database name, proc name and custom input params 
	 * 
	 * @param procName
	 * @param param1 - this is the start date filter (for now)
	 * @param param2 - this is the end date filter
	 * @return
	 */
	public CallableStatement prepareCall(String procName, String param1, String param2)
	{
		String call = "{CALL " + dbName + "/" + procName + "('" + param1 + "', '" + param2 + "')}";
		PluginLog.info("call Statement: " + call);
		CallableStatement cs = null;
		try
		{
			cs = conn.prepareCall(call);
		}
		catch (Exception e)
		{
			throw new ReconciliationRuntimeException("Unable to prepareCall: " + call + ", " + e.getMessage());
		}
		
		return cs;
	}

	/**
	 * Disconnect the connection
	 */
	public void disconnect() 
	{	
		try 
		{
			if (conn != null && !conn.isClosed() && conn.isValid(1000)) 
			{
				conn.close();
				conn = null;
				
				PluginLog.info("Connection closed!");
			}	
		} 
		catch (SQLException e) 
		{
			throw new ReconciliationRuntimeException("Error closing connection or checking if connection is closed!", e);
		}
	}
	
	@Override
	public String toString() 
	{
		return serverName + "\\" + dbName + "(user= " + user + ")";
	}

	/**
	 * Connect to an external database that lives on an external server using the relevant JDBC driver (AS400 in this case)
	 */
	public void connect() 
	{
		PluginLog.info("Establishing a connection to " + serverName + "\\" + dbName + " as user " + user);
		
		try 
		{
			if (conn == null || conn.isClosed() || !conn.isValid(1000)) 
			{
				Class.forName(JDBC_DRIVER_CLASS_AS400);
				String connString = generateConnectionString(serverName, dbName);
				conn = DriverManager.getConnection(connString, user, password);
			}
		} 
		catch (ClassNotFoundException e) 
		{
			throw new ReconciliationRuntimeException("Could not locate the JDBC drivers class " + JDBC_DRIVER_CLASS_AS400 + 
					". Please check if JT400.jar is included in a) classpath or b) imported to Endur as a library", e);
		} 
		catch (SQLException e) 
		{
			throw new ReconciliationRuntimeException("Could not establish a connection to the remote system using the following "
					+ " connection properties: \nserver name = " + serverName + "\n databaseConn name = " + dbName
					+ "\nuser = " + user + "\npassword = " + password.replaceAll("(?s).", "*"));
		}
		
		PluginLog.info("Connection to " + toString() + " has been established.");
	}

	/**
	 * Generate an initial connection string for a connection to the specified server and database
	 * 
	 * @param serverName
	 * @param dbName
	 * @return
	 */
	private String generateConnectionString(String serverName, String dbName) 
	{
		StringBuilder sb = new StringBuilder ();
		
		sb.append ("jdbc:as400:");
		sb.append(serverName);
		sb.append(";database name=");
		sb.append(dbName);
		sb.append(";prompt=false;translate binary=true;naming=system");
		
		return sb.toString();
	}
}