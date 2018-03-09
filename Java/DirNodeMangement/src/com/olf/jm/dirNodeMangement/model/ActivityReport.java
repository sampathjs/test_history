package com.olf.jm.dirNodeMangement.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;
import com.openlink.util.logging.PluginLog;

public class ActivityReport {

	private static String fullFileName = null;

	private static boolean initilised = false;

	public static void start() {
		init();
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		
		writeLine("********************************** Start dir_node purge process " + timestamp + "**********************************");
	}

	public static void finish() {
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		
		writeLine("********************************** End dir_node purge process " + timestamp + " **********************************");

	}
	public static void error(String error) {
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		writeLine(error);
		writeLine("********************************** End dir_node purge process " + timestamp + " **********************************");

	}
	
	public static void recordsToPurge(int numRecords) {
		writeLine("Processing " + numRecords + " records.");
	}
	
	public static void purge( int nodeId, String nodeName, String fileObjectName,  String fileObjectSource) {
		String message = "Purging node id " + nodeId + " name  " + nodeName + " file object name " + fileObjectName + " file object source " + fileObjectSource;
		PluginLog.info(message);
		writeLine(message);
	}

	public static void purge( int dealTrackingNumber, int docId, int nodeId, String nodeName, String fileObjectName,  String fileObjectSource) {
		String message = "Purging deal num " + dealTrackingNumber + " document id " + docId + " node id " + nodeId + " name  " + nodeName + " file object name " + fileObjectName + " file object source " + fileObjectSource;
		PluginLog.info(message);
		writeLine(message);
	}
	
	private static void init() {
		if (!initilised) {
			Session session = Application.getInstance().getCurrentSession();
			String reportingDirectory = session.getIOFactory()
					.getReportDirectory();
			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

			String fileName = "dirNodePurge_"
					+ format.format(session.getServerTime()) + ".log";

			fullFileName = reportingDirectory + "\\" + fileName;

			initilised = true;
		}
	}

	private static void writeLine(String line) {
		PrintWriter printWritter = null;

		try {

			File myFile = new File(fullFileName);
			// check if file exist, otherwise create the file before writing
			if (!myFile.exists()) {
				myFile.createNewFile();
			}
			Writer writer = new FileWriter(myFile, true);
			printWritter = new PrintWriter(writer);
				
			printWritter.println(line);
			
		} catch (IOException e) {
			throw new RuntimeException("Error writing to activity log." + e.getMessage());
		} finally {
			try {
				if (printWritter != null)
					printWritter.close();
			} catch (Exception ex) {
				throw new RuntimeException("Error writing to activity log." + ex.getMessage());
			}
		}
	}
}

