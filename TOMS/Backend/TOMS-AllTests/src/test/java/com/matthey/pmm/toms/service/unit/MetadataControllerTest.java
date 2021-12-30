package com.matthey.pmm.toms.service.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import com.matthey.pmm.toms.model.AttributeCalculation;
import com.matthey.pmm.toms.model.ProcessTransition;
import com.matthey.pmm.toms.repository.AttributeCalculationRepository;
import com.matthey.pmm.toms.repository.ProcessTransitionRepository;
import com.matthey.pmm.toms.service.mock.MockMetadataController;
import com.matthey.pmm.toms.service.mock.testdata.TestBunit;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet1;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet2;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet3;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet4;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet5;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet6;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet7;
import com.matthey.pmm.toms.service.mock.testdata.TestCounterPartyTickerRuleSet8;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerFxRefSourceRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerPortfolioRule;
import com.matthey.pmm.toms.service.mock.testdata.TestTickerRefSourceRule;
import com.matthey.pmm.toms.testall.TestServiceApplication;
import com.matthey.pmm.toms.transport.AttributeCalculationTo;
import com.matthey.pmm.toms.transport.CounterPartyTickerRuleTo;
import com.matthey.pmm.toms.transport.ImmutableReferenceOrderTo;
import com.matthey.pmm.toms.transport.ProcessTransitionTo;
import com.matthey.pmm.toms.transport.ReferenceOrderTo;
import com.matthey.pmm.toms.transport.TickerFxRefSourceRuleTo;
import com.matthey.pmm.toms.transport.TickerPortfolioRuleTo;
import com.matthey.pmm.toms.transport.TickerRefSourceRuleTo;

@RunWith(SpringJUnit4ClassRunner.class)
@AutoConfigureTestDatabase(replace=Replace.NONE)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes={TestServiceApplication.class}) 
@ContextConfiguration
public class MetadataControllerTest {
	@Autowired
	protected MockMetadataController metadataController;
	
	@Autowired
	private AttributeCalculationRepository attrCalcRepo;
	
	@Autowired
	private ProcessTransitionRepository transitionRepo;
	
	@Test
	public void testGetAllAttributeCalculations () {
		Set<AttributeCalculationTo> attributeCalculations = metadataController.getAttributeCalculations(null);
		List<AttributeCalculation> fromRepo = attrCalcRepo.findAll();
		assertThat(attributeCalculations).isNotNull();
		assertThat(attributeCalculations).isNotEmpty();
		assertThat(attributeCalculations).hasSameSizeAs(fromRepo);
		assertThat(attributeCalculations.stream().map(x -> x.id()).collect(Collectors.toList()))
			.hasSameElementsAs(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));
	}
	
	@Test
	public void testGetAttributeCalculationsForRefOrder() {
		Set<AttributeCalculationTo> calcsForRefOrder = metadataController.getAttributeCalculations(ImmutableReferenceOrderTo.class.getCanonicalName());
		List<AttributeCalculation> fromRepo = attrCalcRepo.findByClassName(ImmutableReferenceOrderTo.class.getCanonicalName());
		assertThat(calcsForRefOrder).isNotNull();
		assertThat(calcsForRefOrder).isNotEmpty();
		assertThat(calcsForRefOrder).hasSameSizeAs(fromRepo);
		assertThat(calcsForRefOrder.stream().map(x -> x.id()).collect(Collectors.toList()))
			.hasSameElementsAs(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));		
	}
	
	@Test
	public void testGetAllProcessTransitions() {
		Set<ProcessTransitionTo> transitions =  metadataController.getProcessTransitions(null);
		List<ProcessTransition> fromRepo = transitionRepo.findAll();
		assertThat(transitions).isNotNull();
		assertThat(transitions).isNotEmpty();
		assertThat(transitions).hasSameSizeAs(fromRepo);
		assertThat(transitions.stream().map(x -> x.id()).collect(Collectors.toList()))
			.hasSameElementsAs(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));
	}
	
	@Test
	public void testGetProcessTransitionsForLimitOrder() {
		Set<ProcessTransitionTo> transitions =  metadataController.getProcessTransitions(DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id());
		List<ProcessTransition> fromRepo = transitionRepo.findByReferenceCategoryId(DefaultReference.LIMIT_ORDER_TRANSITION.getEntity().id());
		assertThat(transitions).isNotNull();
		assertThat(transitions).isNotEmpty();
		assertThat(transitions).hasSameSizeAs(fromRepo);
		assertThat(transitions.stream().map(x -> x.id()).collect(Collectors.toList()))
			.hasSameElementsAs(fromRepo.stream().map(x -> x.getId()).collect(Collectors.toList()));
	}
	
	@Test
	public void testGetAllCounterPartyTickerRulesWithDisplayStrings() {
		Set<CounterPartyTickerRuleTo> rules =  metadataController.getCounterPartyTickerRules(null, true);
		List<CounterPartyTickerRuleTo> allRules = new ArrayList<>(TestCounterPartyTickerRuleSet1.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet2.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet3.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet4.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet5.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet6.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet7.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet8.asList());
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules).hasSameElementsAs(allRules);
		assertThat(rules.stream().map(x -> x.counterPartyDisplayString()).collect(Collectors.toList())).doesNotContainNull();
		assertThat(rules.stream().map(x -> x.metalLocationDisplayString()).collect(Collectors.toList())).doesNotContainNull();
		assertThat(rules.stream().map(x -> x.metalFormDisplayString()).collect(Collectors.toList())).doesNotContainNull();
	}
	
	@Test
	public void testGetAllCounterPartyTickerRulesWithNoDisplayStrings() {
		Set<CounterPartyTickerRuleTo> rules =  metadataController.getCounterPartyTickerRules(null, false);
		List<CounterPartyTickerRuleTo> allRules = new ArrayList<>(TestCounterPartyTickerRuleSet1.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet2.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet3.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet4.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet5.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet6.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet7.asList());
		allRules.addAll(TestCounterPartyTickerRuleSet8.asList());
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules).hasSameElementsAs(allRules);
		assertThat(rules.stream().map(x -> x.counterPartyDisplayString()).collect(Collectors.toList())).containsOnlyNulls();
		assertThat(rules.stream().map(x -> x.metalLocationDisplayString()).collect(Collectors.toList())).containsOnlyNulls();
		assertThat(rules.stream().map(x -> x.metalFormDisplayString()).collect(Collectors.toList())).containsOnlyNulls();
	}
	

	@Test
	public void testGetAllCounterPartyTickerRulesForSingleParty() {
		Set<CounterPartyTickerRuleTo> rules =  metadataController.getCounterPartyTickerRules(TestBunit.JM_PMM_UK.getEntity().id(), false);
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules.stream().map(x -> x.idCounterParty()).collect(Collectors.toList())).containsOnly(TestBunit.JM_PMM_UK.getEntity().id());
	}	
	
	@Test 
	public void testGetTickerFxRefSourceRules() {
		Set<TickerFxRefSourceRuleTo> rules = metadataController.getTickerFxRefSourceRules();
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules).hasSameElementsAs(TestTickerFxRefSourceRule.asList());
	}
	
	@Test 
	public void testGetTickerPortfolioRules() {
		Set<TickerPortfolioRuleTo> rules = metadataController.getTickerPortfolioRules();
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules).hasSameElementsAs(TestTickerPortfolioRule.asList());
	}	
	
	@Test 
	public void testGetTickerRefSourceRules() {
		Set<TickerRefSourceRuleTo> rules = metadataController.getTickerRefSourceRules();
		assertThat(rules).isNotNull();
		assertThat(rules).isNotEmpty();
		assertThat(rules).hasSameElementsAs(TestTickerRefSourceRule.asList());
	}	
	
}
