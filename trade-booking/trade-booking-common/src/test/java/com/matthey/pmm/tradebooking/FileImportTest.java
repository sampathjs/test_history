package com.matthey.pmm.tradebooking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.val;
import org.junit.Test;

import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileImportTest {

    @Test
    public void validationTest() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

        try (
                InputStream jsonStream = inputStreamFromClasspath("Sample.json");
                InputStream schemaStream = inputStreamFromClasspath("schemas/Transaction.json")
        ) {
            JsonNode json = objectMapper.readTree(jsonStream);
            JsonSchema schema = schemaFactory.getSchema(schemaStream);
            Set<ValidationMessage> validationResult = schema.validate(json);

            // print validation errors
            if (!validationResult.isEmpty()) {
                validationResult.forEach(vm -> System.out.println(vm.getMessage()));
            }

            assertTrue(validationResult.isEmpty());
        }
    }

    private static InputStream inputStreamFromClasspath(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }

    @Test
    public void dataBindTest() throws Exception {

        try (
                InputStream jsonStream = inputStreamFromClasspath("Sample.json");
        ) {

            ObjectMapper objectMapper = new ObjectMapper();
            val transactionTo = objectMapper.readValue(jsonStream, TransactionTo.class);
            assertEquals(2, transactionTo.getLegs().size());
        }

    }
}