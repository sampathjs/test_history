package com.matthey.pmm.gmm;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.UserTable;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;
import org.apache.commons.text.StringSubstitutor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class ForecastUpdater {
    
    private static final String FORECAST_TABLE = "USER_gmm_forecast";
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Session session;
    
    public ForecastUpdater(Session session) {
        this.session = session;
    }
    
    public Set<Forecast> retrieve() {
        String earliestCreateTime = LocalDateTime.now().minusMonths(1).format(formatter).toString();
        //language=TSQL
        String sqlTemplate = "SELECT * FROM ${FORECAST_TABLE} WHERE create_time > '${earliestCreateTime}'";
        Map<String, String> variables = new HashMap<>();
        variables.put("FORECAST_TABLE", FORECAST_TABLE);
        variables.put("earliestCreateTime", earliestCreateTime);
        String sql = new StringSubstitutor(variables).replace(sqlTemplate);
        try (Table table = session.getIOFactory().runSQL(sql)) {
            Map<ForecastKey, List<TableRow>> groupedRows = table.getRows().stream().collect(groupingBy(this::getKey));
            return groupedRows.entrySet().stream().map(entry -> {
                ForecastKey key = entry.getKey();
                List<Balance> balances = entry.getValue().stream().map(this::toBalance).collect(toList());
                return ImmutableForecast.builder()
                        .group(key.group())
                        .balanceDate(key.balanceDate())
                        .metal(key.metal())
                        .user(key.user())
                        .companyCode(key.companyCode())
                        .unit(key.unit())
                        .deliverable(key.deliverable())
                        .addAllBalances(balances)
                        .comments(key.comments())
                        .createTime(key.createTime())
                        .build();
            }).collect(toSet());
        }
    }
    
    private Balance toBalance(TableRow row) {
        return ImmutableBalance.builder()
                .customer(row.getString("customer"))
                .currentBalance(row.getDouble("current_balance"))
                .inUse(row.getDouble("in_use"))
                .shipmentWindow(row.getInt("shipment_window"))
                .basisOfAssumption(row.getString("basis_of_assumption"))
                .build();
    }
    
    private ForecastKey getKey(TableRow row) {
        return ImmutableForecastKey.builder()
                .group(row.getString("group_name"))
                .balanceDate(row.getString("balance_date"))
                .metal(row.getString("metal"))
                .user(row.getString("user_name"))
                .companyCode(row.getString("company_code"))
                .unit(row.getString("unit"))
                .deliverable(row.getDouble("deliverable"))
                .comments(row.getString("comments"))
                .createTime(row.getString("create_time"))
                .build();
    }
    
    public void save(Forecast forecast) {
        try (UserTable userTable = session.getIOFactory().getUserTable(FORECAST_TABLE);
             Table newRows = userTable.retrieveTable().cloneStructure()) {
            String createTime = LocalDateTime.now().format(formatter).toString();
            for (Balance balance : forecast.balances()) {
                TableRow row = newRows.addRow();
                row.getCell("group_name").setString(forecast.group());
                row.getCell("balance_date").setString(forecast.balanceDate());
                row.getCell("metal").setString(forecast.metal());
                row.getCell("user_name").setString(forecast.user());
                row.getCell("company_code").setString(forecast.companyCode());
                row.getCell("unit").setString(forecast.unit());
                row.getCell("comments").setString(forecast.comments());
                row.getCell("deliverable").setDouble(forecast.deliverable());
                row.getCell("customer").setString(balance.customer());
                row.getCell("current_balance").setDouble(balance.currentBalance());
                // row.getCell("shipment_volume").setDouble(Optional.ofNullable(balance.shipmentVolume()).orElse(Double.valueOf(0)));
                row.getCell("in_use").setDouble(Optional.ofNullable(balance.inUse()).orElse(Double.valueOf(0)));
                row.getCell("shipment_window").setInt(Optional.ofNullable(balance.shipmentWindow()).orElse(0));
                row.getCell("basis_of_assumption").setString(balance.basisOfAssumption());
                
                row.getCell("create_time").setString(createTime);
            }
            userTable.insertRows(newRows);
        }
    }
}
