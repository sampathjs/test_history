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
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class OrderStatusRepoTest extends AbstractRepositoryTestBase<OrderStatus, Long, OrderStatusRepository> {
	@Autowired
	protected ReferenceConverter refConverter;
	
	@Override
	protected Supplier<List<OrderStatus>> listProvider() {
		return () -> {
			return Arrays.asList(new OrderStatus(refConverter.toManagedEntity(DefaultReference.ORDER_STATUS_PART_EXPIRED.getEntity()),  
							refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity()),  
							refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_CATEGORY_OPEN.getEntity()),
							333333333l),
					new OrderStatus(refConverter.toManagedEntity(DefaultReference.ORDER_STATUS_PARTIAL_CANCELLED.getEntity()),  
							refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity()),  
							refConverter.toManagedEntity(DefaultReference.ORDER_TYPE_CATEGORY_OPEN.getEntity()),
							333333333l)
					);
		};
	}

	@Override
	protected Function<OrderStatus, Long> idProvider() {
		return x -> x.getId();
	}
}
