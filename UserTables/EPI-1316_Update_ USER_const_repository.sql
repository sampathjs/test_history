Update USER_const_repository
set string_value = 'ivan.fernandes@matthey.com;Arjit.Agrawal@matthey.com;Charles.Badcock@matthey.com;Jacqueline.Robbins@matthey.com'
where context ='EOD'
and sub_context = 'ResetFixings'
and name = 'email_recipients1';

Update USER_const_repository
set string_value = 'Richard.Knott@jmusa.com;Satpal.Panesar@matthey.com;Isha.Sharma@matthey.com;Vivek.Chauhan@matthey.com;Endur_Support'
where context ='EOD'
and sub_context = 'ResetFixings'
and name = 'email_recipients2';