package com.olf.jm.pricewebservice.app;

import com.olf.jm.pricewebservice.persistence.XMLHelper;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.jm.logging.Logging;

public class ApplyStylesheetStandalone implements IScript {	
	
	
	@Override
	public void execute(IContainerContext context) throws OException {
		applyXMLTransformationIfNecessary ("\\\\vfiler_odsl\\qsi007file1\\Data\\slsql3_uk_jmtest1\\outdir\\reports\\15Feb24\\NM.csv");
		applyXMLTransformationIfNecessary ("\\\\vfiler_odsl\\qsi007file1\\Data\\slsql3_uk_jmtest1\\outdir\\reports\\15Feb24\\XML.xml");
		applyXMLTransformationIfNecessary ("\\\\vfiler_odsl\\qsi007file1\\Data\\slsql3_uk_jmtest1\\outdir\\reports\\15Feb24\\GENERAL.csv");
	}
	
	private void applyXMLTransformationIfNecessary (String file) throws OException {
		String stylesheet;
		if (XMLHelper.isFileXML(file) && !(stylesheet = XMLHelper.retrieveStylesheetFromXML(file)).isEmpty()) {
			Logging.info ("Processing stylesheet " + stylesheet);
			XMLHelper.applyStylesheet(file, stylesheet);
		}		
	}
}
