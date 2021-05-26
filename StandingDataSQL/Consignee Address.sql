select p.short_name,p.long_name,pat.address_type_name,pa.addr1,pa.addr2, pa.city,pa.country from party p 
INNER JOIN party_address pa ON p.party_id=pa.party_id 
INNER JOIN party_address_type pat ON pa.address_type=pat.address_type_id 
WHERE pa.country=' ' AND 
pa.address_type= 20003 AND
pa.addr1 NOT LIKE 'Customer Arranged Pick%%' AND
p.short_name NOT LIKE '%%IN-TRANSIT%%' AND
p.party_status=1


