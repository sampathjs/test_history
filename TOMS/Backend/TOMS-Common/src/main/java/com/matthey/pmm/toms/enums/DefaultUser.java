package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableUser;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultUser {
	SERVICE_USER(20046, "GRPEndurSupportTeam@matthey.com", "Service", "Account", Boolean.TRUE, DefaultReference.USER_ROLE_SERVICE_USER.getEntity()),
	DENNIS_WILDISH(20035, "dennis.wildish@matthey.com", "Dennis", "Wildish", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity()),
	JENS_WAECHTER(23113, "jens.waetcher@matthey.com", "Jens", "Wächter", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity()),
	MURALI_KRISHNAN(24208, "Murali.Krishnan@matthey.com", "Murali", "Krishnan", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity()),
	ARINDAM_RAY(20014, "Arindam.Ray@matthey.com", "Arindam", "Ray", Boolean.FALSE, DefaultReference.USER_ROLE_ADMIN.getEntity()),
	NIVEDITH_SAJJA(20073, "Nivedith.Sajja3@matthey.com", "Nivedith", "Sajja", Boolean.TRUE, DefaultReference.USER_ROLE_ADMIN.getEntity()),
	JACOB_SMITH(20026, "Jacob.Smith@matthey.com", "Jacob", "Smith", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity()),	
	PAT_MCCOURT(20944, "Pat.McCourt@jmusa.com", "Patrick", "McCourt", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_USER.getEntity()),
	ANDREW_BAYNES (23211, "Andrew.Baynes@matthey.com", "Andrew", "Baynes", Boolean.TRUE, DefaultReference.USER_ROLE_PMM_TRADER.getEntity()),
	;
	
	private final User user;
	
	private DefaultUser (int id, String email, String firstName,
			String lastName, Boolean active, Reference role) {
		user = ImmutableUser.builder()
				.id(id)
				.email(email)
				.firstName(firstName)
				.lastName(lastName)
				.active(active)
				.roleId(role.id())
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
