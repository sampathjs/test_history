package com.matthey.pmm.toms.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderComment;

@Repository
public interface OrderCommentRepository extends CrudRepository<OrderComment, Long> {

}