package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.transaction.Transactional;

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
import com.matthey.pmm.toms.service.mock.MockUserController;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.UserTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class UserControllerTest {
	@Autowired
	protected MockUserController userController;	
	
	@Test
	@Transactional
	public void testAllUsersPresent() {
		Set<UserTo> allUsers = userController.getUser(null, null, null);
		assertThat(allUsers.size()).isEqualTo(TestUser.asList().size());
		assertThat(allUsers).isSubsetOf(TestUser.asList());
		assertThat(TestUser.asList()).isSubsetOf(allUsers);
	}
	
	@Test
	@Transactional
	public void testQuerySingleUserId() {
		Set<UserTo> allUsers = userController.getUser(TestUser.ANDREW_BAYNES.getEntity().id(), null, null);
		assertThat(allUsers.size()).isEqualTo(1l);
		assertThat(allUsers).contains(TestUser.ANDREW_BAYNES.getEntity());
	}
	
	@Test
	@Transactional
	public void testQueryByEmail() {
		Set<UserTo> allUsers = userController.getUser(null, TestUser.ANDREW_BAYNES.getEntity().email(), null);
		assertThat(allUsers.size()).isEqualTo(1l);
		assertThat(allUsers).contains(TestUser.ANDREW_BAYNES.getEntity());
	}

	@Test
	@Transactional
	public void testQueryByRole() {
		Set<UserTo> allUsers = userController.getUser(null, null, DefaultReference.USER_ROLE_SERVICE_USER.getEntity().id());
		assertThat(allUsers.size()).isEqualTo(TestUser.asListByRole(DefaultReference.USER_ROLE_SERVICE_USER).size());
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
	
	@Test
	@Transactional
	public void testQueryAll() {
		Set<UserTo> allUsers = userController.getUser(TestUser.SERVICE_USER.getEntity().id(), TestUser.SERVICE_USER.getEntity().email(), DefaultReference.USER_ROLE_SERVICE_USER.getEntity().id());
		assertThat(allUsers.size()).isEqualTo(TestUser.asListByRole(DefaultReference.USER_ROLE_SERVICE_USER).size());
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
	
	@Test
	@Transactional
	public void testQueryByEmailAndRole() {
		Set<UserTo> allUsers = userController.getUser(null, TestUser.SERVICE_USER.getEntity().email(), DefaultReference.USER_ROLE_SERVICE_USER.getEntity().id());
		assertThat(allUsers.size()).isEqualTo(1);
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
	
	@Test
	@Transactional
	public void testQueryBySingleUserIdAndRole() {
		Set<UserTo> allUsers = userController.getUser(TestUser.SERVICE_USER.getEntity().id(), null, DefaultReference.USER_ROLE_SERVICE_USER.getEntity().id());
		assertThat(allUsers.size()).isEqualTo(1);
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
	
	@Test
	@Transactional
	public void testQueryBySingleUserIdAndEmail() {
		Set<UserTo> allUsers = userController.getUser(TestUser.SERVICE_USER.getEntity().id(), TestUser.SERVICE_USER.getEntity().email(), null);
		assertThat(allUsers.size()).isEqualTo(1);
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
}
