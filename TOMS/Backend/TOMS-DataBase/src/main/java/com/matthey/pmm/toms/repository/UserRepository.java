package com.matthey.pmm.toms.repository;

import java.util.Set;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
  Set<User> findByEmail(String email);

  Set<User> findByEmailAndRole(String name, Reference role);

  Set<User> findByIdAndEmailAndRole(Long id, String name, Reference role);
  
  Set<User> findByIdAndRole(Long id, Reference role);

  Set<User> findByIdAndEmail(Long id, String email);
  
  Set<User> findByFirstName(String firstName);

  Set<User> findByLastName(String lastName);
  
  Set<User> findByRole(Reference role);
  
  Set<User> findByRoleId(long roleId);  

}