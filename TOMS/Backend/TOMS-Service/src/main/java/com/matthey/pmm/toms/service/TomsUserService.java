package com.matthey.pmm.toms.service;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.cache.annotation.Cacheable;

import com.matthey.pmm.toms.model.ReferenceType;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

@Api(tags = {"User Data"}, description = "APIs for relevant User data")
@RequestMapping(API_PREFIX)
public interface TomsUserService {
    @Cacheable({"User"})
    @ApiOperation("User Data Retrieval")
	@GetMapping("/user")
	public Set<User> getUser (
			@ApiParam(value = "User ID, 0 or null = all", example = "20046", required = false) @RequestParam(required=false) Integer userId,
			@ApiParam(value = "User Email Address, null = all", example = "jens.waetcher@matthey.com", required = false) @RequestParam(required=false) String email,
			@ApiParam(value = "ID of reference type stating the User Role, 0 or null = all", example = "3", required = false) @RequestParam(required=false) Integer userRoleId);
}
