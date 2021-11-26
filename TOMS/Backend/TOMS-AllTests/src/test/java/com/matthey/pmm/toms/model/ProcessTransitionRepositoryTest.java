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
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class ProcessTransitionRepositoryTest extends AbstractRepositoryTestBase<ProcessTransition, Long, ProcessTransitionRepository> {
	@Autowired
	protected ReferenceConverter referenceCon;
		
	
	@Before
	public void setupTestData () {

	}
	
	@Override
	protected Supplier<List<ProcessTransition>> listProvider() {		
		return () -> {
			final List<ProcessTransition> processTransitions = Arrays.asList(new ProcessTransition(referenceCon.toManagedEntity(DefaultReference.REFERENCE_ORDER_LEG_TRANSITION.getEntity()),
					DefaultReference.ORDER_STATUS_PART_EXPIRED.getEntity().id(), DefaultReference.ORDER_STATUS_MATURED.getEntity().id(),
					12345l, Arrays.asList("attribute1", "attribute2", "attribute3")),
					new ProcessTransition(referenceCon.toManagedEntity(DefaultReference.REFERENCE_ORDER_LEG_TRANSITION.getEntity()),
							DefaultReference.ORDER_STATUS_PULLED.getEntity().id(), DefaultReference.ORDER_STATUS_MATURED.getEntity().id(),
							12345l, Arrays.asList("attribute5", "attribute6", "attribute7"))
					);
			return processTransitions;
		};
	}

	@Override
	protected Function<ProcessTransition, Long> idProvider() {
		return x -> x.getId();
	}
}
