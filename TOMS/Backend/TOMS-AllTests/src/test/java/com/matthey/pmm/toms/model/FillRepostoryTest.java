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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.repository.FillRepository;
import com.matthey.pmm.toms.service.conversion.FillConverter;
import com.matthey.pmm.toms.service.conversion.PartyConverter;
import com.matthey.pmm.toms.service.conversion.ReferenceConverter;
import com.matthey.pmm.toms.service.conversion.UserConverter;
import com.matthey.pmm.toms.service.mock.testdata.TestParty;
import com.matthey.pmm.toms.service.mock.testdata.TestUser;
import com.matthey.pmm.toms.testall.TestJpaApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(classes={TestJpaApplication.class})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class FillRepostoryTest extends AbstractRepositoryTestBase<Fill, Long, FillRepository> {	
	@Autowired
	protected FillConverter conv;

	@Autowired
	protected UserConverter userConv;

	@Autowired
	protected ReferenceConverter referenceCon;
	
	@Autowired
	protected PartyConverter partyConverter;	
	
	@Before
	public void setupTestData () {
		TestParty.asList() // legal entities first (LEs are not assigned to another LE)
  			.stream()
  			.filter(x -> x.idLegalEntity() <= 0)
  			.forEach(x -> partyConverter.toManagedEntity(x));
		TestParty.asList() // now the business units (everything that has an LE)
			.stream()
			.filter(x -> x.idLegalEntity() > 0)
			.forEach(x -> partyConverter.toManagedEntity(x));		
	}

	
	@Override
	protected Supplier<List<Fill>> listProvider() {		
		return () -> {
			final List<Fill> fills = Arrays.asList(new Fill(1000d, 345d, 1234567,
							userConv.toManagedEntity(TestUser.JENS_WAECHTER.getEntity()), userConv.toManagedEntity(TestUser.JENS_WAECHTER.getEntity()),
							new Date(), referenceCon.toManagedEntity(DefaultReference.FILL_STATUS_OPEN.getEntity()), "Error Message"),
					new Fill(2000d, 678d, 1234568,
							userConv.toManagedEntity(TestUser.JENS_WAECHTER.getEntity()), userConv.toManagedEntity(TestUser.JENS_WAECHTER.getEntity()),
							new Date(), referenceCon.toManagedEntity(DefaultReference.FILL_STATUS_FAILED.getEntity()), "Error Message"),
					new Fill(3000d, 678d, 1234569,
							userConv.toManagedEntity(TestUser.JACOB_SMITH.getEntity()), userConv.toManagedEntity(TestUser.PAT_MCCOURT.getEntity()),
							new Date(), referenceCon.toManagedEntity(DefaultReference.FILL_STATUS_COMPLETED.getEntity()), null));
			return fills;
		};
	}

	@Override
	protected Function<Fill, Long> idProvider() {
		return x -> x.getId();
	}
}
