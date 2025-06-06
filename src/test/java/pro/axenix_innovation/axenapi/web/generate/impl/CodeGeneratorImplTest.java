package pro.axenix_innovation.axenapi.web.generate.impl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import pro.axenix_innovation.axenapi.web.model.ServiceInfo;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CodeGeneratorImplTest {

    @BeforeEach
    void setUp() {
        MessageSource messageSource = Mockito.mock(MessageSource.class);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn("stub message");
        MessageHelper.setStaticMessageSource(messageSource);
    }


    @Test
    void testGenerateCode_withEmptyServiceInfoList() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        generator.generateCode(List.of(), "out/");
    }

    @Test
    void testGenerateCode_withNullServiceInfoList() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        try {
            generator.generateCode(null, "out/");
        } catch (Exception e) {
            throw new RuntimeException("Method threw exception on null list", e);
        }
    }

    @Test
    void testGenerateCode_withDirectoryEndingWithSlash() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName("Test Service");
        serviceInfo.setSpecificationPath("specs/api-docs.json");
        serviceInfo.setBrokerAddress("localhost:29092");
        serviceInfo.setPort("1234");

        generator.generateCode(List.of(serviceInfo), "out/");
    }

    @Test
    void testGenerateCode_withDirectoryWithoutSlash() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName("Test Service");
        serviceInfo.setSpecificationPath("specs/api-docs.json");
        serviceInfo.setBrokerAddress("localhost:29092");
        serviceInfo.setPort("1234");

        generator.generateCode(List.of(serviceInfo), "out");
    }

    @Test
    void testGenerateCode_withInvalidDirectory() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName("Invalid Service");
        serviceInfo.setSpecificationPath("nonexistent-spec.json");
        serviceInfo.setBrokerAddress("localhost:29092");
        serviceInfo.setPort("1234");

        generator.generateCode(List.of(serviceInfo), "invalid\\path");
    }

    @Test
    void testGenerateCode_createsDirectory() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName("MyService");
        serviceInfo.setSpecificationPath("path/to/spec.json");
        serviceInfo.setBrokerAddress("localhost:9092");
        serviceInfo.setPort("8080");

        String outputDir = "test-output/";

        generator.generateCode(List.of(serviceInfo), outputDir);

        File expectedDir = new File(outputDir + "MyService");
        assertTrue(expectedDir.exists() && expectedDir.isDirectory());

        expectedDir.delete();
    }

    @Test
    void testGenerateCode_withNullServiceInfoList_shouldNotThrowAndPrintWarning() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        generator.generateCode(null, "out/");

        String output = outContent.toString();
        assertFalse(output.isEmpty());
    }

    @Test
    void testGenerateCode_withEmptyServiceInfoList_shouldNotThrowAndPrintWarning() {
        CodeGeneratorImpl generator = new CodeGeneratorImpl();

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        generator.generateCode(Collections.emptyList(), "out/");

        String output = outContent.toString();
        assertFalse(output.isEmpty());
    }

}
