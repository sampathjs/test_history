package com.matthey.pmm.toms.service.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
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
public class OrderControllerTest {
	private static final int NUMBER_OF_ORDERS_TO_BOOK = 10;
	
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
	public void testUpdateSimpleExistingLimitOrder () {
		// order in status pending, update on reference, expect order version to be incremented, but order status to stay the same
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.reference("New Reference")
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		Optional<LimitOrder> fromDbOld = limitOrderRepo.findById(new OrderVersionId(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().id(), 
				TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDbOld).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(withUpdatedReference.idOrderStatus());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
		assertThat(fromDbOld.get().getReference()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().reference());
	}
	
	@Test
	@Transactional
	public void testUpdateSimpleExistingReferenceOrder () {
		// order in status pending, update on reference, expect order version to be incremented, but order status to stay the same
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity())
				.reference("New Reference")
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		Optional<ReferenceOrder> fromDbOld = referenceOrderRepo.findById(new OrderVersionId(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().id(), 
				TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDbOld).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(withUpdatedReference.idOrderStatus());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
		assertThat(fromDbOld.get().getReference()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToConfirmed () {
		// positive test case, as pending to confirmed is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToPulled () {
		// positive test case, as pending to pulled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_PULLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_PULLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToRejected () {
		// positive test case, as pending to pulled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_REJECTED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_REJECTED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}

	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToCancelled () {
		// negative test case as limit order pending to cancelled is not allowed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CANCELLED.getEntity().id())
				.build();
		assertThatThrownBy(() -> { updateLimitOrder(withUpdatedReference); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateLimitOrderConfirmedToCancelled () {
		// positive test case, as confirmed to cancelled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CANCELLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_CANCELLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderConfirmedToExpired () {
		// positive test case, as confirmed to expired is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_EXPIRED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_EXPIRED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderConfirmedToFilled () {
		// positive test case, as confirmed to filled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderConfirmedToPartFilled () {
		// positive test case, as confirmed to part filled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPulledToMatured () {
		// positive test case, as pulled to matured is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PULLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PULLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderRejectedToMatured () {
		// positive test case, as rejected to matured is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_REJECTED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_REJECTED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderExpiredToMatured () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_EXPIRED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_EXPIRED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderCancelledToMatured () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_CANCELLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CANCELLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPartFilledToFilled () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PART_FILLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_FILLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_CANCELLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPartFilledToPartCancelled () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PART_FILLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_PART_FILLED_CANCELLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PART_FILLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPartFilledToPartExpired () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PART_FILLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_PART_EXPIRED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PART_FILLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}

	@Test
	@Transactional
	public void testUpdateLimitOrderFilledToMatured () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_FILLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_FILLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPartExpiredMatured () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PART_EXPIRED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PART_EXPIRED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPartCancelledMatured () {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PART_FILLED_CANCELLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id())
				.build();
		LimitOrderTo updatedOrder = updateLimitOrder(withUpdatedReference);
		Optional<LimitOrder> fromDb = limitOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.LIMIT_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestLimitOrder.TEST_IN_STATUS_PART_FILLED_CANCELLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderPendingToPulled () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_PULLED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_PULLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderPendingToConfirmed () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderPendingToRejected () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_REJECTED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_REJECTED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderConfirmedToFilled () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_FILLED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_FILLED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderPulledToMatured () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}

	@Test
	@Transactional
	public void testUpdateReferenceOrderFilledToMatured () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}

	@Test
	@Transactional
	public void testUpdateReferenceOrderRejectedToMatured () {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id())
				.build();
		ReferenceOrderTo updatedOrder = updateReferenceOrder(withUpdatedReference);
		Optional<ReferenceOrder> fromDb = referenceOrderRepo.findById(new OrderVersionId(updatedOrder.id(), updatedOrder.version()));
		assertThat(fromDb).isNotEmpty();
		assertThat(fromDb.get().getOrderStatus().getId()).isEqualTo(DefaultOrderStatus.REFERENCE_ORDER_MATURED.getEntity().id());
		assertThat(fromDb.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity().version()+1);
		assertThat(fromDb.get().getVersion()).isEqualTo(updatedOrder.version());
		assertThat(fromDb.get().getReference()).isEqualTo(withUpdatedReference.reference());
	}

	@Test
	public void testMockGenerateOrder() {
		long oldCountLimitOrder = limitOrderRepo.count();
		long oldCountReferenceOrder = referenceOrderRepo.count();
		orderController.createTestOrders(NUMBER_OF_ORDERS_TO_BOOK);
		long newCountLimitOrder = limitOrderRepo.count();
		long newCountReferenceOrder = referenceOrderRepo.count();
		// count = for each version of a single order at least one row. Potentially more.
		assertThat(newCountLimitOrder + newCountReferenceOrder - oldCountLimitOrder - oldCountReferenceOrder).isGreaterThanOrEqualTo(NUMBER_OF_ORDERS_TO_BOOK);
	}

	
}
