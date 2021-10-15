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
import com.matthey.pmm.toms.repository.ExpirationStatusRepository;
import com.matthey.pmm.toms.service.conversion.ExpirationStatusConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class ExpirationStatusRepositoryTest extends AbstractRepositoryTestBase<ExpirationStatus, Long, ExpirationStatusRepository> {	
	@Autowired
	protected ExpirationStatusConverter conv;

	@Autowired
	protected ReferenceConverter referenceCon;

	
	@Override
	protected Supplier<List<ExpirationStatus>> listProvider() {
		return () -> {
			final List<ExpirationStatus> expirationStatus = Arrays.asList(new ExpirationStatus(referenceCon.toManagedEntity(DefaultReference.EXPIRATION_STATUS_ACTIVE.getEntity()),
					referenceCon.toManagedEntity(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity())),
					new ExpirationStatus(referenceCon.toManagedEntity(DefaultReference.EXPIRATION_STATUS_EXPIRED.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity())));
			return expirationStatus;
		};
	}

	@Override
	protected Function<ExpirationStatus, Long> idProvider() {
		return x -> x.getId();
	}
}
