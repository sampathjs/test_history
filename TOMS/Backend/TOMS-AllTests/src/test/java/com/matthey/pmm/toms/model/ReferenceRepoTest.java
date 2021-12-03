package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceTypeConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class ReferenceRepoTest extends AbstractRepositoryTestBase<Reference, Long, ReferenceRepository> {
	@Autowired
	protected ReferenceConverter converter;

	@Autowired
	protected ReferenceTypeConverter refTypeConverter;

	
	@Override
	protected Supplier<List<Reference>> listProvider() {
		return () -> {
			return Arrays.asList(new Reference(refTypeConverter.toManagedEntity(DefaultReferenceType.BUY_SELL.getEntity()), "value", "display name", -1, 
					converter.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()), 1000l));
		};
	}

	@Override
	protected Function<Reference, Long> idProvider() {
		return x -> x.getId();
	}
}
