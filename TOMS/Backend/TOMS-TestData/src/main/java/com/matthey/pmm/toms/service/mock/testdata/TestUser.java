package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.model.ImmutableUser;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum TestUser {
	SERVICE_USER(20046, "GRPEndurSupportTeam@matthey.com", "Service", "Account", Boolean.TRUE, DefaultReference.USER_ROLE_SERVICE_USER.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	DENNIS_WILDISH(20035, "dennis.wildish@matthey.com", "Dennis", "Wildish", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	JENS_WAECHTER(23113, "jens.waetcher@matthey.com", "Jens", "Wächter", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	MURALI_KRISHNAN(24208, "Murali.Krishnan@matthey.com", "Murali", "Krishnan", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	ARINDAM_RAY(20014, "Arindam.Ray@matthey.com", "Arindam", "Ray", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	NIVEDITH_SAJJA(20073, "Nivedith.Sajja3@matthey.com", "Nivedith", "Sajja", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	JACOB_SMITH(20026, "Jacob.Smith@matthey.com", "Jacob", "Smith", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity(),
			Arrays.asList(TestParty.JM_PLC_LE.getEntity(), TestParty.JM_PMM_UK_BU.getEntity()),
			Arrays.asList(TestParty.TANAKA_LE.getEntity(), TestParty.TANAKA_BU.getEntity(),
						  TestParty.GOLDEN_BILLION_LE.getEntity(), TestParty.GOLDEN_BILLION_BU.getEntity(),
						  TestParty.UK_TAX_BU.getEntity(),
						  TestParty.ANGLO_PLATINUM_LE.getEntity(), TestParty.ANGLO_PLATINUM_BU.getEntity())),
	PAT_MCCOURT(20944, "Pat.McCourt@jmusa.com", "Patrick", "McCourt", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity(),
			Arrays.asList(TestParty.JM_INC_LE.getEntity(), TestParty.JM_PMM_US_BU.getEntity()),
			Arrays.asList(TestParty.BARCLAYS_BANK_LE.getEntity(), TestParty.BARCLAYS_BANK_BU.getEntity(),
						  TestParty.INEOS_LE.getEntity(), TestParty.INEOS_BU.getEntity(),
						  TestParty.JM_PLC_LE_EXT.getEntity(), TestParty.JM_METAL_JOINING_BU.getEntity(),
						  TestParty.JM_SYNGAS_BU.getEntity(), TestParty.JM_REFINING_BU.getEntity())),
	ANDREW_BAYNES (23211, "Andrew.Baynes@matthey.com", "Andrew", "Baynes", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_TRADER.getEntity(),
			TestParty.asListInternal(), TestParty.asListExternal()),
	;
	
	private final User user;
	
	private TestUser (int id, String email, String firstName,
			String lastName, Boolean active, Reference role,
			List<Party> tradeableInternalPartyIds,
			List<Party> tradeableCounterPartyIds) {
		user = ImmutableUser.builder()
				.id(id)
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.active(active)
				.roleId(role.id())
				.tradeableCounterPartyIds(tradeableCounterPartyIds.stream().map(x -> x.id()).collect(Collectors.toList()))
				.tradeableInternalPartyIds(tradeableInternalPartyIds.stream().map(x -> x.id()).collect(Collectors.toList()))
				.build();
	}

	public User getEntity () {
		return user;
	}

	public static List<User> asList () {
		return Arrays.asList(TestUser.values())
				.stream().map(TestUser::getEntity).collect(Collectors.toList());
	}
	
	
}
