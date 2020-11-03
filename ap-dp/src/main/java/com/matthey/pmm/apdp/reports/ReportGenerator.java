package com.matthey.pmm.apdp.reports;

import ch.qos.logback.classic.Logger;
import com.matthey.pmm.EndurLoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.List;

import static com.matthey.pmm.apdp.reports.ExcelStyleProvider.columnNameStyle;
import static com.matthey.pmm.apdp.reports.ExcelStyleProvider.dateStyle;

public class ReportGenerator {
    
    private static final int START_ROW_IDX = 1;
    final String reportPath;
    
    private final List<AccountBalanceDetails> accountBalanceDetailsList;
    private final List<CustomerDetails> customerDetailsList;
    private final Logger logger = EndurLoggerFactory.getLogger(ReportGenerator.class);
    
    public ReportGenerator(List<AccountBalanceDetails> accountBalanceDetailsList,
                           List<CustomerDetails> customerDetailsList,
                           String reportPath) {
        this.accountBalanceDetailsList = accountBalanceDetailsList;
        this.customerDetailsList = customerDetailsList;
        this.reportPath = reportPath;
    }
    
    public void generate() {
        XSSFWorkbook workbook = new XSSFWorkbook();
        generateCustomerDetailsTab(workbook);
        generateAccountBalanceDetailsTab(workbook);
        
        try (FileOutputStream file = new FileOutputStream(reportPath)) {
            logger.info("writing the Excel report {}", reportPath);
            workbook.write(file);
            workbook.close();
            logger.info("finished the Excel report");
        } catch (IOException e) {
            throw new RuntimeException("error occurred when saving the report: " + e.getMessage(), e);
        }
    }
    
    private void generateCustomerDetailsTab(Workbook workbook) {
        logger.info("generating the tab for customer details");
        Sheet sheet = workbook.createSheet("Customer Details");
        Row columnNames = sheet.createRow(0);
        int columnIdx = 0;
        String[] columns = new String[]{"Customer",
                                        "Total Deferred Toz",
                                        "Total Priced Toz",
                                        "Total Un-priced Toz",
                                        "Total Deferred Value (USD)",
                                        "Total Priced Value (USD)",
                                        "Avg Delivery price per ToZ(USD)",
                                        "Total Un-priced Value(USD)",
                                        "Total Deferred Value (HKD)",
                                        "Total Priced Value (HKD)",
                                        "Total Un-priced Value(HKD)",
                                        "Avg Delivery price per ToZ(HKD)",
                                        "Adjustment Value (USD)",
                                        "Adjustment Value (HKD)"};
        for (String column : columns) {
            Cell cell = columnNames.createCell(columnIdx);
            cell.setCellValue(column);
            cell.setCellStyle(columnNameStyle(sheet.getWorkbook()));
            columnIdx++;
        }
        for (int customerCount = 0; customerCount < customerDetailsList.size(); customerCount++) {
            CustomerDetails customerDetails = customerDetailsList.get(customerCount);
            Row row = sheet.createRow(customerCount + +START_ROW_IDX);
            row.createCell(0).setCellValue(customerDetails.customer());
            row.createCell(1).setCellValue(customerDetails.deferredAmount());
            row.createCell(2).setCellValue(customerDetails.pricedAmount());
            row.createCell(3).setCellValue(customerDetails.unpricedAmount());
            row.createCell(4).setCellValue(customerDetails.deferredValueUsd());
            row.createCell(5).setCellValue(customerDetails.pricedValueUsd());
            row.createCell(6).setCellValue(customerDetails.avgPriceUsd());
            row.createCell(7).setCellValue(customerDetails.unpricedValueUsd());
            row.createCell(8).setCellValue(customerDetails.deferredValueHkd());
            row.createCell(9).setCellValue(customerDetails.pricedValueHkd());
            row.createCell(10).setCellValue(customerDetails.unpricedValueHkd());
            row.createCell(11).setCellValue(customerDetails.avgPriceHkd());
            row.createCell(12).setCellValue(customerDetails.adjustmentValueUsd());
            row.createCell(13).setCellValue(customerDetails.adjustmentValueHkd());
        }
        autoSizeAllColumns(sheet, columns.length);
        logger.info("finished the tab for customer details");
    }
    
    private void autoSizeAllColumns(Sheet sheet, int numOfColumns) {
        for (int columnIdx = 0; columnIdx < numOfColumns; columnIdx++) {
            sheet.autoSizeColumn(columnIdx);
        }
    }
    
    private void generateAccountBalanceDetailsTab(Workbook workbook) {
        logger.info("generating the tab for account balance details");
        Sheet sheet = workbook.createSheet("Account Balance Details");
        Row columnNames = sheet.createRow(0);
        int columnIdx = 0;
        String[] columns = new String[]{"Customer",
                                        "Event Num",
                                        "Event Date",
                                        "Internal Account",
                                        "Currency",
                                        "Settle Amount",
                                        "Actual Amount",
                                        "Settle Difference"};
        for (String column : columns) {
            Cell cell = columnNames.createCell(columnIdx);
            cell.setCellValue(column);
            cell.setCellStyle(columnNameStyle(sheet.getWorkbook()));
            columnIdx++;
        }
        for (int balanceCount = 0; balanceCount < accountBalanceDetailsList.size(); balanceCount++) {
            AccountBalanceDetails accountBalanceDetails = accountBalanceDetailsList.get(balanceCount);
            Row row = sheet.createRow(balanceCount + +START_ROW_IDX);
            row.createCell(0).setCellValue(accountBalanceDetails.customer());
            row.createCell(1).setCellValue(accountBalanceDetails.eventNum());
            
            Cell cell = row.createCell(2);
            cell.setCellValue(Date.valueOf(accountBalanceDetails.eventDate()));
            cell.setCellStyle(dateStyle(row.getSheet().getWorkbook()));
            
            row.createCell(3).setCellValue(accountBalanceDetails.internalAccount());
            row.createCell(4).setCellValue(accountBalanceDetails.metal());
            row.createCell(5).setCellValue(accountBalanceDetails.settleAmount());
            row.createCell(6).setCellValue(accountBalanceDetails.actualAmount());
            row.createCell(7).setCellValue(accountBalanceDetails.settleDifference());
        }
        autoSizeAllColumns(sheet, columns.length);
        sheet.createFreezePane(0, 1);
        logger.info("finished the tab for account balance details");
    }
}
