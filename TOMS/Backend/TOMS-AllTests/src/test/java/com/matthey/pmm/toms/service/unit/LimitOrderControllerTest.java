package com.matthey.pmm.toms.service.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.context.web.WebAppConfiguration;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.mock.MockOrderController;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class}) 
@ContextConfiguration
public class LimitOrderControllerTest {
	@Autowired
	protected MockOrderController orderController;
	
	@Autowired 
	protected LimitOrderRepository limitOrderRepo;

	@Autowired 
	protected ReferenceOrderRepository referenceOrderRepo;

	
	protected List<LimitOrderTo> limitOrderToBeDeleted;
	protected List<ReferenceOrderTo> referenceOrderToBeDeleted;

	
	
	@Before
	public void initTest () {
		limitOrderToBeDeleted = new ArrayList<>(5);
		referenceOrderToBeDeleted = new ArrayList<>(5);
	}
	
	@After
	public void clearAfterTest () {
		for (LimitOrderTo order : limitOrderToBeDeleted) {
			limitOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
		for (ReferenceOrderTo order : referenceOrderToBeDeleted) {
			referenceOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
	}
	
	@Transactional
	protected long submitNewLimitOrder (LimitOrderTo orderTo) {
		LimitOrderTo withResetOrderIdAndVersion = ImmutableLimitOrderTo.builder()
				.from(orderTo)
				.id(0l)
				.version(0)
				.build();
		long ret = orderController.postLimitOrder(withResetOrderIdAndVersion);
		LimitOrderTo withOrderIdAndVersion = ImmutableLimitOrderTo.builder()
				.from(orderTo)
				.id(ret)
				.version(1)
				.build();
		limitOrderToBeDeleted.add(withOrderIdAndVersion);
		return ret;
	}
	
	@Transactional
	protected long submitNewReferenceOrder (ReferenceOrderTo orderTo) {
		ReferenceOrderTo withResetOrderIdAndVersion = ImmutableReferenceOrderTo.builder()
				.from(orderTo)
				.id(0l)
				.version(0)
				.build();
		long ret = orderController.postReferenceOrder(withResetOrderIdAndVersion);
		ReferenceOrderTo withOrderIdAndVersion = ImmutableReferenceOrderTo.builder()
				.from(orderTo)
				.id(ret)
				.version(1)
				.build();
		referenceOrderToBeDeleted.add(withOrderIdAndVersion);
		return ret;
	}
	
	@Transactional
	protected LimitOrderTo updateLimitOrder (LimitOrderTo toBeSaved) {
		orderController.updateLimitOrder(toBeSaved);
		LimitOrderTo versionInc = ImmutableLimitOrderTo.builder()
				.from(toBeSaved)
				.version(toBeSaved.version()+1)
				.build();
		limitOrderToBeDeleted.add(versionInc);
		return versionInc;
	}
	
	@Transactional
	protected ReferenceOrderTo updateReferenceOrder (ReferenceOrderTo toBeSaved) {
		orderController.updateReferenceOrder(toBeSaved);
		ReferenceOrderTo versionInc = ImmutableReferenceOrderTo.builder()
				.from(toBeSaved)
				.version(toBeSaved.version()+1)
				.build();
		referenceOrderToBeDeleted.add(versionInc);
		return versionInc;
	}
	
	@Test
	@Transactional
	public void testCreateSimpleNewLimitOrder () {
		long newOrderId = submitNewLimitOrder(TestLimitOrder.TEST_ORDER_3.getEntity());
		Optional<LimitOrder> newOrder = limitOrderRepo.findById(new OrderVersionId(newOrderId, 1));
		assertThat(newOrder).isNotEmpty();
		assertThat(newOrder.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_PENDING.getEntity().id());
	}
	
	@Test
	@Transactional
	public void testCreateSimpleNewReferenceOrder () {
		long newOrderId = submitNewReferenceOrder(TestReferenceOrder.TEST_ORDER_3.getEntity());
		Optional<ReferenceOrder> newOrder = referenceOrderRepo.findById(new OrderVersionId(newOrderId, 1));
		assertThat(newOrder).isNotEmpty();
		assertThat(newOrder.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_PENDING.getEntity().id());
	}
	
	@Test
	@Transactional
	public void testUpdateSimpleExistingimitOrder () {
		// order in status pending, update on reference, expect order version to be incremented, but order status to stay the same
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_SIMPLE_UPDATE.getEntity())
				.reference("New Reference")
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(withUpdatedReference.idOrderStatus());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_SIMPLE_UPDATE.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
}
