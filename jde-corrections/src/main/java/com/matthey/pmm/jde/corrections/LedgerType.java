package com.matthey.pmm.jde.corrections;

public enum LedgerType {
    GL("USER_jm_bt_out_gl", "tran_num"), SL("USER_jm_bt_out_sl", "endur_doc_num");
    
    public final String table;
    public final String runLogColumn;
    
    LedgerType(String table, String runLogColumn) {
        this.table = table;
        this.runLogColumn = runLogColumn;
    }
}
