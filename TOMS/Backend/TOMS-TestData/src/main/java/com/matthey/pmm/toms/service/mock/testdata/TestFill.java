package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableFillTo;
import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.FillTo;

public enum TestFill {
	TEST_LIMIT_ORDER_FILL_1 (100000, 100d, 200d, 1001000, TestUser.ARINDAM_RAY, TestUser.ARINDAM_RAY, "2000-01-02 16:00:00", DefaultReference.FILL_STATUS_OPEN, null),
	TEST_LIMIT_ORDER_FILL_2 (100001, 100d, 500d, 1002000, TestUser.JENS_WAECHTER, TestUser.NIVEDITH_SAJJA, "2000-01-05 12:00:00", DefaultReference.FILL_STATUS_FAILED, "Error"),
	TEST_REFERENCE_ORDER_FILL_1 (100002, 100d, 200d, 1003000, TestUser.PAT_MCCOURT, TestUser.ARINDAM_RAY, "2000-11-05 08:00:00", DefaultReference.FILL_STATUS_COMPLETED, null),	
	;
	
	private final FillTo orderFill;
	
	private TestFill (long id, double fillQuantity, double fillPrice, long tradeId, 
			TestUser trader, TestUser updatedBy, String lastUpdateDateTime,
			DefaultReference fillStatus, String errorMessage) {
		this.orderFill = ImmutableFillTo.builder()
				.id(id)
				.fillQuantity(fillQuantity)
				.fillPrice(fillPrice)
				.idTrade(tradeId)
				.idTrader(trader.getEntity().id())
				.idUpdatedBy(updatedBy.getEntity().id())
				.lastUpdateDateTime(lastUpdateDateTime)
				.idFillStatus(fillStatus.getEntity().id())
				.errorMessage(errorMessage)
				.build();
	}

	public FillTo getEntity () {
		return orderFill;
	}
	
	public static List<FillTo> asList () {
		return Arrays.asList(TestFill.values())
				.stream().map(TestFill::getEntity).collect(Collectors.toList());
	}
	
	public static List<FillTo> asListByIds (List<Long> ids) {
		return Arrays.asList(TestFill.values())
				.stream()
				.map(TestFill::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
}