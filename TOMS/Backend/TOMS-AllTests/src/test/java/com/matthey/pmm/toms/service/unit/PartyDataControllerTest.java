package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import com.matthey.pmm.toms.service.mock.MockPartyDataController;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestLenit;
import com.matthey.pmm.toms.testall.TestServiceApplication;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class})
@ContextConfiguration
public class PartyDataControllerTest {
	@Autowired
	protected MockPartyDataController partyDataController;
	
	@Test
	public void testGetAllParties() {
		List<Long> allPartyIdsFromController = partyDataController.getParties(null, null).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allPartyIds = new ArrayList<>(TestBunit.values().length + TestLenit.values().length);
		allPartyIds.addAll(TestBunit.asList().stream()
				.map(x -> x.id())
				.collect(Collectors.toList()));
		allPartyIds.addAll(TestLenit.asList().stream()
				.map(x -> x.id())
				.collect(Collectors.toList()));
		assertThat(allPartyIdsFromController).containsExactlyInAnyOrderElementsOf(allPartyIds);		
	}
	
	@Test
	public void testGetPartiesByPartyType() {
		List<Long> allPartyIdsFromController = partyDataController.getParties(DefaultReference.PARTY_TYPE_EXTERNAL_LE.getEntity().id(), null).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		List<Long> allPartyIds = new ArrayList<>(TestLenit.values().length);
		allPartyIds.addAll(TestLenit.asListExternalLe().stream()
				.map(x -> x.id())
				.collect(Collectors.toList()));
		assertThat(allPartyIdsFromController).containsExactlyInAnyOrderElementsOf(allPartyIds);		
	}
	
	@Test
	public void testGetPartiesByLegalEntity() {
		List<Long> allPartyIdsFromController = partyDataController.getParties(null, TestLenit.JM__CN__CATALYST_CO___LE.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		assertThat(allPartyIdsFromController).containsExactlyInAnyOrder(TestBunit.JM_PMM_CN.getEntity().id());
	}
	
	@Test
	public void testGetPartiesByPartyTypeAndLegalEntity() {
		List<Long> allPartyIdsFromController = partyDataController.getParties(DefaultReference.PARTY_TYPE_INTERNAL_BUNIT.getEntity().id(), 
				TestLenit.JM__CN__CATALYST_CO___LE.getEntity().id()).stream()
				.map(x -> x.id())
				.collect(Collectors.toList());
		assertThat(allPartyIdsFromController).containsExactlyInAnyOrder(TestBunit.JM_PMM_CN.getEntity().id());
	}
}
