package com.matthey.pmm.toms.service.impl;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.matthey.pmm.toms.enums.v1.DefaultOrderStatus;
import com.matthey.pmm.toms.enums.v1.DefaultReferenceType;
import com.matthey.pmm.toms.model.CacheInvalidate;
import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Email;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.ReferenceOrder;
import com.matthey.pmm.toms.repository.CacheInvalidateRepository;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.EmailRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.service.TomsCacheService;
import com.matthey.pmm.toms.service.TomsEmailService;
import com.matthey.pmm.toms.service.TomsService;
import com.matthey.pmm.toms.service.common.TomsValidator;
import com.matthey.pmm.toms.service.conversion.DatabaseFileConverter;
import com.matthey.pmm.toms.service.conversion.EmailConverter;
import com.matthey.pmm.toms.service.exception.IllegalDateFormatException;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableEmailTo;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Transactional
public abstract class EmailServiceImpl implements TomsEmailService {    
    @Autowired
    protected EmailRepository emailRepo;

    @Autowired
    protected DatabaseFileRepository dbFileRepo;

    @Autowired
    protected DatabaseFileConverter dbFileConverter;
    
    @Autowired
    protected EmailConverter emailConverter;
    
    @Autowired
    protected TomsValidator validator;    

    @ApiOperation("Retrieval of a single database file")
	public DatabaseFileTo getDatabaseFile (
			@ApiParam(value = "Mandatory ID of the database file", example = "100000", required = true) @PathVariable(name="databaseFileId", required=true) Long databaseFileId) {
    	Optional<DatabaseFile> databaseFile = validator.verifyDatabaseFileId (this.getClass(), "getDatabaseFile", "databaseFileId", databaseFileId, false);
   		return dbFileConverter.toTo(databaseFile.get());
    }
    
    @ApiOperation("Post a new file into the database, ID has to be 0, returns the assigned ID")
	public long postDatabaseFile (@ApiParam(value = "The new database file'", required = true) @RequestBody(required=true) DatabaseFileTo newFile) {
    	validator.validateDatabaseFileFields (this.getClass(), "postDatabaseFile", "newFile", null, newFile, true);
    	
    	Date now = new Date();
    	SimpleDateFormat sdf = new SimpleDateFormat(TomsService.DATE_TIME_FORMAT);
    	newFile = ImmutableDatabaseFileTo.builder()
    			.from(newFile)
    			.createdAt(sdf.format(now))
    			.lastUpdate(sdf.format(now))    			
    			.build();
    	
		DatabaseFile managedEntity = dbFileConverter.toManagedEntity(newFile);
    	return managedEntity.getId();	
    }
    
    @ApiOperation("Retrieval of a single email request")
    public EmailTo getEmailRequest(@ApiParam(value = "Mandatory ID of the email request", example = "100000", required = true)	@PathVariable(name="emailId", required=true) Long emailId) {
    	Optional<Email> email = validator.validateEmailId (this.getClass(), "getEmailRequest", "emailId", emailId, false);
    	return emailConverter.toTo(email.get());
    }
    
    @ApiOperation("Retrieval of a all email request for a specified order")
    public Collection<EmailTo> getEmailRequestForOrder(@ApiParam(value = "Mandatory ID of the order the email request has been created for", example = "100000", required = true) @RequestParam(name="orderId", required=true) Long orderId) {
    	Collection<Email> emails = emailRepo.findEmailsBelongingToOrderId(orderId);
    	Collection<EmailTo> asTos = emails.stream()
    			.map(x -> emailConverter.toTo(x))
    			.collect(Collectors.toSet());
    	return asTos;
    }

    @ApiOperation("Creation of a new email request")
    public long postEmailRequest(@ApiParam(value = "New email request, ID has to be 0, returns the assigned ID", required = true) @RequestBody(required=true) EmailTo emailRequest) {
    	validator.validateEmailFields (this.getClass(), "postEmailRequest", "emailRequest", null, emailRequest, true);
    	
    	SimpleDateFormat sdf = new SimpleDateFormat(TomsService.DATE_TIME_FORMAT);
    	
    	Date now = new Date();
    	emailRequest = ImmutableEmailTo.builder()
    			.from(emailRequest)
    			.lastUpdate(sdf.format(now))
    			.createdAt(sdf.format(now))
    			.build();
    	
		Email managedEntity = emailConverter.toManagedEntity(emailRequest);
    	return managedEntity.getId();	
    }

    @ApiOperation("Update of a new email request")
    public void updateEmailRequest(@ApiParam(value = "Mandatory ID of the email request, has to match the ID of the email request in the body", example = "100000", required = true) @PathVariable(name="emailId", required=true) long emailId,
    		@ApiParam(value = "Updated existing email request", required = true) @RequestBody(required=true) EmailTo emailRequest) {
    	Email oldEmail = validator.validateEmailId (this.getClass(), "getEmailRequest", "emailId", emailId, false).get();
    	
    	validator.validateEmailFields (this.getClass(), "postEmailRequest", "emailRequest", oldEmail, emailRequest, false);
    	
    	Date now = new Date();
    	SimpleDateFormat sdf = new SimpleDateFormat(TomsService.DATE_TIME_FORMAT);
    	emailRequest = ImmutableEmailTo.builder()
    			.from(emailRequest)
    			.lastUpdate(sdf.format(now))
    			.build();
    	
		Email managedEntity = emailConverter.toManagedEntity(emailRequest);
    }
}
