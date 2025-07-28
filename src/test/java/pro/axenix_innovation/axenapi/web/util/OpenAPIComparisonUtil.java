package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Utility class for comparing OpenAPI specifications
 */
public class OpenAPIComparisonUtil {

    /**
     * Compare two OpenAPI specifications focusing on structure rather than content
     * @param expected The expected OpenAPI specification
     * @param actual The actual OpenAPI specification
     */
    public static void compareOpenAPIStructure(OpenAPI expected, OpenAPI actual) {
        // Compare paths
        comparePaths(expected, actual);

        // Compare components/schemas
        compareComponents(expected, actual);
    }

    /**
     * Compare paths between two OpenAPI specifications
     */
    private static void comparePaths(OpenAPI expected, OpenAPI actual) {
        assertNotNull(actual.getPaths(), "Actual specification has no paths");

        expected.getPaths().forEach((pathKey, expectedPath) -> {
            assertTrue(actual.getPaths().containsKey(pathKey),
                    "Path not found in actual specification: " + pathKey);

            PathItem actualPath = actual.getPaths().get(pathKey);

            // Compare operations for this path
            compareOperation(expectedPath.getGet(), actualPath.getGet(), "GET", pathKey);
            compareOperation(expectedPath.getPost(), actualPath.getPost(), "POST", pathKey);
            compareOperation(expectedPath.getPut(), actualPath.getPut(), "PUT", pathKey);
            compareOperation(expectedPath.getDelete(), actualPath.getDelete(), "DELETE", pathKey);
            compareOperation(expectedPath.getPatch(), actualPath.getPatch(), "PATCH", pathKey);
            compareOperation(expectedPath.getHead(), actualPath.getHead(), "HEAD", pathKey);
            compareOperation(expectedPath.getOptions(), actualPath.getOptions(), "OPTIONS", pathKey);
        });
    }

    /**
     * Compare operations between expected and actual paths
     */
    private static void compareOperation(Operation expected, Operation actual, String method, String path) {
        if (expected == null) {
            return; // Operation not defined in expected spec, so skip
        }

        assertNotNull(actual, method + " operation missing for path " + path);

        // Compare parameters
        compareParameters(expected.getParameters(), actual.getParameters(), method, path);

        // Compare request body if present
        if (expected.getRequestBody() != null) {
            assertNotNull(actual.getRequestBody(),
                    method + " request body missing for path " + path);

            // Compare request content
            compareContent(expected.getRequestBody().getContent(),
                    actual.getRequestBody().getContent(),
                    method + " request for " + path);
        }

        // Compare responses
        expected.getResponses().forEach((statusCode, expectedResponse) -> {
            assertTrue(actual.getResponses().containsKey(statusCode),
                    method + " for " + path + " is missing response code " + statusCode);

            ApiResponse actualResponse = actual.getResponses().get(statusCode);

            // Compare response content
            if (expectedResponse.getContent() != null) {
                assertNotNull(actualResponse.getContent(),
                        method + " " + statusCode + " response for " + path + " is missing content");

                compareContent(expectedResponse.getContent(), actualResponse.getContent(),
                        method + " " + statusCode + " response for " + path);
            }
        });
    }

    /**
     * Compare parameters between operations
     */
    private static void compareParameters(List<Parameter> expected, List<Parameter> actual, String method, String path) {
        if (expected == null || expected.isEmpty()) {
            return;
        }

        assertNotNull(actual, method + " parameters missing for path " + path);
        assertFalse(actual.isEmpty(), method + " parameters empty for path " + path);

        assertEquals(expected.size(), actual.size(),
                "Number of parameters doesn't match for " + method + " " + path);

        for (Parameter expectedParam : expected) {
            boolean found = false;
            for (Parameter actualParam : actual) {
                if (Objects.equals(expectedParam.getName(), actualParam.getName()) &&
                        Objects.equals(expectedParam.getIn(), actualParam.getIn())) {
                    found = true;

                    // Compare schema if present
                    if (expectedParam.getSchema() != null) {
                        assertNotNull(actualParam.getSchema(),
                                "Parameter " + expectedParam.getName() + " schema missing");

                        compareSchema(expectedParam.getSchema(), actualParam.getSchema(),
                                "Parameter " + expectedParam.getName());
                    }
                    break;
                }
            }
            assertTrue(found, "Parameter " + expectedParam.getName() + " not found in " + method + " " + path);
        }
    }

    /**
     * Compare content objects between request/response bodies
     */
    private static void compareContent(Content expected, Content actual, String context) {
        if (expected == null) {
            return;
        }

        assertNotNull(actual, context + " content is missing");

        expected.forEach((mediaTypeKey, expectedMediaType) -> {
            assertTrue(actual.containsKey(mediaTypeKey),
                    context + " is missing media type " + mediaTypeKey);

            MediaType actualMediaType = actual.get(mediaTypeKey);

            // Compare schema
            if (expectedMediaType.getSchema() != null) {
                assertNotNull(actualMediaType.getSchema(),
                        context + " media type " + mediaTypeKey + " is missing schema");

                compareSchema(expectedMediaType.getSchema(), actualMediaType.getSchema(),
                        context + " media type " + mediaTypeKey);
            }
        });
    }

    /**
     * Compare schemas between components
     */
    private static void compareSchema(Schema<?> expected, Schema<?> actual, String context) {
        // Compare type
        if (expected.getType() != null) {
            assertEquals(expected.getType(), actual.getType(),
                    context + " has incorrect type");
        }

        // Compare format
        if (expected.getFormat() != null) {
            assertEquals(expected.getFormat(), actual.getFormat(),
                    context + " has incorrect format");
        }

        // Compare reference
        if (expected.get$ref() != null) {
            assertEquals(expected.get$ref(), actual.get$ref(),
                    context + " has incorrect reference");
        }
    }

    /**
     * Compare components between OpenAPI specifications
     */
    private static void compareComponents(OpenAPI expected, OpenAPI actual) {
        if (expected.getComponents() == null || expected.getComponents().getSchemas() == null) {
            return;
        }

        assertNotNull(actual.getComponents(), "Actual specification has no components");
        assertNotNull(actual.getComponents().getSchemas(), "Actual specification has no schemas");

        expected.getComponents().getSchemas().forEach((schemaKey, expectedSchema) -> {
            assertTrue(actual.getComponents().getSchemas().containsKey(schemaKey),
                    "Schema not found in actual specification: " + schemaKey);

            Schema<?> actualSchema = actual.getComponents().getSchemas().get(schemaKey);

            // Compare properties
            compareSchemaProperties(expectedSchema, actualSchema, schemaKey);
        });
    }

    /**
     * Compare schema properties between components
     */
    private static void compareSchemaProperties(Schema<?> expected, Schema<?> actual, String schemaName) {
        if (expected.getProperties() == null) {
            return;
        }

        assertNotNull(actual.getProperties(),
                "Schema " + schemaName + " is missing properties");

        expected.getProperties().forEach((propName, expectedProp) -> {
            assertTrue(actual.getProperties().containsKey(propName),
                    "Schema " + schemaName + " is missing property " + propName);

            Schema<?> actualProp = (Schema<?>) actual.getProperties().get(propName);

            // Compare property type
            if (expectedProp.getType() != null) {
                assertEquals(expectedProp.getType(), actualProp.getType(),
                        "Property " + propName + " in schema " + schemaName + " has incorrect type");
            }

            // Compare property format
            if (expectedProp.getFormat() != null) {
                assertEquals(expectedProp.getFormat(), actualProp.getFormat(),
                        "Property " + propName + " in schema " + schemaName + " has incorrect format");
            }

            // Compare property reference
            if (expectedProp.get$ref() != null) {
                assertEquals(expectedProp.get$ref(), actualProp.get$ref(),
                        "Property " + propName + " in schema " + schemaName + " has incorrect reference");
            }
        });
    }
}