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
import com.matthey.pmm.toms.model.CreditCheck;
import com.matthey.pmm.toms.model.LimitOrder;
import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.CreditCheckRepository;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.exception.IllegalIdException;
import com.matthey.pmm.toms.service.mock.MockCreditCheckService;
import com.matthey.pmm.toms.service.mock.testdata.TestCreditCheck;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.CreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.OrderTo;



@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class CreditCheckControllerTest {	
	@Autowired
	protected MockCreditCheckService creditCheckService;	
	
    @Autowired
    protected CreditCheckRepository creditCheckRepo;

    @Autowired
    protected CreditCheckConverter creditCheckConverter;
    
	@Autowired
	protected LimitOrderConverter limitOrderConverter;
	
	@Autowired
	protected ReferenceOrderConverter refOrderConverter;
	
	@Autowired
	protected LimitOrderRepository limitOrderRepo;
	
	@Autowired
	protected ReferenceOrderRepository refOrderRepo;
    
    
	protected SimpleDateFormat dateTimeFormat = new SimpleDateFormat (TomsService.DATE_TIME_FORMAT);
	
	private Map<OrderTo, CreditCheckTo> checksToDelete;
	private Map<OrderTo, CreditCheckTo> checksToRestore;	
	
	@Before
	public void init() {
		checksToDelete = new HashMap<>();
		checksToRestore = new HashMap<>();
	}
	
	@After
	public void tearDown() {
		for (Map.Entry<OrderTo, CreditCheckTo> entry : checksToDelete.entrySet()) {
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
			creditCheckRepo.deleteById(entry.getValue().id());
		}
		for (Map.Entry<OrderTo, CreditCheckTo> entry : checksToRestore.entrySet()) {
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
			creditCheckConverter.toManagedEntity(entry.getValue());			
		}
	}
	
	@Transactional
	protected long submitNewCreditCheck (OrderTo order, CreditCheckTo check) {
		CreditCheckTo clearedId = ImmutableCreditCheckTo.builder()
				.from(check)
				.id(0)
				.build();
			
		long newCreditCheckId = 0;
		OrderTo updatedOrder;
		if (order instanceof LimitOrderTo) {
			newCreditCheckId = creditCheckService.postLimitOrderCreditCheck(order.id(), clearedId);	
			updatedOrder = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();
		} else {
			newCreditCheckId = creditCheckService.postReferenceOrderCreditCheck(order.id(), clearedId);
			updatedOrder = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version()+1)
					.build();			
		}
		CreditCheckTo withId = ImmutableCreditCheckTo.builder()
				.from(check)
				.id(newCreditCheckId)
				.build();
		checksToDelete.put(updatedOrder, withId);
		return newCreditCheckId;
	}
	
	@Transactional
	protected void updateCreditCheck (OrderTo order, CreditCheckTo oldComment, CreditCheckTo newCreditCheck) {
		if (order instanceof LimitOrderTo) {
			OrderTo orderVersionInc = ImmutableLimitOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			checksToRestore.put(orderVersionInc, oldComment);
			creditCheckService.updateLimitOrderCreditCheck(order.id(), newCreditCheck.id(), newCreditCheck);				
		} else {
			OrderTo orderVersionInc = ImmutableReferenceOrderTo.builder()
					.from(order)
					.version(order.version() + 1)
					.build();
			checksToRestore.put(orderVersionInc, oldComment);
			creditCheckService.updateReferenceOrderCreditCheck(order.id(), newCreditCheck.id(), newCreditCheck);			
		}
	}
	
	@Test
	public void testGetLimitOrderCreditChecks () {
		Set<CreditCheckTo> checks = creditCheckService.getCreditCheckLimitOrders(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(checks).hasSize(TestLimitOrder.TEST_ORDER_1B.getEntity().creditChecksIds().size());
		assertThat(checks.stream().map(x-> x.id()).collect(Collectors.toList())).containsExactlyElementsOf(TestLimitOrder.TEST_ORDER_1B.getEntity().creditChecksIds());
	}
	
	@Test
	public void testGetReferenceOrderCreditChecks () {
		Set<CreditCheckTo> checks = creditCheckService.getCreditChecksReferenceOrders(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(checks).hasSize(TestReferenceOrder.TEST_ORDER_1B.getEntity().creditChecksIds().size());
		assertThat(checks.stream().map(x-> x.id()).collect(Collectors.toList())).containsExactlyElementsOf(TestReferenceOrder.TEST_ORDER_1B.getEntity().creditChecksIds());
	}
	
	@Test
	public void testGetLimitOrderCreditChecksOnOrderWithoutCreditChecks () {
		Set<CreditCheckTo> checks = creditCheckService.getCreditCheckLimitOrders(TestLimitOrder.TEST_ORDER_3.getEntity().id());
		assertThat(checks).isNull();
	}
	
	@Test
	public void testGetReferenceOrderCreditChecksOnOrderWithoutCreditChecks  () {
		Set<CreditCheckTo> checks = creditCheckService.getCreditChecksReferenceOrders(TestReferenceOrder.TEST_ORDER_3.getEntity().id());
		assertThat(checks).isNull();
	}

	
	@Test
	public void testGetLimitOrderCreditChecksOnIllegalOrderId () {
		assertThatThrownBy(() -> { creditCheckService.getCreditCheckLimitOrders(-1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetReferenceOrderCreditChecksOnIllegalOrderId () {
		assertThatThrownBy(() -> {creditCheckService.getCreditChecksReferenceOrders(-1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetLimitOrderCreditCheck () {
		CreditCheckTo check = creditCheckService.getCreditCheckLimitOrder(TestLimitOrder.TEST_ORDER_1B.getEntity().id(), TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity().id());
		assertThat(check.id()).isEqualTo(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity().id());		
	}
	
	@Test
	public void testGetReferenceOrderCreditCheck () {
		CreditCheckTo check = creditCheckService.getCreditChecksReferenceOrder(TestReferenceOrder.TEST_ORDER_1B.getEntity().id(), TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity().id());
		assertThat(check.id()).isEqualTo(TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity().id());		
	}
	
	@Test
	public void testGetLimitOrderCreditCheckOnOrderWithoutCreditChecks () {
		CreditCheckTo check = creditCheckService.getCreditCheckLimitOrder(TestLimitOrder.TEST_ORDER_3.getEntity().id(), TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity().id());
		assertThat(check).isNull();
	}
	
	@Test
	public void testGetReferenceOrderCreditCheckOnOrderWithoutCreditChecks () {
		CreditCheckTo check = creditCheckService.getCreditChecksReferenceOrder(TestReferenceOrder.TEST_ORDER_3.getEntity().id(), TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity().id());
		assertThat(check).isNull();
	}

	@Test
	public void testGetLimitOrderCreditCheckForIllegalCreditCheckId () {
		assertThatThrownBy(() -> { creditCheckService.getCreditCheckLimitOrder(TestLimitOrder.TEST_ORDER_1B.getEntity().id(), -1);  })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	public void testGetReferenceOrderCreditCheckForIllegalCreditCheckId () {
		assertThatThrownBy(() -> { creditCheckService.getCreditChecksReferenceOrder(TestReferenceOrder.TEST_ORDER_1B.getEntity().id(), -1); })
			.isInstanceOf(IllegalIdException.class);
	}
	
	@Test
	@Transactional
	public void testPostLimitOrderCreditCheck() {
		long newCreditCheckId = submitNewCreditCheck (TestLimitOrder.TEST_ORDER_1B.getEntity(), TestCreditCheck.TEST_CREDIT_CHECK_FOR_INSERT.getEntity());
		Optional<CreditCheck> newCreditCheck = creditCheckRepo.findById(newCreditCheckId);
		assertThat(newCreditCheck).isNotEmpty();
		assertThat(newCreditCheck.get().getParty().getId()).isEqualTo(TestCreditCheck.TEST_CREDIT_CHECK_FOR_INSERT.getEntity().idParty());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(order).isNotEmpty();
		// + 2 because the mock controller simulates Endur and adds a credit check outcome after submitting a new one
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_1B.getEntity().version()+2);
		assertThat(order.get().getCreditChecks().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newCreditCheckId);
	}
	
	@Test
	@Transactional
	public void testPostReferenceOrderCreditCheck() {
		long newCreditCheckId = submitNewCreditCheck (TestReferenceOrder.TEST_ORDER_1B.getEntity(), TestCreditCheck.TEST_CREDIT_CHECK_FOR_INSERT.getEntity());
		Optional<CreditCheck> newCreditCheck = creditCheckRepo.findById(newCreditCheckId);
		assertThat(newCreditCheck).isNotEmpty();
		assertThat(newCreditCheck.get().getParty().getId()).isEqualTo(TestCreditCheck.TEST_CREDIT_CHECK_FOR_INSERT.getEntity().idParty());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(order).isNotEmpty();
		// + 2 because the mock controller simulates Endur and adds a credit check outcome after submitting a new one
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_ORDER_1B.getEntity().version()+2);
		assertThat(order.get().getCreditChecks().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(newCreditCheckId);
	}
	
	@Test
	@Transactional
	public void testUpdateLimitOrderCreditCheck () {
		CreditCheckTo newCreditCheckStatus = ImmutableCreditCheckTo.builder()
				.from(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity())
				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED.getEntity().id())
				.build();
		updateCreditCheck(TestLimitOrder.TEST_ORDER_1B.getEntity(), TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity(), newCreditCheckStatus);
		Optional<CreditCheck> updatedCreditCheck = creditCheckRepo.findById(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity().id());
		Optional<LimitOrder> order = limitOrderRepo.findLatestByOrderId(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedCreditCheck).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestLimitOrder.TEST_ORDER_1B.getEntity().version()+1);		
		assertThat(updatedCreditCheck.get().getCreditCheckRunStatus().getId()).isEqualTo(newCreditCheckStatus.idCreditCheckRunStatus());
		assertThat(order.get().getCreditChecks().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity().id());
	}
	
	@Test
	@Transactional
	public void testUpdateReferenceOrderCreditCheck () {
		CreditCheckTo newCreditCheckStatus = ImmutableCreditCheckTo.builder()
				.from(TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity())
				.idCreditCheckRunStatus(DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED.getEntity().id())
				.build();
		updateCreditCheck(TestReferenceOrder.TEST_ORDER_1B.getEntity(), TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity(), newCreditCheckStatus);
		Optional<CreditCheck> updatedCreditCheck = creditCheckRepo.findById(TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity().id());
		Optional<ReferenceOrder> order = refOrderRepo.findLatestByOrderId(TestReferenceOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(updatedCreditCheck).isNotEmpty();
		assertThat(order).isNotEmpty();
		assertThat(order.get().getVersion()).isEqualTo(TestReferenceOrder.TEST_ORDER_1B.getEntity().version()+1);		
		assertThat(updatedCreditCheck.get().getCreditCheckRunStatus().getId()).isEqualTo(newCreditCheckStatus.idCreditCheckRunStatus());
		assertThat(order.get().getCreditChecks().stream().map(x -> x.getId()).collect(Collectors.toList())).contains(TestCreditCheck.TEST_CREDIT_CHECK_8.getEntity().id());
	}
	
}
