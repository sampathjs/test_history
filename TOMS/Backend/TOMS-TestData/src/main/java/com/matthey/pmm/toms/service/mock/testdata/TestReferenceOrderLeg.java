package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;

public enum TestReferenceOrderLeg {
	TEST_LEG_1(100000l, "2000-04-15", "2000-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_GBP, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_2(100001l, "2001-04-15", "2001-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_GBP, DefaultReference.REF_SOURCE_JM_NY_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_3(100002l, "2002-04-15", "2002-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_NY_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_4(100003l, "2003-04-15", null, "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_NY_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_5(100004l, "2004-05-15", "2004-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_CLOSING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_6(100005l, "2005-04-15", "2005-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_CLOSING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_7(100006l, "2006-04-15", "2006-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_NY_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_8(100007l, "2007-04-15", "2007-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_9(100008l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_10(100009l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_11(100010l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_12(100011l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_13(100012l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_14(100013l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_LEG_NOT_IN_ANY_ORDER(100014l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	TEST_FOR_LEG_DELETION(100015l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),	
	MAIN_LEG_FOR_LEG_DELETION(100016l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),
	MAIN_LEG_FOR_LEG_DELETION_ALL_LEGS(100017l, "2008-04-15", "2008-05-30", "2002-02-27",
			30.0d, DefaultReference.CCY_EUR, DefaultReference.REF_SOURCE_JM_HK_OPENING, DefaultReference.REF_SOURCE_BFIX_1400),			
	;
	
	private ReferenceOrderLegTo referenceOrderLeg;
	
	private TestReferenceOrderLeg (long referenceOrderLegId, 
			String fixingStartDate, String fixingEndDate, String paymentDate,
			Double notional, DefaultReference settleCurrency, DefaultReference refSource,
			DefaultReference fxIndexRefSource) {
		// order type has to be limit order always
		referenceOrderLeg = ImmutableReferenceOrderLegTo.builder()
				.id(referenceOrderLegId)
				.fixingStartDate(fixingStartDate)
				.fixingEndDate(fixingEndDate)
				.paymentDate(paymentDate)
				.notional(notional)
				.idSettleCurrency(settleCurrency != null?settleCurrency.getEntity().id():null)
				.idRefSource(refSource.getEntity().id())
				.idFxIndexRefSource (fxIndexRefSource != null?fxIndexRefSource.getEntity().id():null)
				.build();
	}

	public ReferenceOrderLegTo getEntity () {
		return referenceOrderLeg;
	}

	public void setEntity (ReferenceOrderLegTo referenceOrderLeg) {
		this.referenceOrderLeg = referenceOrderLeg;
	}
	
	public static List<ReferenceOrderLegTo> asList () {
		return Arrays.asList(TestReferenceOrderLeg.values())
				.stream().map(TestReferenceOrderLeg::getEntity).collect(Collectors.toList());
	}	
	
	public static List<TestReferenceOrderLeg> asEnumList () {
		return Arrays.asList(TestReferenceOrderLeg.values())
				.stream().collect(Collectors.toList());
	}	
}
