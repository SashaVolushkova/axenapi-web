package pro.axenix_innovation.axenapi.web.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolidOpenAPITranslatorTest {

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn("stub message");
        MessageHelper.setStaticMessageSource(messageSource);
    }


    @Test
    void testGetWithoutRequestBody() throws OpenAPISpecParseException {
        String spec = """
openapi: 3.0.0
info:
  title: Simple API
  version: 1.0.0
paths:
  /test:
    get:
      summary: Simple get request
      responses:
        '200':
          description: OK
""";
        EventGraphFacade graph = SolidOpenAPITranslator.parseOPenAPI(spec);
        assertNotNull(graph);
        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getLinks().size());
        assertNull(graph.getLinks().get(0).getEventId());
    }

}
