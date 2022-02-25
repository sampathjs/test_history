package com.matthey.pmm.endur.database.repository;

import com.matthey.pmm.endur.database.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Repository;

@NoRepositoryBean
public interface PortfolioRepository extends JpaRepository<Portfolio, Integer> {
}
