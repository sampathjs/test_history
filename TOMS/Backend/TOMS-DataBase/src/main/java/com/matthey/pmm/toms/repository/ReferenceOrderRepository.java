package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.OrderVersionId;
import com.matthey.pmm.toms.model.ReferenceOrder;

@Repository
@Transactional
public interface ReferenceOrderRepository  extends PagingAndSortingRepository<ReferenceOrder, OrderVersionId>, JpaSpecificationExecutor<ReferenceOrder> {
   List<ReferenceOrder> findByOrderId(long orderId);

   @Query("SELECT o FROM ReferenceOrder o WHERE o.orderId = :orderId AND o.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = :orderId)") 
   Optional<ReferenceOrder> findLatestByOrderId(@Param("orderId")long orderId);
  // https://docs.spring.io/spring-data/rest/docs/2.0.0.M1/reference/html/paging-chapter.html
   
   @Query("SELECT o FROM ReferenceOrder o WHERE o.version = (SELECT MAX(ro2.version) FROM ReferenceOrder ro2 WHERE ro2.orderId = o.orderId)") 
   List<ReferenceOrder> findLatest();
}