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
				.idOrderTypeCategory(entity.getOrderTypeCategory().getId())
				.sortColumn(entity.getSortColumn())
				.build();
	}

	@Override
	public OrderStatus toManagedEntity(OrderStatusTo to) {
		Reference orderStatus = loadRef(to, to.idOrderStatusName());
		Reference orderTypeName = loadRef(to, to.idOrderTypeName());
		Reference orderTypeCategory = loadRef(to, to.idOrderTypeCategory());
		
		Optional<OrderStatus> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setOrderStatusName(orderStatus);
			existingEntity.get().setOrderType(orderTypeName);
			existingEntity.get().setOrderTypeCategory(orderTypeCategory);
			existingEntity.get().setSortColumn(to.sortColumn());
			return existingEntity.get();
		}
		OrderStatus newEntity = new OrderStatus(orderStatus, orderTypeName, orderTypeCategory, to.sortColumn());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
