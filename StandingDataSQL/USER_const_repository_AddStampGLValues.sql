DELETE FROM [dbo].[USER_const_repository] WHERE [context] = 'BackOffice' AND [sub_context] LIKE 'Stamp%'
GO

INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'lastUpdate', 11, N' ', 0, 0, CAST(0x0000A73000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'logDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\outdir\error_logs', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'logFile', 2, N'SalesLedgerStamping.log', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'logLevel', 2, N'Debug', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'csvOutputDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\Outdir\reports\Stamping', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampSalesLedger', N'csvOutputFile', 2, N'SalesLedgerStamping.csv', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO

INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'savedQuery', 2, N'Cancelled Trades GL', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'tranInfoField', 2, N'General Ledger', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'logDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\outdir\error_logs', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'logFile', 2, N'GeneralLedgerStamping.log', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'logLevel', 2, N'Debug', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'csvOutputDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\Outdir\reports\Stamping', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampGeneralLedger', N'csvOutputFile', 2, N'GeneralLedgerStamping.csv', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO

INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'savedQuery', 2, N'Cancelled Trades ML', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'tranInfoField', 2, N'Metal Ledger', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'logDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\outdir\error_logs', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'logFile', 2, N'MetalLedgerStamping.log', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'logLevel', 2, N'Debug', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'csvOutputDir', 2, N'\\gbromeolfs01d\endur_dev\Dirs\DM2\Outdir\reports\Stamping', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO
INSERT [dbo].[USER_const_repository] ([context], [sub_context], [name], [type], [string_value], [double_value], [int_value], [date_value]) VALUES (N'BackOffice', N'StampMetalLedger', N'csvOutputFile', 2, N'MetalLedgerStamping.csv', 0, 0, CAST(0x0000000000000000 AS DateTime))
GO