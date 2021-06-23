package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableOrderCreditCheckTo;
import com.matthey.pmm.toms.transport.OrderCreditCheckTo;

public enum TestOrderCreditCheck {
	TEST_CREDIT_CHECK_1 (1, TestParty.ANGLO_PLATINUM_BU, 1000000d, 500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_PASSED),
	TEST_CREDIT_CHECK_2 (2, TestParty.ANGLO_PLATINUM_BU, null, null, 
			null, DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, null),
	TEST_CREDIT_CHECK_3 (3, TestParty.ANGLO_PLATINUM_BU, null, null, 
			null, DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED, null),
	TEST_CREDIT_CHECK_4 (4, TestParty.ANGLO_PLATINUM_BU, 1000000d, 1500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_FAILED),
	;
	private OrderCreditCheckTo orderCreditCheck;
	
	private TestOrderCreditCheck (int id, TestParty party, Double creditLimit, Double currentUtilization,
			String runDateTime, DefaultReference creditCheckRunStatus, DefaultReference creditCheckOutcome) {
		this.orderCreditCheck = ImmutableOrderCreditCheckTo.builder()
				.id(id)
				.idParty(party.getEntity().id())
				.creditLimit(creditLimit!=null?creditLimit:null)
				.currentUtilization(currentUtilization!=null?currentUtilization:null)
				.runDateTime(runDateTime!=null?runDateTime:null)
				.idCreditCheckRunStatus(creditCheckRunStatus.getEntity().id())
				.idCreditCheckOutcome(creditCheckOutcome!=null?creditCheckOutcome.getEntity().id():null)
				.build();
	}

	public OrderCreditCheckTo getEntity () {
		return orderCreditCheck;
	}
	
	public void setEntity(OrderCreditCheckTo orderCreditCheck) {
		this.orderCreditCheck = orderCreditCheck;
	}

	public static List<OrderCreditCheckTo> asList () {
		return Arrays.asList(TestOrderCreditCheck.values())
				.stream().map(TestOrderCreditCheck::getEntity).collect(Collectors.toList());
	}
	
	public static List<OrderCreditCheckTo> asListByIds (List<Integer> ids) {
		return Arrays.asList(TestOrderCreditCheck.values())
				.stream()
				.map(TestOrderCreditCheck::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
	
	public static List<TestOrderCreditCheck> asEnumList () {
		return Arrays.asList(TestOrderCreditCheck.values())
				.stream().collect(Collectors.toList());
	}
}