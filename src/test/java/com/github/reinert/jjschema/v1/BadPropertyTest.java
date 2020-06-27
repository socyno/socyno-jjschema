package com.github.reinert.jjschema.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.TestCase;

public class BadPropertyTest extends TestCase {

    static ObjectMapper MAPPER = new ObjectMapper();
    JsonSchemaFactory schemaFactory = new JsonSchemaV4Factory();

    public void testGenerateSchema() throws Exception {
        ObjectNode expected = MAPPER.createObjectNode();
        expected.putObject("x").put("type", "number");

        JsonNode schema = schemaFactory.createSchema(BadClass.class);

        assertEquals(expected, schema.get("properties"));
    }

    static class BadClass {
    	private double x = 9d;
        public double getX() {
            return x;
        }
    }
}