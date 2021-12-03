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
import com.matthey.pmm.toms.repository.PartyRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class PartyRepostoryTest extends AbstractRepositoryTestBase<Party, Long, PartyRepository> {	
	@Autowired
	protected ReferenceConverter referenceCon;	
	
	protected Party internalTestLe;
	
	protected Party externalTestLe;
	
	@Before
	public void setupTestData () {
		internalTestLe = repo.save(new Party(1000l, "Internal Test LE - Persisted", 
				referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity()), 
				null, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
				123456l));
		externalTestLe = repo.save(new Party(1001l, "External Test LE - Persisted", 
				referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity()), 
				null, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
				123457l));
	}
	
	@Override
	protected Supplier<List<Party>> listProvider() {		
		return () -> {
			final List<Party> parties = Arrays.asList(new Party(10000l, "External Test LE", 
					referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity()), 
					null, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 12345l),
					new Party(10001l, "Internal Test LE", 
							referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_INTERNAL_LE.getEntity()), 
							null, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 12346l),
					new Party(10002l, "Internal Test BU", 
							referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity()), 
							internalTestLe, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 12346l),
					new Party(10003l,"External Test BU", 
							referenceCon.toManagedEntity(DefaultReference.PARTY_TYPE_EXTERNAL_BUNIT.getEntity()), 
							externalTestLe, referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 12346l)				
					);			
			return parties;
		};
	}

	@Override
	protected Function<Party, Long> idProvider() {
		return x -> x.getId();
	}
}
