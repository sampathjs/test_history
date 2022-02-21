package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableUserTo;
import com.matthey.pmm.toms.transport.UserTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class UserConverterTest {
	@Autowired
	protected UserConverter userConverter;

	@Autowired
	protected UserRepository userRepo;

	@Autowired
	protected ReferenceConverter referenceCon;

	@Autowired
	protected PartyConverter partyConverter;

	
	protected List<User> userToDelete;	
	
	@Before
	public void initTestData () {
		userToDelete = new ArrayList<>();
	}
	
	@After
	public void cleanUpTestData () {
		for (User doDelete : userToDelete) {
			if (userRepo.existsById(doDelete.getId())) {
				userRepo.delete(doDelete);				
			}
		}
	}
	
	@Test
	@Transactional
	public void testUpdateOfEmptyPortfolioListFromTo() {
		User testUserForUpdate = new User(3333350l, "forUpdate@matthey.com", 
				"FirstnameForUpdate", "LastnameForUpdate",
				referenceCon.toManagedEntity(DefaultReference.USER_ROLE_ADMIN.getEntity()), 
				referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
				Arrays.asList(), // tradeableParties
				Arrays.asList(), // tradeablePortfolios
				partyConverter.toManagedEntity(TestBunit.JM_PMM_CN.getEntity()), 
				referenceCon.toManagedEntity(DefaultReference.PORTFOLIO_US_GOLD.getEntity()),
				"System Name");	
		testUserForUpdate = userRepo.save(testUserForUpdate);
		assertThat(testUserForUpdate.getId()).isNotNull();
		assertThat(testUserForUpdate.getId()).isNotZero();
		userToDelete.add(testUserForUpdate);
		UserTo asTo = userConverter.toTo(testUserForUpdate);
		asTo = ImmutableUserTo.builder()
				.from(asTo)
				.addAllTradeablePortfolioIds(Arrays.asList(DefaultReference.PORTFOLIO_US_GOLD.getEntity().id(), 
						DefaultReference.PORTFOLIO_US_PLATINUM.getEntity().id()))
				.build();
		User updatedUser = userConverter.toManagedEntity(asTo);
		assertThat(updatedUser.getTradeablePortfolios()).isNotNull();
		assertThat(updatedUser.getTradeablePortfolios()).hasSize(2);
		assertThat(updatedUser.getTradeablePortfolios().stream().map(x -> x.getId()).collect(Collectors.toList()))
			.containsExactlyInAnyOrder(DefaultReference.PORTFOLIO_US_GOLD.getEntity().id(), DefaultReference.PORTFOLIO_US_PLATINUM.getEntity().id());		
	}
	
	@Test
	@Transactional
	public void testUpdateOfEmptyPartyListFromTo() {
		User testUserForUpdate = new User(3333350l, "forUpdate@matthey.com", 
				"FirstnameForUpdate", "LastnameForUpdate",
				referenceCon.toManagedEntity(DefaultReference.USER_ROLE_ADMIN.getEntity()), 
				referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
				Arrays.asList(), // tradeableParties
				Arrays.asList(), // tradeablePortfolios
				partyConverter.toManagedEntity(TestBunit.JM_PMM_CN.getEntity()), 
				referenceCon.toManagedEntity(DefaultReference.PORTFOLIO_US_GOLD.getEntity()),
				"System Name");	
		testUserForUpdate = userRepo.save(testUserForUpdate);
		assertThat(testUserForUpdate.getId()).isNotNull();
		assertThat(testUserForUpdate.getId()).isNotZero();
		userToDelete.add(testUserForUpdate);
		UserTo asTo = userConverter.toTo(testUserForUpdate);
		asTo = ImmutableUserTo.builder()
				.from(asTo)
				.addAllTradeableCounterPartyIds(Arrays.asList(TestBunit.ABBOTT_BALL___BU.getEntity().id(),
						TestBunit.JM_PMM_UK.getEntity().id()))
				.build();
		User updatedUser = userConverter.toManagedEntity(asTo);
		assertThat(updatedUser.getTradeableParties()).isNotNull();
		assertThat(updatedUser.getTradeableParties()).hasSize(2);
		assertThat(updatedUser.getTradeableParties().stream().map(x -> x.getId()).collect(Collectors.toList()))
			.containsExactlyInAnyOrder(TestBunit.ABBOTT_BALL___BU.getEntity().id(), TestBunit.JM_PMM_UK.getEntity().id());		
	}

	
	
}
