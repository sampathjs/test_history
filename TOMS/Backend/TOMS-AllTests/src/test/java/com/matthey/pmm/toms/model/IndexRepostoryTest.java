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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.service.conversion.IndexConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class IndexRepostoryTest extends AbstractRepositoryTestBase<IndexEntity, Long, IndexRepository> {	
	@Autowired
	protected IndexConverter conv;

	@Autowired
	protected ReferenceConverter referenceCon;	
	
	@Before
	public void setupTestData () {
	
	}
	
	@Override
	protected Supplier<List<IndexEntity>> listProvider() {		
		return () -> { // in prod ID is managed by Endur		
			final List<IndexEntity> indices = Arrays.asList(new IndexEntity(10000000l, referenceCon.toManagedEntity(DefaultReference.AVERAGING_RULES_SAMPLE1.getEntity()), 
					referenceCon.toManagedEntity(DefaultReference.METAL_XPT.getEntity()), 
					referenceCon.toManagedEntity(DefaultReference.CCY_GBP.getEntity()),
					referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 1000l),
					new IndexEntity(10000001l, referenceCon.toManagedEntity(DefaultReference.AVERAGING_RULES_SAMPLE2.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.CCY_EUR.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.CCY_GBP.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),2000l),
					new IndexEntity(10000002l, referenceCon.toManagedEntity(DefaultReference.FILL_STATUS_OPEN.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.METAL_XAU.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.CCY_USD.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),3000l)				
					);
			return indices;
		};
	}

	@Override
	protected Function<IndexEntity, Long> idProvider() {
		return x -> x.getId();
	}
}
