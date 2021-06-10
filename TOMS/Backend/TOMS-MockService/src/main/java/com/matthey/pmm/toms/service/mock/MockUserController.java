package com.matthey.pmm.toms.service.mock;


import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.enums.DefaultUser;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.enums.DefaultReferenceType;

import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.enums.DefaultReference;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import com.matthey.pmm.toms.service.TomsUserService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.service.exception.IllegalReferenceTypeException;
import com.matthey.pmm.toms.service.exception.IllegalReferenceException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
public class MockUserController implements TomsUserService {

	@Override
	@ApiOperation("Retrieval of Users")
	public Set<User> getUser (
			@ApiParam(value = "User ID, 0 or null = all", example = "23113", required = false) @RequestParam(required=false) Integer userId,
			@ApiParam(value = "User Email Address, null = all", example = "jens.waetcher@matthey.com", required = false) @RequestParam(required=false) String email,
			@ApiParam(value = "ID of reference type stating the User Role, 0 or null = all", example = "5", required = false) @RequestParam(required=false) Integer userRoleId) {
		Optional<Reference> reference = null;
		if (userRoleId != null && userRoleId !=  0) {
			reference = DefaultReference.findById(userRoleId);
			if (!reference.isPresent()) {
				throw new IllegalReferenceException(this.getClass(), "getUser", "userRoleId", 
						"(Several)", "Unknown(" + userRoleId + ")");
			}
			Reference ref = reference.get();
			if (ref.typeId() != DefaultReferenceType.UserRole.getEntity().id()) {
				Optional<ReferenceType> refType = DefaultReferenceType.findById (ref.typeId());
				String refTypeName = "Unknown";
				if (refType.isPresent()) {
					refTypeName = refType.get().name();
				}
				throw new IllegalReferenceTypeException(this.getClass(), "getUser", "userRoleId",
						DefaultReferenceType.UserRole.getEntity().name(), refTypeName + "(" + userRoleId + ")");
			} else {
				if (email == null) {
					if (userId == null || userId == 0) {
						return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.roleId() == userRoleId).collect(Collectors.toList()));
					} else {
						return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.id() == userId).collect(Collectors.toList()));
					}
				} else {
					if (userId == null || userId == 0) {
						return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.email().equalsIgnoreCase(email)).collect(Collectors.toList()));
					} else {
						return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.email().equalsIgnoreCase(email) && x.id() == userId).collect(Collectors.toList()));
					}
				}
			}
		} else {
			if (email == null) {
				if (userId == null || userId == 0) {
					return new HashSet<>(DefaultUser.asList());
				} else {
					return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.id() == userId).collect(Collectors.toList()));
				}
			} else {
				if (userId == null || userId == 0) {
					return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.email().equalsIgnoreCase(email)).collect(Collectors.toList()));
				} else {
					return new HashSet<>(DefaultUser.asList().stream().filter(x -> x.email().equalsIgnoreCase(email) && x.id() == userId).collect(Collectors.toList()));
				}
			}
		}
	}
}
