package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Email;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.EmailRepository;
import com.matthey.pmm.toms.service.conversion.DatabaseFileConverter;
import com.matthey.pmm.toms.service.conversion.EmailConverter;
import com.matthey.pmm.toms.service.exception.IllegalStateChangeException;
import com.matthey.pmm.toms.service.mock.MockEmailController;
import com.matthey.pmm.toms.service.mock.testdata.TestDatabaseFile;
import com.matthey.pmm.toms.service.mock.testdata.TestEmail;
import com.matthey.pmm.toms.service.mock.testdata.TestLimitOrder;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.EmailTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableEmailTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class EmailControllerTest {
	@Autowired
	protected MockEmailController emailController;
	
	@Autowired
	protected DatabaseFileRepository dbFileRepo;
	
	@Autowired
	protected EmailRepository emailRepo;
	
	@Autowired
	protected DatabaseFileConverter dbFileConverter;

	@Autowired
	protected EmailConverter emailConverter;
	
	protected List<DatabaseFileTo> dbFilesToDelete;
	protected List<EmailTo> emailsToDelete;
	
	protected List<EmailTo> emailsToRestore;
	
	@Before
	public void initTest () {
		dbFilesToDelete = new ArrayList<>(5);
		emailsToDelete = new ArrayList<>(5);
		emailsToRestore = new ArrayList<>(5);
	}
	
	@After
	public void clearAfterTest () {
		for (DatabaseFileTo dbFile : dbFilesToDelete) {
			if (dbFileRepo.existsById(dbFile.id())) {
				dbFileRepo.deleteById(dbFile.id());
			}
		}

		for (EmailTo email : emailsToDelete) {
			if (emailRepo.existsById(email.id())) {
				emailRepo.deleteById(email.id());
			}
		}
		for (EmailTo email : emailsToRestore) {
			emailRepo.save(emailConverter.toManagedEntity(email));
		}
	}
	
	@Transactional
	protected long submitNewDatabaseFile (DatabaseFileTo dbFileTo) {
		DatabaseFileTo withResetId = ImmutableDatabaseFileTo.builder()
				.from(dbFileTo)
				.id(0l)
				.build();
		long ret = emailController.postDatabaseFile(withResetId);
		DatabaseFileTo withId = ImmutableDatabaseFileTo.builder()
				.from(dbFileTo)
				.id(ret)
				.build();
		dbFilesToDelete.add(withId);
		return ret;
	}
	
	@Transactional
	protected long submitNewEmailRequest (EmailTo emailTo) {
		EmailTo withResetId = ImmutableEmailTo.builder()
				.from(emailTo)
				.id(0l)
				.build();
		long ret = emailController.postEmailRequest(withResetId);
		EmailTo withId = ImmutableEmailTo.builder()
				.from(emailTo)
				.id(ret)
				.build();
		emailsToDelete.add(withId);
		return ret;
	}
	
	@Transactional
	protected void updateEmailRequest (EmailTo toBeSaved) {
		EmailTo oldEmail = emailConverter.toTo(emailRepo.findById(toBeSaved.id()).get());
		emailsToRestore.add(oldEmail);
		emailController.updateEmailRequest(toBeSaved.id(), toBeSaved);
	}	
	
	@Test
	@Transactional
	public void testGetDatabaseFile() {
		DatabaseFileTo dbFile = emailController.getDatabaseFile(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().id());
		assertThat(dbFile).isNotNull();
		assertThat(dbFile.id()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().id());
		assertThat(dbFile.createdAt()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().createdAt());
		assertThat(dbFile.fileContent()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().fileContent());
		assertThat(dbFile.idCreatedByUser()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().idCreatedByUser());
		assertThat(dbFile.idFileType()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().idFileType());
		assertThat(dbFile.idLifecycle()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().idLifecycle());
		assertThat(dbFile.idUpdatedByUser()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().idUpdatedByUser());
		assertThat(dbFile.lastUpdate()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().lastUpdate());
		assertThat(dbFile.name()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().name());
		assertThat(dbFile.path()).isEqualTo(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity().path());		
	}
	
	@Test
	@Transactional
	public void testTestPostNewDatabaseFile() {
		DatabaseFileTo withUpdatedName = ImmutableDatabaseFileTo.builder()
				.from(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity())
				.name(TestDatabaseFile.TEST_DATABASE_FILE_1.getEntity() + "2")
				.build();
		
		long newId = submitNewDatabaseFile(withUpdatedName);
		Optional<DatabaseFile> newFile = dbFileRepo.findById(newId);
		assertThat(newFile).isNotEmpty();
		assertThat(newFile.get().getName()).isEqualTo(withUpdatedName.name());
		assertThat(newFile.get().getCreatedAt()).isCloseTo(new Date(), 5000);
		assertThat(Base64.getEncoder().encodeToString(newFile.get().getFileContent())).isEqualTo(withUpdatedName.fileContent());
		assertThat(newFile.get().getCreatedByUser().getId()).isEqualTo(withUpdatedName.idCreatedByUser());
		assertThat(newFile.get().getFileType().getId()).isEqualTo(withUpdatedName.idFileType());
		assertThat(newFile.get().getLifecycleStatus().getId()).isEqualTo(withUpdatedName.idLifecycle());
		assertThat(newFile.get().getPath()).isEqualTo(withUpdatedName.path());
		assertThat(newFile.get().getLastUpdate()).isCloseTo(new Date(), 5000);
	}
	
	@Test
	@Transactional
	public void testGetEmail() {
		EmailTo email = emailController.getEmailRequest(TestEmail.TEST_EMAIL1.getEntity().id());
		assertThat(email).isNotNull();
		assertThat(email.id()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().id());
		assertThat(email.associatedOrderIds()).containsAll(TestEmail.TEST_EMAIL1.getEntity().associatedOrderIds());
		assertThat(email.attachments()).containsAll(TestEmail.TEST_EMAIL1.getEntity().attachments());
		assertThat(email.bccList()).containsAll(TestEmail.TEST_EMAIL1.getEntity().bccList());
		assertThat(email.ccList()).containsAll(TestEmail.TEST_EMAIL1.getEntity().ccList());
		assertThat(email.toList()).containsAll(TestEmail.TEST_EMAIL1.getEntity().toList());
		assertThat(email.body()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().body());
		assertThat(email.errorMessage()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().errorMessage());			
		assertThat(email.idCreatedByUser()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idCreatedByUser());
		assertThat(email.idEmailStatus()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idEmailStatus());
		assertThat(email.idSendAs()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idSendAs());
		assertThat(email.idUpdatedByUser()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idUpdatedByUser());
		assertThat(email.retryCount()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().retryCount());
		assertThat(email.subject()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().subject());		
	}
	
	@Test
	@Transactional
	public void testGetEmailFromOrderId() {
		Collection<EmailTo> emails = emailController.getEmailRequestForOrder(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
		assertThat(emails).isNotNull();
		assertThat(emails.stream().map(x -> x.id()).collect(Collectors.toList())).contains(TestLimitOrder.TEST_ORDER_1B.getEntity().id());
	}
	
	@Test
	@Transactional
	public void testTestPostNewEmailRequest() {		
		long newId = submitNewEmailRequest(TestEmail.TEST_EMAIL1.getEntity());
		Optional<Email> email = emailRepo.findById(newId);
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(newId);
		assertThat(email.get().getAssociatedOrders()).containsAll(TestEmail.TEST_EMAIL1.getEntity().associatedOrderIds());
		assertThat(email.get().getAttachments().stream().map(x -> x.getId()).collect(Collectors.toList())).containsAll(TestEmail.TEST_EMAIL1.getEntity().attachments());
		assertThat(email.get().getBccSet()).containsAll(TestEmail.TEST_EMAIL1.getEntity().bccList());
		assertThat(email.get().getCcSet()).containsAll(TestEmail.TEST_EMAIL1.getEntity().ccList());
		assertThat(email.get().getToSet()).containsAll(TestEmail.TEST_EMAIL1.getEntity().toList());
		assertThat(email.get().getBody()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().body());
		assertThat(email.get().getErrorMessage()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().errorMessage());			
		assertThat(email.get().getCreatedByUser().getId()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idCreatedByUser());
		assertThat(email.get().getEmailStatus().getId()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idEmailStatus());
		assertThat(email.get().getSendAs().getId()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idSendAs());
		assertThat(email.get().getUpdatedByUser().getId()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().idUpdatedByUser());
		assertThat(email.get().getRetryCount()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().retryCount());
		assertThat(email.get().getSubject()).isEqualTo(TestEmail.TEST_EMAIL1.getEntity().subject());	
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequest() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL1.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENDING.getEntity().id())
				.build();
		
		updateEmailRequest(updatedStatus);
		Optional<Email> email = emailRepo.findById(updatedStatus.id());
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(updatedStatus.id());
		assertThat(email.get().getAssociatedOrders()).containsAll(updatedStatus.associatedOrderIds());
		assertThat(email.get().getAttachments().stream().map(x -> x.getId()).collect(Collectors.toList())).containsAll(updatedStatus.attachments());
		assertThat(email.get().getBccSet()).containsAll(updatedStatus.bccList());
		assertThat(email.get().getCcSet()).containsAll(updatedStatus.ccList());
		assertThat(email.get().getToSet()).containsAll(updatedStatus.toList());
		assertThat(email.get().getBody()).isEqualTo(updatedStatus.body());
		assertThat(email.get().getErrorMessage()).isEqualTo(updatedStatus.errorMessage());			
		assertThat(email.get().getCreatedByUser().getId()).isEqualTo(updatedStatus.idCreatedByUser());
		assertThat(email.get().getEmailStatus().getId()).isEqualTo(updatedStatus.idEmailStatus());
		assertThat(email.get().getSendAs().getId()).isEqualTo(updatedStatus.idSendAs());
		assertThat(email.get().getUpdatedByUser().getId()).isEqualTo(updatedStatus.idUpdatedByUser());
		assertThat(email.get().getRetryCount()).isEqualTo(updatedStatus.retryCount());
		assertThat(email.get().getSubject()).isEqualTo(updatedStatus.subject());	
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSubmittedToEmailStatusSending() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SUBMITTED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENDING.getEntity().id())
				.build();
		
		updateEmailRequest(updatedStatus);
		Optional<Email> email = emailRepo.findById(updatedStatus.id());
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(updatedStatus.id());
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSendingToEmailStatusSentSuccess() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENDING.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_SUCCESS.getEntity().id())
				.build();
		
		updateEmailRequest(updatedStatus);
		Optional<Email> email = emailRepo.findById(updatedStatus.id());
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(updatedStatus.id());
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSendingToEmailStatusSentFailed() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENDING.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_FAILED.getEntity().id())
				.build();
		
		updateEmailRequest(updatedStatus);
		Optional<Email> email = emailRepo.findById(updatedStatus.id());
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(updatedStatus.id());
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSentFailedToEmailStatusSending() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENT_FAILED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENDING.getEntity().id())
				.build();
		
		updateEmailRequest(updatedStatus);
		Optional<Email> email = emailRepo.findById(updatedStatus.id());
		assertThat(email).isNotNull();
		assertThat(email.get().getId()).isEqualTo(updatedStatus.id());
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSendingToEmailSubmittedFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENDING.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SUBMITTED.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateEmailRequestSentFailedToEmailSubmittedFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENT_FAILED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SUBMITTED.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSentFailedToEmailSuccessFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENT_FAILED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_SUCCESS.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSubmittedToEmailSentSuccessFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SUBMITTED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_SUCCESS.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateEmailRequestSubmittedToEmailSentFailFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SUBMITTED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_FAILED.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}
	
	@Test
	@Transactional
	public void testUpdateEmailRequestSendingToEmailSendingFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENDING.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENDING.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateEmailRequestSubmittedToEmailSubmittedFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SUBMITTED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SUBMITTED.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateEmailRequestSentSuccessToEmailSentSuccessFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENT_SUCCESS.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_SUCCESS.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}

	@Test
	@Transactional
	public void testUpdateEmailRequestSentFailedToEmailSentFailedFails() {		
		EmailTo updatedStatus = ImmutableEmailTo.builder()
				.from(TestEmail.TEST_EMAIL_SENT_FAILED.getEntity())
				.idEmailStatus(DefaultReference.EMAIL_STATUS_SENT_FAILED.getEntity().id())
				.build();
		
		assertThatThrownBy(() -> {updateEmailRequest(updatedStatus); })
			.isInstanceOf(IllegalStateChangeException.class);
	}
}
