package com.jm.util.partyinfoimporter;

import com.olf.openjvs.DBaseTable;
import com.olf.openjvs.IContainerContext;
import com.olf.openjvs.IScript;
import com.olf.openjvs.OException;
import com.olf.openjvs.Table;
import com.olf.openjvs.enums.OLF_RETURN_CODE;
import com.olf.openjvs.enums.SEARCH_ENUM;
import com.olf.openjvs.fnd.RefBase;
import com.openlink.util.misc.TableUtilities;

import java.util.HashSet;
import java.util.Set;

public class PartyInfoImporterScript implements IScript {
    public void execute(IContainerContext context) throws OException {
        Table intable = null;
        Table outtable = null;
        Table importResultTable = null;
        Set<Integer> toRetain = new HashSet<>();
        try {
            intable = getInputTable();
            outtable = Table.tableNew("output table");
            int ret = RefBase.exportParties(intable, outtable);
            if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
                throw new RuntimeException("Error retrieving party table data");
            }
            updatePartyInfoFields(outtable, toRetain);
            removeAllExceptRetainedParties(outtable, toRetain);
            importResultTable = RefBase.importPartyTable(outtable);
            outtable.viewTable();
        } finally {
            intable = TableUtilities.destroy(intable);
            outtable = TableUtilities.destroy(outtable);
            importResultTable = TableUtilities.destroy(importResultTable);
        }
    }

    private void removeAllExceptRetainedParties(Table outtable, Set<Integer> toRetain) throws OException {
        for (int row = outtable.getNumRows(); row > 0; row--) {
            int partyId = outtable.getInt("party_id", row);
            if (!toRetain.contains(partyId)) {
                outtable.delRow(row);
            }
        }
    }

    private void updatePartyInfoField(Table outtable,
                                      String partyName,
                                      String partyInfoField,
                                      String partyInfoFieldValue,
                                      Set<Integer> toRetain) throws OException {
        outtable.sortCol("short_name");
        int rowPartyTable = outtable.findString("short_name", partyName, SEARCH_ENUM.FIRST_IN_GROUP);
        if (rowPartyTable < 1) {
            throw new OException("Could not update party '" +
                                 partyName +
                                 "', party info '" +
                                 partyInfoField +
                                 "' with value '" +
                                 partyInfoFieldValue +
                                 "'" +
                                 " as the party was not found.");
        }
        int partyClass = outtable.getInt("party_class", rowPartyTable);
        int intExt = outtable.getInt("int_ext", rowPartyTable);
        int partyInfoFieldId = getPartyInfoId(partyInfoField, partyClass, intExt, partyName);
        Table partyInfoTable = outtable.getTable("party_info", rowPartyTable);
        partyInfoTable.sortCol("type_id");
        int rowPartyInfo = partyInfoTable.findInt("type_id", partyInfoFieldId, SEARCH_ENUM.FIRST_IN_GROUP);
        if (rowPartyInfo <= 0) {
            rowPartyInfo = partyInfoTable.addRow();
            partyInfoTable.setInt("type_id", rowPartyInfo, partyInfoFieldId);
        }
        partyInfoTable.setString("value", rowPartyInfo, partyInfoFieldValue);
        int partyId = outtable.getInt("party_id", rowPartyTable);
        toRetain.add(partyId);
    }

    private int getPartyInfoId(String partyInfoField, int partyClass, int intExt, String partyName) throws OException {
        String sql = "\nSELECT type_id" +
                     "\nFROM party_info_types" +
                     "\nWHERE type_name = '" +
                     partyInfoField +
                     "'" +
                     "  \nAND party_class = " +
                     partyClass +
                     "  \nAND int_ext = " +
                     intExt;
        Table retTable = null;
        try {
            retTable = Table.tableNew("Party Info ID Retrieval Result");
            int ret = DBaseTable.execISql(retTable, sql);
            if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue() || retTable.getNumRows() == 0) {
                throw new RuntimeException("Error retrieving ID of party info field '" +
                                           partyInfoField +
                                           "' for party '" +
                                           partyName +
                                           "'. Does it exist?");
            }
            return retTable.getInt(1, 1);
        } finally {
            retTable = TableUtilities.destroy(retTable);
        }
    }

    private Table getInputTable() throws OException {
        final String sql = "\nSELECT short_name AS party_name " + "\n,party_class" + "\n,party_id" + "\nFROM party";
        Table inputTable = Table.tableNew("input table");
        int ret = DBaseTable.execISql(inputTable, sql);
        if (ret != OLF_RETURN_CODE.OLF_RETURN_SUCCEED.jvsValue()) {
            throw new RuntimeException("Error executing SQL " + sql);
        }
        return inputTable;
    }

    private void updatePartyInfoFields(Table outtable, Set<Integer> toRetain) throws OException {
        updatePartyInfoField(outtable, "NORTHAM PLATINUM LTD - BU", "Customer Code USD", "35158", toRetain);
        updatePartyInfoField(outtable, "NOVARTIS GRIMSBY LTD - BU", "Customer Code GBP", "40168", toRetain);
        updatePartyInfoField(outtable, "NOVARTIS GRIMSBY LTD - BU", "Customer Code USD", "40150", toRetain);
        updatePartyInfoField(outtable, "NOVARTIS PHARMA AG - BU", "Customer Code EUR", "36919", toRetain);
        updatePartyInfoField(outtable, "NOVARTIS PHARMA AG - BU", "Customer Code USD", "36901", toRetain);
        updatePartyInfoField(outtable, "OEGUSSA GMBH - BU", "Customer Code EUR", "37954", toRetain);
        updatePartyInfoField(outtable, "OEGUSSA GMBH - BU", "Customer Code USD", "37946", toRetain);
        updatePartyInfoField(outtable, "OLEON NV - BU", "Customer Code EUR", "45399", toRetain);
        updatePartyInfoField(outtable, "OLEON NV - BU", "Customer Code USD", "45381", toRetain);
        updatePartyInfoField(outtable, "OREGON LABWARE - BU", "Customer Code USD", "37866", toRetain);
        updatePartyInfoField(outtable, "OXEA CORPORATION - BU", "Customer Code USD", "43449", toRetain);
        updatePartyInfoField(outtable, "OXEA GMBH - BU", "Customer Code EUR", "40862", toRetain);
        updatePartyInfoField(outtable, "OXEA GMBH - BU", "Customer Code GBP", "37698", toRetain);
        updatePartyInfoField(outtable, "OXEA GMBH - BU", "Customer Code USD", "35211", toRetain);
        updatePartyInfoField(outtable, "PAMP SA - BU", "Customer Code GBP", "32133", toRetain);
        updatePartyInfoField(outtable, "PAMP SA - BU", "Customer Code USD", "32096", toRetain);
        updatePartyInfoField(outtable, "PEAK SENSORS LTD - BU", "Customer Code GBP", "36708", toRetain);
        updatePartyInfoField(outtable, "PEAK SENSORS LTD - BU", "Customer Code USD", "36695", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL - BU", "Customer Code EUR", "32168", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL - BU", "Customer Code GBP", "14752", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL - BU", "Customer Code USD", "23085", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL 02 (PM) LTD - BU", "Customer Code EUR", "43351", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL 02 (PM) LTD - BU", "Customer Code GBP", "43246", toRetain);
        updatePartyInfoField(outtable, "PETER HARTNELL 02 (PM) LTD - BU", "Customer Code USD", "43369", toRetain);
        updatePartyInfoField(outtable, "PETROGAL S.A. - BU", "Customer Code USD", "31165", toRetain);
        updatePartyInfoField(outtable, "PFIZER IRELAND - BU", "Customer Code EUR", "39319", toRetain);
        updatePartyInfoField(outtable, "PFIZER IRELAND - BU", "Customer Code GBP", "39335", toRetain);
        updatePartyInfoField(outtable, "PFIZER IRELAND - BU", "Customer Code USD", "39327", toRetain);
        updatePartyInfoField(outtable, "PFIZER PERTH PTY LTD - BU", "Customer Code USD", "36505", toRetain);
        updatePartyInfoField(outtable, "PGM TRADING CORPORATION - BU", "Customer Code USD", "29882", toRetain);
        updatePartyInfoField(outtable, "PGM VECTRA CO BRASIL LTDA - BU", "Customer Code USD", "43326", toRetain);
        updatePartyInfoField(outtable, "PHILLIPS 66 LTD - BU", "Customer Code GBP", "42243", toRetain);
        updatePartyInfoField(outtable, "ACTIVE CHAR PRODUCTS PVT - BU", "Customer Code USD", "40096", toRetain);
        updatePartyInfoField(outtable, "AFRICA ETF ISSUER (RF) LTD - BU", "Customer Code USD", "43300", toRetain);
        updatePartyInfoField(outtable, "AGKEM IMPEX PVT LTD - BU", "Customer Code USD", "44089", toRetain);
        updatePartyInfoField(outtable, "AIRPROTEKT LTD - BU", "Customer Code GBP", "44353", toRetain);
        updatePartyInfoField(outtable, "AIRPROTEKT LTD - BU", "Customer Code USD", "44361", toRetain);
        updatePartyInfoField(outtable, "ALBEMARLE CATALYSTS CO BV - BU", "Customer Code EUR", "33540", toRetain);
        updatePartyInfoField(outtable, "ALBEMARLE CATALYSTS CO BV - BU", "Customer Code GBP", "3181", toRetain);
        updatePartyInfoField(outtable, "ALBEMARLE CATALYSTS CO BV - BU", "Customer Code USD", "3631", toRetain);
        updatePartyInfoField(outtable, "ALEMBIC PHARMACEUTICAL LTD - BU", "Customer Code USD", "27676", toRetain);
        updatePartyInfoField(outtable, "ALEXANDRIA NAT REF & PC CO - BU", "Customer Code USD", "39600", toRetain);
        updatePartyInfoField(outtable, "ALIQUOT GOLD BULLION INC - BU", "Customer Code USD", "43781", toRetain);
        updatePartyInfoField(outtable, "ALIQUOT PRECIOUS METALS INC - BU", "Customer Code USD", "41347", toRetain);
        updatePartyInfoField(outtable, "ALLIANCE INDUSTRIES LTD - BU", "Customer Code USD", "29903", toRetain);
        updatePartyInfoField(outtable, "ALLIED GOLD LTD - BU", "Customer Code GBP", "40029", toRetain);
        updatePartyInfoField(outtable, "ALLIED GOLD LTD - BU", "Customer Code USD", "40011", toRetain);
        updatePartyInfoField(outtable, "AMES GOLDSMITH CORPORATION - BU", "Customer Code EUR", "39773", toRetain);
        updatePartyInfoField(outtable, "HSBC USA NEW YORK - BU", "Customer Code GBP", "35334", toRetain);
        updatePartyInfoField(outtable, "HSBC USA NEW YORK - BU", "Customer Code USD", "35342", toRetain);
        updatePartyInfoField(outtable, "HUBERT CHRISTOPHER MILES - BU", "Customer Code GBP", "35123", toRetain);
        updatePartyInfoField(outtable, "HYUNDAI AND SHELL BASE OIL - BU", "Customer Code USD", "40942", toRetain);
        updatePartyInfoField(outtable, "HYUNDAI CHEMICAL CO. - BU", "Customer Code USD", "42307", toRetain);
        updatePartyInfoField(outtable, "HYUNDAI COSMO PETROCHEM - BU", "Customer Code USD", "43705", toRetain);
        updatePartyInfoField(outtable, "HYUNDAI ENGINEERING CO. LTD - BU", "Customer Code USD", "37196", toRetain);
        updatePartyInfoField(outtable, "HYUNDAI OILBANK CO. LTD - BU", "Customer Code USD", "42163", toRetain);
        updatePartyInfoField(outtable, "ICBC STANDARD BANK PLC - BU", "Customer Code GBP", "2727", toRetain);
        updatePartyInfoField(outtable, "ICBC STANDARD BANK PLC - BU", "Customer Code USD", "3252", toRetain);
        updatePartyInfoField(outtable, "IGR GERMANY GMBH - BU", "Customer Code EUR", "40598", toRetain);
        updatePartyInfoField(outtable, "IGR GERMANY GMBH - BU", "Customer Code USD", "40580", toRetain);
        updatePartyInfoField(outtable, "IMPALA PLATINUM LTD (RAND)- BU", "Customer Code ZAR", "41443", toRetain);
        updatePartyInfoField(outtable, "IMPALA PLATINUM LTD - BU", "Customer Code GBP", "32221", toRetain);
        updatePartyInfoField(outtable, "IMPALA PLATINUM LTD - BU", "Customer Code USD", "27035", toRetain);
        updatePartyInfoField(outtable, "IMPALA REFINING SERVICES - BU", "Customer Code USD", "26585", toRetain);
        updatePartyInfoField(outtable, "INEOS MANUFACTURING HULL - BU", "Customer Code GBP", "41970", toRetain);
        updatePartyInfoField(outtable, "INEOS MANUFACTURING HULL - BU", "Customer Code USD", "41961", toRetain);
        updatePartyInfoField(outtable, "INEOS TECHNOLOGIES LTD - BU", "Customer Code GBP", "29081", toRetain);
        updatePartyInfoField(outtable, "INEOS TECHNOLOGIES LTD - BU", "Customer Code USD", "29090", toRetain);
        updatePartyInfoField(outtable, "INFINIUM PRECIOUS RESOURCES - BU", "Customer Code USD", "42411", toRetain);
        updatePartyInfoField(outtable, "INTERKAT CATALYST GMBH - BU", "Customer Code USD", "44986", toRetain);
        updatePartyInfoField(outtable, "INTERNATIONAL ACETYL COMP - BU", "Customer Code USD", "35051", toRetain);
        updatePartyInfoField(outtable, "INTL FC STONE LTD - BU", "Customer Code GBP", "42606", toRetain);
        updatePartyInfoField(outtable, "INTL FC STONE LTD - BU", "Customer Code USD", "42593", toRetain);
        updatePartyInfoField(outtable, "INVESTMENT BARS BRANDENBERGER - BU", "Customer Code GBP", "11797", toRetain);
        updatePartyInfoField(outtable, "J ARON NEW YORK - BU", "Customer Code GBP", "24731", toRetain);
        updatePartyInfoField(outtable, "J ARON NEW YORK - BU", "Customer Code USD", "3965", toRetain);
        updatePartyInfoField(outtable, "JANE STREET FINANCIAL - BU", "Customer Code GBP", "43553", toRetain);
        updatePartyInfoField(outtable, "JANE STREET FINANCIAL - BU", "Customer Code USD", "43545", toRetain);
        updatePartyInfoField(outtable, "JM AGT HOLLAND - BU", "Customer Code EUR", "35465", toRetain);
        updatePartyInfoField(outtable, "JM AGT HOLLAND - BU", "Customer Code GBP", "37153", toRetain);
        updatePartyInfoField(outtable, "JM AGT HOLLAND - BU", "Customer Code USD", "11455", toRetain);
        updatePartyInfoField(outtable, "JM AGT UK - BU", "Customer Code GBP", "25734", toRetain);
        updatePartyInfoField(outtable, "JM AGT UK - BU", "Customer Code USD", "29461", toRetain);
        updatePartyInfoField(outtable, "JM BRANDENBERGER - BU", "Customer Code EUR", "28821", toRetain);
        updatePartyInfoField(outtable, "JM BRANDENBERGER - BU", "Customer Code GBP", "11797", toRetain);
        updatePartyInfoField(outtable, "JM BRANDENBERGER - BU", "Customer Code USD", "11578", toRetain);
        updatePartyInfoField(outtable, "JM CATALYSTS KOREA - BU", "Customer Code USD", "35289", toRetain);
        updatePartyInfoField(outtable,
                             "JM CHEMICALS INDIA (TALOJA) PVT LTD - BU",
                             "Customer Code GBP",
                             "33321",
                             toRetain);
        updatePartyInfoField(outtable,
                             "JM CHEMICALS INDIA (TALOJA) PVT LTD - BU",
                             "Customer Code USD",
                             "33312",
                             toRetain);
        updatePartyInfoField(outtable, "JM CHEMICALS NA - BU", "Customer Code USD", "27465", toRetain);
        updatePartyInfoField(outtable, "JM FUEL CELLS JAPAN - BU", "Customer Code USD", "31907", toRetain);
        updatePartyInfoField(outtable, "JM GROUP UNHEDGED METAL - BU", "Customer Code GBP", "31659", toRetain);
        updatePartyInfoField(outtable, "JM GROUP UNHEDGED METAL - BU", "Customer Code USD", "31641", toRetain);
        updatePartyInfoField(outtable, "JM INDIA P LTD - BU", "Customer Code USD", "36986", toRetain);
        updatePartyInfoField(outtable, "JM INORGANICS SDN MBH - BU", "Customer Code USD", "33056", toRetain);
        updatePartyInfoField(outtable, "JM JAPAN CHEMICALS GK - BU", "Customer Code USD", "29145", toRetain);
        updatePartyInfoField(outtable, "JM JAPAN G.K - BU", "Customer Code GBP", "27297", toRetain);
        updatePartyInfoField(outtable, "JM JAPAN G.K - BU", "Customer Code USD", "27300", toRetain);
        updatePartyInfoField(outtable, "JM JAPAN R & D - BU", "Customer Code USD", "34972", toRetain);
        updatePartyInfoField(outtable, "JM KOREA LTD - BU", "Customer Code USD", "40678", toRetain);
        updatePartyInfoField(outtable, "JM MACEDONIA - BU", "Customer Code EUR", "36353", toRetain);
        updatePartyInfoField(outtable, "JM MACEDONIA - BU", "Customer Code GBP", "36345", toRetain);
        updatePartyInfoField(outtable, "JM MACEDONIA - BU", "Customer Code USD", "36337", toRetain);
        updatePartyInfoField(outtable, "JM MALAYSIA - BU", "Customer Code GBP", "28610", toRetain);
        updatePartyInfoField(outtable, "JM MALAYSIA - BU", "Customer Code USD", "28601", toRetain);
        updatePartyInfoField(outtable, "JM NOBLE METALS AUSTRALIA - BU", "Customer Code GBP", "36994", toRetain);
        updatePartyInfoField(outtable, "JM NOBLE METALS AUSTRALIA - BU", "Customer Code USD", "31974", toRetain);
        updatePartyInfoField(outtable, "JM NOBLE METALS UK - BU", "Customer Code EUR", "28741", toRetain);
        updatePartyInfoField(outtable, "JM NOBLE METALS UK - BU", "Customer Code GBP", "11893", toRetain);
        updatePartyInfoField(outtable, "JM NOBLE METALS UK - BU", "Customer Code USD", "4600", toRetain);
        updatePartyInfoField(outtable, "JM PACIFIC LTD (NM) - BU", "Customer Code USD", "43916", toRetain);
        updatePartyInfoField(outtable, "JM PACIFIC LTD (PCT) - BU", "Customer Code USD", "33865", toRetain);
        updatePartyInfoField(outtable, "JM PCT INDIA - BU", "Customer Code GBP", "32424", toRetain);
        updatePartyInfoField(outtable, "JM PCT INDIA - BU", "Customer Code USD", "32416", toRetain);
        updatePartyInfoField(outtable, "JM PHARMMATERIALSEU - BU", "Customer Code GBP", "45233", toRetain);
        updatePartyInfoField(outtable, "JM PLC FUEL CELLS - BU", "Customer Code EUR", "37701", toRetain);
        updatePartyInfoField(outtable, "JM PLC FUEL CELLS - BU", "Customer Code GBP", "29840", toRetain);
        updatePartyInfoField(outtable, "JM PLC FUEL CELLS - BU", "Customer Code USD", "29858", toRetain);
        updatePartyInfoField(outtable, "JM PMM HK", "Customer Code GBP", "11674", toRetain);
        updatePartyInfoField(outtable, "JM PMM HK", "Customer Code USD", "4634", toRetain);
        updatePartyInfoField(outtable, "JM PMM UK", "Customer Code USD", "14365", toRetain);
        updatePartyInfoField(outtable, "JM PMM US", "Customer Code GBP", "20010", toRetain);
        updatePartyInfoField(outtable, "JM PMM US", "Customer Code USD", "12378", toRetain);
        updatePartyInfoField(outtable, "JM REFINING NA - BU", "Customer Code GBP", "22091", toRetain);
        updatePartyInfoField(outtable, "JM REFINING NA - BU", "Customer Code USD", "22841", toRetain);
        updatePartyInfoField(outtable, "JM RESEARCH CHEMICALS UK - BU", "Customer Code GBP", "29196", toRetain);
        updatePartyInfoField(outtable, "JM RESEARCH CHEMICALS UK - BU", "Customer Code USD", "41187", toRetain);
        updatePartyInfoField(outtable, "JM ROY ECT (RUSSIA) - BU", "Customer Code EUR", "35721", toRetain);
        updatePartyInfoField(outtable, "JM ROY ECT (RUSSIA) - BU", "Customer Code GBP", "35730", toRetain);
        updatePartyInfoField(outtable, "JM ROY ECT (RUSSIA) - BU", "Customer Code USD", "35713", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON AGT UK - BU", "Customer Code EUR", "38172", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON AGT UK - BU", "Customer Code GBP", "38181", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON AGT UK - BU", "Customer Code USD", "38164", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON CHEM PRODUCTS - BU", "Customer Code EUR", "28767", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON CHEM PRODUCTS - BU", "Customer Code GBP", "14779", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON CHEM PRODUCTS - BU", "Customer Code USD", "14859", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON ECT - BU", "Customer Code EUR", "28556", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON ECT - BU", "Customer Code GBP", "11826", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON ECT - BU", "Customer Code USD", "4669", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON EMMERICH PGM PLANT - BU", "Customer Code EUR", "45196", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON EMMERICH PGM PLANT - BU", "Customer Code GBP", "45188", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON EMMERICH PGM PLANT - BU", "Customer Code USD", "45170", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON METAL JOINING - BU", "Customer Code EUR", "28791", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON METAL JOINING - BU", "Customer Code GBP", "14664", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON METAL JOINING - BU", "Customer Code USD", "25101", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON REFINING - BU", "Customer Code EUR", "28759", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON REFINING - BU", "Customer Code GBP", "11885", toRetain);
        updatePartyInfoField(outtable, "JM ROYSTON REFINING - BU", "Customer Code USD", "4597", toRetain);
        updatePartyInfoField(outtable, "JM SONNING RESEARCH - BU", "Customer Code GBP", "11877", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT (RAND ANGLO) - BU", "Customer Code USD", "45559", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT (RAND ANGLO) - BU", "Customer Code ZAR", "45567", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT (RAND) - BU", "Customer Code USD", "38252", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT (RAND) - BU", "Customer Code ZAR", "30921", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT - BU", "Customer Code GBP", "11703", toRetain);
        updatePartyInfoField(outtable, "JM SOUTH AFRICA ECT - BU", "Customer Code USD", "11771", toRetain);
        updatePartyInfoField(outtable, "JM SYNGAS - BU", "Customer Code EUR", "28564", toRetain);
        updatePartyInfoField(outtable, "JM SYNGAS - BU", "Customer Code GBP", "2866", toRetain);
        updatePartyInfoField(outtable, "JM SYNGAS - BU", "Customer Code USD", "3324", toRetain);
        updatePartyInfoField(outtable, "JM TC CHILTON - BU", "Customer Code GBP", "41363", toRetain);
        updatePartyInfoField(outtable, "JM TREASURY - BU", "Customer Code EUR", "39909", toRetain);
        updatePartyInfoField(outtable, "JM TREASURY - BU", "Customer Code GBP", "19254", toRetain);
        updatePartyInfoField(outtable, "JM TREASURY - BU", "Customer Code USD", "18147", toRetain);
        updatePartyInfoField(outtable, "JM TREASURY - BU", "Customer Code ZAR", "46156", toRetain);
        updatePartyInfoField(outtable, "JOHNSON MATTHEY (THAILAND) LTD - BU", "Customer Code GBP", "44724", toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY (ZHANGJIAGANG) PRECIOUS METALS TECHNOLOGY CO LTD - BU",
                             "Customer Code GBP",
                             "44661",
                             toRetain);
        updatePartyInfoField(outtable, "JOHNSON MATTHEY AGT MEXICO - BU", "Customer Code GBP", "44708", toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY APPLIED MATERIALS TECHNOLOGIES - BU",
                             "Customer Code GBP",
                             "44687",
                             toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY COLOUR TECHNOLOGIES KOREA - BU",
                             "Customer Code GBP",
                             "44644",
                             toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY PIEZO PRODUCTS GMBH - BU",
                             "Customer Code GBP",
                             "44601",
                             toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY SHANGHAI CATALYSTS - BU",
                             "Customer Code GBP",
                             "44610",
                             toRetain);
        updatePartyInfoField(outtable,
                             "JOHNSON MATTHEY SHANGHAI CATALYSTS - BU",
                             "Customer Code USD",
                             "35191",
                             toRetain);
        updatePartyInfoField(outtable, "JP MORGAN CHASE BANK - BU", "Customer Code GBP", "25831", toRetain);
        updatePartyInfoField(outtable, "JP MORGAN CHASE BANK - BU", "Customer Code USD", "20693", toRetain);
        updatePartyInfoField(outtable, "K.A. RASMUSSEN AS - BU", "Customer Code USD", "25152", toRetain);
        updatePartyInfoField(outtable, "KLK EMMERICH GMBH - BU", "Customer Code EUR", "40985", toRetain);
        updatePartyInfoField(outtable, "KLK EMMERICH GMBH - BU", "Customer Code USD", "40977", toRetain);
        updatePartyInfoField(outtable, "KRASTSVETMET - BU", "Customer Code USD", "44177", toRetain);
        updatePartyInfoField(outtable, "KROSGLASS S.A. - BU", "Customer Code EUR", "43828", toRetain);
        updatePartyInfoField(outtable, "KROSGLASS S.A. - BU", "Customer Code USD", "43810", toRetain);
        updatePartyInfoField(outtable, "KUUSAKOSKI OY - BU", "Customer Code EUR", "45760", toRetain);
        updatePartyInfoField(outtable, "KUUSAKOSKI OY - BU", "Customer Code USD", "43166", toRetain);
        updatePartyInfoField(outtable, "LEKONGERMESS SIA - BU", "Customer Code USD", "36880", toRetain);
        updatePartyInfoField(outtable, "LPPM - BU", "Customer Code GBP", "43271", toRetain);
        updatePartyInfoField(outtable, "LYONDELL CHEMIE NEDELAND - BU", "Customer Code EUR", "40184", toRetain);
        updatePartyInfoField(outtable, "MAIREC EDELMETALL - BU", "Customer Code EUR", "39124", toRetain);
        updatePartyInfoField(outtable, "MAIREC EDELMETALL - BU", "Customer Code GBP", "39968", toRetain);
        updatePartyInfoField(outtable, "MAIREC EDELMETALL - BU", "Customer Code USD", "39491", toRetain);
        updatePartyInfoField(outtable, "MAP TA PHUT OLEFINS CO., LTD - BU", "Customer Code USD", "45364", toRetain);
        updatePartyInfoField(outtable, "MASTERMELT LTD - BU", "Customer Code EUR", "34999", toRetain);
        updatePartyInfoField(outtable, "MASTERMELT LTD - BU", "Customer Code GBP", "28951", toRetain);
        updatePartyInfoField(outtable, "MASTERMELT LTD - BU", "Customer Code USD", "33566", toRetain);
        updatePartyInfoField(outtable, "MASTERMELT REFINING SERVICES - BU", "Customer Code GBP", "33814", toRetain);
        updatePartyInfoField(outtable, "MASTERMELT REFINING SERVICES - BU", "Customer Code USD", "33822", toRetain);
        updatePartyInfoField(outtable, "MEDIMPEX UK LIMITED - BU", "Customer Code GBP", "31667", toRetain);
        updatePartyInfoField(outtable, "MEDIMPEX UK LIMITED - BU", "Customer Code USD", "27844", toRetain);
        updatePartyInfoField(outtable, "METAL DEPOT ZURICH AG - BU", "Customer Code USD", "42083", toRetain);
        updatePartyInfoField(outtable, "METAL TRADE OVERSEAS AG - BU", "Customer Code EUR", "33751", toRetain);
        updatePartyInfoField(outtable, "METAL TRADE OVERSEAS AG - BU", "Customer Code GBP", "33831", toRetain);
        updatePartyInfoField(outtable, "METAL TRADE OVERSEAS AG - BU", "Customer Code USD", "33742", toRetain);
        updatePartyInfoField(outtable, "METALOR TECHNOLOGIES SA - BU", "Customer Code USD", "35107", toRetain);
        updatePartyInfoField(outtable, "MINISTRY OF DEFENCE - BU", "Customer Code GBP", "44521", toRetain);
        updatePartyInfoField(outtable, "MITSUBISHI CORP RTM JAPAN - BU", "Customer Code USD", "40619", toRetain);
        updatePartyInfoField(outtable, "MITSUBISHI INTL CORP - BU", "Customer Code USD", "28548", toRetain);
        updatePartyInfoField(outtable, "MITSUI & CO LTD JAPAN - BU", "Customer Code USD", "20896", toRetain);
        updatePartyInfoField(outtable, "MM SWITZERLAND AG - BU", "Customer Code EUR", "45479", toRetain);
        updatePartyInfoField(outtable, "MM SWITZERLAND AG - BU", "Customer Code GBP", "45487", toRetain);
        updatePartyInfoField(outtable, "MM SWITZERLAND AG - BU", "Customer Code USD", "45575", toRetain);
        updatePartyInfoField(outtable, "MOMENTIVE PM GMBH - BU", "Customer Code EUR", "41806", toRetain);
        updatePartyInfoField(outtable, "MOMENTIVE PM GMBH - BU", "Customer Code USD", "37583", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY CAPITAL GRP - BU", "Customer Code USD", "22040", toRetain);
        updatePartyInfoField(outtable, "MTSS UK LTD - BU", "Customer Code EUR", "45671", toRetain);
        updatePartyInfoField(outtable, "MTSS UK LTD - BU", "Customer Code GBP", "45663", toRetain);
        updatePartyInfoField(outtable, "MTSS UK LTD - BU", "Customer Code USD", "45655", toRetain);
        updatePartyInfoField(outtable, "NADIR GOLD LLC - BU", "Customer Code USD", "40889", toRetain);
        updatePartyInfoField(outtable, "NAN YA PLASTICS CORPORATION - BU", "Customer Code USD", "42147", toRetain);
        updatePartyInfoField(outtable, "NATIXIS UK - BU", "Customer Code EUR", "39183", toRetain);
        updatePartyInfoField(outtable, "NATIXIS UK - BU", "Customer Code GBP", "39175", toRetain);
        updatePartyInfoField(outtable, "NATIXIS UK - BU", "Customer Code USD", "39167", toRetain);
        updatePartyInfoField(outtable, "NEELAM JEWELS - BU", "Customer Code USD", "42104", toRetain);
        updatePartyInfoField(outtable, "NESTE - BU", "Customer Code EUR", "42622", toRetain);
        updatePartyInfoField(outtable, "NIPRO PHARMAPACKAGING - BU", "Customer Code EUR", "43684", toRetain);
        updatePartyInfoField(outtable, "NIPRO PHARMAPACKAGING - BU", "Customer Code USD", "43676", toRetain);
        updatePartyInfoField(outtable, "NORILSK NICKEL USA INC - BU", "Customer Code USD", "40900", toRetain);
        updatePartyInfoField(outtable, "PHILLIPS 66 LTD - BU", "Customer Code USD", "42235", toRetain);
        updatePartyInfoField(outtable, "PM CHEMISTRY SRL - BU", "Customer Code USD", "40117", toRetain);
        updatePartyInfoField(outtable, "PREEM AB - BU", "Customer Code USD", "34788", toRetain);
        updatePartyInfoField(outtable, "PSW METALS LTD - BU", "Customer Code EUR", "45401", toRetain);
        updatePartyInfoField(outtable, "PSW METALS LTD - BU", "Customer Code GBP", "42471", toRetain);
        updatePartyInfoField(outtable, "PSW METALS LTD - BU", "Customer Code USD", "42489", toRetain);
        updatePartyInfoField(outtable, "PTT GLOBAL CHEMICAL - BU", "Customer Code USD", "41208", toRetain);
        updatePartyInfoField(outtable, "QATAR FUEL ADDITIVES - BU", "Customer Code USD", "35588", toRetain);
        updatePartyInfoField(outtable, "QATAR SHELL GTL LTD - BU", "Customer Code USD", "36177", toRetain);
        updatePartyInfoField(outtable, "RANBAXY LABORATORIES LTD - BU", "Customer Code GBP", "34585", toRetain);
        updatePartyInfoField(outtable, "RANBAXY LABORATORIES LTD - BU", "Customer Code USD", "33267", toRetain);
        updatePartyInfoField(outtable, "RASHTRIYA CHEMICALS AND - BU", "Customer Code USD", "24150", toRetain);
        updatePartyInfoField(outtable, "RAVINDRA HERAEUS PVT. LTD - BU", "Customer Code USD", "39271", toRetain);
        updatePartyInfoField(outtable, "RAYONG OLEFINS CO., LTD - BU", "Customer Code USD", "45348", toRetain);
        updatePartyInfoField(outtable, "RBC TORONTO - BU", "Customer Code GBP", "34403", toRetain);
        updatePartyInfoField(outtable, "RECOM METALLGESELLSCHAFT - BU", "Customer Code EUR", "36599", toRetain);
        updatePartyInfoField(outtable, "RECOM METALLGESELLSCHAFT - BU", "Customer Code USD", "36581", toRetain);
        updatePartyInfoField(outtable, "RECOM RECYCLING GMBH - BU", "Customer Code EUR", "40483", toRetain);
        updatePartyInfoField(outtable, "RECOM RECYCLING GMBH - BU", "Customer Code USD", "40475", toRetain);
        updatePartyInfoField(outtable, "REMONDIS PMR B.V. - BU", "Customer Code EUR", "43570", toRetain);
        updatePartyInfoField(outtable, "REMONDIS PMR B.V. - BU", "Customer Code USD", "44290", toRetain);
        updatePartyInfoField(outtable, "RIDDHI SIDDHI BULLION LTD - BU", "Customer Code USD", "40643", toRetain);
        updatePartyInfoField(outtable, "RJH TRADING LTD - BU", "Customer Code GBP", "39731", toRetain);
        updatePartyInfoField(outtable, "RJH TRADING LTD - BU", "Customer Code USD", "39722", toRetain);
        updatePartyInfoField(outtable, "ROBERT BOSCH GMBH - BU", "Customer Code EUR", "37321", toRetain);
        updatePartyInfoField(outtable, "ROBERT BOSCH GMBH - BU", "Customer Code USD", "37313", toRetain);
        updatePartyInfoField(outtable, "RONCA HOLDING B.V. - BU", "Customer Code EUR", "44329", toRetain);
        updatePartyInfoField(outtable, "RONCA HOLDING B.V. - BU", "Customer Code GBP", "44311", toRetain);
        updatePartyInfoField(outtable, "RONCA HOLDING B.V. - BU", "Customer Code USD", "44337", toRetain);
        updatePartyInfoField(outtable,
                             "ROYAL BANK OF CANADA, CAPITAL MARKETS - BU",
                             "Customer Code GBP",
                             "45874",
                             toRetain);
        updatePartyInfoField(outtable,
                             "ROYAL BANK OF CANADA, CAPITAL MARKETS - BU",
                             "Customer Code USD",
                             "45866",
                             toRetain);
        updatePartyInfoField(outtable, "ROYAL CHEM & STONE - BU", "Customer Code USD", "41128", toRetain);
        updatePartyInfoField(outtable, "SABIC UK PETROCHEMICALS LTD - BU", "Customer Code GBP", "30470", toRetain);
        updatePartyInfoField(outtable, "SABIC UK PETROCHEMICALS LTD - BU", "Customer Code USD", "30488", toRetain);
        updatePartyInfoField(outtable, "SABIN METAL CORP - BU", "Customer Code GBP", "27756", toRetain);
        updatePartyInfoField(outtable, "SABIN METAL CORP - BU", "Customer Code USD", "27748", toRetain);
        updatePartyInfoField(outtable, "SADARA CHEMICAL COMPANY - BU", "Customer Code USD", "42278", toRetain);
        updatePartyInfoField(outtable, "SAFIMET SPA - BU", "Customer Code EUR", "44468", toRetain);
        updatePartyInfoField(outtable, "SAFINA - BU", "Customer Code EUR", "45321", toRetain);
        updatePartyInfoField(outtable, "SAFINA - BU", "Customer Code USD", "23536", toRetain);
        updatePartyInfoField(outtable, "SAINT GOBAIN ACHATS - BU", "Customer Code EUR", "38851", toRetain);
        updatePartyInfoField(outtable, "SAINT GOBAIN ACHATS - BU", "Customer Code USD", "18438", toRetain);
        updatePartyInfoField(outtable, "SAINT GOBAIN INDIA", "Customer Code EUR", "45102", toRetain);
        updatePartyInfoField(outtable, "SAINT GOBAIN INDIA", "Customer Code USD", "45090", toRetain);
        updatePartyInfoField(outtable, "SAMSUNG ENGINEERING CO LTD - BU", "Customer Code USD", "39044", toRetain);
        updatePartyInfoField(outtable, "SARGI LTD - BU", "Customer Code GBP", "37452", toRetain);
        updatePartyInfoField(outtable, "SASOL CHEMICALS - BU", "Customer Code USD", "34059", toRetain);
        updatePartyInfoField(outtable, "SASOL CHEVRON HOLDINGS LTD - BU", "Customer Code USD", "42198", toRetain);
        updatePartyInfoField(outtable, "SATYADIVIS PHARMACEUTICAL - BU", "Customer Code USD", "41769", toRetain);
        updatePartyInfoField(outtable, "SAUDI ARAMCO BASE OIL CO - BU", "Customer Code USD", "42438", toRetain);
        updatePartyInfoField(outtable, "SAUDI ARAMCO TOTAL REFINING - BU", "Customer Code USD", "39001", toRetain);
        updatePartyInfoField(outtable, "SAUDI HOLLAND BANK - BU", "Customer Code USD", "39888", toRetain);
        updatePartyInfoField(outtable, "SCHOTT AG - BU", "Customer Code USD", "34497", toRetain);
        updatePartyInfoField(outtable, "SECUNDA SYNFUELS OPERATIONS - BU", "Customer Code USD", "34884", toRetain);
        updatePartyInfoField(outtable, "SEEF LTD - BU", "Customer Code USD", "35932", toRetain);
        updatePartyInfoField(outtable, "SEMPSA JOYERIA PLATERIA - BU", "Customer Code EUR", "34500", toRetain);
        updatePartyInfoField(outtable, "SEMPSA JOYERIA PLATERIA - BU", "Customer Code USD", "3885", toRetain);
        updatePartyInfoField(outtable, "SG CONSTRUCTION PRODS SA - BU", "Customer Code USD", "44003", toRetain);
        updatePartyInfoField(outtable,
                             "SHELL CATALYSTS & TECHNOLOGIES LTD - BU",
                             "Customer Code GBP",
                             "33523",
                             toRetain);
        updatePartyInfoField(outtable, "SHISH JEWELS - BU", "Customer Code USD", "43035", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD - BU", "Customer Code EUR", "EUR TBC", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD - BU", "Customer Code GBP", "GBP TBC", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD - BU", "Customer Code USD", "46033", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD - BU", "Customer Code ZAR", "46041", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD JM PML - BU", "Customer Code EUR", "EUR TBC", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD JM PML - BU", "Customer Code GBP", "GBP TBC", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD JM PML - BU", "Customer Code USD", "46009", toRetain);
        updatePartyInfoField(outtable, "SIBANYE RPM (PTY) LTD JM PML - BU", "Customer Code ZAR", "46017", toRetain);
        updatePartyInfoField(outtable, "SIMS M + R GMBH - BU", "Customer Code EUR", "37170", toRetain);
        updatePartyInfoField(outtable, "SK INNOVATION CO LTD - BU", "Customer Code USD", "41144", toRetain);
        updatePartyInfoField(outtable, "SKANDINAVISKA ENSKILDA BANKEN - BU", "Customer Code USD", "44388", toRetain);
        updatePartyInfoField(outtable, "SOCIETE GENERALE LONDON - BU", "Customer Code EUR", "42905", toRetain);
        updatePartyInfoField(outtable, "SOCIETE GENERALE LONDON - BU", "Customer Code GBP", "42913", toRetain);
        updatePartyInfoField(outtable, "SOCIETE GENERALE LONDON - BU", "Customer Code USD", "31536", toRetain);
        updatePartyInfoField(outtable, "SOCIETE GENERALE NEWEDGE - NYMEX - BU", "Customer Code USD", "42930", toRetain);
        updatePartyInfoField(outtable, "SOJITZ CORPORATION - BU", "Customer Code USD", "35692", toRetain);
        updatePartyInfoField(outtable, "SOLAR APPLIED MATERIALS - BU", "Customer Code USD", "37014", toRetain);
        updatePartyInfoField(outtable, "SOLVAY SA - BU", "Customer Code EUR", "45639", toRetain);
        updatePartyInfoField(outtable, "SOLVAY SA - BU", "Customer Code USD", "45621", toRetain);
        updatePartyInfoField(outtable, "STANDARD BANK OF S AFRICA - BU", "Customer Code USD", "42999", toRetain);
        updatePartyInfoField(outtable, "STANDARD CHARTERED BANK LDN - BU", "Customer Code GBP", "34649", toRetain);
        updatePartyInfoField(outtable, "STANDARD CHARTERED BANK LDN - BU", "Customer Code USD", "34657", toRetain);
        updatePartyInfoField(outtable, "STECHESON METALS LLC - BU", "Customer Code EUR", "36628", toRetain);
        updatePartyInfoField(outtable, "STECHESON METALS LLC - BU", "Customer Code USD", "36610", toRetain);
        updatePartyInfoField(outtable, "SUD CHEMIE INDIA PVT LTD - BU", "Customer Code USD", "41646", toRetain);
        updatePartyInfoField(outtable, "SUMITOMO CORP GLOBAL COMM - BU", "Customer Code GBP", "19668", toRetain);
        updatePartyInfoField(outtable, "SUMITOMO CORP GLOBAL COMM - BU", "Customer Code USD", "3404", toRetain);
        updatePartyInfoField(outtable, "SUN PHARMACEUTICAL IND - BU", "Customer Code GBP", "29815", toRetain);
        updatePartyInfoField(outtable, "SUN PHARMACEUTICAL IND - BU", "Customer Code USD", "29823", toRetain);
        updatePartyInfoField(outtable, "SUNGEEL HIMETAL CO LTD - BU", "Customer Code USD", "43625", toRetain);
        updatePartyInfoField(outtable, "SUREPURE METALS & CHEMICALS - BU", "Customer Code USD", "38463", toRetain);
        updatePartyInfoField(outtable, "SYNGENTA CROP PROTECTION AG - BU", "Customer Code USD", "42737", toRetain);
        updatePartyInfoField(outtable, "T.C.A. SPA - BU", "Customer Code EUR", "36046", toRetain);
        updatePartyInfoField(outtable, "T.C.A. SPA - BU", "Customer Code USD", "36038", toRetain);
        updatePartyInfoField(outtable, "TANAKA KIKINZOKU KOGYO KK - BU", "Customer Code GBP", "20423", toRetain);
        updatePartyInfoField(outtable, "TANAKA KIKINZOKU KOGYO KK - BU", "Customer Code USD", "12124", toRetain);
        updatePartyInfoField(outtable, "TARGET TRADING S.A. - BU", "Customer Code USD", "43385", toRetain);
        updatePartyInfoField(outtable, "TD BANK FINANCIAL GROUP - BU", "Customer Code USD", "37364", toRetain);
        updatePartyInfoField(outtable, "TECHEMET METAL TRADING LLC - BU", "Customer Code USD", "41929", toRetain);
        updatePartyInfoField(outtable, "THE BAHRAIN PETROLEUM COMPANY  - BU", "Customer Code USD", "36821", toRetain);
        updatePartyInfoField(outtable, "THE NATIONAL COMMERCIAL BANK - BU", "Customer Code USD", "40361", toRetain);
        updatePartyInfoField(outtable, "TOYOTA TSUSHO CORPORATION - BU", "Customer Code USD", "34201", toRetain);
        updatePartyInfoField(outtable, "TRANS EUROPEAN LTD - BU", "Customer Code EUR", "45241", toRetain);
        updatePartyInfoField(outtable, "TRANS EUROPEAN LTD - BU", "Customer Code GBP", "45250", toRetain);
        updatePartyInfoField(outtable, "TRANS EUROPEAN LTD - BU", "Customer Code USD", "41291", toRetain);
        updatePartyInfoField(outtable, "TREASURE HOUSE LTD - BU", "Customer Code GBP", "38754", toRetain);
        updatePartyInfoField(outtable, "UAB NOVITERA - BU", "Customer Code EUR", "36943", toRetain);
        updatePartyInfoField(outtable, "UAB NOVITERA - BU", "Customer Code USD", "36935", toRetain);
        updatePartyInfoField(outtable, "UBS AG PM OPS - BU", "Customer Code GBP", "18201", toRetain);
        updatePartyInfoField(outtable, "UBS AG PM OPS - BU", "Customer Code USD", "3949", toRetain);
        updatePartyInfoField(outtable, "UMICORE AG & CO - BU", "Customer Code EUR", "36556", toRetain);
        updatePartyInfoField(outtable, "UMICORE AG & CO - BU", "Customer Code GBP", "12466", toRetain);
        updatePartyInfoField(outtable, "UMICORE AG & CO - BU", "Customer Code USD", "3607", toRetain);
        updatePartyInfoField(outtable, "UMICORE MARKETING SERVICES - BU", "Customer Code EUR", "34091", toRetain);
        updatePartyInfoField(outtable, "UMICORE MARKETING SERVICES - BU", "Customer Code GBP", "34075", toRetain);
        updatePartyInfoField(outtable, "UMICORE MARKETING SERVICES - BU", "Customer Code USD", "34083", toRetain);
        updatePartyInfoField(outtable, "UMICORE PM NJ - LLC - BU", "Customer Code USD", "17697", toRetain);
        updatePartyInfoField(outtable, "UMICORE PRECIOUS METAL REFINING - BU", "Customer Code EUR", "35676", toRetain);
        updatePartyInfoField(outtable, "UMICORE PRECIOUS METAL REFINING - BU", "Customer Code GBP", "24571", toRetain);
        updatePartyInfoField(outtable, "UMICORE PRECIOUS METAL REFINING - BU", "Customer Code USD", "20482", toRetain);
        updatePartyInfoField(outtable, "UOP CH SARL - BU", "Customer Code EUR", "36231", toRetain);
        updatePartyInfoField(outtable, "UOP CH SARL - BU", "Customer Code GBP", "35641", toRetain);
        updatePartyInfoField(outtable, "UOP CH SARL - BU", "Customer Code USD", "35748", toRetain);
        updatePartyInfoField(outtable, "UOP LONDON - BU", "Customer Code GBP", "3084", toRetain);
        updatePartyInfoField(outtable, "UOP LONDON - BU", "Customer Code USD", "3447", toRetain);
        updatePartyInfoField(outtable, "VALE INCO EUROPE LTD - BU", "Customer Code GBP", "2874", toRetain);
        updatePartyInfoField(outtable, "VALE INCO EUROPE LTD - BU", "Customer Code USD", "3332", toRetain);
        updatePartyInfoField(outtable, "WESTERN PLATINUM LTD (RAND) - BU", "Customer Code USD", "41591", toRetain);
        updatePartyInfoField(outtable, "WESTERN PLATINUM LTD (RAND) - BU", "Customer Code ZAR", "41603", toRetain);
        updatePartyInfoField(outtable, "WESTERN PLATINUM LTD - BU", "Customer Code USD", "31034", toRetain);
        updatePartyInfoField(outtable, "WOGEN RESOURCES LTD - BU", "Customer Code GBP", "3105", toRetain);
        updatePartyInfoField(outtable, "WOGEN RESOURCES LTD - BU", "Customer Code USD", "3463", toRetain);
        updatePartyInfoField(outtable, "YARA NORGE AS (NOR) - BU", "Customer Code USD", "35801", toRetain);
        updatePartyInfoField(outtable, "SAFIMET SPA - BU", "Customer Code USD", "44898", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY & CO.INTERNATIONAL PLC", "Customer Code EUR", "46084", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY & CO.INTERNATIONAL PLC", "Customer Code GBP", "46076", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY & CO.INTERNATIONAL PLC", "Customer Code USD", "46068", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY CAPITAL VAT - BU", "Customer Code GBP", "43473", toRetain);
        updatePartyInfoField(outtable, "MORGAN STANLEY CAPITAL VAT - BU", "Customer Code USD", "43465", toRetain);
        updatePartyInfoField(outtable, "AMES GOLDSMITH CORPORATION - BU", "Customer Code USD", "39765", toRetain);
        updatePartyInfoField(outtable, "ANCO CATALYSTS LTD - BU", "Customer Code EUR", "45428", toRetain);
        updatePartyInfoField(outtable, "ANCO CATALYSTS LTD - BU", "Customer Code GBP", "27174", toRetain);
        updatePartyInfoField(outtable, "ANCO CATALYSTS LTD - BU", "Customer Code USD", "29031", toRetain);
        updatePartyInfoField(outtable, "ANGLO PLATINUM MARKETING (RAND) - BU", "Customer Code USD", "45604", toRetain);
        updatePartyInfoField(outtable, "ANGLO PLATINUM MARKETING (RAND) - BU", "Customer Code ZAR", "45591", toRetain);
        updatePartyInfoField(outtable, "ANGLO PLATINUM MARKETING - BU", "Customer Code EUR", "29024", toRetain);
        updatePartyInfoField(outtable, "ANGLO PLATINUM MARKETING - BU", "Customer Code GBP", "29016", toRetain);
        updatePartyInfoField(outtable, "ANGLO PLATINUM MARKETING - BU", "Customer Code USD", "29008", toRetain);
        updatePartyInfoField(outtable, "API RAFFINERIA DI ANCONA SPA", "Customer Code EUR", "44396", toRetain);
        updatePartyInfoField(outtable, "API RAFFINERIA DI ANCONA SPA", "Customer Code USD", "32475", toRetain);
        updatePartyInfoField(outtable, "ARORA MATTHEY LTD - BU", "Customer Code USD", "3682", toRetain);
        updatePartyInfoField(outtable, "AURAMET INTERNATIONAL LLC - BU", "Customer Code USD", "33638", toRetain);
        updatePartyInfoField(outtable, "AUSTRALIA & NZ BANKING GRP - BU", "Customer Code USD", "39829", toRetain);
        updatePartyInfoField(outtable, "AVRA LABORATORIES PVT LTD - BU", "Customer Code USD", "41890", toRetain);
        updatePartyInfoField(outtable, "AXENS - BU", "Customer Code EUR", "39343", toRetain);
        updatePartyInfoField(outtable, "AXENS - BU", "Customer Code GBP", "3130", toRetain);
        updatePartyInfoField(outtable, "AXENS - BU", "Customer Code USD", "24483", toRetain);
        updatePartyInfoField(outtable, "CLARIANT PRODUKTE - BU", "Customer Code EUR", "42609", toRetain);
        updatePartyInfoField(outtable, "CLARIANT PRODUKTE - BU", "Customer Code GBP", "42374", toRetain);
        updatePartyInfoField(outtable, "CLARIANT PRODUKTE - BU", "Customer Code USD", "42366", toRetain);
        updatePartyInfoField(outtable, "COMETOX - BU", "Customer Code EUR", "32951", toRetain);
        updatePartyInfoField(outtable, "COMETOX - BU", "Customer Code USD", "29381", toRetain);
        updatePartyInfoField(outtable, "COMMONWEALTH BANK OF AUSTRALIA - BU", "Customer Code USD", "44804", toRetain);
        updatePartyInfoField(outtable, "COOKSON PM LTD (UK) - BU", "Customer Code EUR", "30285", toRetain);
        updatePartyInfoField(outtable, "COOKSON PM LTD (UK) - BU", "Customer Code GBP", "25179", toRetain);
        updatePartyInfoField(outtable, "COOKSON PM LTD (UK) - BU", "Customer Code USD", "30120", toRetain);
        updatePartyInfoField(outtable, "CORNING PHARMA GLASS - BU", "Customer Code USD", "42956", toRetain);
        updatePartyInfoField(outtable, "BAIRD & CO LTD - BU", "Customer Code GBP", "30832", toRetain);
        updatePartyInfoField(outtable, "BAIRD & CO LTD - BU", "Customer Code USD", "30904", toRetain);
        updatePartyInfoField(outtable, "BANK OF MONTREAL - BU", "Customer Code USD", "45840", toRetain);
        updatePartyInfoField(outtable, "BANK OF NOVA SCOTIA LDN - BU", "Customer Code EUR", "29663", toRetain);
        updatePartyInfoField(outtable, "BANK OF NOVA SCOTIA LDN - BU", "Customer Code GBP", "28151", toRetain);
        updatePartyInfoField(outtable, "BANK OF NOVA SCOTIA LDN - BU", "Customer Code USD", "28142", toRetain);
        updatePartyInfoField(outtable, "BANK OF NOVA SCOTIA NY - BU", "Customer Code GBP", "41849", toRetain);
        updatePartyInfoField(outtable, "BANK OF NOVA SCOTIA NY - BU", "Customer Code USD", "41831", toRetain);
        updatePartyInfoField(outtable, "BARCLAYS CAPITAL - BU", "Customer Code GBP", "23309", toRetain);
        updatePartyInfoField(outtable, "BARCLAYS CAPITAL - BU", "Customer Code USD", "22672", toRetain);
        updatePartyInfoField(outtable, "BASF CORPORATION - BU", "Customer Code USD", "4001", toRetain);
        updatePartyInfoField(outtable, "BASF METALS LTD - BU", "Customer Code EUR", "2815", toRetain);
        updatePartyInfoField(outtable, "BASF METALS LTD - BU", "Customer Code GBP", "2807", toRetain);
        updatePartyInfoField(outtable, "BASF METALS LTD - BU", "Customer Code USD", "3308", toRetain);
        updatePartyInfoField(outtable, "BEN GOW - BU", "Customer Code GBP", "35254", toRetain);
        updatePartyInfoField(outtable, "BIOTECH S.R.L - BU", "Customer Code EUR", "39407", toRetain);
        updatePartyInfoField(outtable, "BIOTECH S.R.L - BU", "Customer Code USD", "39415", toRetain);
        updatePartyInfoField(outtable, "BIRMINGHAM METAL CO LTD - BU", "Customer Code GBP", "32272", toRetain);
        updatePartyInfoField(outtable, "BIRMINGHAM METALS US - BU", "Customer Code USD", "45786", toRetain);
        updatePartyInfoField(outtable, "BM CATALYSTS LTD - BU", "Customer Code EUR", "43975", toRetain);
        updatePartyInfoField(outtable, "BM CATALYSTS LTD - BU", "Customer Code GBP", "43967", toRetain);
        updatePartyInfoField(outtable, "BM CATALYSTS LTD - BU", "Customer Code USD", "43983", toRetain);
        updatePartyInfoField(outtable, "BMW AG PURCHASING - BU", "Customer Code EUR", "43086", toRetain);
        updatePartyInfoField(outtable, "BMW AG PURCHASING - BU", "Customer Code USD", "43078", toRetain);
        updatePartyInfoField(outtable, "BMW AG TREASURY - BU", "Customer Code EUR", "34260", toRetain);
        updatePartyInfoField(outtable, "BMW AG TREASURY - BU", "Customer Code USD", "34251", toRetain);
        updatePartyInfoField(outtable, "BNP PARIBAS LONDON BRANCH - BU", "Customer Code GBP", "41064", toRetain);
        updatePartyInfoField(outtable, "BNP PARIBAS LONDON BRANCH - BU", "Customer Code USD", "37604", toRetain);
        updatePartyInfoField(outtable, "BOREALIS CHIMIE - BU", "Customer Code EUR", "43414", toRetain);
        updatePartyInfoField(outtable, "BORSODCHEM ZRT - BU", "Customer Code USD", "36484", toRetain);
        updatePartyInfoField(outtable, "BRITANNIC STRATEGIES LTD - BU", "Customer Code GBP", "32045", toRetain);
        updatePartyInfoField(outtable, "BRITANNIC STRATEGIES LTD - BU", "Customer Code USD", "32053", toRetain);
        updatePartyInfoField(outtable, "BRUCE METALS REFINING LTD - BU", "Customer Code GBP", "21776", toRetain);
        updatePartyInfoField(outtable, "CALIMARA MATTERS LLP - BU", "Customer Code USD", "45903", toRetain);
        updatePartyInfoField(outtable, "CENNABRAS IND LTDA - BU", "Customer Code USD", "38914", toRetain);
        updatePartyInfoField(outtable, "CF FERTILISERS UK LTD - BU", "Customer Code GBP", "36759", toRetain);
        updatePartyInfoField(outtable, "CF FERTILISERS UK LTD - BU", "Customer Code USD", "36741", toRetain);
        updatePartyInfoField(outtable, "CHIMET S.P.A. - BU", "Customer Code GBP", "30390", toRetain);
        updatePartyInfoField(outtable, "CHIMET S.P.A. - BU", "Customer Code USD", "26534", toRetain);
        updatePartyInfoField(outtable, "CIBC WORLD MARKETS - BU", "Customer Code USD", "24213", toRetain);
        updatePartyInfoField(outtable, "CIPAN S.A. - BU", "Customer Code EUR", "45823", toRetain);
        updatePartyInfoField(outtable, "CIPAN S.A. - BU", "Customer Code USD", "45815", toRetain);
        updatePartyInfoField(outtable, "CITIBANK N.A. - BU", "Customer Code GBP", "38826", toRetain);
        updatePartyInfoField(outtable, "CITIBANK N.A. - BU", "Customer Code USD", "38834", toRetain);
        updatePartyInfoField(outtable, "CITIBANK N.A. LONDON - BU", "Customer Code EUR", "40230", toRetain);
        updatePartyInfoField(outtable, "CITIBANK N.A. LONDON - BU", "Customer Code GBP", "40221", toRetain);
        updatePartyInfoField(outtable, "CITIBANK N.A. LONDON - BU", "Customer Code USD", "40213", toRetain);
        updatePartyInfoField(outtable, "CJ CHAMBERS HISPANIA SL - BU", "Customer Code EUR", "32301", toRetain);
        updatePartyInfoField(outtable, "CLARIANT CATALYST JAPAN KK - BU", "Customer Code USD", "43764", toRetain);
        updatePartyInfoField(outtable, "CLARIANT PRODOTTI ITALIA - BU", "Customer Code EUR", "35393", toRetain);
        updatePartyInfoField(outtable, "CLARIANT PRODOTTI ITALIA - BU", "Customer Code USD", "16627", toRetain);
        updatePartyInfoField(outtable, "CPIC ABAHSAIN FIBERGLASS - BU", "Customer Code USD", "36011", toRetain);
        updatePartyInfoField(outtable, "CPIC BRASIL FIBRAS DE VIDRO LTDA - BU", "Customer Code USD", "45508", toRetain);
        updatePartyInfoField(outtable, "CREASY DISCRETIONARY TRUST - BU", "Customer Code USD", "43342", toRetain);
        updatePartyInfoField(outtable, "CREDIT SUISSE ZURICH - BU", "Customer Code USD", "12538", toRetain);
        updatePartyInfoField(outtable, "CRYSTALOX LIMITED - BU", "Customer Code GBP", "45313", toRetain);
        updatePartyInfoField(outtable, "CUMMINS LTD - BU", "Customer Code EUR", "42526", toRetain);
        updatePartyInfoField(outtable, "CUMMINS LTD - BU", "Customer Code GBP", "42534", toRetain);
        updatePartyInfoField(outtable, "DAIMLER AG CARS - BU", "Customer Code USD", "39458", toRetain);
        updatePartyInfoField(outtable, "DAIMLER AG TRUCKS - BU", "Customer Code USD", "40133", toRetain);
        updatePartyInfoField(outtable, "DAKRAM MATERIALS LTD - BU", "Customer Code GBP", "36661", toRetain);
        updatePartyInfoField(outtable, "DAKRAM MATERIALS LTD - BU", "Customer Code USD", "37330", toRetain);
        updatePartyInfoField(outtable, "DANSK AEDELMETAL AS - BU", "Customer Code EUR", "43609", toRetain);
        updatePartyInfoField(outtable, "DANSK AEDELMETAL AS - BU", "Customer Code USD", "43596", toRetain);
        updatePartyInfoField(outtable, "DARTON COMMODITIES LTD - BU", "Customer Code GBP", "35895", toRetain);
        updatePartyInfoField(outtable, "DARTON COMMODITIES LTD - BU", "Customer Code USD", "35908", toRetain);
        updatePartyInfoField(outtable, "DB PHYSICAL RHODIUM EUR ETC - BU", "Customer Code EUR", "38421", toRetain);
        updatePartyInfoField(outtable, "DB PHYSICAL RHODIUM EUR ETC - BU", "Customer Code GBP", "38404", toRetain);
        updatePartyInfoField(outtable, "DB PHYSICAL RHODIUM EUR ETC - BU", "Customer Code USD", "38412", toRetain);
        updatePartyInfoField(outtable, "DE NORA DEUTSCHLAND GMBH - BU", "Customer Code EUR", "36724", toRetain);
        updatePartyInfoField(outtable, "DE NORA DEUTSCHLAND GMBH - BU", "Customer Code USD", "34948", toRetain);
        updatePartyInfoField(outtable, "DE NORA ITALY - BU", "Customer Code EUR", "37911", toRetain);
        updatePartyInfoField(outtable, "DE NORA ITALY - BU", "Customer Code USD", "37903", toRetain);
        updatePartyInfoField(outtable, "DEGUSSA SONNE-MOND GOLD - BU", "Customer Code EUR", "41558", toRetain);
        updatePartyInfoField(outtable, "DEGUSSA SONNE-MOND GOLD - BU", "Customer Code USD", "41540", toRetain);
        updatePartyInfoField(outtable, "DEUTSCHE BANK AG - BU", "Customer Code GBP", "3025", toRetain);
        updatePartyInfoField(outtable, "DEUTSCHE BANK AG - BU", "Customer Code USD", "18420", toRetain);
        updatePartyInfoField(outtable, "DIVIS LABORATORIES LTD - BU", "Customer Code USD", "40408", toRetain);
        updatePartyInfoField(outtable, "DOW CHEMICAL IBERICA - BU", "Customer Code EUR", "43650", toRetain);
        updatePartyInfoField(outtable, "DOW CHEMICAL IBERICA - BU", "Customer Code USD", "43641", toRetain);
        updatePartyInfoField(outtable, "DOWA METALS & MINING - BU", "Customer Code USD", "41996", toRetain);
        updatePartyInfoField(outtable, "DSM NUTRITIONAL PRODUCTS AG - BU", "Customer Code USD", "45751", toRetain);
        updatePartyInfoField(outtable, "EASTERN INDUSTRIAL CO - BU", "Customer Code USD", "40504", toRetain);
        updatePartyInfoField(outtable, "ECO MASTERMELT PTE LTD - BU", "Customer Code USD", "45217", toRetain);
        updatePartyInfoField(outtable, "ELEGANT COLLECTION - BU", "Customer Code USD", "41662", toRetain);
        updatePartyInfoField(outtable, "EPCOTRADE INTERNATIONAL - BU", "Customer Code USD", "35086", toRetain);
        updatePartyInfoField(outtable, "EVANS CHEM INDIA PVT LTD - BU", "Customer Code USD", "40740", toRetain);
        updatePartyInfoField(outtable, "EVONIK CATALYSTS INDIA - BU", "Customer Code USD", "43211", toRetain);
        updatePartyInfoField(outtable, "EVONIK NUTRITION & CARE - BU", "Customer Code USD", "44126", toRetain);
        updatePartyInfoField(outtable,
                             "EVONIK PERFORMANCE MATERIALS GMBH - BU",
                             "Customer Code EUR",
                             "44783",
                             toRetain);
        updatePartyInfoField(outtable, "EVONIK RESOURCE EFFICENCY - BU", "Customer Code EUR", "42833", toRetain);
        updatePartyInfoField(outtable, "EVONIK RESOURCE EFFICENCY - BU", "Customer Code GBP", "42825", toRetain);
        updatePartyInfoField(outtable, "EVONIK RESOURCE EFFICENCY - BU", "Customer Code USD", "42817", toRetain);
        updatePartyInfoField(outtable,
                             "EXXONMOBIL CATALYST SERVICES INC - BU UK",
                             "Customer Code USD",
                             "17161",
                             toRetain);
        updatePartyInfoField(outtable, "FAGGI ENRICO SPA - BU", "Customer Code EUR", "45701", toRetain);
        updatePartyInfoField(outtable, "FAGGI ENRICO SPA - BU", "Customer Code USD", "45698", toRetain);
        updatePartyInfoField(outtable, "FORMOSA CHEM & FIBRE CORP - BU", "Customer Code USD", "42121", toRetain);
        updatePartyInfoField(outtable, "FURUYA METAL CO. LTD - BU", "Customer Code USD", "42577", toRetain);
        updatePartyInfoField(outtable, "GAMMA TECH FZE - BU", "Customer Code USD", "43844", toRetain);
        updatePartyInfoField(outtable, "GEBRUDER NAIM RECYCLING - BU", "Customer Code EUR", "45284", toRetain);
        updatePartyInfoField(outtable, "GEBRUDER NAIM RECYCLING - BU", "Customer Code USD", "45276", toRetain);
        updatePartyInfoField(outtable, "GENERAL MOTORS COMPANY - BU", "Customer Code USD", "45014", toRetain);
        updatePartyInfoField(outtable, "GERRARDS (PM) LTD - BU", "Customer Code EUR", "34796", toRetain);
        updatePartyInfoField(outtable, "GERRARDS (PM) LTD - BU", "Customer Code GBP", "25996", toRetain);
        updatePartyInfoField(outtable, "GIMAT S.A.S. - BU", "Customer Code EUR", "33881", toRetain);
        updatePartyInfoField(outtable, "GIMAT S.A.S. - BU", "Customer Code USD", "45719", toRetain);
        updatePartyInfoField(outtable, "GLENCORE AG - BU", "Customer Code GBP", "31931", toRetain);
        updatePartyInfoField(outtable, "GLENCORE AG - BU", "Customer Code USD", "19465", toRetain);
        updatePartyInfoField(outtable, "GLENCORE OPERATIONS SA - BU", "Customer Code USD", "43511", toRetain);
        updatePartyInfoField(outtable,
                             "GM EUROMETALS, INC CORPORATION TRUST CENTER - BU",
                             "Customer Code EUR",
                             "30621",
                             toRetain);
        updatePartyInfoField(outtable,
                             "GM EUROMETALS, INC CORPORATION TRUST CENTER - BU",
                             "Customer Code GBP",
                             "30605",
                             toRetain);
        updatePartyInfoField(outtable,
                             "GM EUROMETALS, INC CORPORATION TRUST CENTER - BU",
                             "Customer Code USD",
                             "30613",
                             toRetain);
        updatePartyInfoField(outtable, "GOLDCORP AUSTRALIA - BU", "Customer Code USD", "18323", toRetain);
        updatePartyInfoField(outtable, "GOLDMAN SACHS INTERNATIONAL - BU", "Customer Code EUR", "41574", toRetain);
        updatePartyInfoField(outtable, "GOLDMAN SACHS INTERNATIONAL - BU", "Customer Code GBP", "16715", toRetain);
        updatePartyInfoField(outtable, "GOLDMAN SACHS INTERNATIONAL - BU", "Customer Code USD", "17048", toRetain);
        updatePartyInfoField(outtable, "GOWAX SRO LTD - BU", "Customer Code EUR", "40281", toRetain);
        updatePartyInfoField(outtable, "GOWAX SRO LTD - BU", "Customer Code USD", "40272", toRetain);
        updatePartyInfoField(outtable, "GRUPA AZOTY ZAKLADY AZOTOWE PUL - BU", "Customer Code EUR", "44417", toRetain);
        updatePartyInfoField(outtable, "GS CALTEX CORPORATION - BU", "Customer Code USD", "41179", toRetain);
        updatePartyInfoField(outtable, "GS ENGINEERING & CONSTRUCTION CO - BU", "Customer Code USD", "45031", toRetain);
        updatePartyInfoField(outtable, "GT COMMODITIES LLC - BU", "Customer Code USD", "39802", toRetain);
        updatePartyInfoField(outtable, "HALDOR TOPSOE AS - BU", "Customer Code USD", "45145", toRetain);
        updatePartyInfoField(outtable, "HANWHA TOTAL PETROCHEMICAL CO.,  - BU", "Customer Code USD", "44880", toRetain);
        updatePartyInfoField(outtable, "HEESUNG PMTECH CORP - BU", "Customer Code USD", "44054", toRetain);
        updatePartyInfoField(outtable, "HELENIC PETROLEUM S.A. - BU", "Customer Code USD", "43932", toRetain);
        updatePartyInfoField(outtable, "HENGYI INDUSTRIES GROUP - BU", "Customer Code USD", "44214", toRetain);
        updatePartyInfoField(outtable, "HENRY YIM (HONG KONG) LTD - BU", "Customer Code USD", "41734", toRetain);
        updatePartyInfoField(outtable, "HENSEL REC - BU", "Customer Code EUR", "39925", toRetain);
        updatePartyInfoField(outtable, "HENSEL REC - BU", "Customer Code GBP", "39917", toRetain);
        updatePartyInfoField(outtable, "HENSEL REC - BU", "Customer Code USD", "35609", toRetain);
        updatePartyInfoField(outtable, "HENSEL RECYCLING (UK) LTD - BU", "Customer Code EUR", "40791", toRetain);
        updatePartyInfoField(outtable, "HENSEL RECYCLING (UK) LTD - BU", "Customer Code GBP", "40782", toRetain);
        updatePartyInfoField(outtable, "HENSEL RECYCLING (UK) LTD - BU", "Customer Code USD", "40774", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METAL PROCESSING - BU", "Customer Code EUR", "38842", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METAL PROCESSING - BU", "Customer Code GBP", "26315", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METAL PROCESSING - BU", "Customer Code USD", "28898", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METALS GERMANY - BU", "Customer Code GBP", "18008", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METALS GERMANY - BU", "Customer Code USD", "3615", toRetain);
        updatePartyInfoField(outtable, "HERAEUS METALS NEW YORK LLC - BU", "Customer Code USD", "4095", toRetain);
        updatePartyInfoField(outtable, "HI-TECH CARBON & CATALYSTS - BU", "Customer Code USD", "41398", toRetain);
        updatePartyInfoField(outtable, "HINDUSTAN PLATINUM DMCC - BU", "Customer Code USD", "44767", toRetain);
        updatePartyInfoField(outtable, "HINDUSTAN PLATINUM PRIVATE - BU", "Customer Code USD", "36151", toRetain);
        updatePartyInfoField(outtable, "HOLDEC MP - BU", "Customer Code GBP", "22277", toRetain);
        updatePartyInfoField(outtable, "HOLDEC MP - BU", "Customer Code USD", "3560", toRetain);
        updatePartyInfoField(outtable, "HONDA TRADING AMERICA CORP - BU", "Customer Code USD", "45532", toRetain);
        updatePartyInfoField(outtable, "HONEYWELL INTERNATIONAL INC - BU", "Customer Code USD", "43861", toRetain);
        updatePartyInfoField(outtable, "HOVIONE FARMACIENCIA S.A - BU", "Customer Code USD", "36290", toRetain);
        updatePartyInfoField(outtable, "HOVIONE LTD - BU", "Customer Code USD", "36311", toRetain);
        updatePartyInfoField(outtable, "HOVIONE PHARMASCIENCE LTD - BU", "Customer Code USD", "37567", toRetain);
        updatePartyInfoField(outtable, "HSBC BANK PLC - BU", "Customer Code EUR", "45410", toRetain);
        updatePartyInfoField(outtable, "HSBC BANK PLC - BU", "Customer Code GBP", "31579", toRetain);
        updatePartyInfoField(outtable, "HSBC BANK PLC - BU", "Customer Code USD", "31561", toRetain);
    }
}
