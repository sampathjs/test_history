package com.matthey.pmm.toms.service.mock;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.DefaultReferenceType;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.TomsUserService;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.transport.UserTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
public class MockUserController implements TomsUserService {
	@Override
	@ApiOperation("Retrieval of Users")
	public Set<UserTo> getUser (
			@ApiParam(value = "User ID, 0 or null = all", example = "23113", required = false) @RequestParam(required=false) Integer userId,
			@ApiParam(value = "User Email Address, null = all", example = "jens.waetcher@matthey.com", required = false) @RequestParam(required=false) String email,
			@ApiParam(value = "ID of reference type stating the User Role, 0 or null = all", example = "5", required = false) @RequestParam(required=false) Integer userRoleId) {
		if (TomsService.verifyDefaultReference (userRoleId,
				Arrays.asList(DefaultReferenceType.USER_ROLE),
				this.getClass(), "getUser", "userRoleId", false)) {
			if (email == null) {
				if (userId == null || userId == 0) {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.roleId() == userRoleId).collect(Collectors.toList()));
				} else {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.id() == userId).collect(Collectors.toList()));
				}
			} else {
				if (userId == null || userId == 0) {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.email().equalsIgnoreCase(email)).collect(Collectors.toList()));
				} else {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.roleId() == userRoleId && x.email().equalsIgnoreCase(email) && x.id() == userId).collect(Collectors.toList()));
				}
			}			
		} else {
			if (email == null) {
				if (userId == null || userId == 0) {
					return new HashSet<>(TestUser.asList());
				} else {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.id() == userId).collect(Collectors.toList()));
				}
			} else {
				if (userId == null || userId == 0) {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.email().equalsIgnoreCase(email)).collect(Collectors.toList()));
				} else {
					return new HashSet<>(TestUser.asList().stream().filter(x -> x.email().equalsIgnoreCase(email) && x.id() == userId).collect(Collectors.toList()));
				}
			}
		}
	}
}
