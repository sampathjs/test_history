package com.matthey.pmm.toms.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Fill;

@Repository
@Transactional
public interface FillRepository extends JpaRepository<Fill, Long> {
  Optional<Fill> findByTradeId(long tradeId);
  
  Optional<Fill> findTopByOrderByTradeIdDesc ();
}