package com.matthey.pmm.toms.service;

import static com.matthey.pmm.toms.service.TomsService.API_PREFIX;

import java.util.Collection;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = {"Email Service"}, description = "APIs to submit the request to send out emails and to upload attachments")
@RequestMapping(API_PREFIX)
public interface TomsEmailService {
    @ApiOperation("Retrieval of a single database file")
	@GetMapping("/databaseFile/{databaseFileId}")
	public DatabaseFileTo getDatabaseFile (
			@ApiParam(value = "Mandatory ID of the database file", example = "100000l", required = true) @PathVariable(name="databaseFileId", required=true) Long databaseFileId);
    
    @ApiOperation("Post a new file into the database, ID has to be 0, returns the assigned ID")
    @PostMapping("/databaseFile")
	public long postDatabaseFile (@ApiParam(value = "The new database file'", required = true) @RequestBody(required=true) DatabaseFileTo newFile);
    
    @ApiOperation("Retrieval of a single email request")
    @GetMapping("/email/{emailId}")
    public EmailTo getEmailRequest(@ApiParam(value = "Mandatory ID of the email request", example = "100000l", required = true)	@PathVariable(name="emailId", required=true) Long emailId);
    
    @ApiOperation("Retrieval of a all email request for a specified order")
    @GetMapping("/email")
    public Collection<EmailTo> getEmailRequestForOrder(@ApiParam(value = "Mandatory ID of the order the email request has been created for", example = "100000l", required = true) @RequestParam(name="orderId", required=true) Long orderId);

    @ApiOperation("Creation of a new email request")
    @PostMapping("/email")
    public long postEmailRequest(@ApiParam(value = "New email request, ID has to be 0, returns the assigned ID", required = true) @RequestBody(required=true) EmailTo emailRequest);

    @ApiOperation("Update of a new email request")
    @PutMapping("/email/{emailId}")
    public void updateEmailRequest(@ApiParam(value = "Mandatory ID of the email request, has to match the ID of the email request in the body", example = "100000l", required = true) @PathVariable(name="emailId", required=true) long emailId,
    		@ApiParam(value = "Updated existing email request", required = true) @RequestBody(required=true) EmailTo emailRequest);
}
