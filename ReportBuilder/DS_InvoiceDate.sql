select

abe.tran_num,
sd.event_num,
sdi.document_num,
sdit.type_name,
sdi.value

from ab_tran_event abe
inner join stldoc_details sd on(abe.tran_num=sd.tran_num and abe.event_num=sd.event_num)
inner join stldoc_header sh on(sd.document_num=sh.document_num)
inner join stldoc_info sdi on (sh.document_num=sdi.document_num)
inner join stldoc_info_types sdit on (sdi.type_id=sdit.type_id and sdit.type_id=20001)