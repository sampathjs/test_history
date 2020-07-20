package com.openlink.jm.bo.docoutput;

import com.olf.openjvs.DocGen;
import com.olf.openjvs.FileUtil;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.OException;
import com.olf.openjvs.Ref;
import com.olf.openjvs.SystemUtil;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.SHM_USR_TABLES_ENUM;
import com.olf.openjvs.enums.STLDOC_OUTPUT_TYPES_ENUM;

import java.io.File;

import com.olf.jm.logging.Logging;

/* Main logic (outdated!):
 * 
 * If a document is previewed (thru Document Processing > Preview Output):
 * 1. retrieve/generate folder path on FS for storing the created file at this location
 * 2. generate a temporary filename using the desired name and a random id string
 * 3. create the file using core functionality
 * 4. display the file using the associated (OS) application
 * 
 * For real output as usual in business process (eq. thru Output button):
 * 1. generate the final filename, in -maybe- respect of creating a cancellation doc
 * 2. create the file using core functionality at desired location
 * 3. provide parameters for additional actions, eq mailing
 * 4. display the file using the associated (OS) application on 'View'
 */

class DocOutput_DMS extends DocOutput_Base
{
	// @formatter:off

	private final String
	OUTPUT_DESTINATION        = "Openlink Doc Destination",
	OUTPUT_EXPORT_FILE        = "Openlink Doc Export Filename",
	OUTPUT_EXPORT_PATH        = "Openlink Doc Export Path",
	OUTPUT_EXPORT_TYPE        = "Openlink Doc Export Type",
	OUTPUT_STORE_OUTPUT_IN_DB = "Openlink Doc Store Output in DB",
	OUTPUT_TEMPLATE_FILE      = "Openlink Doc Template Filename",
	OUTPUT_TEMPLATE_PATH      = "Openlink Doc Template Path";

	private String
	strOutputDestination     = null,
	strOutputExportFile      = null,
	strOutputExportPath      = null,
	strOutputExportType      = null,
	strOutputStoreOutputInDB = null,
	strOutputTemplateFile    = null,
	strOutputTemplatePath    = null;

	private String
	strOutputTempPath        = null;

	// @formatter:on

	@Override
	protected void process(IContainerContext context) throws OException
	{
		// set common work data
		super.process(context);

		ProcessData processData = new ProcessData(context);
		//if (!"OpenLink Doc".equalsIgnoreCase(processData.getOutputTypeName()))
		if (processData.getOutputType() != STLDOC_OUTPUT_TYPES_ENUM.STLDOC_OUTPUT_TYPE_OPENLINK_DOC)
			return; // script was invoked for another/not-supported Output Type

		@SuppressWarnings("unused")
		int ret;
		if (processData.OutputData.containsKey(OUTPUT_DESTINATION))
		{
			strOutputDestination  = processData.OutputData.getValue(OUTPUT_DESTINATION);
			strOutputTemplatePath = processData.OutputData.getValue(OUTPUT_TEMPLATE_PATH);
			strOutputTemplateFile = processData.OutputData.getValue(OUTPUT_TEMPLATE_FILE);

			strOutputExportFile = processData.OutputData.getValue(OUTPUT_EXPORT_FILE);
			strOutputExportPath = processData.OutputData.getValue(OUTPUT_EXPORT_PATH);
			strOutputExportType = processData.OutputData.getValue(OUTPUT_EXPORT_TYPE);

			strOutputStoreOutputInDB = processData.OutputData.getValue(OUTPUT_STORE_OUTPUT_IN_DB);

			boolean isExportToView = "View".equalsIgnoreCase(strOutputDestination), 
					isPreview = "View".equals(processData.UserData.getStringValue("View/Process"));
			// fix destination on preview mode
			if (isPreview && !isExportToView)
				strOutputDestination = "View";
			isDocumentPreviewed = isPreview;

			String fileExtension = getFileExtension(strOutputExportType);
			String userSelectedFileExtension = "";
			if (isPreview)
			{
				// adjust filename and path for use as temporary file

				// redirect output path to temp folder
				strOutputExportPath = getOutputTempPath();

				// generate a temp filename using a random id string
				if (strOutputExportFile.indexOf("%") >= 0)
					// get rid off tokens, use template name instead
					strOutputExportFile = strOutputTemplateFile;
				// get rid off any extension, use Export Type specific instead
				int extensionIndex = strOutputExportFile.lastIndexOf('.');
				if (extensionIndex >= 0)
					strOutputExportFile = strOutputExportFile.substring(0, extensionIndex);
				// add random id string and extension
				strOutputExportFile += "_" + createId(6) + fileExtension;
			}
			else
			{
				TokenHandler token = new TokenHandler();
				//token.createDateTimeMap();
				token.setCancellationParams(isCancellationDoc, cancellationDocSuffix);

				// enhance output path only for non-Openlink Docs
				if (Ref.getValue(SHM_USR_TABLES_ENUM.OLFDOC_EXPORT_TYPE_TABLE, strOutputExportType) != 0)
				{
					strOutputExportPath = token.replaceTokens(strOutputExportPath, processData.UserData.getTable(), token.getDateTimeTokenMap(), "Export Path");
					if (!strOutputExportPath.endsWith("\\"))
						strOutputExportPath += "\\";
				}
				else
					if (!strOutputExportPath.endsWith("/"))
						strOutputExportPath += "/";

				// enhance output filename
				userSelectedFileExtension = fileExtension;
				int extensionIndex = strOutputExportFile.lastIndexOf('.');
				if (extensionIndex >= 0)
				{
					if (!fileExtension.toString().equals(strOutputExportFile.substring(extensionIndex).toString()))
						Logging.debug(String.format("Removing extension '%s' - '%s' will be used instead", strOutputExportFile.substring(extensionIndex), fileExtension));
					userSelectedFileExtension = strOutputExportFile.substring(extensionIndex);
					strOutputExportFile = strOutputExportFile.substring(0, extensionIndex);
				}
				if (strOutputExportFile.indexOf("%")<0)
					strOutputExportFile = strOutputExportFile + String.format("_%s_%s", processData.DocumentNum, isCancellationDoc ? cancellationDocSuffix : ""+processData.DocOutputSeqnum);
				else
				{
					if (strOutputExportFile.indexOf("DocNum")<0)
						if (strOutputExportFile.indexOf("OutputSeqNum")<0)
							strOutputExportFile += String.format("_%s_%s", processData.DocumentNum, isCancellationDoc ? cancellationDocSuffix : ""+processData.DocOutputSeqnum);
						else
							strOutputExportFile = strOutputExportFile.replace("%OutputSeqNum%", ""+processData.DocumentNum+"_%OutputSeqNum%");
					else
						if (strOutputExportFile.indexOf("OutputSeqNum")<0)
							strOutputExportFile = strOutputExportFile.replace("Num%", "Num%_"+(isCancellationDoc ? cancellationDocSuffix : ""+processData.DocOutputSeqnum));
					strOutputExportFile = token.replaceTokens(strOutputExportFile, processData.UserData.getTable(), token.getDateTimeTokenMap(), "Export Name");
				}
				strOutputExportFile += fileExtension;
			}

			// fix template path, append path separator
			if (!strOutputTemplatePath.endsWith("/"))
				strOutputTemplatePath += "/";

//			strDmsTempDir = getDmsTempDir();

//			String documentTempPath = strDmsTempDir + finalDocumentName;
//			documentExportPath = documentTempPath;

//			ret = DocGen.generateDocument(strOutputTemplatePath+"/"+strOutputTemplateFile, documentTempPath, xml_data);
		//	ret = DocGen.generateDocument(template_name, output_filename, xml_data, xml_mapping, output_type, keep_document, category, document_type, client_data, copy_path);

			/*
			 * template_name   String  The name of the template to use during generation - can be full path or just the name
			 * output_filename String  The name of the document to be generated - can be full path or just the name
			 * xml_data        String  The actual XML to use during generation of the final document. This XML must be well formed.
			 * xml_mapping     String  (optional) The actual XML to use for clause mapping during generation. If provided, this XML must be well formed.
			 * output_type     int     The enumeration value for the file type to convert the document to (PDF, HTML, etc). Use 0 for the normal .olx format.
			 * keep_document   int     A flag which says whether to keep the original document. This field is only used if a non- .olx output type is chosen.
			 * category        String  (optional) The category which the template and/or document belong to. If not using full paths for the template and document, this field must be provided.
			 * document_type   String  (optional) The document type which the template and/or document belong to. If not using full paths for the template and document, this field must be provided.
			 * client_data     String  (optional) The actual XML client data to be stored in the final document. If provided, this XML must be well formed.
			 * copy_path       String  (optional) The path in the local file system in which to place a copy of the final document. 
			 */

			if (strOutputExportPath.contains(" "))
				strOutputExportPath = strOutputExportPath.replaceAll("\\s", "_");
			if (strOutputExportFile.contains(" "))
				strOutputExportFile = strOutputExportFile.replaceAll("\\s", "_");

			String
				template_name   = strOutputTemplatePath + strOutputTemplateFile,
				output_filename = strOutputExportPath + strOutputExportFile,
				xml_data        = processData.XmlData.getXmlDataString(),
				xml_mapping     = "",
				category        = "",
				document_type   = "",
				client_data     = "",
				copy_path       = "";
			int output_type     = Ref.getValue(SHM_USR_TABLES_ENUM.OLFDOC_EXPORT_TYPE_TABLE, strOutputExportType),
				keep_document   = 0; // false

			if (!isPreview && output_type != 0 && "yes".equalsIgnoreCase(strOutputStoreOutputInDB))
			{
//				keep_document = 1;
			}
			if (isCancellationDoc)
				xml_data = fixXmlDataForCancellation(processData.DocumentNum, xml_data, docStatusCancelled);

			/*
			if (PluginLog.LogLevel.DEBUG.equals(PluginLog.getLogLevel()))
			{
				String xmlFile = PluginLog.getLogDir()+"\\"+strOutputExportFile.replaceAll(fileExtension, ".xml");
				BufferedWriter out = null;
				try
				{
					out = new BufferedWriter(new FileWriter(xmlFile));
					out.write(xml_data);
					out.close();
					Logging.debug("Xml Data written to file: "+xmlFile);
				}
				catch (Exception e)
				{
					Logging.warn("Failed logging Xml Data to file: "+xmlFile);
				}
				finally
				{
					out = null;
				}
			}
			*/
			Logging.debug("Before generating output via DMS");
			ret = DocGen.generateDocument(template_name, output_filename, xml_data, xml_mapping, output_type, keep_document, category, document_type, client_data, copy_path);
			Logging.debug("After generating output via DMS");
			waitForFile(output_filename);

			if ("View".equalsIgnoreCase(strOutputDestination))
			{
				if (output_type == 0) // 'Openlink Doc'
					ret = DocGen.openDocument(output_filename);
				else
					ret = SystemUtil.createProcess(output_filename);
			}
			else if ("Export To File".equalsIgnoreCase(strOutputDestination) ||
					 "Export To Database".equalsIgnoreCase(strOutputDestination))
			{
				// if strOutputStoreOutputInDB = 'yes':
				if (keep_document == 1) // store original file for generated non-.olx in DB
				{
					String strOlxFile = strOutputExportFile.replace(fileExtension, ".olx");
					String strDBpath = strOutputTemplatePath.replace("Templates", "Documents");//.replace("/User", "");
					ret = FileUtil.importFileToDB(strOutputExportPath + strOlxFile, strDBpath);
				}

				documentExportPath = output_filename; //strOutputExportPath + strOutputExportFile;
				isDocumentForMail = Ref.getValue(SHM_USR_TABLES_ENUM.OLFDOC_EXPORT_TYPE_TABLE, strOutputExportType) != 0;

				isDocumentExported = true;
			}
			if ("Export To File".equalsIgnoreCase(strOutputDestination)) {
				File file = new File(output_filename);
				int extensionIndex = output_filename.lastIndexOf('.');
				output_filename = output_filename.substring(0, extensionIndex);  
				String finalFileName = output_filename + userSelectedFileExtension;
				file.renameTo(new File (finalFileName));
			}
		}
	}

	private String fixXmlDataForCancellation(int doc_num, String xml_data, String docStatusCancelled)
	{
		String docStatusField = "olfDocStatus";
		xml_data = fixXmlDataForCancellation(doc_num, xml_data, docStatusCancelled, docStatusField);
		return xml_data;
	}

	private String fixXmlDataForCancellation(final int doc_num, final String xml_data, final String docStatusCancelled, final String docStatusField)
	{
		int indexOf_olfDocStatus = xml_data.indexOf(docStatusField);
		if (indexOf_olfDocStatus < 0)
			return xml_data;

		int indexOf_LT = 0, indexOf_GT;
		StringBuilder builder = new StringBuilder();
		do
		{
			indexOf_GT = xml_data.indexOf(">", indexOf_olfDocStatus);
			builder.append(xml_data.substring(indexOf_LT, indexOf_GT+1));
			builder.append(docStatusCancelled);
			indexOf_LT = xml_data.indexOf("<", indexOf_GT);
			indexOf_olfDocStatus = xml_data.indexOf("<"+docStatusField, indexOf_LT);
		}
		while (indexOf_olfDocStatus > 0);
		builder.append(xml_data.substring(indexOf_LT));

		return builder.toString();
	}

	private final String getFileExtension(String strExportType) throws OException
	{
		// for whatever reason this enum is not known in API
		/*switch (OLFDOC_OUTPUT_TYPE_ENUM.valueOf(strExportType))
		{
			case OLFDOC_OUTPUT_TYPE_CSV:        return ".csv";
			case OLFDOC_OUTPUT_TYPE_HTML:       return ".html";
			case OLFDOC_OUTPUT_TYPE_MS_WORD:    return ".docx";
			case OLFDOC_OUTPUT_TYPE_OLX:        return ".olx";
			case OLFDOC_OUTPUT_TYPE_PDF:        return ".pdf";
			case OLFDOC_OUTPUT_TYPE_PLAIN_TEXT: return ".txt";
			case OLFDOC_OUTPUT_TYPE_TELEX:      return ".txt";
			default: return ""; // should never happen
		}
		*/
		if (strExportType == null || strExportType.trim().length() == 0)
			throw new OException ("Export Type must not be null or empty");

		if (strExportType.equalsIgnoreCase("Openlink Doc")) return ".olx";
		if (strExportType.equalsIgnoreCase("PDF"))          return ".pdf";
		if (strExportType.equalsIgnoreCase("DOCX"))         return ".docx";
		if (strExportType.equalsIgnoreCase("HTML"))         return ".html";
		if (strExportType.equalsIgnoreCase("TELEX"))        return ".txt";
		if (strExportType.equalsIgnoreCase("Plain Text"))   return ".txt";
		if (strExportType.equalsIgnoreCase("CSV"))          return ".csv";

		return ""; // should never happen
	}

	private String getOutputTempPath() throws OException
	{
		/*
		String strOutputTempPath = Util.getEnv("AB_DMS_TEMP_PATH_FOLDER");

		if (strOutputTempPath.trim().length() == 0)
			strOutputTempPath = 
		else
			strOutputTempPath += "\\";

		return strOutputTempPath;
		*/
		if (strOutputTempPath == null || strOutputTempPath.trim().length() == 0)
		{
			try
			{
				strOutputTempPath = Util.getEnv("AB_DMS_TEMP_PATH_FOLDER");
			}
			catch (Throwable t) {}

			if (strOutputTempPath == null || strOutputTempPath.trim().length() == 0)
			{
				Logging.info("Environment variable AB_DMS_TEMP_PATH_FOLDER is not configured... using default temp directory");
				//String test = Util.getEnv("TEMP") + "\\"; // better than ... ?
				strOutputTempPath = System.getProperty("java.io.tmpdir");
			}

			if (!strOutputTempPath.endsWith("\\"))
			{
				strOutputTempPath += "\\";
			}
		}

		return strOutputTempPath;
	}
}
