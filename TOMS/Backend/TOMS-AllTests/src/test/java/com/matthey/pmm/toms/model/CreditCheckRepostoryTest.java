package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;
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
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class CreditCheckRepostoryTest extends AbstractRepositoryTestBase<CreditCheck, Long, CreditCheckRepository> {	
	@Autowired
	protected CreditCheckConverter conv;

	@Autowired
	protected PartyConverter partyConv;

	@Autowired
	protected ReferenceConverter referenceCon;

	
	@Override
	protected Supplier<List<CreditCheck>> listProvider() {
		return () -> {
			final List<CreditCheck> creditChecks = Arrays.asList(new CreditCheck(partyConv.toManagedEntity(TestBunit.ANGLO_PLATINUM_MARKETING___BU.getEntity()), 100000d, 50000d,
					new Date(), referenceCon.toManagedEntity(DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN.getEntity()),
					null),
					new CreditCheck(partyConv.toManagedEntity(TestBunit.GOLDEN_BILLION___BU.getEntity()), 100000d, 50000d,
							new Date(), referenceCon.toManagedEntity(DefaultReference.CREDIT_CHECK_RUN_STATUS_TRANSITION.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.CREDIT_CHECK_OUTCOME_PASSED.getEntity()))
					);
			
			return creditChecks;
		};
	}

	@Override
	protected Function<CreditCheck, Long> idProvider() {
		return x -> x.getId();
	}
}
