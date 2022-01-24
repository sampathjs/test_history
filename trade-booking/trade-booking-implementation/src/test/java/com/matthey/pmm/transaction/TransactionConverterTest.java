package com.matthey.pmm.transaction;

import lombok.val;

import com.matthey.pmm.transaction.TransactionConverter;
import com.matthey.pmm.transaction.TransactionItemsListExecutor;
import com.olf.openrisk.application.Session;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

public class TransactionConverterTest {

    @Test
    public void test() {
        TransactionTo tx = new TransactionTo.TransactionToBuilder()
                .withTransactionProperties(
                        Arrays.asList(
                                new PropertyTo.PropertyToBuilder().withName("p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(3).build(),
                                new PropertyTo.PropertyToBuilder().withName("p2n").withValue("22").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId(1).build()
                        )
                )
                .withLegTos(
                		Arrays.asList(
                                new LegTo.LegToBuilder().withLegId(1).withLegProperties(
                                		Arrays.asList(
                                                new PropertyTo.PropertyToBuilder().withName("l1p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(2).build(),
                                                new PropertyTo.PropertyToBuilder().withName("l1p2n").withValue("22").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId(4).build()
                                        )
                                ).build(),
                                new LegTo.LegToBuilder().withLegId(2).withLegProperties(
                                		Arrays.asList(
                                                new PropertyTo.PropertyToBuilder().withName("l2p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(6).build(),
                                                new PropertyTo.PropertyToBuilder().withName("l2p2n").withValue("22").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId(5).build()
                                        )
                                ).build()
                        )
                )
                .withProcessingInstructionTo(
                        new ProcessingInstructionTo.ProcessingInstructionToBuilder()
                                .withInitialization(
                                        new Initialization.InitializationBuilder().withByTemplate(new InitializationByTemplateTo.InitializationByTemplateToBuilder().withTemplateReference("template-id").build()).build()
                                )
                                .withTransactionProcessingTo(
                                		Arrays.asList(new TransactionProcessingTo.TransactionProcessingToBuilder().withStatus("new").withGlobalOrderId("MAX").build())
                                )
                                .build()
                )
                .build();

        System.out.println("---------");

        val result = new TransactionConverter().apply(Mockito.mock(Session.class), tx);

        result.forEach(ti -> System.out.println("" + ti + " " + ti.getClass().getName()));

        new TransactionItemsListExecutor().apply(result);

    }

}