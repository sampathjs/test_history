--tabletitle BU_LE_relation "Business unit to Legal entity mapping"

select pr.business_unit_id, pr.legal_entity_id, p.long_name, p.short_name from party_relationship pr
JOIN party p ON pr.legal_entity_id = p.party_id and p.party_status =1