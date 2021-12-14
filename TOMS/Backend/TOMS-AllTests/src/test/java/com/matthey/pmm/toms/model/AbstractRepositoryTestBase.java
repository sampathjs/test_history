package com.matthey.pmm.toms.model;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class AbstractRepositoryTestBase <E, ID, R extends CrudRepository<E, ID>> {	
	protected abstract Supplier<List<E>> listProvider ();

	protected abstract Function<E, ID> idProvider ();

	
	@Autowired 
	protected R repo;
	
	protected List<E> persistedEntities = persistedEntities = new ArrayList<>();
;

   @Before
   public void initTestFramework () {
	   persistedEntities = new ArrayList<>();
   }

   @After
   public void deletePersistedEntites () {
	   for (E pesistedEntity : persistedEntities) {
		   repo.delete(pesistedEntity);
	   }
   }
   
   @Test
   public void testSavingAllEntities () {

	    for (E toBePersisted : listProvider().get()) {
		    long count = repo.count();
	    	long expectedCount = 0;
	    	ID id = idProvider().apply(toBePersisted);
	    	Optional<E> existingEntity = id != null?repo.findById(id):Optional.empty();
	    	if (existingEntity.isPresent()) {
	    		 expectedCount = count; 
	    	} else {
	    		expectedCount = count+1;
	    	}
	    	E persisted = repo.save(toBePersisted);
	    	if (existingEntity.isEmpty()) {
		    	persistedEntities.add(persisted);	    		
	    	}
	    	long newCount = repo.count();
	    	assertThat(newCount).isEqualTo(expectedCount);
	    }
   }
   
   @Test
   public void testSavingAndLoadingAllEntities () {
	    for (E toBePersisted : listProvider().get()) {
	    	ID id = idProvider().apply(toBePersisted);
	    	Optional<E> existingEntity = id != null?repo.findById(id):Optional.empty();
	    	E persisted = repo.save(toBePersisted);
	    	if (existingEntity.isEmpty()) {
		    	persistedEntities.add(persisted);	    		
	    	}
	    	assertThat(repo.findById(idProvider().apply(persisted)).isPresent()).isTrue();
	    }
   }

}
