--tabletitle account_reporting_unit "Account Reporting Unit"
--argument $$ReportingUnit$$ "" "Reporting Unit"
SET ANSI_WARNINGS OFF

SELECT a.account_id, info.info_value as ReportingUnit ,iu.unit_id,conversion.factor
  FROM account a
    LEFT JOIN account_info_type  ait ON (ait.type_name = 'Reporting Unit')
    LEFT JOIN account_info info ON (a.account_id =  info.account_id AND info.info_type_id = ait.type_id)
    LEFT JOIN idx_unit iu ON (iu.unit_label=info.info_value)
	INNER JOIN (
		SELECT uc.src_unit_id,uc.factor,uc.dest_unit_id, iu3.unit_label
		  FROM unit_conversion uc
		    INNER JOIN idx_unit iu2 ON (iu2.unit_id=uc.src_unit_id AND iu2.unit_label='gms')
		    INNER JOIN idx_unit iu3 ON (iu3.unit_id=uc.dest_unit_id)
		UNION ALL
		SELECT uc.unit_id,1,uc.unit_id, uc.unit_label
		  FROM idx_unit uc 
		  WHERE uc.unit_label='gms'
	) conversion ON (conversion.dest_unit_id =  iu.unit_id)