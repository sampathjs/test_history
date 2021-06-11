package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableUser;
import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultUser {
	SERVICE_USER(20046, "GRPEndurSupportTeam@matthey.com", "Service", "Account", Boolean.TRUE, DefaultReference.USER_ROLE_SERVICE_USER.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	DENNIS_WILDISH(20035, "dennis.wildish@matthey.com", "Dennis", "Wildish", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	JENS_WAECHTER(23113, "jens.waetcher@matthey.com", "Jens", "Wächter", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	MURALI_KRISHNAN(24208, "Murali.Krishnan@matthey.com", "Murali", "Krishnan", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	ARINDAM_RAY(20014, "Arindam.Ray@matthey.com", "Arindam", "Ray", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	NIVEDITH_SAJJA(20073, "Nivedith.Sajja3@matthey.com", "Nivedith", "Sajja", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	JACOB_SMITH(20026, "Jacob.Smith@matthey.com", "Jacob", "Smith", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity(),
			Arrays.asList(DefaultParty.JM_PLC_LE.getEntity(), DefaultParty.JM_PMM_UK_BU.getEntity()),
			Arrays.asList(DefaultParty.TANAKA_LE.getEntity(), DefaultParty.TANAKA_BU.getEntity(),
						  DefaultParty.GOLDEN_BILLION_LE.getEntity(), DefaultParty.GOLDEN_BILLION_BU.getEntity(),
						  DefaultParty.UK_TAX_BU.getEntity(),
						  DefaultParty.ANGLO_PLATINUM_LE.getEntity(), DefaultParty.ANGLO_PLATINUM_BU.getEntity())),
	PAT_MCCOURT(20944, "Pat.McCourt@jmusa.com", "Patrick", "McCourt", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity(),
			Arrays.asList(DefaultParty.JM_INC_LE.getEntity(), DefaultParty.JM_PMM_US_BU.getEntity()),
			Arrays.asList(DefaultParty.BARCLAYS_BANK_LE.getEntity(), DefaultParty.BARCLAYS_BANK_BU.getEntity(),
						  DefaultParty.INEOS_LE.getEntity(), DefaultParty.INEOS_BU.getEntity(),
						  DefaultParty.JM_PLC_LE_EXT.getEntity(), DefaultParty.JM_METAL_JOINING_BU.getEntity(),
						  DefaultParty.JM_SYNGAS_BU.getEntity(), DefaultParty.JM_REFINING_BU.getEntity())),
	ANDREW_BAYNES (23211, "Andrew.Baynes@matthey.com", "Andrew", "Baynes", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_TRADER.getEntity(),
			DefaultParty.asListInternal(), DefaultParty.asListExternal()),
	;
	
	private final User user;
	
	private DefaultUser (int id, String email, String firstName,
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
		return Arrays.asList(DefaultUser.values())
				.stream().map(DefaultUser::getEntity).collect(Collectors.toList());
	}
	
	
}
