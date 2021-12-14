package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.repository.EmailRepository;
import com.matthey.pmm.toms.service.conversion.CreditCheckConverter;
import com.matthey.pmm.toms.service.conversion.DatabaseFileConverter;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.LimitOrderConverter;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceOrderLegConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestDatabaseFile;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestReferenceOrder;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class EmailRepositoryTest extends AbstractRepositoryTestBase<Email, Long, EmailRepository> {	
	@Autowired
	protected ReferenceConverter referenceCon;	

	@Autowired
	protected UserConverter userConverter;	
	
	@Autowired 
	protected PartyConverter partyConverter;
	
	@Autowired 
	protected DatabaseFileConverter databaseFileConverter;
	
	@Autowired 
	protected LimitOrderConverter limitOrderConverter;

	@Autowired 
	protected ReferenceOrderConverter referenceOrderConverter;

	@Autowired 
	protected OrderCommentConverter orderCommentConverter;
	
	@Autowired 
	protected FillConverter fillConverterConverter;

	@Autowired 
	protected CreditCheckConverter creditCheckConverter;

	@Autowired 
	protected ReferenceOrderLegConverter referenceOrderLegConverter;

	
	@Before
	public void setupTestData () {		
	}
	
	@Override
	public void deletePersistedEntites () {
		
	}   
	
	@Override
	protected Supplier<List<Email>> listProvider() {		
		return () -> {
			final List<Email> users = Arrays.asList(new Email( // id
					userConverter.toManagedEntity(TestUser.JACOB_SMITH.getEntity()), // sendAs
					"Subject", // subject
					"<HTML> <BODY> </BODY> </HTML>", // body
					new HashSet<>(Arrays.asList("jens.waetcher@matthey.com", "Jacob.Smith@matthey.com")), // toList
					new HashSet<>(Arrays.asList("Pat.McCourt@jmusa.com", "Andrew.Baynes@matthey.com")),  // ccList
					new HashSet<>(Arrays.asList("GRPEndurSupportTeam@matthey.com", "dennis.wildish@matthey.com")), // bccList
					new HashSet<>(Arrays.asList(databaseFileConverter.toManagedEntity(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity()), 
								  databaseFileConverter.toManagedEntity(TestDatabaseFile.TEST_DATABASE_FILE_2.getEntity()))), // attachments
					referenceCon.toManagedEntity(DefaultReference.EMAIL_STATUS_SENT_FAILED.getEntity()), // email status
					"Error Message", // errorMessage
					9, // retryCount
					new Date(), // createdAt
					userConverter.toManagedEntity(TestUser.JACOB_SMITH.getEntity()), // createdByUser
					new Date(),  // lastUpdate
					userConverter.toManagedEntity(TestUser.SERVICE_USER.getEntity()), // updatedByUser
					new HashSet<>(Arrays.asList(limitOrderConverter.toManagedEntity(TestLimitOrder.TEST_ORDER_1B.getEntity()),
							referenceOrderConverter.toManagedEntity(TestReferenceOrder.TEST_ORDER_1B.getEntity())))) // associatedOrders
					);  // 
			return users;
		};
	}

	@Override
	protected Function<Email, Long> idProvider() {
		return x -> x.getId();
	}
}
