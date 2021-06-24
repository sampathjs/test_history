package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutablePartyTo;
import com.matthey.pmm.toms.transport.PartyTo;

public enum TestParty {
	JM_PLC_LE(20004, "JM PLC", DefaultReference.PARTY_TYPE_INTERNAL_LE, null)
	,JM_PMM_UK_BU(20006, "JM PMM UK - BU", DefaultReference.PARTY_TYPE_INTERNAL_BUNIT, JM_PLC_LE)
	,JM_INC_LE(20002, "JM INC", DefaultReference.PARTY_TYPE_INTERNAL_LE, null)
	,JM_PMM_US_BU(20001, "JM PMM US - BU", DefaultReference.PARTY_TYPE_INTERNAL_BUNIT, JM_INC_LE)
	,JM_PACIFIC_LTD_LE(20003, "JM PACIFIC LTD", DefaultReference.PARTY_TYPE_INTERNAL_LE, null)
	,JM_PMM_HK_BU(20007, "JM PMM HK - BU", DefaultReference.PARTY_TYPE_INTERNAL_BUNIT, JM_PACIFIC_LTD_LE)
	,JM_CN_CATALYST_CO_LE(20756, "JM (CN) CATALYST CO - LE", DefaultReference.PARTY_TYPE_INTERNAL_LE, null)
	,JM_PMM_CN_BU(20755, "JM PMM CN - BU", DefaultReference.PARTY_TYPE_INTERNAL_BUNIT, JM_CN_CATALYST_CO_LE)
	,TANAKA_LE(20012, "TANAKA KIKINZOKU KOGYO KK - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,TANAKA_BU(20011, "TANAKA KIKINZOKU KOGYO KK - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, TANAKA_LE)
	,GOLDEN_BILLION_LE(20018, "GOLDEN BILLION - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,GOLDEN_BILLION_BU(20017, "GOLDEN BILLION - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, GOLDEN_BILLION_LE)
	,HSBC_LE(20014, "HSBC BAMK USA, NA NY BRANCH - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,HSBC_BU(20019, "HSBC USA NEW YORK - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, HSBC_LE)
	,UK_TAX_BU(20021, "UK TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, null) // No LE
	,ANGLO_PLATINUM_LE(20023, "ANGLO PLATINUM MARKETING - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,ANGLO_PLATINUM_BU(20022, "ANGLO PLATINUM MARKETING - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, ANGLO_PLATINUM_LE)
	,SA_TAX_BU(20024, "SA TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, null) // No LE
	,US_TAX_BU(20026, "US TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, null) // No LE
	,BARCLAYS_BANK_LE(20089, "HOLDING BANKS CASH - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,BARCLAYS_BANK_BU(20027, "BARCLAYS BANK PLC, SOUTHAMPTON, UK - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, BARCLAYS_BANK_LE)
	,INEOS_LE(20030, "INEOS MANUFACTURING HULL - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,INEOS_BU(20029, "INEOS TECHNOLOGIES LTD - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, INEOS_LE)
	,JM_PLC_LE_EXT(20039, "JM PLC - LE", DefaultReference.PARTY_TYPE_EXTERNAL_LE, null)
	,JM_SYNGAS_BU(20041, "JM SYNGAS - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, JM_PLC_LE_EXT) // LE -> JM_PLC_LE_EXT
	,JM_REFINING_BU(20042, "JM ROYSTON REFINING - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, JM_PLC_LE_EXT) // LE -> JM_PLC_LE_EXT
	,JM_METAL_JOINING_BU(20043, "JM ROYSTON METAL JOINING - BU", DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT, JM_PLC_LE_EXT) // LE -> JM_PLC_LE_EXT
	;
	
	private final PartyTo party;
	
	private TestParty (int id, String name, DefaultReference partyType, TestParty legalEntity) {
		party = ImmutablePartyTo.builder()
				.id(id)
				.name(name)
				.typeId(partyType.getEntity().id())
				.legalEntity(legalEntity!=null?legalEntity.getEntity().id():0)
				.build();
	}

	public PartyTo getEntity () {
		return party;
	}
	
	public static List<PartyTo> asList () {
		return Arrays.asList(TestParty.values())
				.stream().map(TestParty::getEntity).collect(Collectors.toList());
	}
	
	public static List<PartyTo> asListInternal () {
		return Arrays.asList(TestParty.values())
				.stream().map(TestParty::getEntity).filter(x -> x.typeId() == DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id() 
				                                                || x.typeId() == DefaultReference.PARTY_TYPE_INTERNAL_LE.getEntity().id()).collect(Collectors.toList());
	}
	
	public static List<PartyTo> asListExternal () {
		return Arrays.asList(TestParty.values())
				.stream().map(TestParty::getEntity).filter(x -> x.typeId() == DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity().id() 
				                                                || x.typeId() == DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity().id()).collect(Collectors.toList());
	}
}