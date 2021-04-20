IF NOT EXISTS (SELECT * FROM USER_const_repository WHERE context='JDE Corrections' AND name='Start Date' )
BEGIN
   INSERT INTO USER_const_repository (context, sub_context, name, type, string_value) VALUES ('JDE Corrections','', 'Start Date', 2, '2020-10-08')
END