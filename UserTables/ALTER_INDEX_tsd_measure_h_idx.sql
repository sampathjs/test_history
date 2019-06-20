DROP index  [tsd_measure_h_idx] ON [dbo].[tsd_measure_h]
GO

CREATE CLUSTERED INDEX [tsd_measure_h_idx] ON [dbo].[tsd_measure_h]
(
       [schedule_id] ASC,
       [tran_version_num] ASC
)WITH (DATA_COMPRESSION=PAGE,FILLFACTOR=100)
ON [PRIMARY]
GO  --5 mins 08
