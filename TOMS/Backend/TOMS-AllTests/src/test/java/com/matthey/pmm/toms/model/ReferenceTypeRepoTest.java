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

import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.service.conversion.ReferenceTypeConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class ReferenceTypeRepoTest extends AbstractRepositoryTestBase<ReferenceType, Long, ReferenceTypeRepository> {
	@Autowired
	protected ReferenceTypeConverter converter;
	
	@Override
	protected Supplier<List<ReferenceType>> listProvider() {
		return () -> {
			return Arrays.asList(new ReferenceType("UnitTest", 1000l));
		};
	}

	@Override
	protected Function<ReferenceType, Long> idProvider() {
		return x -> x.getId();  
	}
}
