update USER_utm_menu
set where_clause ='party_class = 1 and party_status=1'
where menu_name= 'jm_group_bu' 
and menu_table = 'party'
and menu_column ='short_name'