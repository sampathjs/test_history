IF EXISTS (SELECT name FROM sysindexes WHERE name = 'stldoc_header_hist_idx3')
DROP INDEX stldoc_header_hist_idx3 ON [dbo].[stldoc_header_hist]
GO

CREATE NONCLUSTERED INDEX [stldoc_header_hist_idx3]
ON [dbo].[stldoc_header_hist] ([doc_type],[doc_status],[last_update])
INCLUDE ([document_num],[doc_version],[stldoc_hdr_hist_id],[last_doc_status],[personnel_id])
GO
