IF NOT EXISTS (SELECT * FROM USER_const_repository WHERE context='JDE Corrections' AND name='Start Date HK' )
BEGIN
   INSERT INTO USER_const_repository (context, sub_context, name, type, string_value) VALUES ('JDE Corrections','', 'Start Date HK', 2, '2020-10-08')
END

IF NOT EXISTS (SELECT * FROM USER_const_repository WHERE context='JDE Corrections' AND name='Start Date Other Regions' )
BEGIN
   INSERT INTO USER_const_repository (context, sub_context, name, type, string_value) VALUES ('JDE Corrections','', 'Start Date Other Regions', 2, '2020-10-08')
END