package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.Fill;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.mock.MockFillController;
import com.matthey.pmm.toms.service.mock.testdata.TestFill;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.FillTo;
import com.matthey.pmm.toms.transport.ImmutableFillTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;



@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class FillControllerTest {	
	@Autowired
	protected MockFillController fillController;	
	
    @Autowired
    protected FillRepository fillRepo;

    @Autowired
    protected FillConverter fillConverter;

    
	@Autowired
	protected LimitOrderConverter limitOrderConverter;
	
	@Autowired
	protected ReferenceOrderConverter refOrderConverter;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository refOrderRepo;
    
    
	protected SimpleDateFormat dateTimeFormat = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
	
	private Map<OrderTo, FillTo> fillsToDelete;
	private Map<OrderTo, FillTo> fillsToRestore;	
	
	@Before
	public void init() {
		fillsToDelete = new HashMap<>();
		fillsToRestore = new HashMap<>();
	}
	
	@After
	public void tearDown() {
		for (Map.Entry<OrderTo, FillTo> entry : fillsToDelete.entrySet()) {
			if (entry.getKey() instanceof LimitOrderTo) {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (limitOrderRepo.existsById(id)) {
					limitOrderRepo.deleteById(id);
				}
			} else {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (refOrderRepo.existsById(id)) {
					refOrderRepo.deleteById(id);
				}
			}
			fillRepo.deleteById(entry.getValue().id());
		}
		for (Map.Entry<OrderTo, FillTo> entry : fillsToRestore.entrySet()) {
			if (entry.getKey() instanceof LimitOrderTo) {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (limitOrderRepo.existsById(id)) {
					limitOrderRepo.deleteById(id);
				}
			} else {
				OrderVersionId id = new OrderVersionId(entry.getKey().id(), entry.getKey().version());
				if (refOrderRepo.existsById(id)) {
					refOrderRepo.deleteById(id);
				}
			}
			fillConverter.toManagedEntity(entry.getValue());			
		}
	}
	
	@Transactional
	protected long submitNewFill (OrderTo order, FillTo fill) {
		FillTo clearedId = ImmutableFillTo.builder()
				.from(fill)
				.id(0)
				.idTrade(null)
				.build();
			
		long newFillId = 0;
		OrderTo updatedOrder;
		if (order instanceof LimitOrderTo) {
			newFillId = fillController.postLimitOrderFill(order.id(), clearedId);	
			updatedOrder = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();
		} else {
			newFillId = fillController.postReferenceOrderFill(order.id(), clearedId);
			updatedOrder = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();			
		}
		FillTo withId = ImmutableFillTo.builder()
				.from(fill)
				.id(newFillId)
				.build();
		fillsToDelete.put(updatedOrder, withId);
		return newFillId;
	}
	
	@Transactional
	protected void updateFill (OrderTo order, FillTo oldComment, FillTo newFill) {
		if (order instanceof LimitOrderTo) {
			OrderTo orderVersionInc = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			fillsToRestore.put(orderVersionInc, oldComment);
			fillController.updateLimitOrderFill(order.id(), newFill.id(), newFill);				
		} else {
			OrderTo orderVersionInc = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			fillsToRestore.put(orderVersionInc, oldComment);
			fillController.updateReferenceOrderFill(order.id(), newFill.id(), newFill);
		}
	}
	
	@Test
	public void testGetLimitOrderFills () {
		Set<FillTo> fills = fillController.getLimitOrderFills(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(fills).hasSize(TestLimitOrder.TEST_ORDER_1B.getEntity().fillIds().size());
		assertThat(fills.stream().map(x-> x.id()).collect(Collectors.toList())).containsExactlyElementsOf(TestLimitOrder.TEST_ORDER_1B.getEntity().fillIds());
	}
	
	@Test
	public void testGetReferenceOrderFills () {
		Set<FillTo> fills = fillController.getReferenceOrderFills(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(fills).hasSize(TestReferenceOrder.TEST_ORDER_1B.getEntity().fillIds().size());
		assertThat(fills.stream().map(x-> x.id()).collect(Collectors.toList())).containsExactlyElementsOf(TestReferenceOrder.TEST_ORDER_1B.getEntity().fillIds());
	}
	
	@Test
	public void testGetLimitOrderFillsOnOrderWithoutFills () {
		Set<FillTo> fills = fillController.getLimitOrderFills(TestLimitOrder.TEST_ORDER_3.getEntity().id());
		assertThat(fills).isNull();
	}
	
	@Test
	public void testGetReferenceOrderFillsOnOrderWithoutFills () {
		Set<FillTo> fills = fillController.getReferenceOrderFills(TestReferenceOrder.TEST_ORDER_3.getEntity().id());
		assertThat(fills).isNull();
	}

	
	@Test
	public void testGetLimitOrderFillsOnIllegalOrderId () {
		assertThatThrownBy(() -> { fillController.getLimitOrderFills(-1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetReferenceOrderFillsOnIllegalOrderId () {
		assertThatThrownBy(() -> {fillController.getReferenceOrderFills(-1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetLimitOrderFill () {
		FillTo fill = fillController.getLimitOrderFill(TestLimitOrder.TEST_ORDER_1B.getEntity().id(), TestFill.TEST_LIMIT_ORDER_FILL_1.getEntity().id());
		assertThat(fill.id()).isEqualTo(TestFill.TEST_LIMIT_ORDER_FILL_1.getEntity().id());		
	}
	
	@Test
	public void testGetReferenceOrderFill () {
		FillTo fill = fillController.getReferenceOrderFill(TestReferenceOrder.TEST_ORDER_1B.getEntity().id(), TestFill.TEST_REFERENCE_ORDER_FILL_1.getEntity().id());
		assertThat(fill.id()).isEqualTo(TestFill.TEST_REFERENCE_ORDER_FILL_1.getEntity().id());		
	}
	
	@Test
	public void testGetLimitOrderFillOnOrderWithoutFills () {
		FillTo fill = fillController.getLimitOrderFill(TestLimitOrder.TEST_ORDER_3.getEntity().id(), TestFill.TEST_LIMIT_ORDER_FILL_1.getEntity().id());
		assertThat(fill).isNull();
	}
	
	@Test
	public void testGetReferenceOrderFillOnOrderWithoutFills () {
		FillTo fill = fillController.getReferenceOrderFill(TestReferenceOrder.TEST_ORDER_3.getEntity().id(), TestFill.TEST_REFERENCE_ORDER_FILL_1.getEntity().id());
		assertThat(fill).isNull();
	}

	@Test
	public void testGetLimitOrderFillForIllegalFillId () {
		assertThatThrownBy(() -> { fillController.getLimitOrderFill(TestLimitOrder.TEST_ORDER_1B.getEntity().id(), -1);  })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetReferenceOrderFillForIllegalFillId () {
		assertThatThrownBy(() -> { fillController.getReferenceOrderFill(TestReferenceOrder.TEST_ORDER_1B.getEntity().id(), -1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	@Transactional
	public void testPostLimitOrderFill() {
		long newFillId = submitNewFill (TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity(), TestFill.TEST_FILL_FOR_INSERT.getEntity());
		Optional<Fill> newFill = fillRepo.findById(newFillId);
		assertThat(newFill).isNotEmpty();
		assertThat(newFill.get().getFillQuantity()).isEqualTo(TestFill.TEST_FILL_FOR_INSERT.getEntity().fillQuantity());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity().id());
		assertThat(order).isNotEmpty();
		// + 2 because the mock controller is going to simulate the endur side backend directly, resulting in version incremented by 2		
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity().version()+2);
		assertThat(order.get().getFills().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newFillId);
	}
	
	@Test
	@Transactional
	public void testPostReferenceOrderFill() {
		long newFillId = submitNewFill (TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity(), TestFill.TEST_FILL_FOR_INSERT.getEntity());
		Optional<Fill> newFill = fillRepo.findById(newFillId);
		assertThat(newFill).isNotEmpty();
		assertThat(newFill.get().getFillQuantity()).isEqualTo(TestFill.TEST_FILL_FOR_INSERT.getEntity().fillQuantity());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id());
		assertThat(order).isNotEmpty();
		// + 2 because the mock controller is going to simulate the endur side backend directly, resulting in version incremented by 2
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+2);
		assertThat(order.get().getFills().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newFillId);
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderFill () {
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity().id());
		assertThat(order.get().getFillPercentage()).isEqualTo(0);		
		FillTo newFillStatus = ImmutableFillTo.builder()
				.from(TestFill.TEST_LIMIT_ORDER_FILL_3.getEntity())
				.idFillStatus(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id())
				.build();
		updateFill(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity(), TestFill.TEST_LIMIT_ORDER_FILL_3.getEntity(), newFillStatus);
		Optional<Fill> updatedFill = fillRepo.findById(TestFill.TEST_LIMIT_ORDER_FILL_3.getEntity().id());
		order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity().id());
		assertThat(updatedFill).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_FOR_FILL_TEST.getEntity().version()+1);		
		assertThat(updatedFill.get().getFillStatus().getId()).isEqualTo(newFillStatus.idFillStatus());
		assertThat(order.get().getFills().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestFill.TEST_LIMIT_ORDER_FILL_3.getEntity().id());
		assertThat(order.get().getFillPercentage()).isEqualTo(100);
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderFill () {
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id());
		assertThat(order.get().getFillPercentage()).isEqualTo(0);
		FillTo newFillStatus = ImmutableFillTo.builder()
				.from(TestFill.TEST_REFERENCE_ORDER_FILL_4.getEntity())
				.idFillStatus(DefaultReference.FILL_STATUS_COMPLETED.getEntity().id())
				.build();
		updateFill(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity(), TestFill.TEST_REFERENCE_ORDER_FILL_4.getEntity(), newFillStatus);
		Optional<Fill> updatedFill = fillRepo.findById(TestFill.TEST_REFERENCE_ORDER_FILL_4.getEntity().id());
		order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().id());
		assertThat(updatedFill).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_IN_STATUS_CONFIRMED.getEntity().version()+1);		
		assertThat(updatedFill.get().getFillStatus().getId()).isEqualTo(newFillStatus.idFillStatus());
		assertThat(order.get().getFills().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestFill.TEST_REFERENCE_ORDER_FILL_4.getEntity().id());
		assertThat(order.get().getFillPercentage()).isEqualTo(100);
	}
	
}
