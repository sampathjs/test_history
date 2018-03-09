WITH summary AS  (
   SELECT pr.*,
      RANK() OVER(PARTITION BY pr.business_unit_id ORDER BY pr.def_legal_flag) AS rank
   FROM party_relationship pr)
SELECT s.business_unit_id, s.legal_entity_id
   FROM summary s
WHERE s.rank=1
--When there are several LEs associated to a BU, picks the BU-LE relationship with def_legal_flag=1