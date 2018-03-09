package com.olf.jm.payment_report.model;

import java.util.LinkedList;
import java.util.List;

/*
 * History:
 * 2015-05-06	V1.0	jwaechter	- initial version
 */

/**
 * Enum containing the names of the file extensions the DMS can use as output
 * as well as their respective numbers to be passed to the DMS when generating a
 * document.
 * @author jwaechter
 * @version 1.0
 */
public enum DMSOutputType {
	// output_type:
	// 0 = .olx
	// 1 = .pdf
	// 2 = .docx (word)
	// 3 = .html
	// 4 = .txt
	// 5 = .txt
	// 6 = .csv
	// >= 7 = exception
	OLX ("OLX", 0),
	PDF ("PDF", 1),
	DOCX ("DOCX", 2),
	HTML ("HTML", 3),
	TEXT ("TXT", 4),
	CSV ("CSV", 6)
	;
	
	private final String fileExtension;
	private final int outputTypeId;
	
	private DMSOutputType (String fileExtension, int outputTypeId) {
		this.fileExtension = fileExtension;
		this.outputTypeId = outputTypeId;
	}

	public int getOutputTypeId() {
		return outputTypeId;
	}
	
	public boolean matchExtension (String extension) {
		return fileExtension.equalsIgnoreCase(extension.trim());
	}
	
	public String getFileExtension() {
		return fileExtension;
	}

	public static DMSOutputType fromExtension (String extension) {
		List<String>  fileExtensions = new LinkedList<String> ();
		for (DMSOutputType type : DMSOutputType.values()) {
			if (type.matchExtension(extension)) {
				return type;
			}
			fileExtensions.add(type.fileExtension);
		}
		throw new IllegalArgumentException ("The supplied extension " + extension + " is not known. It has to be one of " + fileExtensions);
	}
}
