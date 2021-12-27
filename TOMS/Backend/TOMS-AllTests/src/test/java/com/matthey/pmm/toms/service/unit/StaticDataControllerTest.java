package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.service.mock.MockStaticDataController;
import com.matthey.pmm.toms.testall.TestServiceApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class StaticDataControllerTest {
	@Autowired
	protected MockStaticDataController staticDataController;
	
	@Test
	public void testGetAllReferenceTypes () {
		List<Long> allReferenceTypeIdsFromController = staticDataController.getAllReferenceTypes().stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allReferenceTypeIds = DefaultReferenceType.asListOfIds();
		assertThat(allReferenceTypeIds).containsExactlyInAnyOrderElementsOf(allReferenceTypeIdsFromController);
	}
	
	@Test
	public void testGetAllReferences () {
		List<Long> allReferenceIdsFromController = staticDataController.getReferences(null).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allReferenceIds = DefaultReference.asListOfIds();
		assertThat(allReferenceIds).containsExactlyInAnyOrderElementsOf(allReferenceIdsFromController);		
	}
	
	@Test
	public void testGetAllReferencesOfSpecificType () {
		List<Long> allReferenceIdsFromController = staticDataController.getReferences(DefaultReferenceType.CCY_METAL.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allReferenceIds = DefaultReference.asListOfIdsByType(DefaultReferenceType.CCY_METAL);
		assertThat(allReferenceIds).containsExactlyInAnyOrderElementsOf(allReferenceIdsFromController);		
	}
	
	@Test
	public void testGetAllOrderStatus () {
		List<Long> allOrderStatusIdsFromController = staticDataController.getOrderStatus(null, null).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allOrderStatusIds = DefaultOrderStatus.asListOfIds();
		assertThat(allOrderStatusIds).containsExactlyInAnyOrderElementsOf(allOrderStatusIdsFromController);				
	}
	
	@Test
	public void testOrderStatusByOrderStatusId () {
		List<Long> allOrderStatusIdsFromController = staticDataController.getOrderStatus(
				DefaultReference.ORDER_STATUS_PENDING.getEntity().id(), null).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> relevantOrderStatusIds = DefaultOrderStatus.asListOfIdsByName(DefaultReference.ORDER_STATUS_PENDING);
		assertThat(allOrderStatusIdsFromController).containsExactlyInAnyOrderElementsOf(relevantOrderStatusIds);
	}
	
	@Test
	public void testOrderStatusByOrderStatusNameId () {
		List<Long> allOrderStatusIdsFromController = staticDataController.getOrderStatus(
				null, DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> relevantOrderStatusIds = DefaultOrderStatus.asListOfIdsByType(DefaultReference.ORDER_TYPE_REFERENCE_ORDER);		
		assertThat(allOrderStatusIdsFromController).containsExactlyInAnyOrderElementsOf(relevantOrderStatusIds);
	}	
	
	@Test
	public void testOrderStatusByOrderStatusNameIdAndOrderStatusId () {
		List<Long> allOrderStatusIdsFromController = staticDataController.getOrderStatus(
				DefaultReference.ORDER_STATUS_PENDING.getEntity().id(), DefaultReference.ORDER_TYPE_REFERENCE_ORDER.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> relevantOrderStatusIds = DefaultOrderStatus.asListOfIdsByType(DefaultReference.ORDER_TYPE_REFERENCE_ORDER);
		relevantOrderStatusIds.retainAll(DefaultOrderStatus.asListOfIdsByName(DefaultReference.ORDER_STATUS_PENDING));
		
		assertThat(allOrderStatusIdsFromController).containsExactlyInAnyOrderElementsOf(relevantOrderStatusIds);
	}	

}
