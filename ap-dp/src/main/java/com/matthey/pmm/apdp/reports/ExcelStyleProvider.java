package com.matthey.pmm.apdp.reports;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.DateFormatConverter;

import java.util.Locale;

public class ExcelStyleProvider {
    
    static CellStyle columnNameStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        cellStyle.setFont(font);
        
        return cellStyle;
    }
    
    static CellStyle dateStyle(Workbook workbook) {
        String excelFormatPattern = DateFormatConverter.convert(Locale.ENGLISH, "yyyy-MM-dd");
        CellStyle cellStyle = workbook.createCellStyle();
        DataFormat poiFormat = workbook.createDataFormat();
        cellStyle.setDataFormat(poiFormat.getFormat(excelFormatPattern));
        
        return cellStyle;
    }
}
