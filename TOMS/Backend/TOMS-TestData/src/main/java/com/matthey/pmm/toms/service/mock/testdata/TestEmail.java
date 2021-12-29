package com.matthey.pmm.toms.service.mock.testdata;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.ImmutableEmailTo;
import com.matthey.pmm.toms.transport.OrderTo;

public enum TestEmail {
		TEST_EMAIL1(100000l, TestUser.JENS_WAECHTER, "Subject", "<HTML>Email Body Text</HTML>", // id, sendAs, Subject, Body 
				Arrays.asList("jens.waetcher@matthey.com", "Murali.Krishnan@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList("Nivedith.Sajja3@matthey.com"), //bcc
				Arrays.asList(TestDatabaseFile.TEST_DATABASE_FILE_1, TestDatabaseFile.TEST_DATABASE_FILE_2), // attachments
				DefaultReference.EMAIL_STATUS_SUBMITTED,  // email status
				null, // error message
				10, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JENS_WAECHTER, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.JENS_WAECHTER, // updated by
				Arrays.asList(TestLimitOrder.TEST_ORDER_1B.getEntity(), TestReferenceOrder.TEST_ORDER_1B.getEntity())),				
		TEST_EMAIL2(100001l, TestUser.JACOB_SMITH, "Order #12345", "Plain Text Body", // id, sendAs, Subject, Body 
				Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList(), //bcc
				Arrays.asList(), // attachments
				DefaultReference.EMAIL_STATUS_SENT_SUCCESS,  // email status
				null, // error message
				9, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JACOB_SMITH, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.SERVICE_USER, // updated by
				Arrays.asList()),
		TEST_EMAIL_SUBMITTED(100002l, TestUser.JACOB_SMITH, "Order #12345", "Plain Text Body", // id, sendAs, Subject, Body 
				Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList(), //bcc
				Arrays.asList(), // attachments
				DefaultReference.EMAIL_STATUS_SUBMITTED,  // email status
				null, // error message
				9, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JACOB_SMITH, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.SERVICE_USER, // updated by
				Arrays.asList()),		
		TEST_EMAIL_SENDING(100003l, TestUser.JACOB_SMITH, "Order #12345", "Plain Text Body", // id, sendAs, Subject, Body 
				Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList(), //bcc
				Arrays.asList(), // attachments
				DefaultReference.EMAIL_STATUS_SENDING,  // email status
				null, // error message
				9, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JACOB_SMITH, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.SERVICE_USER, // updated by
				Arrays.asList()),			
		TEST_EMAIL_SENT_SUCCESS(100004l, TestUser.JACOB_SMITH, "Order #12345", "Plain Text Body", // id, sendAs, Subject, Body 
				Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList(), //bcc
				Arrays.asList(), // attachments
				DefaultReference.EMAIL_STATUS_SENT_SUCCESS,  // email status
				null, // error message
				9, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JACOB_SMITH, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.SERVICE_USER, // updated by
				Arrays.asList()),		
		TEST_EMAIL_SENT_FAILED(100005l, TestUser.JACOB_SMITH, "Order #12345", "Plain Text Body", // id, sendAs, Subject, Body 
				Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com"), // to
				Arrays.asList("GRPEndurSupportTeam@matthey.com"), // cc
				Arrays.asList(), //bcc
				Arrays.asList(), // attachments
				DefaultReference.EMAIL_STATUS_SENT_FAILED,  // email status
				null, // error message
				9, // retry count
				"2000-01-01 08:00:00",  // created at
				TestUser.JACOB_SMITH, // created by
				"2000-01-01 08:00:00",  // updated at
				TestUser.SERVICE_USER, // updated by
				Arrays.asList()),				
		;
	
	private final EmailTo email;
	
	private TestEmail    (long id,
			TestUser sendAs,
			String subject,
			String body,
			List<String> toList,
			List<String> ccList,
			List<String> bccList,
			List<TestDatabaseFile> attachments,
			DefaultReference emailStatus,
			String errorMessage,
			int retryCount,
			String createdAt,
			TestUser createdByUser,
			String lastUpdate,
			TestUser updatedByUser,
			List<OrderTo> associatedOrders) {
		this.email = ImmutableEmailTo.builder()
				.id(id)
				.idSendAs(sendAs.getEntity().id())
				.subject(subject)
				.body(body)
				.toList(toList)
				.ccList(ccList)
				.bccList(bccList)
				.attachments(attachments.stream().map(x -> x.getEntity().id()).collect(Collectors.toList()))
				.idEmailStatus(emailStatus.getEntity().id())
				.errorMessage(errorMessage)
				.retryCount(retryCount)
				.createdAt(createdAt)
				.idCreatedByUser(createdByUser.getEntity().id())
				.lastUpdate(lastUpdate)
				.idUpdatedByUser(updatedByUser.getEntity().id())
				.associatedOrderIds(associatedOrders.stream().map(x -> x.id()).collect(Collectors.toList()))
				.build();
	}
	
	public EmailTo getEntity () {
		return email;
	}
	
	public static List<EmailTo> asList () {
		return Arrays.asList(TestEmail.values())
				.stream().map(TestEmail::getEntity).collect(Collectors.toList());
	}
	
	public static List<EmailTo> asListByIds (List<Long> ids) {
		return Arrays.asList(TestEmail.values())
				.stream()
				.map(TestEmail::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
}