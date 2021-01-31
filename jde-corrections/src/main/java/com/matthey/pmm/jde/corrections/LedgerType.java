package com.matthey.pmm.jde.corrections;

public enum LedgerType {
    GL("USER_jm_bt_out_gl", "deal_num", "GL"),
    SL("USER_jm_bt_out_sl", "endur_doc_num", "SL"),
    SL_CN("USER_jm_bt_out_sl", "deal_num", "SL");
    
    public final String table;
    public final String runLogColumn;
    public final String interfaceMode;
    
    LedgerType(String table, String runLogColumn, String interfaceMode) {
        this.table = table;
        this.runLogColumn = runLogColumn;
        this.interfaceMode = interfaceMode;
    }
}
