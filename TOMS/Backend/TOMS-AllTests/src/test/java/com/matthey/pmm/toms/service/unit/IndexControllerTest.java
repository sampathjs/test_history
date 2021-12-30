package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.IndexEntity;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.IndexRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.mock.MockIndexController;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.IndexTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class IndexControllerTest {
	@Autowired
	protected MockIndexController indexController;	
	
    @Autowired
    protected IndexRepository indexRepo;

    @Autowired
    protected ReferenceRepository refRepo;

    
	@Test
	public void testGetAllIndex() {
		Set<IndexTo> allIndex = indexController.getIndexes(null);
		List<IndexEntity> fromRepo = indexRepo.findAll();
		assertThat(allIndex).isNotNull();
		assertThat(allIndex).isNotEmpty();	
		assertThat(allIndex.stream().map( x -> x.id()).collect(Collectors.toList()))
			.containsAll(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));
	}

	@Test
	public void testGetAllIndexesForUsd() {
		Set<IndexTo> allIndex = indexController.getIndexes(DefaultReference.CCY_USD.getEntity().id());
		
		Optional<Reference> currencyRef =  refRepo.findById(DefaultReference.CCY_USD.getEntity().id());
				
		Set<IndexEntity> fromRepo = indexRepo.findByCurrencyOneNameOrCurrencyTwoName(currencyRef.get(), currencyRef.get());
		assertThat(allIndex).isNotNull();
		assertThat(allIndex).isNotEmpty();	
		assertThat(allIndex.stream().map( x -> x.id()).collect(Collectors.toList()))
			.containsAll(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));
	}
	
	@Test
	public void testGetAllIndexesFailsForIllegalId() {
		assertThatThrownBy(() -> { indexController.getIndexes(DefaultReference.CACHE_TYPE_BUY_SELL.getEntity().id()); })
			.isInstanceOf(IllegalReferenceTypeException.class);
	}

}
