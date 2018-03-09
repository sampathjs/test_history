SELECT DISTINCT 
      tn.tran_num,  
      tn.line_num,
      tn.note_type,
case ativ.value when 'TR' then  (
       Isnull(NULLIF(LEFT(tn.line_text, Charindex (Char(10), tn.line_text)), ''), 
       line_text) ) else '' end AS first_line 
FROM ab_tran ab
  LEFT OUTER  JOIN tran_notepad tn
    ON ab.tran_num = tn.tran_num
  LEFT OUTER JOIN ab_tran_info_view ativ 
   ON tn.tran_num=ativ.tran_num AND ativ.type_name='JM_Transaction_Id' 
WHERE tn.note_type in (20001, 20002) 
  AND tn.comment_num = (SELECT max(tn2.comment_num) FROM tran_notepad tn2 WHERE tn2.tran_num = tn.tran_num AND tn2.note_type = tn.note_type)
--  AND ab.tran_num = 141040
