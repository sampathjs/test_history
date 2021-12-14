package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.repository.OrderRepository;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.OrderStatusConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestCreditCheck;
import com.matthey.pmm.toms.service.mock.testdata.TestFill;
import com.matthey.pmm.toms.service.mock.testdata.TestLenit;
import com.matthey.pmm.toms.service.mock.testdata.TestOrderComment;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class OrderRepoTest extends AbstractRepositoryTestBase<Order, OrderVersionId, OrderRepository> {
	@Autowired
	protected LimitOrderConverter converter;
		
	@Autowired
	protected PartyConverter partyConverter;

	@Autowired
	protected ReferenceConverter referenceConverter;

	@Autowired
	protected OrderStatusConverter orderStatusConverter;
	
	@Autowired
	protected UserConverter userConverter;
	
	@Autowired
	protected OrderCommentConverter orderCommentConverter;
	
	@Autowired
	protected FillConverter fillConverter;

	@Autowired
	protected CreditCheckConverter creditCheckConverter;

	@Autowired
	protected FillRepository fillRepo;;

	
	@Before
	public void insertTestData () {

	}
	
	@After
	public void forDebug () {
		fillRepo.findAll();
	}	
	
	@Override
	protected Supplier<List<Order>> listProvider() {
		// TODO: Add Limit Order with all optional fields being null
		return () -> {
			// 1st test case with all fields filled
			List<Order> list =  Arrays.asList(new LimitOrder (referenceConverter.toManagedEntity(DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity()), // order type name
										1,   // version
										partyConverter.toManagedEntity(TestBunit.JM_PMM_UK.getEntity()),  // internal bu 
										partyConverter.toManagedEntity(TestBunit.ANGLO_PLATINUM_MARKETING___BU.getEntity()), // external bu
										partyConverter.toManagedEntity(TestLenit.JM_PLC.getEntity()),  // internal le
										partyConverter.toManagedEntity(TestLenit.ANGLO_PLATINUM_MARKETING___LE.getEntity()), // external le
										referenceConverter.toManagedEntity(DefaultReference.PORTFOLIO_UK_GOLD.getEntity()), // internal portfolio
										referenceConverter.toManagedEntity(DefaultReference.PORTFOLIO_US_GOLD.getEntity()),  // external portfolio
										referenceConverter.toManagedEntity(DefaultReference.BUY_SELL_BUY.getEntity()), // buy / sell
										referenceConverter.toManagedEntity(DefaultReference.METAL_XAU.getEntity()), // base currency
										1000d, // base quantity
										referenceConverter.toManagedEntity(DefaultReference.QUANTITY_TOZ.getEntity()),  // base quantity unit
										referenceConverter.toManagedEntity(DefaultReference.CCY_GBP.getEntity()), // term currency 
										"Reference", // reference 
										referenceConverter.toManagedEntity(DefaultReference.METAL_FORM_INGOT.getEntity()), // metal form
										referenceConverter.toManagedEntity(DefaultReference.METAL_LOCATION_ROYSTON.getEntity()), // metal location 
										orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_PENDING.getEntity()),  // order status
										new Date(),  // created at
										userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),  // created by
										new Date(), // last update
										userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),  // updated by
										0.0d, // fillPercentage, going to get updated automatically before persisting to database
										referenceConverter.toManagedEntity(DefaultReference.CONTRACT_TYPE_LIMIT_RELATIVE.getEntity()), // contract type
										referenceConverter.toManagedEntity(DefaultReference.TICKER_XIRUSD.getEntity()),	// ticker	
										Arrays.asList(orderCommentConverter.toManagedEntity(TestOrderComment.TEST_COMMENT_1.getEntity())), // order comments
										Arrays.asList(fillConverter.toManagedEntity(TestFill.TEST_LIMIT_ORDER_FILL_2.getEntity())),  // fills
										Arrays.asList(creditCheckConverter.toManagedEntity(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity())), // credit checks
										// << order fields
										new Date(), // settle date
										new Date(),  // concrete start date
										referenceConverter.toManagedEntity(DefaultReference.SYMBOLIC_DATE_1D.getEntity()), // start date symbolic
										2000d,  // limit price
										referenceConverter.toManagedEntity(DefaultReference.PRICE_TYPE_SPOT.getEntity()), // price type
										referenceConverter.toManagedEntity(DefaultReference.STOP_TRIGGER_TYPE_SAMPLE1.getEntity()),  // stop trigger type
										referenceConverter.toManagedEntity(DefaultReference.METAL_XOS.getEntity()), // currency cross metal
										referenceConverter.toManagedEntity(DefaultReference.YES_NO_NO.getEntity()), // part fillable
										referenceConverter.toManagedEntity(DefaultReference.VALIDATION_TYPE_GOOD_TIL_CANCELLED.getEntity()), // validation type
										new Date(), // expiry date
										1.0d) // execution likelihood
					// 2nd test case with only mandatory fields being filled.
					,new LimitOrder (referenceConverter.toManagedEntity(DefaultReference.ORDER_TYPE_LIMIT_ORDER.getEntity()), // order type name
							1,   // version
							partyConverter.toManagedEntity(TestBunit.JM_PMM_UK.getEntity()),  // internal bu 
							partyConverter.toManagedEntity(TestBunit.ANGLO_PLATINUM_MARKETING___BU.getEntity()), // external bu
							partyConverter.toManagedEntity(TestLenit.JM_PLC.getEntity()),  // internal le
							partyConverter.toManagedEntity(TestLenit.ANGLO_PLATINUM_MARKETING___LE.getEntity()), // external le
							referenceConverter.toManagedEntity(DefaultReference.PORTFOLIO_UK_GOLD.getEntity()), // internal portfolio
							null,  // external portfolio
							referenceConverter.toManagedEntity(DefaultReference.BUY_SELL_BUY.getEntity()), // buy / sell
							referenceConverter.toManagedEntity(DefaultReference.METAL_XAU.getEntity()), // base currency
							1000d, // base quantity
							referenceConverter.toManagedEntity(DefaultReference.QUANTITY_TOZ.getEntity()),  // base quantity unit
							referenceConverter.toManagedEntity(DefaultReference.CCY_GBP.getEntity()), // term currency 
							"Reference", // reference
							referenceConverter.toManagedEntity(DefaultReference.METAL_FORM_INGOT.getEntity()), // metal form
							referenceConverter.toManagedEntity(DefaultReference.METAL_LOCATION_ROYSTON.getEntity()), // metal location 
							orderStatusConverter.toManagedEntity(DefaultOrderStatus.LIMIT_ORDER_PENDING.getEntity()),  // order status
							new Date(),  // created at
							userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),  // created by
							new Date(), // last update
							userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),  // updated by
							0.0d, // fillPercentage, going to get updated automatically before persisting to database
							referenceConverter.toManagedEntity(DefaultReference.CONTRACT_TYPE_LIMIT_FIXED.getEntity()),
							referenceConverter.toManagedEntity(DefaultReference.TICKER_XAUEUR.getEntity()),
							Arrays.asList(orderCommentConverter.toManagedEntity(TestOrderComment.TEST_COMMENT_1.getEntity())), // order comments
							Arrays.asList(fillConverter.toManagedEntity(TestFill.TEST_LIMIT_ORDER_FILL_2.getEntity())),  // fills
							Arrays.asList(creditCheckConverter.toManagedEntity(TestCreditCheck.TEST_CREDIT_CHECK_1.getEntity())), // credit checks
							// << order fields
							null, // settle date
							null,  // concrete start date
							null, // start date symbolic
							null,  // limit price
							null, // price type
							null,  // stop trigger type
							null, // currency cross metal
							null, // part fillable
							referenceConverter.toManagedEntity(DefaultReference.VALIDATION_TYPE_GOOD_TIL_CANCELLED.getEntity()), // validation type
							null, // expiry date
							null) // execution likelihood
					);
			list.get(0).setOrderId(1000000l);
			list.get(1).setOrderId(1000001l);
			return list;
		};
	}

	@Override
	protected Function<Order, OrderVersionId> idProvider() {
		return x -> new OrderVersionId(x.getOrderId(), x.getVersion());
	}
}
