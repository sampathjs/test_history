--tabletitle reporting_conversion "Reporting Conversion"

SELECT uc.src_unit_id,uc.factor,uc.dest_unit_id, iu3.unit_label
  FROM unit_conversion uc
   INNER JOIN idx_unit iu2 ON iu2.unit_id=uc.src_unit_id AND iu2.unit_label='$$SelectionUnit$$'
   INNER JOIN idx_unit iu3 ON iu3.unit_id=uc.dest_unit_id
UNION ALL
SELECT uc.unit_id,1,uc.unit_id, uc.unit_label
  FROM idx_unit uc 
  WHERE uc.unit_label='$$SelectionUnit$$'
