BEGIN TRANSACTION

IF OBJECT_ID('dbo.USER_jm_metal_ledger_rec_notes' , 'U') IS NOT NULL
         DROP TABLE [dbo].[USER_jm_metal_ledger_rec_notes] 

GO

CREATE TABLE [dbo].[USER_jm_metal_ledger_rec_notes] (
       [deal_num] [int] NULL,
       [break_description] [varchar](255) NOT NULL
) ON [PRIMARY]


grant select, insert, update, delete on [dbo].[USER_jm_metal_ledger_rec_notes] to olf_user

grant select on [dbo].[USER_jm_metal_ledger_rec_notes] to olf_readonly

grant select, insert, update, delete on [dbo].[USER_jm_metal_ledger_rec_notes] to olf_user_manual

GO

CREATE CLUSTERED INDEX [USER_jm_ml_rec_index] ON [dbo].[USER_jm_metal_ledger_rec_notes]
(
       [deal_num] ASC
)
GO

COMMIT; 