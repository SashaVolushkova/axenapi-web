package pro.axenix_innovation.axenapi.web.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPIToEventGraphFacadeTest {

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MessageHelper.setStaticMessageSource(messageSource);
    }
    @Test
    void test_outgoing_event_and_link_have_tag_1event() throws OpenAPISpecParseException {
        String spec = """
        openapi: 3.0.1
        info:
          title: 1
          description: AxenAPI Specification for 1
          version: 1.0.0
        paths: {}
        components:
          schemas:
            1:
              type: object
              properties:
                newProperty_1752127370778:
                  type: string
              x-outgoing:
                topics: ["KAFKA/Topic_0"]
                tags: ["1event"]
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        assertFalse(graphDTO.getEvents().isEmpty(), "Должно быть хотя бы одно событие");

        EventDTO eventWithTag = graphDTO.getEvents().stream()
                .filter(event -> event.getTags() != null && event.getTags().contains("1event"))
                .findFirst()
                .orElse(null);

        assertNotNull(eventWithTag, "Событие должно содержать тэг '1event'");

        LinkDTO linkToEvent = graphDTO.getLinks().stream()
                .filter(link -> link.getEventId() != null && link.getEventId().equals(eventWithTag.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(linkToEvent, "Должна существовать связь, ссылающаяся на событие с тэгом '1event'");

        assertTrue(
                linkToEvent.getTags() != null && linkToEvent.getTags().contains("1event"),
                "Связь должна содержать тэг '1event'"
        );

        NodeDTO serviceNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);

        assertNotNull(serviceNode, "Сервисный узел должен существовать");

        assertTrue(
                serviceNode.getTags() != null && serviceNode.getTags().contains("1event"),
                "Сервисный узел должен содержать тэг '1event'"
        );
    }

    @Test
    void test_outgoing_incoming_and_http_tags() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "2",
            "description" : "AxenAPI Specification for 2",
            "version" : "1.0.0"
          },
          "paths" : {
            "/jms/Topic_1/2" : {
              "post" : {
                "tags" : [ "2event" ],
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            },
            "/http1" : {
              "summary" : "Retrieve a specific event by ID",
              "get" : {
                "tags" : [ "HTTP", "http3event" ],
                "parameters" : [ {
                  "name" : "eventId",
                  "in" : "path",
                  "description" : "ID of the event to retrieve",
                  "required" : true,
                  "schema" : {
                    "type" : "string"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Event retrieved successfully",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/3"
                        }
                      }
                    }
                  },
                  "404" : {
                    "description" : "Event not found"
                  }
                }
              },
              "patch" : {
                "tags" : [ "HTTP", "http3event" ],
                "parameters" : [ {
                  "name" : "eventId",
                  "in" : "path",
                  "description" : "ID of the event to retrieve",
                  "required" : true,
                  "schema" : {
                    "type" : "string"
                  }
                } ],
                "responses" : {
                  "200" : {
                    "description" : "Event retrieved successfully",
                    "content" : {
                      "application/json" : {
                        "schema" : {
                          "$ref" : "#/components/schemas/3"
                        }
                      }
                    }
                  },
                  "404" : {
                    "description" : "Event not found"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {
              "2" : {
                "type" : "object",
                "properties" : {
                  "newProperty_1752127377116" : {
                    "type" : "string"
                  }
                },
                "x-incoming" : {
                  "topics" : [ "Topic_1" ],
                  "tags" : [ "2event" ]
                }
              },
              "3" : {
                "type" : "object",
                "properties" : {
                  "newProperty_1752127615498" : {
                    "type" : "string"
                  }
                },
                "x-incoming" : {
                  "topics" : [ "Http_0" ],
                  "tags" : [ "HTTP", "http3event" ]
                }
              }
            }
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        assertFalse(graphDTO.getEvents().isEmpty(), "Должно быть хотя бы одно событие");

        EventDTO eventWithTag = graphDTO.getEvents().stream()
                .filter(event -> event.getTags() != null && event.getTags().contains("2event"))
                .findFirst()
                .orElse(null);

        assertNotNull(eventWithTag, "Событие должно содержать тэг '2event'");

        LinkDTO linkToEvent = graphDTO.getLinks().stream()
                .filter(link -> link.getEventId() != null && link.getEventId().equals(eventWithTag.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(linkToEvent, "Должна существовать связь, ссылающаяся на событие с тэгом '2event'");

        assertTrue(
                linkToEvent.getTags() != null && linkToEvent.getTags().contains("2event"),
                "Связь должна содержать тэг '2event'"
        );

        NodeDTO serviceNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);

        assertNotNull(serviceNode, "Сервисный узел должен существовать");

        assertTrue(
                serviceNode.getTags() != null && serviceNode.getTags().contains("2event"),
                "Сервисный узел должен содержать тэг '2event'"
        );


        EventDTO httpEvent = graphDTO.getEvents().stream()
                .filter(event -> event.getName().equals("3")) // из $ref: #/components/schemas/3
                .findFirst()
                .orElse(null);

        assertNotNull(httpEvent, "Событие '3' должно существовать");

        assertTrue(
                httpEvent.getTags() != null && httpEvent.getTags().contains("HTTP"),
                "Событие '3' должно содержать тэг 'HTTP'"
        );

        assertTrue(
                httpEvent.getTags() != null && httpEvent.getTags().contains("http3event"),
                "Событие '3' должно содержать тэг 'http3event'"
        );


        List<LinkDTO> linksToHttpEvent = graphDTO.getLinks().stream()
                .filter(link -> link.getEventId() != null && link.getEventId().equals(httpEvent.getId()))
                .toList();

        // В новой логике HTTP методы без request body не создают связи с событиями
        // GET и PATCH методы в этом тесте ссылаются на схему "3" только в response, не в request body
        assertTrue(linksToHttpEvent.isEmpty(), "Не должны существовать связи, ссылающиеся на событие '3', так как это response-схема");

        // Проверяем, что HTTP узлы создались с null eventId
        List<NodeDTO> httpNodes = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.HTTP)
                .toList();

        assertFalse(httpNodes.isEmpty(), "HTTP узлы должны существовать");

        List<LinkDTO> httpLinks = graphDTO.getLinks().stream()
                .filter(link -> httpNodes.stream().anyMatch(node -> node.getId().equals(link.getFromId())))
                .toList();

        assertFalse(httpLinks.isEmpty(), "Связи от HTTP узлов должны существовать");

        boolean allHttpLinksHaveNullEvent = httpLinks.stream()
                .allMatch(link -> link.getEventId() == null);

        assertTrue(allHttpLinksHaveNullEvent, "Все связи от HTTP узлов должны иметь null eventId, так как нет request body");
    }

    @Test
    void test_kafka_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/kafka/default/TopicA/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался сервисный узел
        NodeDTO serviceNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode, "Сервисный узел должен существовать");
        assertEquals("ServiceA", serviceNode.getName());

        // Проверяем, что создался топик узел
        NodeDTO topicNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(topicNode, "Топик узел должен существовать");
        assertEquals("TopicA", topicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, topicNode.getBrokerType());

        // Проверяем, что создалась связь с null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .filter(l -> l.getFromId().equals(topicNode.getId()) && l.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь между топиком и сервисом должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для undefined_event");
    }

    @Test
    void test_jms_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/jms/TopicA/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался топик узел с JMS брокером
        NodeDTO topicNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(topicNode, "Топик узел должен существовать");
        assertEquals("TopicA", topicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.JMS, topicNode.getBrokerType());

        // Проверяем, что создалась связь с null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для undefined_event");
    }

    @Test
    void test_rabbitmq_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/rabbitmq/TopicA/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался топик узел с RabbitMQ брокером
        NodeDTO topicNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(topicNode, "Топик узел должен существовать");
        assertEquals("TopicA", topicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.RABBITMQ, topicNode.getBrokerType());

        // Проверяем, что создалась связь с null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для undefined_event");
    }

    @Test
    void test_undefined_broker_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/undefined_broker/TopicA/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался топик узел с undefined брокером
        NodeDTO topicNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(topicNode, "Топик узел должен существовать");
        assertEquals("TopicA", topicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.UNDEFINED,topicNode.getBrokerType(), "BrokerType должен быть null для undefined_broker");

        // Проверяем, что создалась связь с null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для undefined_event");
    }

    @Test
    void test_http_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/test" : {
              "get" : {
                "responses" : {
                  "200" : {
                    "description" : "OK"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался сервисный узел
        NodeDTO serviceNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode, "Сервисный узел должен существовать");

        // Проверяем, что создался HTTP узел
        NodeDTO httpNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.HTTP)
                .findFirst()
                .orElse(null);
        assertNotNull(httpNode, "HTTP узел должен существовать");
        assertEquals("/test", httpNode.getNodeUrl());
        assertEquals(NodeDTO.MethodTypeEnum.GET, httpNode.getMethodType());

        // Проверяем, что создалась связь с null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .filter(l -> l.getFromId().equals(httpNode.getId()) && l.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь между HTTP узлом и сервисом должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для HTTP запроса без события");
    }

    @Test
    void test_http_post_undefined_event_creates_null_event_link() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/test" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "OK"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался HTTP узел с POST методом
        NodeDTO httpNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.HTTP)
                .findFirst()
                .orElse(null);
        assertNotNull(httpNode, "HTTP узел должен существовать");
        assertEquals("/test", httpNode.getNodeUrl());
        assertEquals(NodeDTO.MethodTypeEnum.POST, httpNode.getMethodType());

        // Проверяем, что создалась связь �� null eventId
        LinkDTO link = graphDTO.getLinks().stream()
                .findFirst()
                .orElse(null);
        assertNotNull(link, "Связь должна существовать");
        assertNull(link.getEventId(), "EventId должен быть null для HTTP POST запроса без события");
    }

    @Test
    void test_multiple_undefined_events_create_multiple_null_event_links() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "ServiceA",
            "description" : "AxenAPI Specification for ServiceA",
            "version" : "1.0.0"
          },
          "paths" : {
            "/kafka/default/TopicA/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            },
            "/jms/TopicB/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            },
            "/test" : {
              "get" : {
                "responses" : {
                  "200" : {
                    "description" : "OK"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создались все узлы
        assertEquals(4, graphDTO.getNodes().size(), "Должно быть 4 узла: 1 сервис, 2 топика, 1 HTTP");

        // Проверяем, что все связи имеют null eventId
        List<LinkDTO> linksWithNullEvent = graphDTO.getLinks().stream()
                .filter(link -> link.getEventId() == null)
                .toList();
        assertEquals(3, linksWithNullEvent.size(), "Должно быть 3 связи с null eventId");

        // Проверяем, что нет событий в графе
        assertTrue(graphDTO.getEvents().isEmpty(), "События не должны создаваться для undefined_event");
    }

    @Test
    void test_http_method_with_empty_request_and_kafka_undefined_event() throws OpenAPISpecParseException {
        String spec = """
        {
          "openapi" : "3.0.1",
          "info" : {
            "title" : "TestService",
            "description" : "AxenAPI Specification for TestService",
            "version" : "1.0.0"
          },
          "paths" : {
            "/api/data" : {
              "get" : {
                "responses" : {
                  "200" : {
                    "description" : "Success"
                  }
                }
              }
            },
            "/kafka/default/TestTopic/undefined_event" : {
              "post" : {
                "responses" : {
                  "200" : {
                    "description" : "Event sent successfully"
                  }
                }
              }
            }
          },
          "components" : {
            "schemas" : {}
          }
        }
        """;

        EventGraphFacade facade = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(facade, "EventGraphFacade не должен быть null");

        EventGraphDTO graphDTO = facade.eventGraph();
        assertNotNull(graphDTO, "EventGraphDTO не должен быть null");

        // Проверяем, что создался сервисный узел
        NodeDTO serviceNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.SERVICE)
                .findFirst()
                .orElse(null);
        assertNotNull(serviceNode, "Сервисный узел должен существовать");
        assertEquals("TestService", serviceNode.getName());

        // Проверяем, что создался HTTP узел с GET методом и пустым запросом
        NodeDTO httpNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.HTTP)
                .findFirst()
                .orElse(null);
        assertNotNull(httpNode, "HTTP узел должен существовать");
        assertEquals("/api/data", httpNode.getNodeUrl());
        assertEquals(NodeDTO.MethodTypeEnum.GET, httpNode.getMethodType());

        // Проверяем, что создался Kafka топик узел
        NodeDTO kafkaTopicNode = graphDTO.getNodes().stream()
                .filter(node -> node.getType() == NodeDTO.TypeEnum.TOPIC)
                .findFirst()
                .orElse(null);
        assertNotNull(kafkaTopicNode, "Kafka топик узел должен существовать");
        assertEquals("TestTopic", kafkaTopicNode.getName());
        assertEquals(NodeDTO.BrokerTypeEnum.KAFKA, kafkaTopicNode.getBrokerType());

        // Проверяем, что создались связи с null eventId
        List<LinkDTO> linksWithNullEvent = graphDTO.getLinks().stream()
                .filter(link -> link.getEventId() == null)
                .toList();
        assertEquals(2, linksWithNullEvent.size(), "Должно быть 2 связи с null eventId");

        // Проверяем связь от HTTP узла к сервису
        LinkDTO httpLink = graphDTO.getLinks().stream()
                .filter(link -> link.getFromId().equals(httpNode.getId()) && 
                               link.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(httpLink, "Связь от HTTP узла к сервису должна существовать");
        assertNull(httpLink.getEventId(), "EventId должен быть null для HTTP запроса без события");

        // Проверяем связь от Kafka топика к сервису
        LinkDTO kafkaLink = graphDTO.getLinks().stream()
                .filter(link -> link.getFromId().equals(kafkaTopicNode.getId()) && 
                               link.getToId().equals(serviceNode.getId()))
                .findFirst()
                .orElse(null);
        assertNotNull(kafkaLink, "Связь от Kafka топика к сервису должна существовать");
        assertNull(kafkaLink.getEventId(), "EventId должен быть null для undefined_event");

        // Проверяем, что нет событий в графе
        assertTrue(graphDTO.getEvents().isEmpty(), "События не должны создаваться для undefined_event и пустых HTTP запросов");

        // Проверяем общее количество узлов
        assertEquals(3, graphDTO.getNodes().size(), "Должно быть 3 узла: 1 сервис, 1 HTTP, 1 Kafka топик");
    }
}