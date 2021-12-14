package com.matthey.pmm.toms.repository;

import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.matthey.pmm.toms.model.Party;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Long> {
  Set<User> findByEmail(String email);

  Set<User> findByEmailAndRole(String name, Reference role);

  Set<User> findByIdAndEmailAndRole(Long id, String name, Reference role);
  
  Set<User> findByIdAndRole(Long id, Reference role);

  Set<User> findByIdAndEmail(Long id, String email);
  
  Set<User> findByFirstName(String firstName);

  Set<User> findByLastName(String lastName);
  
  Set<User> findByRole(Reference role);
  
  Set<User> findByRoleId(long roleId);  
  
  @Query("SELECT tp.id  FROM User u JOIN u.tradeableParties tp WHERE u.id = :userId") 
  Set<Long> findTradeablePartiesIdById (@Param("userId") long userId);

  @Query("SELECT tp.id  FROM User u JOIN u.tradeablePortfolios tp WHERE  u.id = :userId") 
  Set<Long> findTradeablePortfolioIdById (@Param("userId") long userId);
  
  @Query("SELECT u.tradeableParties  FROM User u WHERE u.id = :userId") 
  List<Party> findTradeablePartiesById (@Param("userId") long userId);

  @Query("SELECT u.tradeablePortfolios  FROM User u WHERE u.id = :userId") 
  List<Reference> findTradeablePortfolioById (@Param("userId") long userId);
}