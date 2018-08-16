--tabletitle BalanceLineId "Balance Line Id"
--argument $$QUERY_RESULT_ID$$ "" "QUERY_RESULT_ID"


SELECT q.query_result AS "balance_line_id"
FROM query_result64_dxr q
WHERE q.unique_id = '$$QUERY_RESULT_ID$$'


--

