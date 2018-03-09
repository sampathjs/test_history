package com.matthey.openlink;

import java.awt.print.PrinterJob;
import java.util.Arrays;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Sides;


public class DiscoverPrinters {

	public static void main(String[] args) {

		new DiscoverPrinters().testPrinter/*findPrinters*/();
		
	}

	void findPrinters() {
		DocFlavor flavor = /*DocFlavor.SERVICE_FORMATTED.PAGEABLE;*/
		 DocFlavor.SERVICE_FORMATTED.PRINTABLE;
		PrintRequestAttributeSet patts = new HashPrintRequestAttributeSet();
		//patts.add(Sides.DUPLEX);
		PrintService[] ps = PrintServiceLookup.lookupPrintServices(flavor, patts);
		if (ps.length == 0) {
		    throw new IllegalStateException("No Printer found");
		}
		//PrinterJob job = PrinterJob.getPrinterJob();
		//job.setPrintService(ps[0]);
	}
	
	void runPrintJob() {
//		DocumentAttributeSet datts = new HashDocumentAttributeSet();
//		datts.add(Sides.DUPLEX);
//		PDFParser parser = new PDFParser(pdf);
//		job.setPageable(parser);
//		job.print(datts);
	}
	
	public void testPrinter() {
	    DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
	   		 //DocFlavor.SERVICE_FORMATTED.PRINTABLE;
	    PrintRequestAttributeSet patts = new HashPrintRequestAttributeSet();
	    //patts.add(Sides.DUPLEX);
	    PrintService[] ps = PrintServiceLookup.lookupPrintServices(flavor, patts);
	    if (ps.length == 0) {
	        throw new IllegalStateException("No Printer found");
	    }
	    System.out.println("Available printers: " + Arrays.asList(ps));

	    PrintService myService = null;
	    for (PrintService printService : ps) {
	        if (printService.getName().equals("Your printer name")) {
	            myService = printService;
	            break;
	        }
	    }

	    if (myService == null) {
	        throw new IllegalStateException("Printer not found");
	    }

//	    FileInputStream fis = new FileInputStream("C:/Users/John Doe/Desktop/SamplePDF.pdf");
//	    Doc pdfDoc = new SimpleDoc(fis, DocFlavor.INPUT_STREAM.AUTOSENSE, null);
//	    DocPrintJob printJob = myService.createPrintJob();
//	    printJob.print(pdfDoc, new HashPrintRequestAttributeSet());
//	    fis.close();        
	}
}
