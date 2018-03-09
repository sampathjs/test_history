package com.olf.jm.dealdocs.dispatch;

import java.util.HashMap;

import com.olf.embedded.application.EnumScriptCategory;
import com.olf.embedded.application.ScriptCategory;
import com.olf.embedded.generic.AbstractGenericScript;
import com.olf.jm.logging.Logging;
import com.olf.openjvs.OException;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.table.ConstTable;
import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.Table;
import com.olf.openrisk.table.TableRow;

@ScriptCategory({ EnumScriptCategory.Generic })
public class OrderRequestLegCommentsDataSource extends AbstractGenericScript {

    @Override
    public Table execute(Session session, ConstTable argt) {
        try {
        	Logging.init(session, this.getClass(), "DealDocuments", "ReportBuilder");
            Table returnt = process(session, argt);
            return returnt;
        }
        catch (RuntimeException e) {
            Logging.error("Failed", e);
            throw e;
        }
        finally {
            Logging.close();
        }
    }
    
    /**
     * Main processing method.
     * 
     * @param session
     * @param argt
     * @return
     */
    private Table process(Session session, ConstTable argt) {
        try (Table returnt = setupReturnt(session)) {
            // Get the Report Builder mode
            int mode = argt.getInt("ModeFlag", 0);
            // Mode 0 is meta data only, no actual data
            if (mode == 1) {
                int qid = argt.getInt("QueryResultID", 0);
                String qrTableName = argt.getString("QueryResultTable", 0);
                getLegComments(session, returnt, qid, qrTableName);
            }
            return returnt.cloneData();
        }
    }

    /**
     * Get the leg comments from deal comments.
     * 
     * @param returnt
     * @throws OException
     */
    private void getLegComments(Session session, Table returnt, int qid, String qrTableName) {

        HashMap<Integer, HashMap<Integer, StringBuilder>> tranComments = new HashMap<>();
        
        try (Table comments = session.getIOFactory().runSQL(
                "\n SELECT ab.tran_num" +
                "\n      , ab.ins_num" +
                "\n      , np.comment_num" +
                "\n      , np.line_num" +
                "\n   FROM tran_notepad np" +
                "\n   JOIN ab_tran ab ON (ab.tran_num = np.tran_num)" +
                "\n   JOIN " + qrTableName + " qr ON (qr.query_result = np.tran_num)" +
                "\n  WHERE qr.unique_id = " + qid)) {

            // Iterate for each comment
            for (TableRow row : comments.getRows()) {
                int tranNum = row.getInt("tran_num");
                int insNum = row.getInt("ins_num");
                int commentNum = row.getInt("comment_num");

                // Get all the lines for the comment
                try (Table lines = session.getIOFactory().runSQL(
                        "\n SELECT line_num" +
                        "\n      , line_text" +
                        "\n   FROM tran_notepad" +
                        "\n  WHERE tran_num = " + tranNum +
                        "\n    AND comment_num = " + commentNum)) {
                    
                    StringBuilder comment = new StringBuilder();
                    // Join together all the comment lines into a single string
                    lines.sort("line_num");
                    int leg = 1;
                    for (TableRow lineRow : lines.getRows()) {
                        String lineText = lineRow.getString("line_text");
                        if (lineRow.getNumber() == 0) {
                            // Line 1 of the comment will contain the 'Leg%:' text, from this extract the leg number and remove the text
                            if (lineText.startsWith("Leg")) {
                                // Leg number will be after 'Leg' and before ':'
                                try {
                                    int idx = lineText.indexOf(':');
                                    leg = Integer.valueOf(lineText.substring(3, idx).trim());
                                    comment.append(lineText.substring(idx + 1));
                                }
                                catch (Exception e) {
                                    // Didn't find a valid leg number
                                    comment.append(lineText);
                                }
                            }
                            else {
                                comment.append(lineText);
                            }
                        }
                        else {
                            comment.append(lineText);
                        }
                    }

                    HashMap<Integer, StringBuilder> legComments = null;
                    if (tranComments.containsKey(insNum)) {
                       legComments = tranComments.get(insNum);
                    }
                    else {
                        legComments = new HashMap<>();
                        tranComments.put(insNum, legComments);
                    }
                    if (legComments.containsKey(leg)) {
                        legComments.get(leg).append('\n').append(comment);
                    }
                    else {
                        legComments.put(leg, comment);
                    }
                }
            }
        }

        // Add the full comment into returnt
        // Users will see the physical legs as 1,2,3 etc and that's how they will refer to them when entering comments,
        // but they are actually legs 1,3,5 etc. Here the leg number given by the user in the comment is converted so that
        // leg 1 remains as leg 1, leg 2 becomes leg 3, leg 3 becomes leg 5 etc therefore allowing the comments to be joined
        // to the correct physical leg.
        for (int insNum : tranComments.keySet()) {
            HashMap<Integer, StringBuilder> legComments = tranComments.get(insNum);
            for (int leg : legComments.keySet()) {
                returnt.insertRow(0);
                returnt.setInt("ins_num", 0, insNum);
                returnt.setInt("param_seq_num", 0, (leg - 1) + leg); // Leg conversion
                returnt.setString("comment", 0, legComments.get(leg).toString());
            }
        }
    }

    /**
     * Set up returnt table which will be the structure returned to Report Builder.
     * 
     * @param returnt
     */
    private Table setupReturnt(Session session) {
        Table returnt = session.getTableFactory().createTable();
        returnt.addColumn("ins_num", EnumColType.Int);
        returnt.addColumn("param_seq_num", EnumColType.Int);
        returnt.addColumn("comment", EnumColType.String);
        returnt.addRow();
        return returnt;
    }

}
