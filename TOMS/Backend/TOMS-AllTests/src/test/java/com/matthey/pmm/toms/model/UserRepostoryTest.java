package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class UserRepostoryTest extends AbstractRepositoryTestBase<User, Long, UserRepository> {	
	@Autowired
	protected ReferenceConverter referenceCon;	
	
	@Autowired
	protected PartyConverter partyConverter;
		
	@Before
	public void setupTestData () {
	  	  TestParty.asList() // legal entities first (LEs are not assigned to another LE)
		  		.stream()
		  		.filter(x -> x.idLegalEntity() <= 0)
		  		.forEach(x -> partyConverter.toManagedEntity(x));
	  	  TestParty.asList() // now the business units (everything that has an LE)
		  		.stream()
		  		.filter(x -> x.idLegalEntity() > 0)
		  		.forEach(x -> partyConverter.toManagedEntity(x));
	  	  
	}
	
	@Override
	protected Supplier<List<User>> listProvider() {		
		return () -> {
			final List<User> users = Arrays.asList(new User(3333l, "test1@matthey.com", 
							"Firstname1", "Lastname1",
							referenceCon.toManagedEntity(DefaultReference.USER_ROLE_ADMIN.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
							Arrays.asList(partyConverter.toManagedEntity(TestParty.TANAKA_BU.getEntity()),
									partyConverter.toManagedEntity(TestParty.BARCLAYS_BANK_BU.getEntity())), // tradeableParties
							Arrays.asList(referenceCon.toManagedEntity(DefaultReference.PORTFOLIO_UK_GOLD.getEntity()), // tradeablePortfolios
									referenceCon.toManagedEntity(DefaultReference.PORTFOLIO_US_GOLD.getEntity()))),
					new User(4444l, "test2@matthey.com", 
							"Firstname2", "Lastname2",
							referenceCon.toManagedEntity(DefaultReference.USER_ROLE_ADMIN.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
							null, // tradeableParties
							null));  // tradeablePortfolios
			return users;
		};
	}

	@Override
	protected Function<User, Long> idProvider() {
		return x -> x.getId();
	}
}
