package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.transport.ImmutableOrderFillTo;
import com.matthey.pmm.toms.transport.OrderFillTo;

public enum TestOrderFill {
	TEST_LIMIT_ORDER_FILL_1 (1, 100d, 200d, 1234, TestUser.ARINDAM_RAY, TestUser.ARINDAM_RAY, "2000-01-02 16:00:00"),
	TEST_LIMIT_ORDER_FILL_2 (2, 100d, 500d, 5432, TestUser.JENS_WAECHTER, TestUser.NIVEDITH_SAJJA, "2000-01-05 12:00:00"),
	TEST_REFERENCE_ORDER_FILL_1 (3, 100d, 200d, 1234, TestUser.PAT_MCCOURT, TestUser.ARINDAM_RAY, "2000-11-05 08:00:00"),	
	;
	
	private final OrderFillTo orderFill;
	
	private TestOrderFill (int id, double fillQuantity, double fillPrice, int tradeId, 
			TestUser trader, TestUser updatedBy, String lastUpdateDateTime) {
		this.orderFill = ImmutableOrderFillTo.builder()
				.id(id)
				.fillQuantity(fillQuantity)
				.fillPrice(fillPrice)
				.idTrade(tradeId)
				.idTrader(trader.getEntity().id())
				.idUpdatedBy(updatedBy.getEntity().id())
				.lastUpdateDateTime(lastUpdateDateTime)
				.build();
	}

	public OrderFillTo getEntity () {
		return orderFill;
	}
	
	public static List<OrderFillTo> asList () {
		return Arrays.asList(TestOrderFill.values())
				.stream().map(TestOrderFill::getEntity).collect(Collectors.toList());
	}
	
	public static List<OrderFillTo> asListByIds (List<Integer> ids) {
		return Arrays.asList(TestOrderFill.values())
				.stream()
				.map(TestOrderFill::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
}