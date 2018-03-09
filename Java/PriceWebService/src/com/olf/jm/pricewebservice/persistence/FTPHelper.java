package com.olf.jm.pricewebservice.persistence;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.openlink.util.logging.PluginLog;

/*
 * History:
 * 2015-11-10	V1.0	jwaechter	- initial version
 */

/**
 * Helper class to deal with FTP issues
 * @author jwaechter
 * @version 1.0
 */
public class FTPHelper {


	/**
	 * Upload a file to a FTP server. A FTP URL is generated with the
	 * following syntax:
	 * ftp://user:password@host:port/filePath;type=i.
	 *
	 * @param ftpServer , FTP server address (optional port ':portNumber').
	 * @param user , Optional user name to login.
	 * @param password , Optional password for user.
	 * @param fileName , Destination file name on FTP server (with optional
	 *            preceding relative path, e.g. "myDir/myFile.txt").
	 * @param source , Source file to upload.
	 * @throws MalformedURLException
	 */
	public static void upload( String ftpServer, String user, String password,
			String fileName, File source )
	{
		if (fileName.trim().startsWith("/")) {
			fileName = fileName.trim().substring(1);
		} else {
			fileName = fileName.trim();
		}

		if (ftpServer != null && fileName != null && source != null)
		{
			StringBuilder sb = getConnectionString(ftpServer, user, password,
					fileName);

			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try
			{
				URL url = new URL( sb.toString() );
				URLConnection urlc = url.openConnection();

				bos = new BufferedOutputStream( urlc.getOutputStream() );
				bis = new BufferedInputStream( new FileInputStream( source ) );

				int i;
				// read byte by byte until end of stream
				while ((i = bis.read()) != -1)
				{
					bos.write( i );
				}
				PluginLog.info ("File " + fileName + " successfully transfered to FTP server " + ftpServer + "/" + source.getName());

			}
			catch (IOException ex) {
				PluginLog.info (ex.toString());
				throw new RuntimeException ("Error transfering data to FTP");
			}
			finally
			{
				if (bis != null)
					try
				{
						bis.close();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
				if (bos != null)
					try
				{
						bos.close();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
		else
		{
			System.out.println( "Input not available." );
		}
	}

	public static void deleteFileFromFTP ( String ftpServer, String user, String password,
			String fileName) {
		if (fileName.trim().startsWith("/")) {
			fileName = fileName.trim().substring(1);
		} else {
			fileName = fileName.trim();
		}
		PrintStream ps=null;
		try {
			if (ftpServer != null && fileName != null)
			{
				StringBuilder connectionString = getConnectionString(ftpServer, user, password, fileName);
				URL u = new URL(connectionString.toString());
				URLConnection uc = u.openConnection();
				ps = new PrintStream(uc.getOutputStream());
				ps.println("RMD " + fileName);
			} 
		} 	catch (IOException ex) {
			PluginLog.info (ex.toString());
			throw new RuntimeException ("Error transfering data to FTP");
		}
		finally {
			if (ps != null)  {
				ps.close();
			}
		}
	}

	private static StringBuilder getConnectionString(String ftpServer,
			String user, String password, String fileName) {
		StringBuilder sb = new StringBuilder( "ftp://" );
		// check for authentication else assume its anonymous access.
		if (user != null && !user.trim().equals("") && password != null && !password.trim().equals(""))
		{
			sb.append( user );
			sb.append( ':' );
			sb.append( password );
			sb.append( '@' );
		}
		sb.append( ftpServer );
		sb.append( '/' );
		sb.append( fileName );
		/*
		 * type ==&gt; a=ASCII mode, i=image (binary) mode, d= file directory
		 * listing
		 */
		sb.append( ";type=i" );

		//	         PluginLog.info ("Connection String " + sb.toString());
		return sb;
	}

	/**
	 * To prevent initialization. 
	 */
	private FTPHelper () {

	}
}
