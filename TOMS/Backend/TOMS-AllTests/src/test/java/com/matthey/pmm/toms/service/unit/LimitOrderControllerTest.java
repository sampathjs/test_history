package com.matthey.pmm.toms.service.unit;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.repository.LimitOrderRepository;
import com.matthey.pmm.toms.repository.ReferenceOrderRepository;
import com.matthey.pmm.toms.service.mock.MockOrderController;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.ImmutableLimitOrderTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.LimitOrderTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
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
	protected void initTest () {
		limitOrderToBeDeleted = new ArrayList<>(5);
		referenceOrderToBeDeleted = new ArrayList<>(5);
	}
	
	@After
	protected void clearAfterTest () {
		for (LimitOrderTo order : limitOrderToBeDeleted) {
			limitOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
		for (ReferenceOrderTo order : referenceOrderToBeDeleted) {
			referenceOrderRepo.deleteById(new OrderVersionId(order.id(), order.version()));
		}
	}
	
	protected long submitNewLimitOrder (LimitOrderTo orderTo, boolean expectSuccess) {
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
	
	protected LimitOrderTo updateLimitOrder (LimitOrderTo toBeSaved) {
		orderController.updateLimitOrder(toBeSaved);
		LimitOrderTo versionInc = ImmutableLimitOrderTo.builder()
				.from(toBeSaved)
				.version(toBeSaved.version()+1)
				.build();
		limitOrderToBeDeleted.add(versionInc);
		return versionInc;
	}
	
	protected ReferenceOrderTo updateReferenceOrder (ReferenceOrderTo toBeSaved) {
		orderController.updateReferenceOrder(toBeSaved);
		ReferenceOrderTo versionInc = ImmutableReferenceOrderTo.builder()
				.from(toBeSaved)
				.version(toBeSaved.version()+1)
				.build();
		referenceOrderToBeDeleted.add(versionInc);
		return versionInc;
	}
	
}
