package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pro.axenix_innovation.axenapi.web.model.EventDTO;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.LinkDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.service.AllServicePdf;
import pro.axenix_innovation.axenapi.web.service.AllServicePdfGenerationService;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AxenAPIControllerPostGenerateAllServicePdfTest {

    private static final Logger log = LoggerFactory.getLogger(AxenAPIControllerPostGenerateAllServicePdfTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllServicePdfGenerationService allServicePdfGenerationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGeneratePdfForMultipleServices() throws Exception {
        UUID service1Id = UUID.randomUUID();
        UUID service2Id = UUID.randomUUID();
        UUID topic1Id = UUID.randomUUID();
        UUID topic2Id = UUID.randomUUID();
        UUID event1Id = UUID.randomUUID();
        UUID event2Id = UUID.randomUUID();

        NodeDTO service1 = new NodeDTO()
                .id(service1Id).name("ServiceNode1").type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(service1Id));

        NodeDTO service2 = new NodeDTO()
                .id(service2Id).name("ServiceNode2").type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(service2Id));

        NodeDTO topic1 = new NodeDTO()
                .id(topic1Id).name("TopicNode1").type(NodeDTO.TypeEnum.TOPIC)
                .belongsToGraph(List.of(service1Id));

        NodeDTO topic2 = new NodeDTO()
                .id(topic2Id).name("TopicNode2").type(NodeDTO.TypeEnum.TOPIC)
                .belongsToGraph(List.of(service2Id));

        EventDTO event1 = new EventDTO().id(event1Id).name("Event1").schema("{ \"type\": \"string\" }");
        EventDTO event2 = new EventDTO().id(event2Id).name("Event2").schema("{ \"type\": \"string\" }");

        LinkDTO link1 = new LinkDTO().fromId(service1Id).toId(topic1Id).eventId(event1Id);
        LinkDTO link2 = new LinkDTO().fromId(service2Id).toId(topic2Id).eventId(event2Id);

        EventGraphDTO graph = new EventGraphDTO()
                .name("TestGraph")
                .nodes(List.of(service1, service2, topic1, topic2))
                .events(List.of(event1, event2))
                .links(List.of(link1, link2));

        String graphJson = objectMapper.writeValueAsString(graph);

        byte[] fakePdf = "FAKE_PDF_CONTENT".repeat(1000).getBytes(StandardCharsets.UTF_8);
        AllServicePdf dummy = new AllServicePdf(Map.of("ServiceNode1.md", "# ServiceNode1", "ServiceNode2.md", "# ServiceNode2"), fakePdf);
        when(allServicePdfGenerationService.generateAllServicesPDF(any())).thenReturn(dummy);

        var result = mockMvc.perform(post("/generateAllServicePdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotEmpty();
        assertThat(pdfBytes.length).isGreaterThan(5000);

        File outFile = new File("build/generated/AllServicePDF/test-output.pdf");
        outFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(pdfBytes);
        }
        log.info("Saved test pdf to: {}", outFile.getAbsolutePath());
    }

    @Test
    void testGeneratePdfForMultipleServicesFileWithMock() throws Exception {
        File jsonFile = new File("src/test/resources/results/service_no_common_consume_topics_common_events_common_outgoing_topics_with_tags.json");
        EventGraphDTO graph = objectMapper.readValue(jsonFile, EventGraphDTO.class);

        byte[] fakePdf = "FAKE_PDF_CONTENT".repeat(1000).getBytes(StandardCharsets.UTF_8);
        Map<String, String> fakeMdFiles = Map.of(
                "ServiceNode1.md", "# ServiceNode1 Markdown Content",
                "ServiceNode2.md", "# ServiceNode2 Markdown Content"
        );
        AllServicePdf fakeAllServicePdf = new AllServicePdf(fakeMdFiles, fakePdf);

        when(allServicePdfGenerationService.generateAllServicesPDF(any(EventGraphDTO.class)))
                .thenReturn(fakeAllServicePdf);

        String graphJson = objectMapper.writeValueAsString(graph);

        var result = mockMvc.perform(post("/generateAllServicePdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(graphJson))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, StandardCharsets.UTF_8)).startsWith("FAKE_PDF_CONTENT");

        File outFile = new File("build/generated/AllServicePDF/test-output-mock.pdf");
        outFile.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(pdfBytes);
        }
        log.info("Сохранён моковый PDF в {}", outFile.getAbsolutePath());
    }

    @Test
    void shouldGeneratePdfSuccessfullyWithMultipleHttpNodes() throws Exception {
        UUID serviceId = UUID.randomUUID();
        UUID http1Id = UUID.randomUUID();
        UUID http2Id = UUID.randomUUID();
        UUID topicId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        NodeDTO service = new NodeDTO()
                .id(serviceId)
                .name("MyService")
                .type(NodeDTO.TypeEnum.SERVICE)
                .belongsToGraph(List.of(serviceId));

        NodeDTO http1 = new NodeDTO()
                .id(http1Id)
                .name("Http1")
                .type(NodeDTO.TypeEnum.HTTP)
                .belongsToGraph(List.of(serviceId));

        NodeDTO http2 = new NodeDTO()
                .id(http2Id)
                .name("Http2")
                .type(NodeDTO.TypeEnum.HTTP)
                .belongsToGraph(List.of(serviceId));

        NodeDTO topic = new NodeDTO()
                .id(topicId)
                .name("MyTopic")
                .type(NodeDTO.TypeEnum.TOPIC)
                .belongsToGraph(List.of(serviceId));

        EventDTO event = new EventDTO()
                .id(eventId)
                .name("MyEvent")
                .schema("{ \"type\": \"string\" }");

        LinkDTO linkToHttp1 = new LinkDTO()
                .fromId(serviceId)
                .toId(http1Id)
                .tags(Set.of("calls"));

        LinkDTO linkToHttp2 = new LinkDTO()
                .fromId(serviceId)
                .toId(http2Id)
                .tags(Set.of("invokes"));

        LinkDTO linkToTopic = new LinkDTO()
                .fromId(serviceId)
                .toId(topicId)
                .eventId(eventId);

        EventGraphDTO graph = new EventGraphDTO()
                .name("GraphWithMultipleHttp")
                .nodes(List.of(service, http1, http2, topic))
                .events(List.of(event))
                .links(List.of(linkToHttp1, linkToHttp2, linkToTopic));

        byte[] fakePdf = "PDF".getBytes();

        AllServicePdf fakeDocs = new AllServicePdf(
                Map.of(
                        "MyService_main.md", "# MyService\n\n[Спецификация](MyService_spec.md)",
                        "MyService_spec.md", "# Спецификация сервиса MyService\n```yaml\nopenapi: 3.0.0\ninfo:\n  title: MyService API\n```"
                ),
                fakePdf
        );

        when(allServicePdfGenerationService.generateAllServicesPDF(any(EventGraphDTO.class)))
                .thenReturn(fakeDocs);

        mockMvc.perform(post("/generateAllServicePdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(graph)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=")))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(fakePdf));
    }
}
