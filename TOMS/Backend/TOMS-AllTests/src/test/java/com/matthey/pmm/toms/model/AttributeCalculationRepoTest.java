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

import com.matthey.pmm.toms.repository.AttributeCalculationRepository;
import com.matthey.pmm.toms.service.conversion.AttributeCalculationConverter;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class AttributeCalculationRepoTest extends AbstractRepositoryTestBase<AttributeCalculation, Long, AttributeCalculationRepository> {
	@Autowired
	protected AttributeCalculationConverter converter;

	
	@Override
	protected Supplier<List<AttributeCalculation>> listProvider() {
		return () -> {
			return Arrays.asList(new AttributeCalculation("className", Arrays.asList("dependentAttribute1", "dependentAttribute2"),
					"attributeName", "spelExpression"));
		};
	}

	@Override
	protected Function<AttributeCalculation, Long> idProvider() {
		return x -> x.getId();
	}
}
