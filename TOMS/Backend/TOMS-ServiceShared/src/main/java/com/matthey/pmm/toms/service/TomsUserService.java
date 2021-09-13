package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.UserTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"User Data"}, description = "APIs for relevant User data")
@RequestMapping(API_PREFIX)
public interface TomsUserService {
    @Cacheable({"User"})
    @ApiOperation("User Data Retrieval")
	@GetMapping("/user")
	public Set<UserTo> getUser (
			@ApiParam(value = "User ID, 0 or null = all", example = "20046", required = false) @RequestParam(required=false) Long userId,
			@ApiParam(value = "User Email Address, null = all", example = "jens.waetcher@matthey.com", required = false) @RequestParam(required=false) String email,
			@ApiParam(value = "ID of reference stating the User Role, 0 or null = all", example = "3", required = false) @RequestParam(required=false) Long userRoleId);
}
