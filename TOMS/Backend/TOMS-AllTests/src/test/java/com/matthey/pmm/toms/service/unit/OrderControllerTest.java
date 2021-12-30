package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalLegRemovalException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.exception.IllegalValueException;
import com.matthey.pmm.toms.service.exception.UnknownEntityException;
import com.matthey.pmm.toms.service.mock.MockOrderController;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrderLeg;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;
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
	protected OrderRepository orderRepo;

	@Autowired 
	protected ReferenceOrderRepository referenceOrderRepo;

	@Autowired 
	protected ReferenceOrderLegRepository referenceOrderLegRepo;
	
	@Autowired
	protected ReferenceOrderConverter refOrderConverter;

	@Autowired
	protected ReferenceOrderLegConverter refOrderLegConverter;
	
	@Autowired
	protected LimitOrderConverter limitOrderConverter;

	
	protected List<LimitOrderTo> limitOrderToBeDeleted;
	protected List<ReferenceOrderTo> referenceOrderToBeDeleted;
	protected Map<ReferenceOrderTo, ReferenceOrderLegTo> referenceOrderLegToBeDeleted;
	protected Map<ReferenceOrderTo, ReferenceOrderLegTo> referenceOrderLegToBeRestored;
	
	
	@Before
	public void initTest () {
		limitOrderToBeDeleted = new ArrayList<>(5);
		referenceOrderToBeDeleted = new ArrayList<>(5);
		referenceOrderLegToBeDeleted = new HashMap<>();
		referenceOrderLegToBeRestored = new HashMap<>();
	}
	
	@After
	public void clearAfterTest () {
		for (LimitOrderTo order : limitOrderToBeDeleted) {
			limitOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
		for (ReferenceOrderTo order : referenceOrderToBeDeleted) {
			referenceOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
		for (Map.Entry<ReferenceOrderTo, ReferenceOrderLegTo> entry : referenceOrderLegToBeDeleted.entrySet()) {
			refOrderConverter.toManagedEntity(entry.getKey());
			referenceOrderLegRepo.deleteById(entry.getValue().id());
		}
		for (Map.Entry<ReferenceOrderTo, ReferenceOrderLegTo> entry : 	referenceOrderLegToBeRestored.entrySet()) {
			refOrderLegConverter.toManagedEntity(entry.getValue());
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
	
	@Transactional
	protected long submitNewReferenceOrderLeg (ReferenceOrderTo order, ReferenceOrderLegTo legTo) {
		ReferenceOrderLegTo withResetOrderIdAndVersion = ImmutableReferenceOrderLegTo.builder()
				.from(legTo)
				.id(0l)
				.build();
		long ret = orderController.postReferenceOrderLeg(order.id(), order.version(), withResetOrderIdAndVersion);
		ReferenceOrderLegTo withId = ImmutableReferenceOrderLegTo.builder()
				.from(legTo)
				.id(ret)
				.build();
		referenceOrderLegToBeDeleted.put(order, withId);
		return ret;
	}
	
	/**
	 * Should be used for a single update only. Do not apply multiple updates on the same leg.
	 * @param order
	 * @param toBeSaved
	 * @return
	 */
	@Transactional
	protected void updateReferenceOrderLeg (ReferenceOrderTo order, ReferenceOrderLegTo newLegVersion) {
		ReferenceOrderLegTo oldLegVersion = refOrderLegConverter.toTo(referenceOrderLegRepo.findById(newLegVersion.id()).get());
		orderController.updateReferenceOrderLeg(order.id(), order.version(), newLegVersion.id(), newLegVersion);
		referenceOrderLegToBeRestored.put(order, oldLegVersion);
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
	public void testUpdateLimitOrderForIllegalOrderIdFails() {
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.id(TestReferenceOrder.TEST_ORDER_1A.getEntity().id())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id())
				.build();
		assertThatThrownBy ( () -> { orderController.updateLimitOrder(withUpdatedReference); })
			.isInstanceOf(UnknownEntityException.class);
	}
	
	@Test
	public void testUpdateReferenceOrderForIllegalOrderIdFails() {
		ReferenceOrderTo withUpdatedReference = ImmutableReferenceOrderTo.builder()
				.from(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity())
				.id(TestLimitOrder.TEST_ORDER_1A.getEntity().id())
				.idOrderStatus(DefaultOrderStatus.REFERENCE_ORDER_CONFIRMED.getEntity().id())
				.build();
		assertThatThrownBy ( () -> { orderController.updateReferenceOrder(withUpdatedReference); })
			.isInstanceOf(UnknownEntityException.class);
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToConfirmedNotAllowedWithChangedCreatedBy () {
		// positive test case, as pending to confirmed is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id())
				.idCreatedByUser(TestUser.ARINDAM_RAY.getEntity().id())
				.build();
		assertThatThrownBy(() -> { updateLimitOrder(withUpdatedReference); } )
			.isInstanceOf(IllegalValueException.class);
	}

	@Test
	@Transactional
	public void testUpdateLimitOrderPendingToConfirmedNotAllowedWithChangedCreatedAt () {
		// positive test case, as pending to confirmed is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_CONFIRMED.getEntity().id())
				.createdAt("2020-11-23 15:00:00")
				.build();
		assertThatThrownBy(() -> { updateLimitOrder(withUpdatedReference); } )
			.isInstanceOf(IllegalValueException.class);
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
	public void testUpdateLimitOrderPendingToRejectedBlockedForChangedReference () {
		// positive test case, as pending to pulled is allowed. No attributes except order status are changed.
		LimitOrderTo withUpdatedReference = ImmutableLimitOrderTo.builder()
				.from(TestLimitOrder.TEST_IN_STATUS_PENDING.getEntity())
				.idOrderStatus(DefaultOrderStatus.LIMIT_ORDER_REJECTED.getEntity().id())
				.reference("New Reference that should trigger exception")
				.build();
		assertThatThrownBy(() -> { updateLimitOrder(withUpdatedReference); } )
			.isInstanceOf(IllegalValueException.class);
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

	@Test
	public void testGetAllOrders () {
		List<OrderTo> allOrder = orderRepo.findLatest().stream()
				.map(x -> (OrderTo)((x instanceof LimitOrder)?
						limitOrderConverter.toTo((LimitOrder)x):
						refOrderConverter.toTo((ReferenceOrder)x)))
				.collect(Collectors.toList());
		PageRequest page = PageRequest.of(0, 100000);
		Page<OrderTo> pageOfOrders = orderController.getOrders(null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, page, null);
		assertThat(allOrder).containsAll(pageOfOrders.getContent());
		assertThat(pageOfOrders.getContent()).containsAll(allOrder);		
	}
	
	@Test
	public void testGetOrdersWithFilter () {
		// Please note this test case might fail in the future in case more test orders are added.
		// The failure is going to look like some orders are not going to be in the expected result setl
		// The reason it is going to fail is in the combination of sort by id, idMetalForm, idCreatedByUser
		// idTicker and createdAt in combination with having the idFirstOrderIncluded search parameter set.
		// In case of future failures it is suggested to adjust the test data or to remove the
		// missing orders explicitly from the allOrder list created below.
		
		List<OrderTo> allOrder = orderRepo.findLatest().stream()
				.map(x -> (OrderTo)((x instanceof LimitOrder)?
						limitOrderConverter.toTo((LimitOrder)x):
						refOrderConverter.toTo((ReferenceOrder)x)))
				.collect(Collectors.toList());
		PageRequest page = PageRequest.of(0, 100000, Sort.by(Order.desc("id"),
				                                             Order.asc("idMetalForm"), 
				                                             Order.asc("idCreatedByUser"), 
				                                             Order.asc("idTicker"), 
				                                             Order.asc("createdAt")
				                                             ));
		
		List<Long> allOrderIds = TestLimitOrder.asListOfIds();
		allOrderIds.addAll(TestReferenceOrder.asListOfIds());
		List<Long> allContractTypeIds = DefaultReference.asListOfIdsByType(DefaultReferenceType.CONTRACT_TYPE_LIMIT_ORDER);
		allContractTypeIds.addAll(DefaultReference.asListOfIdsByType(DefaultReferenceType.CONTRACT_TYPE_REFERENCE_ORDER));
		
		Page<OrderTo> pageOfOrders = orderController.getOrders(
				DefaultReference.asListOfIdsByType(DefaultReferenceType.ORDER_TYPE_NAME), 
				allOrderIds, 
				null, // order versions
				TestBunit.asListInternalBu().stream().map(x -> x.id()).collect(Collectors.toList()), 
				TestBunit.asListBu().stream().map(x -> x.id()).collect(Collectors.toList()),
				null, 
				null, 
				null, 
				null, 
				DefaultReference.asListOfIdsByType(DefaultReferenceType.BUY_SELL), 
				DefaultReference.asListOfIdsByType(DefaultReferenceType.CCY_METAL),
				0d, // min base quantity
				Double.MAX_VALUE,  // max base quantity
				DefaultReference.asListOfIdsByType(DefaultReferenceType.QUANTITY_UNIT), 
				DefaultReference.asListOfIdsByType(DefaultReferenceType.CCY_CURRENCY), 
				null, // reference
				DefaultReference.asListOfIdsByType(DefaultReferenceType.METAL_FORM), 
				DefaultReference.asListOfIdsByType(DefaultReferenceType.METAL_LOCATION),
				DefaultOrderStatus.asListOfIds(), 
				TestUser.asListOfIds(), // created by
				"1995-10-31 01:30:00", // min creation date 
				"2030-10-31 01:30:00", // max creation date
				TestUser.asListOfIds(), // updated by
				"1995-10-31 01:30:00", // min updated date 
				"2030-10-31 01:30:00", // max updated date 
				0d, // min fill percentage
				Double.MAX_VALUE, // max fill percentage
				allContractTypeIds, 
				DefaultReference.asListOfIdsByType(DefaultReferenceType.TICKER), 
				page, 
				TestReferenceOrder.TEST_FOR_LEG_DELETION_ALL_LEGS.getEntity().id());
		assertThat(allOrder).containsAll(pageOfOrders.getContent());
		assertThat(pageOfOrders.getContent()).containsAll(allOrder);		
	}
	
	
	@Test
	public void testGetAllLimitOrders () {
		List<OrderTo> allOrder = limitOrderRepo.findLatest().stream()
				.map(x -> limitOrderConverter.toTo(x))
				.collect(Collectors.toList());
		PageRequest page = PageRequest.of(0, 100000);
		Page<OrderTo> pageOfOrders = orderController.getLimitOrders(null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, page);
		assertThat(allOrder).containsAll(pageOfOrders.getContent());
		assertThat(pageOfOrders.getContent()).containsAll(allOrder);		
	}	
	
	@Test
	public void testGetAllReferenceOrders () {
		List<OrderTo> allOrder = referenceOrderRepo.findLatest().stream()
				.map(x -> refOrderConverter.toTo(x))
				.collect(Collectors.toList());
		PageRequest page = PageRequest.of(0, 100000);
		Page<OrderTo> pageOfOrders = orderController.getReferenceOrders(null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, 
				null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, page);
		assertThat(allOrder).containsAll(pageOfOrders.getContent());
		assertThat(pageOfOrders.getContent()).containsAll(allOrder);		
	}
	
	@Test
	public void testGetAllReferenceOrderLegs () {
		Iterable<ReferenceOrder> allRefOrders = referenceOrderRepo.findLatest();
		List<ReferenceOrderLegTo> allLegs = StreamSupport.stream(referenceOrderLegRepo.findAll().spliterator(),false)
				.filter(x -> x.getId() != TestReferenceOrderLeg.TEST_LEG_NOT_IN_ANY_ORDER.getEntity().id())
				.map(x -> refOrderLegConverter.toTo(x))
				.collect(Collectors.toList());
		List<ReferenceOrderLegTo> allLegsFromController = new ArrayList<>(allLegs.size());
		for (ReferenceOrder refOrder : allRefOrders) {
			Set<ReferenceOrderLegTo> legs = orderController.getReferenceOrderLegs(refOrder.getOrderId(), refOrder.getVersion());
			allLegsFromController.addAll(legs);
			assertThat(allLegs).containsAll(legs);
		}
		assertThat(allLegsFromController).containsAll(allLegs);
	}
	
	@Test
	public void testGetAllReferenceOrderLegFailsOnIllegalOrderId () {
		assertThatThrownBy(() -> { orderController.getReferenceOrderLegs(TestLimitOrder.TEST_IN_STATUS_CANCELLED.getEntity().id(), 1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testCreateSimpleReferenceOrderLeg () {
		long newReferenceOrderLegId = submitNewReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity(), 
				TestReferenceOrderLeg.TEST_LEG_NOT_IN_ANY_ORDER.getEntity());
		Optional<ReferenceOrderLeg> newleg = referenceOrderLegRepo.findById(newReferenceOrderLegId);
		assertThat(newleg).isNotEmpty();
		Optional<ReferenceOrder> updatedOrder = referenceOrderRepo.findById(
				new OrderVersionId(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().id(),
								   TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity().version()));
		List<ReferenceOrderLeg> newLegOnOrder = updatedOrder.get().getLegs().stream()
			.filter(x -> x.getId() == newReferenceOrderLegId)
			.collect(Collectors.toList());
		assertThat(newLegOnOrder).isNotEmpty();
	}
	
	@Test
	public void testCreateSimpleReferenceOrderLegNotAllowedInStatusOtherThanPending () {
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version(), 
					TestReferenceOrderLeg.TEST_LEG_11.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity().version(), 
					TestReferenceOrderLeg.TEST_LEG_12.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity().version(), 
					TestReferenceOrderLeg.TEST_LEG_13.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity().version(), 
					TestReferenceOrderLeg.TEST_LEG_14.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);		
	}
	
	@Test
	public void testUpdateSimpleReferenceOrderLeg () {
		ReferenceOrderLegTo updatedLeg = ImmutableReferenceOrderLegTo.builder()
				.from(TestReferenceOrderLeg.TEST_LEG_10.getEntity())
				.notional(99d)
				.build();
		updateReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_PENDING.getEntity(), 
				updatedLeg);
		Optional<ReferenceOrderLeg> newleg = referenceOrderLegRepo.findById(TestReferenceOrderLeg.TEST_LEG_10.getEntity().id());
		assertThat(newleg).isNotEmpty();
		assertThat(newleg.get().getNotional()).isCloseTo(99d, within(0.00001d));
	}
	
	@Test
	public void testUpdateReferenceOrderLegNotAllowedInStatusOtherThanPending () {
		assertThatThrownBy(() -> {
			ReferenceOrderLegTo updatedLeg = ImmutableReferenceOrderLegTo.builder()
					.from(TestReferenceOrderLeg.TEST_LEG_11.getEntity())
					.notional(99d)
					.build();
			updateReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity(), 
					updatedLeg);
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			ReferenceOrderLegTo updatedLeg = ImmutableReferenceOrderLegTo.builder()
					.from(TestReferenceOrderLeg.TEST_LEG_12.getEntity())
					.notional(99d)
					.build();
			updateReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity(), 
					updatedLeg);
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			ReferenceOrderLegTo updatedLeg = ImmutableReferenceOrderLegTo.builder()
					.from(TestReferenceOrderLeg.TEST_LEG_13.getEntity())
					.notional(99d)
					.build();
			updateReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity(), 
					updatedLeg);
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			ReferenceOrderLegTo updatedLeg = ImmutableReferenceOrderLegTo.builder()
					.from(TestReferenceOrderLeg.TEST_LEG_14.getEntity())
					.notional(99d)
					.build();
			updateReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity(), 
					updatedLeg);
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);		
	}
	
	@Test
	public void testDeleteSimpleReferenceOrderLeg () {
		orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_FOR_LEG_DELETION.getEntity().id(),
				TestReferenceOrder.TEST_FOR_LEG_DELETION.getEntity().version(), TestReferenceOrderLeg.TEST_FOR_LEG_DELETION.getEntity().id());
		Optional<ReferenceOrderLeg> deletedLeg = referenceOrderLegRepo.findById(TestReferenceOrderLeg.TEST_FOR_LEG_DELETION.getEntity().id());
		assertThat(deletedLeg).isNotEmpty(); // the leg just removed from the order with the given version but not deleted itself
		Optional<ReferenceOrder> updatedOrder = referenceOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_FOR_LEG_DELETION.getEntity().id());
		assertThat(updatedOrder).isNotEmpty();
		assertThat(updatedOrder.get().getLegs()).isNotEmpty();
		assertThat(updatedOrder.get().getLegs().stream().map(x -> x.getId()).collect(Collectors.toList()))
			.doesNotContain(TestReferenceOrderLeg.TEST_FOR_LEG_DELETION.getEntity().id());
	}
	
	@Test
	public void testDeleteReferenceOrderLegNotAllowedInStatusOtherThanPending () {
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version(), TestReferenceOrderLeg.TEST_LEG_11.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_FILLED.getEntity().version(), TestReferenceOrderLeg.TEST_LEG_12.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_PULLED.getEntity().version(), TestReferenceOrderLeg.TEST_LEG_13.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity().id(),
					TestReferenceOrder.TEST_IN_STATUS_REJECTED.getEntity().version(), TestReferenceOrderLeg.TEST_LEG_14.getEntity().id());
			}).isInstanceOf(com.matthey.pmm.toms.service.exception.IllegalStateException.class);		
	}
	
	@Test
	public void testDeleteAllReferenceOrderLegsIsImpossible() {
		assertThatThrownBy(() -> {
			orderController.deleteReferenceOrderLeg(TestReferenceOrder.TEST_FOR_LEG_DELETION_ALL_LEGS.getEntity().id(),
					TestReferenceOrder.TEST_FOR_LEG_DELETION_ALL_LEGS.getEntity().version(), 
					TestReferenceOrderLeg.MAIN_LEG_FOR_LEG_DELETION_ALL_LEGS.getEntity().id());
			}).isInstanceOf(IllegalLegRemovalException.class);
	}
	
	@Test
	public void testGetAttributeValueLimitOrder () {
		String ret = orderController.getAttributeValueLimitOrder("id", TestLimitOrder.TEST_ORDER_1A.getEntity());
		assertThat(ret).isEqualTo("0");
	}
	
	@Test
	public void testGetAttributeValueReferenceOrder () {
		String ret = orderController.getAttributeValueReferenceOrder("id", TestReferenceOrder.TEST_ORDER_1A.getEntity());
		assertThat(ret).isEqualTo("0");
	}
}
