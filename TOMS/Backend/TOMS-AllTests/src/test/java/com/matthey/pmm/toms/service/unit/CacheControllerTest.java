package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CacheInvalidate;
import com.matthey.pmm.toms.repository.CacheInvalidateRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.mock.MockCacheService;
import com.matthey.pmm.toms.testall.TestServiceApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class CacheControllerTest {
	@Autowired
	protected MockCacheService cacheController;	
	
    @Autowired
    protected CacheInvalidateRepository cacheInvalidateRepo;
	
	protected SimpleDateFormat dateTimeFormat = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
	
	@Test
	public void testRetrieveInvalidCacheCategoriesForNow() {
		Set<Long> invalidCacheCategories = cacheController.getInvalidatedCacheCategories(dateTimeFormat.format(new Date()));
		assertThat(invalidCacheCategories).hasSize(0); // nothing invalidated since now in between
	}

	@Test 
	public void testRetrieveInvalidCacheCategoriesSinceStart () {
		Set<Long> invalidCacheCategories = cacheController.getInvalidatedCacheCategories(null);
		// all categories should be marked as invalidated at the start
		assertThat(invalidCacheCategories).hasSize(DefaultReference.asListByType(DefaultReferenceType.CACHE_TYPE).size());  		
	}
	
	@Test 
	public void testRetrieveWithIncorrectDateSyntax () {
		assertThatThrownBy (() -> {cacheController.getInvalidatedCacheCategories("Wrong Date Format");})
			.isInstanceOf (IllegalDateFormatException.class);
	}
	
	
	@Test
	public void testInvalidateAllCacheCategories() {
		Date startDate = new Date();
		try {
			cacheController.patchInvalidatedCacheCategories(new HashSet<>(DefaultReference.asListOfIdsByType(DefaultReferenceType.CACHE_TYPE)));
			Set<CacheInvalidate> invalidates = cacheInvalidateRepo.findByCutOffDateTimeAfter(startDate);
			assertThat(invalidates).hasSize(DefaultReference.asListOfIdsByType(DefaultReferenceType.CACHE_TYPE).size());
			for (CacheInvalidate invalidate : invalidates) {
				assertThat(invalidate.getCutOffDateTime()).isCloseTo(new Date(), 5000);
			}			
		} finally {
			cacheInvalidateRepo.deleteInBatch(cacheInvalidateRepo.findByCutOffDateTimeAfter(startDate));
		}
	}
	
	
}
