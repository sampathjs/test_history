package com.matthey.pmm.tradebooking;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class TestUtils {

    public TransactionTo buildTransaction() {
        return new TransactionTo.TransactionToBuilder()
                .withTransactionProperties(
                        Arrays.asList(
                                new PropertyTo.PropertyToBuilder().withName("p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(3).build(),
                                new PropertyTo.PropertyToBuilder().withName("p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(3).build(),
                                new PropertyTo.PropertyToBuilder().withName("p2n").withValue("22").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId(1).build(),
                                new PropertyTo.PropertyToBuilder().withName("p3n").withValue("MIN").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId("MIN").build()
                        )
                )
                .withLegs(
                        Arrays.asList(
                                new LegTo.LegToBuilder().withLegId(1).withLegProperties(
                                        Arrays.asList(
                                                new PropertyTo.PropertyToBuilder().withName("l1p1n").withValue("p1v").withValueType(PropertyTo.PropertyValueType.STRING).withGlobalOrderId(2).build(),
                                                new PropertyTo.PropertyToBuilder().withName("l1p2n").withValue("22").withValueType(PropertyTo.PropertyValueType.INTEGER).withGlobalOrderId("MIN").build(),
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
    }
}
