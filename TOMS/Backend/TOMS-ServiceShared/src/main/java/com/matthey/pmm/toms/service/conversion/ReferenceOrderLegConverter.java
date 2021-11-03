package com.matthey.pmm.toms.service.conversion;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrderLeg;
import com.matthey.pmm.toms.repository.ReferenceOrderLegRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderLegTo;
import com.matthey.pmm.toms.transport.ReferenceOrderLegTo;

@Service
public class ReferenceOrderLegConverter extends EntityToConverter<ReferenceOrderLeg, ReferenceOrderLegTo> {
	@Autowired
	private ReferenceRepository refRepo;

	@Autowired
	private ReferenceOrderLegRepository entityRepo;
		
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
		
	@Override
	public ReferenceOrderLegTo toTo (ReferenceOrderLeg entity) {
		return ImmutableReferenceOrderLegTo.builder()
				.id(entity.getId())
				.idFxIndexRefSource(entity.getFxIndexRefSource() != null?entity.getFxIndexRefSource().getId():null)
				.idPaymentOffset(entity.getPaymentOffset() != null?entity.getPaymentOffset().getId():null)
				.idRefSource(entity.getRefSource() != null?entity.getRefSource().getId():null)
				.idSettleCurrency(entity.getSettleCurrency() != null?entity.getSettleCurrency().getId():null)
				.fixingEndDate(entity.getFixingEndDate() != null?formatDate(entity.getFixingEndDate()):null)
				.fixingStartDate(entity.getFixingStartDate() != null?formatDate(entity.getFixingStartDate()):null)
				.notional(entity.getNotional())
				.build();
	}
	
	@Override
	public ReferenceOrderLeg toManagedEntity (ReferenceOrderLegTo to) {
		Reference fxIndexRefSource = to.idFxIndexRefSource() != null?loadRef(to, to.idFxIndexRefSource()):null;
		Reference paymentOffset = to.idPaymentOffset() != null?loadRef (to, to.idPaymentOffset()):null;
		Reference refSource = to.idRefSource() != null?loadRef (to, to.idRefSource()):null;
		Reference settleCurrency = to.idSettleCurrency() != null?loadRef(to, to.idSettleCurrency()):null;
		Date fixingEndDate = to.fixingEndDate() != null?parseDate(to, to.fixingEndDate()):null;
		Date fixingStartDate = to.fixingStartDate() != null?parseDate(to, to.fixingStartDate()):null;
		
		Optional<ReferenceOrderLeg> entity = entityRepo.findById(to.id());
		if (entity.isPresent()) {
			entity.get().setFixingEndDate(fixingEndDate);
			entity.get().setFixingStartDate(fixingStartDate);
			entity.get().setFxIndexRefSource(fxIndexRefSource);
			entity.get().setNotional(to.notional());
			entity.get().setPaymentOffset(paymentOffset);
			entity.get().setRefSource(refSource);
			entity.get().setSettleCurrency(settleCurrency);
			return entity.get();
		}
		ReferenceOrderLeg newEntity = new ReferenceOrderLeg (fixingStartDate, fixingEndDate, paymentOffset, to.notional(), settleCurrency, refSource, fxIndexRefSource);
		newEntity.setId(to.id());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
