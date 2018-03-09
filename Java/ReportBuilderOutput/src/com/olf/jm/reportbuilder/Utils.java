package com.olf.jm.reportbuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class Utils {

    private Utils() {}
    
    /**
     * Substitute variables defined within the string between '$$' characters with actual values.
     * 
     * @param text For substitution
     * @return text with substitution
     */
    public static String substituteValues(String text, boolean substituteForColName, HashMap<String, String> colValues,
            ReportColumns reportCols) {

        StringBuilder newText = new StringBuilder(text);
        
        // Search through the string replacing variables enclosed by '$$' with the last column values.
        int idx1 = 0;
        while ((idx1 = newText.indexOf("$$", idx1)) > -1) {
            idx1 += 2;
            int idx2 = newText.indexOf("$$", idx1);
            // Variable name is found between '$$' characters.
            String varName = newText.substring(idx1, idx2);
            // Use an alternate variable name as the variable may be a reference to a column title.
            String alternateVarName = varName;
            // If the variable is a column title then map to column name.
            if (reportCols.getColName(varName) != null) {
                alternateVarName = reportCols.getColName(varName);
            }
            if (substituteForColName) {
                newText.replace(idx1-2, idx2+2, alternateVarName);
            }
            else if (colValues.containsKey(alternateVarName)) {
                // If variable is found in column values substitute in the text for the column value.
                newText.replace(idx1-2, idx2+2, colValues.get(alternateVarName));
            }
        }
        return newText.toString();
    }
    
    /**
     * Get the xml document as an xml string.
     * 
     * @param doc XML document
     * @return xml string
     * @throws TransformerException
     * @throws IOException
     */
    public static String docAsXml(Document doc) throws TransformerException, IOException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
            return result.getOutputStream().toString();
        }
    }

}
