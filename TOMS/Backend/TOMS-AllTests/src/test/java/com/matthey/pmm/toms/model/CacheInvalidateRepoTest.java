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
import com.matthey.pmm.toms.repository.CacheInvalidateRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class CacheInvalidateRepoTest extends AbstractRepositoryTestBase<CacheInvalidate, Long, CacheInvalidateRepository> {	
	@Autowired
	protected ReferenceConverter refCon;
	
	@Override
	protected Supplier<List<CacheInvalidate>> listProvider() {
		return () -> {
			return Arrays.asList(new CacheInvalidate(refCon.toManagedEntity(DefaultReference.CACHE_TYPE_BUY_SELL.getEntity()), new Date()), 
					new CacheInvalidate(refCon.toManagedEntity(DefaultReference.CACHE_TYPE_BUY_SELL.getEntity()), new Date()),
					new CacheInvalidate(refCon.toManagedEntity(DefaultReference.CACHE_TYPE_ORDER_STATUS.getEntity()), new Date()));
		};
	}

	@Override
	protected Function<CacheInvalidate, Long> idProvider() {
		return x -> x.getId();
	}
}
