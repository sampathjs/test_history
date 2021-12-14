package com.matthey.pmm.toms.model;

import java.util.Arrays;
import java.util.Date;
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
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestLenit;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class DatabaseFileRepostoryTest extends AbstractRepositoryTestBase<DatabaseFile, Long, DatabaseFileRepository> {	
	@Autowired
	protected ReferenceConverter referenceCon;	

	@Autowired
	protected UserConverter userConverter;	
	
	@Autowired 
	protected PartyConverter partyConverter;
	
	private static final byte[] TEST_FILE_CONTENT = "Test File Content".getBytes();
	private static final byte[] TEST_FILE_CONTENT2 = "Test File Content2".getBytes();
	
	@Before
	public void setupTestData () {
	}
	
	@Override
	protected Supplier<List<DatabaseFile>> listProvider() {		
		return () -> {
			final List<DatabaseFile> users = Arrays.asList(new DatabaseFile("Filename.txt", 
							"/test/testdata/",
							referenceCon.toManagedEntity(DefaultReference.FILE_TYPE_TXT.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
							TEST_FILE_CONTENT, 
							new Date(), userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),
							new Date(), userConverter.toManagedEntity(TestUser.ARINDAM_RAY.getEntity())),
					new DatabaseFile("Filename2.txt", 
							"/test/testdata2/",
							referenceCon.toManagedEntity(DefaultReference.FILE_TYPE_TXT.getEntity()), 
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),
							TEST_FILE_CONTENT2, 
							new Date(), userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),
							new Date(), userConverter.toManagedEntity(TestUser.ARINDAM_RAY.getEntity())),
					new DatabaseFile("Filename3.txt", 
							"/test/testdata3/",
							referenceCon.toManagedEntity(DefaultReference.FILE_TYPE_TXT.getEntity()),
							referenceCon.toManagedEntity(DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE.getEntity()),							
							TEST_FILE_CONTENT2, 
							new Date(), userConverter.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),
							new Date(), userConverter.toManagedEntity(TestUser.ARINDAM_RAY.getEntity())
							)
					);  // 
			return users;
		};
	}

	@Override
	protected Function<DatabaseFile, Long> idProvider() {
		return x -> x.getId();
	}
}
