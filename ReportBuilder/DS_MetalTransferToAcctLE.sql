--tabletitle metal_transfer_to_acct_le "MetalTransferToAcctLE"
select tran_num as "tran_num", ab_view.value as "bu_name", bu.party_id as "bu_id",  le.short_name as "le_name" , legal_entity_id as "le_id", bu_view.value as "ext_bu_code", le_view.value as "ext_le_code"
from ab_tran_info_view ab_view
join party bu on bu.short_name = value
join party_relationship on bu.party_id = business_unit_id
join party le on le.party_id = legal_entity_id 
left join party_info_view bu_view on bu.party_id = bu_view.party_id and bu_view.type_name = 'Ext Business Unit Code'
left join party_info_view le_view on le.party_id = le_view.party_id and le_view.type_name = 'Ext Legal Entity Code'
where ab_view.type_name = 'To A/C BU'
