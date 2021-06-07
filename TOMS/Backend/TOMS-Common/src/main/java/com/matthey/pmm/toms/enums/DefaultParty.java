package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableParty;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultParty {
	JM_PMM_UK(20006, "JM PMM UK - BU", DefaultReference.PARTY_TYPE_INTERNAL.getEntity())
	,JM_PMM_US(20001, "JM PMM US - BU", DefaultReference.PARTY_TYPE_INTERNAL.getEntity())
	,JM_PMM_HK(20007, "JM PMM HK - BU", DefaultReference.PARTY_TYPE_INTERNAL.getEntity())	
	,JM_PMM_CN(20755, "JM PMM CN - BU", DefaultReference.PARTY_TYPE_INTERNAL.getEntity())
	,TANAKA(20011, "TANAKA KIKINZOKU KOGYO KK - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,GOLDEN_BILLION(20017, "GOLDEN BILLION - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,HSBC(20019, "HSBC USA NEW YORK - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,UK_TAX(20021, "UK TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,ANGLO_PLATINUM(20022, "ANGLO PLATINUM MARKETING - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,SA_TAX(20024, "SA TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,US_TAX(20026, "US TAX - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,BARCLAYS_BANK(20027, "BARCLAYS BANK PLC, SOUTHAMPTON, UK - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,INEOS(20029, "INEOS TECHNOLOGIES LTD - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,JM_SYNGAS(20041, "JM SYNGAS - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,JM_REFINING(20042, "JM ROYSTON REFINING - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	,JM_METAL_JOINING(20043, "JM ROYSTON METAL JOINING - BU", DefaultReference.PARTY_TYPE_EXTERNAL.getEntity())
	;
	
	private final Party party;
	
	private DefaultParty (int id, String name, Reference partyType) {
		party = ImmutableParty.builder()
				.id(id)
				.name(name)
				.typeId(partyType.id())
				.build();
	}

	public Party getEntity () {
		return party;
	}

	public static List<Party> asList () {
		return Arrays.asList(DefaultParty.values())
				.stream().map(DefaultParty::getEntity).collect(Collectors.toList());
	}
}
