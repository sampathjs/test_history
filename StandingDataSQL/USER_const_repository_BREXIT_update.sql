-- LIVE 193.110.154.33, rprp2034

update USER_const_repository set string_value = '193.110.154.33' 
where context = 'Reports' and sub_context = 'EMIR' and name = 'EMIR_IP'

update USER_const_repository set string_value = 'rprp2034' 
where context = 'Reports' and sub_context = 'EMIR' and name = 'EMIR_User'


-- TEST

--update USER_const_repository set string_value = '193.110.154.33' 
--where context = 'Reports' and sub_context = 'EMIR' and name = 'EMIR_IP'


--update USER_const_repository set string_value = 'rfrp2034' 
--where context = 'Reports' and sub_context = 'EMIR' and name = 'EMIR_User'

--update USER_const_repository set string_value = '55222' 
--where context = 'Reports' and sub_context = 'EMIR' and name = 'EMIR_port'