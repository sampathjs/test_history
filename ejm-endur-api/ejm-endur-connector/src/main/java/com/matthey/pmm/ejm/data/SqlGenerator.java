package com.matthey.pmm.ejm.data;

import com.olf.openrisk.application.Session;
import com.olf.openrisk.staticdata.EnumReferenceTable;
import com.olf.openrisk.staticdata.StaticDataFactory;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class SqlGenerator {

    private static final Logger logger = LogManager.getLogger(SqlGenerator.class);

    private final Map<String, Object> variables = new HashMap<>();

    public SqlGenerator(Session session) {
        StaticDataFactory staticDataFactory = session.getStaticDataFactory();
        int validated = staticDataFactory.getId(EnumReferenceTable.TransStatus, "Validated");
        int matured = staticDataFactory.getId(EnumReferenceTable.TransStatus, "Matured");
        int commodityToolset = staticDataFactory.getId(EnumReferenceTable.Toolsets, "Commodity");
        int cashSettlement = staticDataFactory.getId(EnumReferenceTable.EventType, "Cash Settlement");
        int fromAccount = staticDataFactory.getId(EnumReferenceTable.DealCommentsType, "From Account");
        int toAccount = staticDataFactory.getId(EnumReferenceTable.DealCommentsType, "To Account");
        int closeout = staticDataFactory.getId(EnumReferenceTable.TransStatus, "Closeout");
        int vostro = staticDataFactory.getId(EnumReferenceTable.AccountType, "Vostro");
        int metalAccount = staticDataFactory.getId(EnumReferenceTable.AccountClass, "Metal Account");
        int cash = staticDataFactory.getId(EnumReferenceTable.Instruments, "CASH");
        int physicalSettlement = staticDataFactory.getId(EnumReferenceTable.SettlementType, "Physical Settlement");
        int cashTransfer = staticDataFactory.getId(EnumReferenceTable.InsSubType, "Cash Transfer");
        int buy = staticDataFactory.getId(EnumReferenceTable.BuySell, "Buy");
        int commPhys = staticDataFactory.getId(EnumReferenceTable.Instruments, "COMM-PHYS");
        int commodityCflowType = staticDataFactory.getId(EnumReferenceTable.CflowType, "Commodity");
        int trading = staticDataFactory.getId(EnumReferenceTable.VolumeType, "Trading");
        int nominated = staticDataFactory.getId(EnumReferenceTable.VolumeType, "Nominated");
        int percent = staticDataFactory.getId(EnumReferenceTable.IdxUnit, "%");

        variables.put("validated", validated);
        variables.put("matured", matured);
        variables.put("commodityToolset", commodityToolset);
        variables.put("cashSettlement", cashSettlement);
        variables.put("fromAccount", fromAccount);
        variables.put("toAccount", toAccount);
        variables.put("closeout", closeout);
        variables.put("vostro", vostro);
        variables.put("metalAccount", metalAccount);
        variables.put("cash", cash);
        variables.put("physicalSettlement", physicalSettlement);
        variables.put("cashTransfer", cashTransfer);
        variables.put("buy", buy);
        variables.put("commPhys", commPhys);
        variables.put("commodityCflowType", commodityCflowType);
        variables.put("trading", trading);
        variables.put("nominated", nominated);
        variables.put("percent", percent);
    }

    public void addVariable(String name, Object value) {
        variables.put(name, value);
    }

    public String genSql(String template) {
        String sql = new StringSubstitutor(variables).replace(template);
        logger.info("sql {} {}", System.lineSeparator(), sql);
        return sql;
    }
}
