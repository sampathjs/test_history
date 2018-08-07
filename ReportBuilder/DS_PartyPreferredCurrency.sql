--tabletitle PartyPreferredCurrency  "Party Preferred Currency"

select piv.party_id, ccy.id_number as currency_id, ccy.name, ccy.description
from party_info_view piv
join currency ccy ON (ccy.name = piv.value)
where type_name = 'Preferred Currency'
