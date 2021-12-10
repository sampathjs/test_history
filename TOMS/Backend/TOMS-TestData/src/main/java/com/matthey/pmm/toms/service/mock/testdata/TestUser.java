package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.PartyTo;
import com.matthey.pmm.toms.transport.ReferenceTo;
import com.matthey.pmm.toms.transport.UserTo;

public enum TestUser {
	SERVICE_USER(20046, "GRPEndurSupportTeam@matthey.com", "Service", "Account", DefaultReference.USER_ROLE_SERVICE_USER.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	DENNIS_WILDISH(20035, "dennis.wildish@matthey.com", "Dennis", "Wildish", DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	JENS_WAECHTER(23113, "jens.waetcher@matthey.com", "Jens", "Wächter", DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	MURALI_KRISHNAN(24208, "Murali.Krishnan@matthey.com", "Murali", "Krishnan", DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	ARINDAM_RAY(20014, "Arindam.Ray@matthey.com", "Arindam", "Ray", DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	NIVEDITH_SAJJA(20073, "Nivedith.Sajja3@matthey.com", "Nivedith", "Sajja", DefaultReference.USER_ROLE_ADMIN.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	JACOB_SMITH(20026, "Jacob.Smith@matthey.com", "Jacob", "Smith", DefaultReference.USER_ROLE_PMM_FO.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			Arrays.asList(TestParty.JM_PLC_LE.getEntity(), TestParty.JM_PMM_UK_BU.getEntity()),
			Arrays.asList(TestParty.TANAKA_LE.getEntity(), TestParty.TANAKA_BU.getEntity(),
						  TestParty.GOLDEN_BILLION_LE.getEntity(), TestParty.GOLDEN_BILLION_BU.getEntity(),
						  TestParty.UK_TAX_BU.getEntity(),
						  TestParty.ANGLO_PLATINUM_LE.getEntity(), TestParty.ANGLO_PLATINUM_BU.getEntity()),
			Arrays.asList(DefaultReference.PORTFOLIO_UK_FX.getEntity(), DefaultReference.PORTFOLIO_UK_COPPER.getEntity(),
					DefaultReference.PORTFOLIO_UK_GOLD.getEntity(), DefaultReference.PORTFOLIO_UK_IRIDIUM.getEntity(),
					DefaultReference.PORTFOLIO_UK_LOAN_RENT.getEntity(), DefaultReference.PORTFOLIO_UK_OSMIUM.getEntity(),
					DefaultReference.PORTFOLIO_UK_FEES.getEntity(), DefaultReference.PORTFOLIO_UK_PHYSICAL.getEntity(),
					DefaultReference.PORTFOLIO_UK_PLATINUM.getEntity(), DefaultReference.PORTFOLIO_UK_RHODIUM.getEntity(),
					DefaultReference.PORTFOLIO_UK_RUTHENIUM.getEntity(), DefaultReference.PORTFOLIO_UK_PALLADIUM.getEntity(),
					DefaultReference.PORTFOLIO_UK_ZINC.getEntity(), DefaultReference.PORTFOLIO_UK_SILVER.getEntity(),	
					DefaultReference.PORTFOLIO_UK_GAINS_AND_LOSSES.getEntity(), DefaultReference.PORTFOLIO_UK_MIGRATION.getEntity(),	
					DefaultReference.PORTFOLIO_UK_UNDHEDGED.getEntity(), DefaultReference.PORTFOLIO_UK_AVERAGING.getEntity(),
					DefaultReference.PORTFOLIO_UK_PHYSICAL_OFFSET.getEntity(), DefaultReference.PORTFOLIO_UK_NICKEL.getEntity())),
	
	PAT_MCCOURT(20944, "Pat.McCourt@jmusa.com", "Patrick", "McCourt", DefaultReference.USER_ROLE_PMM_FO.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			Arrays.asList(TestParty.JM_INC_LE.getEntity(), TestParty.JM_PMM_US_BU.getEntity()),
			Arrays.asList(TestParty.BARCLAYS_BANK_LE.getEntity(), TestParty.BARCLAYS_BANK_BU.getEntity(),
						  TestParty.INEOS_LE.getEntity(), TestParty.INEOS_BU.getEntity(),
						  TestParty.JM_PLC_LE_EXT.getEntity(), TestParty.JM_METAL_JOINING_BU.getEntity(),
						  TestParty.JM_SYNGAS_BU.getEntity(), TestParty.JM_REFINING_BU.getEntity()),
			Arrays.asList(DefaultReference.PORTFOLIO_US_FEES.getEntity(), DefaultReference.PORTFOLIO_US_GOLD.getEntity(),
					DefaultReference.PORTFOLIO_US_IRIDIUM.getEntity(), DefaultReference.PORTFOLIO_US_LOAN_RENT.getEntity(),
					DefaultReference.PORTFOLIO_US_OSMIUM.getEntity(), DefaultReference.PORTFOLIO_US_PALLADIUM.getEntity(),
					DefaultReference.PORTFOLIO_US_PHYSICAL.getEntity(), DefaultReference.PORTFOLIO_US_PLATINUM.getEntity(),
					DefaultReference.PORTFOLIO_US_RHODIUM.getEntity(), DefaultReference.PORTFOLIO_US_RUTHENIUM.getEntity(),
					DefaultReference.PORTFOLIO_US_SILVER.getEntity(), DefaultReference.PORTFOLIO_US_GAINS_AND_LOSSES.getEntity(),
					DefaultReference.PORTFOLIO_US_UNHEDGED.getEntity(), DefaultReference.PORTFOLIO_US_AVERAGING.getEntity(),
					DefaultReference.PORTFOLIO_US_PHYSICAL_OFFSET.getEntity())),
	ANDREW_BAYNES (23211, "Andrew.Baynes@matthey.com", "Andrew", "Baynes", DefaultReference.USER_ROLE_PMM_TRADER.getEntity(),
			DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,
			TestParty.asListInternal(), TestParty.asListExternal(), DefaultReference.asListByType(DefaultReferenceType.PORTFOLIO)),
	;
	
	private final UserTo user;
	
	private TestUser (long id, String email, String firstName,
			String lastName, ReferenceTo role, DefaultReference lifecycleStatus,
			List<PartyTo> tradeableInternalPartyIds,
			List<PartyTo> tradeableCounterPartyIds,
			List<ReferenceTo> tradeablePortfolioIds) {
		user = ImmutableUserTo.builder()
				.id(id)
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.idLifecycleStatus(lifecycleStatus.getEntity().id())
				.roleId(role.id())
				.tradeableCounterPartyIds(tradeableCounterPartyIds.stream().map(x -> x.id()).collect(Collectors.toList()))
				.tradeableInternalPartyIds(tradeableInternalPartyIds.stream().map(x -> x.id()).collect(Collectors.toList()))
				.tradeablePortfolioIds(tradeablePortfolioIds != null?tradeablePortfolioIds.stream().map(x -> x.id()).collect(Collectors.toList()):Arrays.asList())
				.build();
	}

	public UserTo getEntity () {
		return user;
	}

	public static List<UserTo> asList () {
		return Arrays.asList(TestUser.values())
				.stream().map(TestUser::getEntity).collect(Collectors.toList());
	}
	
	
}
