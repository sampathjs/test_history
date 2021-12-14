package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;

public enum TestReferenceOrderLeg {
	TEST_LEG_1(100000l, "2000-04-15 16:00:00", "2000-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_2(100001l, "2001-04-15 16:00:00", "2001-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, null, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_3(100002l, "2002-04-15 16:00:00", "2002-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_4(100003l, "2003-04-15 16:00:00", null, DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_5(100004l, null, "2004-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_6(100005l, "2005-04-15 16:00:00", "2005-05-30 08:00:00", null,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_7(100006l, "2006-04-15 16:00:00", "2006-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, null, DefaultReference.REF_SOURCE_CITIBANK),
	TEST_LEG_8(100007l, "2007-04-15 16:00:00", "2007-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, DefaultReference.REF_SOURCE_COMEX, null),
	TEST_LEG_9(100008l, "2008-04-15 16:00:00", "2008-05-30 08:00:00", DefaultReference.SYMBOLIC_DATE_1D,
			30.0d, DefaultReference.CCY_USD, null, null),
	;
	
	private ReferenceOrderLegTo referenceOrderLeg;
	
	private TestReferenceOrderLeg (long referenceOrderLegId, 
			String fixingStartDate, String fixingEndDate, DefaultReference paymentOffset,
			Double notional, DefaultReference settleCurrency, DefaultReference refSource,
			DefaultReference fxIndexRefSource) {
		// order type has to be limit order always
		referenceOrderLeg = ImmutableReferenceOrderLegTo.builder()
				.id(referenceOrderLegId)
				.fixingStartDate(fixingStartDate)
				.fixingEndDate(fixingEndDate)
				.idPaymentOffset(paymentOffset != null?paymentOffset.getEntity().id():null)
				.notional(notional)
				.idSettleCurrency(settleCurrency != null?settleCurrency.getEntity().id():null)
				.idRefSource(refSource != null?refSource.getEntity().id():null)
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
