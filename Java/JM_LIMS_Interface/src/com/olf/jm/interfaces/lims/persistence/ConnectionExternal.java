package com.olf.jm.interfaces.lims.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.olf.jm.logging.Logging;

/*
 * History:
 * 2015-09-01	V1.0	jwaechter	-	initial version
 * 2021-12-15	V2.0	Prashanth   -   Initialise logging		
 */

public class ConnectionExternal {
	/**
	 * Full class name of the JDBC driver used to connect to the AS400 remote system.
	 */
	private static final String JDBC_DRIVER_CLASS_AS400 = "com.ibm.as400.access.AS400JDBCDriver";

	private final String serverName;
	private final String dbName;
	private final String user;
	private final String password;
	private Connection conn;

	public ConnectionExternal (final String serverName, final String dbName, final String user,
			final String password) {
		this.serverName = serverName;
		this.dbName = dbName;
		this.user = user;
		this.password = password;
		Logging.init(this.getClass(), "", "");
	}

	public Object[][] query (String sql) {
		connect();
		Logging.info("Executing query " + sql + " on remote system " +  toString());
		Statement statement = null; 
		try {
			statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(sql);
			Logging.info("Query " + sql + " has been executed on remote system " + toString() + ". Retrieving data");
			ResultSetMetaData rsm = rs.getMetaData();
			int numColumns = rsm.getColumnCount();
			List<Object[]> tableData = new LinkedList<> ();
			Logging.info("Following data is returned from SQL:");
			while (rs.next()) {
				Object[] rowData = new Object[numColumns];
				for (int colNum = 1; colNum <= numColumns; colNum++) {
					rowData[colNum-1] = rs.getObject(colNum);
				}
				Logging.info(Arrays.deepToString(rowData));
				tableData.add(rowData);
			}
			Object[][] tableDataAsArray = new Object[tableData.size()][]; 
			tableDataAsArray = tableData.toArray(tableDataAsArray);
			Logging.info("Query " + sql + " has been executed on remote system " + toString() + " and data received");
			return tableDataAsArray;
		} catch (SQLException e) {
			throw new RuntimeException ("Error executing SQL " + sql + ":\n" + e.toString(), e);
		} finally {
			if (statement != null) {
				try {
					if (statement != null) {
						statement.close();	
					}
				} catch (SQLException e) {
					throw new RuntimeException ("Error closing statement", e);
				}
			}
			disconnect();
		}
	}

	private void disconnect () {
		try {
			if (conn != null || !conn.isClosed() || conn.isValid(1000)) {
				conn.close();
				conn = null;
			}
		} catch (SQLException e) {
			throw new RuntimeException ("Error closing connection or checking if connection is closed", e);
		}
		Logging.info("Disconnected from " + toString() + ".");
	}
	
	@Override
	public String toString () {
		return serverName + "\\" + dbName + "(user= " + user + ")";
	}

	private void connect () {
		Logging.info("Establishing a connection to " + serverName + "\\" + dbName + " as user " + user);
		try {
			if (conn == null || conn.isClosed() || !conn.isValid(1000)) {
				Class.forName(JDBC_DRIVER_CLASS_AS400);
				String connString = generateConnectionString(serverName, dbName);
				conn = DriverManager.getConnection(connString, user, password);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException ("Could not locate the JDBC drivers class " + JDBC_DRIVER_CLASS_AS400 + 
					". Please check if JT400.jar is included in a) classpath or b) imported to Endur as a library", e);
		} catch (SQLException e) {
			throw new RuntimeException ("Could not establish a connection to the remote system using the following "
					+ " connection properties: \nserver name = " + serverName + "\n databaseConn name = " + dbName
					+ "\nuser = " + user + "\npassword = " + password.replaceAll("(?s).", "*"));
		}
		Logging.info("Connection to " + toString() + " has been established.");
	}

	private String generateConnectionString (String serverName, String dbName) {
		StringBuilder sb = new StringBuilder ();
		sb.append ("jdbc:as400:");
		sb.append(serverName);
		sb.append(";database name=");
		sb.append(dbName);
		sb.append(";prompt=false;translate binary=true;naming=system");
		return sb.toString();
	}
}
