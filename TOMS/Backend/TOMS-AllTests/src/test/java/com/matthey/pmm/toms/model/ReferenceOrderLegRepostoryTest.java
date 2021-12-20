package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;
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
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class ReferenceOrderLegRepostoryTest extends AbstractRepositoryTestBase<ReferenceOrderLeg, Long, ReferenceOrderLegRepository> {	
	@Autowired
	protected ReferenceConverter referenceCon;
	
	@Before
	public void setupTestData () {

	}

	
	@Override
	protected Supplier<List<ReferenceOrderLeg>> listProvider() {		
		return () -> {
			final List<ReferenceOrderLeg> legs = Arrays.asList(new ReferenceOrderLeg (new Date(), new Date(), 
					referenceCon.toManagedEntity(DefaultReference.SYMBOLIC_DATE_EOM.getEntity()),
					30d, 
					referenceCon.toManagedEntity(DefaultReference.CCY_GBP.getEntity()),
					referenceCon.toManagedEntity(DefaultReference.REF_SOURCE_JM_HK_CLOSING.getEntity()),
					referenceCon.toManagedEntity(DefaultReference.REF_SOURCE_BLOOMBERG.getEntity()))
					, new ReferenceOrderLeg (new Date(), null, 
							null,
							30d, 
							referenceCon.toManagedEntity(DefaultReference.CCY_GBP.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.REF_SOURCE_JM_HK_CLOSING.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.REF_SOURCE_BLOOMBERG.getEntity())));
			return legs;
		};
	}

	@Override
	protected Function<ReferenceOrderLeg, Long> idProvider() {
		return x -> x.getId();
	}
}
