package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableCreditCheckTo;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.CreditCheckTo;

public enum TestCreditCheck {
	TEST_CREDIT_CHECK_1 (100000, TestLenit.ANGLO_PLATINUM_MARKETING___LE, 1000000d, 500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_OUTCOME_PASSED),
	TEST_CREDIT_CHECK_2 (100001, TestLenit.ANGLO_PLATINUM_MARKETING___LE, null, null, 
			null, DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, null),
	TEST_CREDIT_CHECK_3 (100002, TestLenit.ANGLO_PLATINUM_MARKETING___LE, null, null, 
			null, DefaultReference.CREDIT_CHECK_RUN_STATUS_FAILED, null),
	TEST_CREDIT_CHECK_4 (100003, TestLenit.JM_PLC, 1000000d, 1500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_FAILED),
	TEST_CREDIT_CHECK_5 (100004, TestLenit.JM_PLC, 12345d, 500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_PASSED),
	TEST_CREDIT_CHECK_6 (100005, TestLenit.ANGLO_PLATINUM_MARKETING___LE, 100d, 200d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_FAILED),
	TEST_CREDIT_CHECK_7 (100006, TestLenit.ANGLO_PLATINUM_MARKETING___LE, 200d, 100d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_COMPLETED, DefaultReference.CREDIT_CHECK_OUTCOME_PASSED),
	TEST_CREDIT_CHECK_8 (100007, TestLenit.ANGLO_PLATINUM_MARKETING___LE, 1000000d, 500000d, 
			"2000-01-15 16:00:00", DefaultReference.CREDIT_CHECK_RUN_STATUS_OPEN, DefaultReference.CREDIT_CHECK_OUTCOME_PASSED),
	;
	private CreditCheckTo orderCreditCheck;
	
	private TestCreditCheck (long id, TestLenit party, Double creditLimit, Double currentUtilization,
			String runDateTime, DefaultReference creditCheckRunStatus, DefaultReference creditCheckOutcome) {
		this.orderCreditCheck = ImmutableCreditCheckTo.builder()
				.id(id)
				.idParty(party.getEntity().id())
				.creditLimit(creditLimit!=null?creditLimit:null)
				.currentUtilization(currentUtilization!=null?currentUtilization:null)
				.runDateTime(runDateTime!=null?runDateTime:null)
				.idCreditCheckRunStatus(creditCheckRunStatus.getEntity().id())
				.idCreditCheckOutcome(creditCheckOutcome!=null?creditCheckOutcome.getEntity().id():null)
				.build();
	}

	public CreditCheckTo getEntity () {
		return orderCreditCheck;
	}
	
	public void setEntity(CreditCheckTo orderCreditCheck) {
		this.orderCreditCheck = orderCreditCheck;
	}

	public static List<CreditCheckTo> asList () {
		return Arrays.asList(TestCreditCheck.values())
				.stream().map(TestCreditCheck::getEntity).collect(Collectors.toList());
	}
	
	public static List<CreditCheckTo> asListByIds (List<Long> ids) {
		return Arrays.asList(TestCreditCheck.values())
				.stream()
				.map(TestCreditCheck::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
	
	public static List<TestCreditCheck> asEnumList () {
		return Arrays.asList(TestCreditCheck.values())
				.stream().collect(Collectors.toList());
	}
}