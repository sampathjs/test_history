update user_const_repository 
set string_value = 'JM PNL Record Market Data,AP deals validation and cancellation' 
where 
context = 'Ops' 
and sub_context = 'PostProcessGuard'
and name = 'opsNames'