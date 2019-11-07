package com.matthey.openlink.stamping;

import java.util.Date;

/**
 * POJO for document tracker.
 * 
 */
public class DocumentTracker {

    private final Integer documentNum;
    private final String slStatus;
    private final String sapStatus;
    private final Integer docStatus;
    private final Integer lastDocStatus;
    private final Integer docVersion;
    private final Integer stlDocHistoryId;
    private final Date lastUpdate;
    private final Integer personnelId;
    private final String personnelName;
    private final String coverage;
    private final String docSentReceived;

    private DocumentTracker(DocumentTrackerBuilder builder) {
        this.documentNum = builder.documentNum;
        this.slStatus = builder.slStatus;
        this.sapStatus = builder.sapStatus;
        this.lastUpdate = builder.lastUpdate;
        this.personnelId = builder.personnelId;
        this.docStatus = builder.docStatus;
        this.lastDocStatus = builder.lastDocStatus;
        this.docVersion = builder.docVersion;
        this.stlDocHistoryId = builder.stlDocHistoryId;
        this.personnelName = builder.personnelName;
        this.coverage = builder.coverage;
        this.docSentReceived = builder.docSentReceived;
    }

    public Integer getDocumentNum() {
        return documentNum;
    }

    public Integer getDocStatus() {
        return docStatus;
    }

    public Integer getLastDocStatus() {
        return lastDocStatus;
    }

    public Integer getDocVersion() {
        return docVersion;
    }

    public Integer getDocHistoryId() {
        return stlDocHistoryId;
    }

    public String getSlStatus() {
        return slStatus;
    }

    public String getSapStatus() {
        return sapStatus;
    }

    public Date getlastUpdate() {
        return lastUpdate;
    }

    public Integer getPersonnelId() {
        return personnelId;
    }
    
    public String getPersonnelName() {
        return personnelName;
    }
    
    public String getCoverage() {
        return coverage;
    }
    
    public String getSentReceived() {
        return docSentReceived;
    }
    
    @Override
    public String toString() {
        return "DocumentTracker [Document Num=" + documentNum + ", Doc Version=" + docVersion + ", Doc Status=" + docStatus + ", Last Doc Status=" + lastDocStatus 
        		+ ", Doc History Id=" + stlDocHistoryId + ", SL Status=" + slStatus + ", Sap Status=" + sapStatus + ", Coverage=" + coverage
        		+ ", Doc Sent or Received=" + docSentReceived + ", Personnel Name=" + personnelName + ", Last Update=" + lastUpdate + "]";
    }

    public static class DocumentTrackerBuilder {
        private final Integer documentNum;
        private final Integer docStatus;
        private final Integer docVersion;
        private Integer lastDocStatus = 0;
        private Integer stlDocHistoryId = 0;
        private String slStatus = "";
        private String sapStatus = "";
        private Date lastUpdate = new Date();
        private Integer personnelId = 0;
        private String personnelName = "";
        private String coverage = "Unknown";
        private String docSentReceived = "Unknown";

        public DocumentTrackerBuilder(Integer documentNum, Integer docStatus, Integer docVersion) throws StampingException {
            super();

            StringBuilder fieldList = new StringBuilder(" ");
            if(documentNum == null || documentNum.equals(0)) {
                fieldList.append("documentNum,");
            }

            if(docStatus == null || docStatus.equals(0)) {
                fieldList.append("docStatus,");
            }

            if(docVersion == null || docVersion.equals(0)) {
                fieldList.append("docVersion,");
            }

            fieldList.deleteCharAt(fieldList.length() - 1);
            if(fieldList.length() > 0) {
                throw new StampingException(String.format("Mandatory fields are missing. %s", fieldList.toString()));
            }

            this.documentNum = documentNum;
            this.docStatus = docStatus;
            this.docVersion = docVersion;
        }

        public DocumentTrackerBuilder LastDocStatus(Integer lastDocStatus) {
            this.lastDocStatus = lastDocStatus;
            return this;
        }

        public DocumentTrackerBuilder DocHistoryId(Integer docHistoryId) {
            this.stlDocHistoryId = docHistoryId;
            return this;
        }

        public DocumentTrackerBuilder LastUpdate(Date lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }

        public DocumentTrackerBuilder PersonnelId(Integer personnelId) {
            this.personnelId = personnelId;
            return this;
        }

        public DocumentTrackerBuilder SlStatus(String slStatus) {
            this.slStatus = slStatus;
            return this;
        }

        public DocumentTrackerBuilder SapStatus(String sapStatus) {
            this.sapStatus = sapStatus;
            return this;
        }
        
        public DocumentTrackerBuilder PersonnelName(String personnelName) {
            this.personnelName = personnelName;
            return this;
        }
        
        public DocumentTrackerBuilder Coverage(String coverage) {
            this.coverage = coverage;
            return this;
        }
        
        public DocumentTrackerBuilder SentReceived(String docSentReceived) {
            this.docSentReceived = docSentReceived;
            return this;
        }
        
        public DocumentTracker build() {
            return new DocumentTracker(this);
        }       
    }
}
