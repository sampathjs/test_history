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
import com.matthey.pmm.toms.repository.OrderCommentRepository;
import com.matthey.pmm.toms.service.conversion.OrderCommentConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestLenit;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
public class OrderCommentRepostoryTest extends AbstractRepositoryTestBase<OrderComment, Long, OrderCommentRepository> {	
	@Autowired
	protected OrderCommentConverter conv;

	@Autowired
	protected UserConverter userConv;

	@Autowired
	protected ReferenceConverter referenceCon;
	
	@Autowired
	protected PartyConverter partyConverter;	
	
	
	@Before
	public void setupTestData () {
	}
	
	@Override
	protected Supplier<List<OrderComment>> listProvider() {		
		return () -> {
			final List<OrderComment> comments = Arrays.asList(new OrderComment("Comment",
						new Date(), userConv.toManagedEntity(TestUser.JACOB_SMITH.getEntity()), 
						new Date(), userConv.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),
						referenceCon.toManagedEntity(DefaultReference.DELETION_FLAG_NOT_DELETED.getEntity()),
						referenceCon.toManagedEntity(DefaultReference.ACTION_CANCEL.getEntity())),
					new OrderComment("Comment 2",
						new Date(), userConv.toManagedEntity(TestUser.JACOB_SMITH.getEntity()), 
						new Date(), userConv.toManagedEntity(TestUser.ANDREW_BAYNES.getEntity()),
						referenceCon.toManagedEntity(DefaultReference.DELETION_FLAG_NOT_DELETED.getEntity()),
						null)	
					);
			return comments;
		};
	}

	@Override
	protected Function<OrderComment, Long> idProvider() {
		return x -> x.getId();
	}
}
