package com.matthey.pmm.toms.service.impl;


import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.service.TomsUserService;
import com.matthey.pmm.toms.service.common.Validator;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.transport.UserTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public abstract class UserControllerImpl implements TomsUserService {
	@Autowired
	protected Validator validator;
	
	@Autowired
	protected UserRepository userRepo;
	
	@Autowired
	protected ReferenceRepository refRepo;
	
	@Autowired
	protected UserConverter userConverter;
	
	@Override
	@ApiOperation("Retrieval of Users")
	public Set<UserTo> getUser (
			@ApiParam(value = "User ID, 0 or null = all", example = "23113", required = false) @RequestParam(required=false) Long userId,
			@ApiParam(value = "User Email Address, null = all", example = "jens.waetcher@matthey.com", required = false) @RequestParam(required=false) String email,
			@ApiParam(value = "ID of reference stating the User Role, 0 or null = all", example = "5", required = false) @RequestParam(required=false) Long userRoleId) {
		Optional<Reference> userRole = 
				validator.verifyDefaultReference(userRoleId, Arrays.asList(DefaultReferenceType.USER_ROLE), this.getClass(), "getUser", "userRoleId", true);
		
		if (userRole.isPresent()) {
			if (email == null) {
				if (userId == null || userId == 0) {
					return userRepo.findByRole(userRole.get()).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				} else {
					return userRepo.findByIdAndRole(userId, userRole.get()).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				}
			} else {
				if (userId == null || userId == 0) {
					return userRepo.findByEmailAndRole(email, userRole.get()).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				} else {
					return userRepo.findByIdAndEmailAndRole(userId, email, userRole.get()).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				}
			}			
		} else {
			if (email == null) {
				if (userId == null || userId == 0) {
					return StreamSupport.stream(userRepo.findAll().spliterator(), false)
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				} else {
					return userRepo.findById(userId).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				}
			} else {
				if (userId == null || userId == 0) {
					return userRepo.findByEmail(email).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				} else {
					return userRepo.findByIdAndEmail(userId, email).stream()
							.map(x -> userConverter.toTo(x))
							.collect(Collectors.toSet());
				}
			}
		}
	}
}
