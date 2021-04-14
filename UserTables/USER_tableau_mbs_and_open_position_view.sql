
CREATE VIEW [dbo].[USER_tableau_mbs_and_open_position_view] AS
(
SELECT balance_desc,
	   CONVERT(FLOAT,REPLACE(platinum_actual,',','')) platinum_actual,
	   CONVERT(FLOAT,REPLACE(palladium_actual,',','')) palladium_actual,
	   CONVERT(FLOAT,REPLACE(iridium_actual,',','')) iridium_actual,
	   CONVERT(FLOAT,REPLACE(rhodium_actual,',','')) rhodium_actual,
	   CONVERT(FLOAT,REPLACE(ruthenium_actual,',','')) ruthenium_actual,
	   CONVERT(FLOAT,REPLACE(osmium_actual,',','')) osmium_actual,
	   CONVERT(FLOAT,REPLACE(gold_actual,',','')) gold_actual,
	   CONVERT(FLOAT,REPLACE(silver_actual,',','')) silver_actual,
	   balance_line_id,
	   display_order,
	   last_update,
	   personnel_id
	FROM user_tableau_mbs_combined

UNION

SELECT 'Open Position' balance_desc, ROUND(XPT,2) platinum_actual,
ROUND(XPD,2) palladium_actual, ROUND(XIR,2) iridium_actual,
ROUND(XRH,2) rhodium_actual,
ROUND(XRU,2) ruthenium_actual,
ROUND(XOS,2) osmium_actual,
ROUND(XAU,2) gold_actual,
ROUND(XAG,2) silver_actual,
-100 balance_line_id, 0 display_order, last_update, personnel_id
FROM
(
SELECT CONVERT(float, REPLACE(u.closing_volume_toz,',','')) closing_volume_toz, u.closing_date, u.metal, u.last_update, u.personnel_id
FROM USER_tableau_pmm_global u
) t
PIVOT (
SUM(closing_volume_toz)
FOR metal IN ([XAG],[XAU],[XIR],[XOS],[XPD],[XPT],[XRH],[XRU])
) AS pivot_table
)

GO

GRANT SELECT on user_tableau_mbs_and_open_position_view to olf_readonly

GRANT SELECT,UPDATE,DELETE ON user_tableau_mbs_and_open_position_view to olf_user

GO


