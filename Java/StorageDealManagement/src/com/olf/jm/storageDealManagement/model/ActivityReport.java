package com.olf.jm.storageDealManagement.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;

import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.Session;

public class ActivityReport {

	private static String fullFileName = null;

	private static boolean initilised = false;

	public static void start() {
		init();
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		
		writeLine("********************************** Start Storage Management Process " + timestamp + "**********************************");
	}

	public static void finish() {
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		
		writeLine("********************************** End Storage Management Process " + timestamp + " **********************************");

	}
	public static void error(String error) {
		Session session = Application.getInstance().getCurrentSession();
		SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

		String timestamp = format.format(session.getServerTime());
		writeLine(error);
		writeLine("********************************** End Storage Management Process " + timestamp + " **********************************");

	}
	
	public static void storageDealToProcess(int numDeals) {
		writeLine("Processing " + numDeals + " storage deals.");
	}
	
	public static void transfer(int originalDealNum, int newDealNum, int batchesMoved, String type, String metal, String location) {
		writeLine("Processed storage deal " + originalDealNum + " metal " + metal + " location " + location + " moved " + batchesMoved + " " + type + " batches to deal " + newDealNum);
	}
	
	private static void init() {
		if (!initilised) {
			Session session = Application.getInstance().getCurrentSession();
			String reportingDirectory = session.getIOFactory()
					.getReportDirectory();
			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");

			String fileName = "StorageDealManagement_"
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
