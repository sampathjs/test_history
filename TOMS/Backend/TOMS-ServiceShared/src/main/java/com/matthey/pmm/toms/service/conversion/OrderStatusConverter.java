package com.matthey.pmm.toms.service.conversion;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.OrderStatus;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.OrderStatusRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.ReferenceTypeRepository;
import com.matthey.pmm.toms.transport.OrderStatusTo;
import com.matthey.pmm.toms.transport.ImmutableOrderStatusTo;

@Service
public class OrderStatusConverter extends EntityToConverter<OrderStatus, OrderStatusTo> {
	@Autowired
	private ReferenceRepository refRepo;
	
	@Autowired
	private ReferenceTypeRepository refTypeRepo;

	@Autowired
	private OrderStatusRepository entityRepo;

	
	@Override
	public ReferenceTypeRepository refTypeRepo() {
		return refTypeRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public OrderStatusTo toTo(OrderStatus entity) {
		return ImmutableOrderStatusTo.builder()
				.id(entity.getId())
				.idOrderStatusName(entity.getOrderStatusName().getId())
				.idOrderTypeName(entity.getOrderType().getId())
				.build();
	}

	@Override
	public OrderStatus toManagedEntity(OrderStatusTo to) {
		Reference OrderStatus = loadRef(to, to.idOrderStatusName());
		Reference orderTypeName = loadRef(to, to.idOrderTypeName());
		
		Optional<OrderStatus> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setOrderStatusName(OrderStatus);
			existingEntity.get().setOrderType(orderTypeName);
			return existingEntity.get();
		}
		OrderStatus newEntity = new OrderStatus(OrderStatus, orderTypeName);
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
