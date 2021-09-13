package com.matthey.pmm.toms.repository;

import java.util.Date;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.CacheInvalidate;

@Repository
public interface CacheInvalidateRepository extends CrudRepository<CacheInvalidate, Long> {
	public Set<CacheInvalidate> findByCutOffDateTimeAfter(Date cutOffDateTime);
	
	@Query("SELECT DISTINCT ci.cacheCategory.id FROM CacheInvalidate ci") 
	public Set<Long> findDistinctCacheCategoryId();

}