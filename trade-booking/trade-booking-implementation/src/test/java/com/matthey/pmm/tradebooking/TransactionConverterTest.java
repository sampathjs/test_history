package com.matthey.pmm.tradebooking;

import com.matthey.pmm.tradebooking.processors.LogTable;
import com.olf.openrisk.application.Session;
import com.olf.openrisk.io.IOFactory;

import lombok.val;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

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
                .withLegs(
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
                .withProcessingInstruction(
                        new ProcessingInstructionTo.ProcessingInstructionToBuilder()
                                .withInitialization(
                                        new InitializationTo.InitializationToBuilder().withByTemplate(
                                                new InitializationByTemplateTo.InitializationByTemplateToBuilder().withTemplateReference("template-id").build()
                                        ).build()
                                )
                                .withTransactionProcessing(
                                        Arrays.asList(new TransactionProcessingTo.TransactionProcessingToBuilder().withStatus("new").withGlobalOrderId("MAX").build())
                                )
                                .build()
                )
                .build();

        System.out.println("---------");

//        LogTable mockLogTable = Mockito.mock(LogTable.class);       
//        Session mockSession = Mockito.mock(Session.class);
//        Mockito.when(mockSession.getIOFactory()).thenReturn(Mockito.mock(IOFactory.class));
//        
//        val result = new TransactionConverter(mockLogTable).apply(mockSession, tx);
//
//        result.forEach(ti -> System.out.println("" + ti + " " + ti.getClass().getName()));
//
//        new TransactionItemsListExecutor().apply(result);

    }

}