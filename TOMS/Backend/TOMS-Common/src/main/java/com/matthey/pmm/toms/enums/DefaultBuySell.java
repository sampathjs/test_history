package com.matthey.pmm.toms.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.model.ImmutableBuySell;
import com.matthey.pmm.toms.model.BuySell;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceType;

public enum DefaultBuySell {
		BUY (0, DefaultReference.BUY_SELL_BUY.getEntity()),
		SELL (1, DefaultReference.BUY_SELL_SELL.getEntity())
		;
	;
	
	private final BuySell buySell;
	
	private DefaultBuySell (int id, Reference buySellRef) {
		this.buySell = ImmutableBuySell.builder()
				.id(id)
				.idBuySellReference(buySellRef.id())
				.build();
	}

	public BuySell getEntity () {
		return buySell;
	}

	public static List<BuySell> asList () {
		return Arrays.asList(DefaultBuySell.values())
				.stream().map(DefaultBuySell::getEntity).collect(Collectors.toList());
	}
}
