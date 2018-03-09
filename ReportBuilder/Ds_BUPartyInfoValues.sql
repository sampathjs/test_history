SELECT 
	 pt.party_id 
	,pt.short_name 
	,pt.long_name
	,pt.party_class
	,pt.party_status
	,pfin.type_id
	,pfint.type_name
	,pfin.value  
	
FROM  	party  pt 
	   ,party_info 	    pfin
	   ,party_info_types pfint		
	   
WHERE    pt.party_id  = pfin.party_id
AND      pfin.type_id = pfint.type_id	
AND      pt.party_class = 1  --Only  BU (Internal or External)


