package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
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
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.CacheInvalidateRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.service.mock.MockCacheService;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableReferenceTo;
import com.matthey.pmm.toms.transport.ReferenceTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class ReferenceConversionTest {
	@Autowired
	protected ReferenceConverter referenceConverter;
	
	@Autowired
	protected ReferenceRepository refRepo;
	
	private List<Long> entitiesToDelete;
	
	@Before
	public void setUp () {
		entitiesToDelete = new ArrayList<Long>();
	}
	
	@After
	public void tearDown() {
		for (long id : entitiesToDelete) {
			refRepo.deleteById(id);
		}
	}
	
	@Test
	public void testToManagedEntity() {
		ReferenceTo newRef = ImmutableReferenceTo.builder()
				.displayName(null)
				.endurId(1999l)
				.id(0)
				.idLifecycle(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity().id())
				.idType(DefaultReferenceType.YES_NO.getEntity().id())
				.name("New Buy/Sell")
				.sortColumn(null)
				.build();
		Reference newRefSavedToDb = referenceConverter.toManagedEntity(newRef);
		entitiesToDelete.add(newRefSavedToDb.getId());
		assertThat(newRefSavedToDb.getDisplayName()).isEqualTo(newRef.displayName());
		assertThat(newRefSavedToDb.getEndurId()).isEqualTo(newRef.endurId());
		assertThat(newRefSavedToDb.getId()).isNotZero();
		assertThat(newRefSavedToDb.getLifecycleStatus().getId()).isEqualTo(newRef.idLifecycle());
		assertThat(newRefSavedToDb.getType().getId()).isEqualTo(DefaultReferenceType.YES_NO.getEntity().id());
		assertThat(newRefSavedToDb.getValue()).isEqualTo(newRef.name());
		assertThat(newRefSavedToDb.getSortColumn()).isEqualTo(newRef.sortColumn());
	}

	public void testToManagedEntityWithDataFailedInTomsService() {
		ReferenceTo newRef = ImmutableReferenceTo.builder()
				.displayName(null)
				.endurId(64l)
				.id(0)
				.idLifecycle(291l)
				.idType(10)
				.name("CHF")
				.sortColumn(null)
				.build();
		Reference newRefSavedToDb = referenceConverter.toManagedEntity(newRef);
		entitiesToDelete.add(newRefSavedToDb.getId());
		assertThat(newRefSavedToDb.getDisplayName()).isEqualTo(newRef.displayName());
		assertThat(newRefSavedToDb.getEndurId()).isEqualTo(newRef.endurId());
		assertThat(newRefSavedToDb.getId()).isNotZero();
		assertThat(newRefSavedToDb.getLifecycleStatus().getId()).isEqualTo(newRef.idLifecycle());
		assertThat(newRefSavedToDb.getType().getId()).isEqualTo(DefaultReferenceType.YES_NO.getEntity().id());
		assertThat(newRefSavedToDb.getValue()).isEqualTo(newRef.name());
		assertThat(newRefSavedToDb.getSortColumn()).isEqualTo(newRef.sortColumn());
	}
}
