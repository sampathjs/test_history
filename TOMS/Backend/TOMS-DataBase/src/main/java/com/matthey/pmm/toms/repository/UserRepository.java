package com.matthey.pmm.toms.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
  List<User> findByEmail(String name);

  List<User> findByFirstName(String firstName);

  List<User> findByLastName(String lastName);
  
  List<User> findByRole(Reference role);
  
  List<User> findByRoleId(long roleId);  

}