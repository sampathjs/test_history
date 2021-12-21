package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.service.mock.MockUserController;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.UserTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestServiceApplication.class})
public class UserControllerTest {
	@Autowired
	protected MockUserController userController;	
	
	@Test
	public void testAllUsersPresent() {
		Set<UserTo> allUsers = userController.getUser(null, null, null);
		assertThat(allUsers.size()).isEqualTo(TestUser.asList().size());
		assertThat(allUsers).isSubsetOf(TestUser.asList());
		assertThat(TestUser.asList()).isSubsetOf(allUsers);
	}
	
	@Test
	public void testQuerySingleUserId() {
		Set<UserTo> allUsers = userController.getUser(TestUser.ANDREW_BAYNES.getEntity().id(), null, null);
		assertThat(allUsers.size()).isEqualTo(1l);
		assertThat(allUsers).contains(TestUser.ANDREW_BAYNES.getEntity());
	}
	
	@Test
	public void testQueryByEmail() {
		Set<UserTo> allUsers = userController.getUser(null, TestUser.ANDREW_BAYNES.getEntity().email(), null);
		assertThat(allUsers.size()).isEqualTo(1l);
		assertThat(allUsers).contains(TestUser.ANDREW_BAYNES.getEntity());
	}

	@Test
	public void testQueryByRole() {
		Set<UserTo> allUsers = userController.getUser(null, null, DefaultReference.USER_ROLE_SERVICE_USER.getEntity().id());
		assertThat(allUsers.size()).isEqualTo(TestUser.asListByRole(DefaultReference.USER_ROLE_SERVICE_USER).size());
		assertThat(allUsers).contains(TestUser.SERVICE_USER.getEntity());
	}
}
