package pro.axenix_innovation.axenapi.web.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;
import pro.axenix_innovation.axenapi.web.util.SolidOpenAPITranslator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPITranslatorYMLTest {

    private static final Logger log = LoggerFactory.getLogger(OpenAPITranslatorYMLTest.class);

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn("stub message");
        MessageHelper.setStaticMessageSource(messageSource);
    }

    // Successfully parse OpenAPI specification and create EventGraph with valid nodes and links
    @Test
    public void test_parse_openapi_specification_success() throws OpenAPISpecParseException {
        String specification = """
                openapi: 3.0.0
                info:
                  title: Test Service
                  version: 1.0.0
                paths:
                  /kafka/group1/topic1/Event1:
                    post:
                      responses:
                        '200':
                          description: OK
                components:
                  schemas:
                    Event1:
                      type: object
                      x-incoming:
                        topics: [topic1]
                """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specification);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertEquals("Test Service", result.getName());
        assertEquals(2, result.getNodes().size());
        assertEquals(1, result.getLinks().size());
        assertEquals(1, result.getEvents().size());

        NodeDTO serviceNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode);
        assertEquals("Test Service", serviceNode.getName());

        NodeDTO topicNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(topicNode);
        assertEquals("topic1", topicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicNode.getBrokerType());
    }

    // Process incoming topics and create corresponding nodes with broker types
    @Test
    public void test_process_incoming_topics_and_create_nodes() throws OpenAPISpecParseException {
        String specification = """
                openapi: 3.0.0
                info:
                  title: Sample Service
                  version: 1.0.0
                paths:
                  /kafka/group1/topicA/EventA:
                    post:
                      responses:
                        '200':
                          description: OK
                  /jms/topicB/EventB:
                    post:
                      responses:
                        '200':
                          description: OK
                components:
                  schemas:
                    EventA:
                      type: object
                      x-incoming:
                        topics: [topicA]
                    EventB:
                      type: object
                      x-incoming:
                        topics: [jms/topicB]
                      x-outgoing:
                        topics: [kafka/topicA]
                """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specification);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertEquals("Sample Service", result.getName());
        assertEquals(3, result.getNodes().size());
        assertEquals(3, result.getLinks().size());
        assertEquals(2, result.getEvents().size());

        NodeDTO serviceNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode);
        assertEquals("Sample Service", serviceNode.getName());

        NodeDTO topicANode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicA"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicANode);
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicANode.getBrokerType());

        NodeDTO topicBNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicB"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicBNode);
        assertEquals(NodeDTO.BrokerTypeEnum.JMS, topicBNode.getBrokerType());
        //check links Sample Service -> topicA, topicA -> Sample Service, topicB -> Sample Service
        List<LinkDTO> links = result.getLinks();
        assertEquals(3, links.size());

        LinkDTO serviceToTopicALink = links.stream()
                .filter(link -> link.getFromId().equals(serviceNode.getId()) && link.getToId().equals(topicANode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(serviceToTopicALink);

        LinkDTO topicAToServiceLink = links.stream()
                .filter(link -> link.getFromId().equals(topicANode.getId()) && link.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(topicAToServiceLink);

        LinkDTO topicBToServiceLink = links.stream()
                .filter(link -> link.getFromId().equals(topicBNode.getId()) && link.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(topicBToServiceLink);
    }

    // Parse OpenAPI specification and create EventGraph with multiple incoming and outgoing topics
    @Test
    public void test_multiple_incoming_outgoing_topics() throws OpenAPISpecParseException {
        String specification = """
                openapi: 3.0.0
                info:
                  title: Complex Service
                  version: 1.0.0
                paths:
                  /kafka/group1/topicA/EventA:
                    post:
                      responses:
                        '200':
                          description: OK
                  /kafka/group2/topicB/EventB:
                    post:
                      responses:
                        '200':
                          description: OK
                  /jms/topicC/EventC:
                    post:
                      responses:
                        '200':
                          description: OK
                  /rabbitmq/topicD/EventD:
                    post:
                      responses:
                        '200':
                          description: OK
                components:
                  schemas:
                    EventA:
                      type: object
                      x-incoming:
                        topics: [topicA]
                    EventB:
                      type: object
                      x-incoming:
                        topics: [topicB]
                    EventC:
                      type: object
                      x-incoming:
                        topics: [topicC]
                    EventD:
                      type: object
                      x-incoming:
                        topics: [topicD]
                """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specification);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertEquals("Complex Service", result.getName());
        assertEquals(5, result.getNodes().size());
        assertEquals(4, result.getLinks().size());
        assertEquals(4, result.getEvents().size());

        NodeDTO serviceNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode);
        assertEquals("Complex Service", serviceNode.getName());

        NodeDTO topicANode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicA"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicANode);
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicANode.getBrokerType());

        NodeDTO topicBNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicB"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicBNode);
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicBNode.getBrokerType());

        NodeDTO topicCNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicC"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicCNode);
        assertEquals(NodeDTO.BrokerTypeEnum.JMS, topicCNode.getBrokerType());

        NodeDTO topicDNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicD"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicDNode);
        assertEquals(NodeDTO.BrokerTypeEnum.RABBITMQ, topicDNode.getBrokerType());
    }

    // Handle OpenAPI specification with common elements in incoming and outgoing topics
    @Test
    public void test_common_elements_in_incoming_outgoing() throws OpenAPISpecParseException {
        String specification = """
                openapi: 3.0.0
                info:
                  title: Common Elements Service
                  version: 1.0.0
                paths:
                  /kafka/group1/topicX/EventX:
                    post:
                      responses:
                        '200':
                          description: OK
                  /jms/topicY/EventY:
                    post:
                      responses:
                        '200':
                          description: OK
                components:
                  schemas:
                    EventX:
                      type: object
                      x-incoming:
                        topics: [topicX]
                    EventY:
                      type: object
                      x-incoming:
                        topics: [topicY]
                """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specification);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertEquals("Common Elements Service", result.getName());
        assertEquals(3, result.getNodes().size());
        assertEquals(2, result.getLinks().size());
        assertEquals(2, result.getEvents().size());

        NodeDTO serviceNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode);
        assertEquals("Common Elements Service", serviceNode.getName());

        NodeDTO topicXNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicX"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicXNode);
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicXNode.getBrokerType());

        NodeDTO topicYNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.TOPIC && n.getName().equals("topicY"))
                .findFirst()
                .orElse(null);
        assertNotNull(topicYNode);
        assertEquals(NodeDTO.BrokerTypeEnum.JMS, topicYNode.getBrokerType());
    }

    // Handle missing x-incoming or x-outgoing extensions
    @Test
    public void test_parse_openapi_spec_with_missing_extensions() throws OpenAPISpecParseException {
        String specWithMissingExtensions = """
            openapi: 3.0.0
            info:
              title: Test Service
              version: 1.0.0
            paths:
              /kafka/group1/topic1/Event1:
                post:
                  responses:
                    '200':
                      description: OK
            components:
              schemas:
                Event1:
                  type: object
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specWithMissingExtensions);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertEquals("Test Service", result.getName());
        assertEquals(2, result.getNodes().size());
        assertEquals(1, result.getLinks().size());
        assertEquals(1, result.getEvents().size());

        NodeDTO serviceNode = result.getNodes().stream()
                .filter(n -> n.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode);
        assertEquals("Test Service", serviceNode.getName());
    }

    @Test
    public void test_parse_null_specification() {
        assertThrows(OpenAPISpecParseException.class, () -> SolidOpenAPITranslator.parseOPenAPI(null));
        assertThrows(OpenAPISpecParseException.class, () -> SolidOpenAPITranslator.parseOPenAPI(""));
        assertThrows(OpenAPISpecParseException.class, () -> SolidOpenAPITranslator.parseOPenAPI("                  \n\n\n\t\t\t      "));
    }

    @Test
    public void test_simple_text() {
        assertThrows(OpenAPISpecParseException.class, () -> SolidOpenAPITranslator.parseOPenAPI("not specification. simple text"));
    }

    @Test
    public void test_spec_empty_only_name_and_version() throws OpenAPISpecParseException {
        String specification = """
            openapi: 3.0.0
            info:
              title: Test Service
              version: 1.0.0
            paths:
              /kafka/group1/topic1/Event1:
                post:
                  summary: Retrieve Event1
                  responses:
                    '200':
                      description: Successful response
            components:
              schemas:
                Event1:
                    type: object
            """;
        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(specification);
        EventGraphDTO result = facade != null ? facade.eventGraph() : null;

        assertNotNull(result);
        assertNotNull(result.getName());

        log.info("Parsed EventGraphDTO:");
        log.info("Name: " + result.getName());

        log.info("Nodes (" + result.getNodes().size() + "):");
        result.getNodes().forEach(node ->
                log.info(" - Node ID: " + node.getId() + ", Name: " + node.getName() + ", Type: " + node.getType())
        );

        log.info("Events (" + result.getEvents().size() + "):");
        result.getEvents().forEach(event ->
                log.info(" - Event ID: " + event.getId() + ", Name: " + event.getName() + ", Tags: " + event.getTags())
        );

        assertEquals(2, result.getNodes().size(), "Expected 2 nodes");
        assertEquals("Test Service", result.getNodes().getFirst().getName(), "First node name mismatch");
        assertEquals(1, result.getLinks().size(), "Expected 1 link");
        assertEquals(1, result.getEvents().size(), "Expected 1 event");
        assertEquals("Test Service", result.getName(), "Graph name mismatch");
    }

}