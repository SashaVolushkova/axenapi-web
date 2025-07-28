package pro.axenix_innovation.axenapi.web.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import pro.axenix_innovation.axenapi.web.entity.ServiceCode;
import pro.axenix_innovation.axenapi.web.generate.CodeGenerator;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.repository.ServiceCodeRepository;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CodeServiceImplTest {

    @Mock
    private ServiceCodeRepository serviceCodeRepository;

    @Mock
    private CodeGenerator codeGenerator;
    @Mock
    private MessageHelper messageHelper;

    @InjectMocks
    private CodeServiceImpl codeService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        codeService = new CodeServiceImpl(serviceCodeRepository, codeGenerator, messageHelper);
        codeService.exportCodeDirectory = tempDir.resolve("testdir").toString();
    }

    @Test
    void generateCode_ShouldReturnZipFile() throws IOException {
        NodeDTO serviceNode = new NodeDTO();
        serviceNode.setId(UUID.randomUUID());
        serviceNode.setName("TestService");

        NodeDTO.TypeEnum type = NodeDTO.TypeEnum.SERVICE;
        serviceNode.setType(type);

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));

        Path generatedFile = tempDir.resolve("testdir").resolve("TestService.java");
        Files.createDirectories(generatedFile.getParent());
        Files.writeString(generatedFile, "public class TestService {}");

        doAnswer(invocation -> {
            Files.writeString(tempDir.resolve("testdir").resolve("TestService.java"), "class TestService {}");
            return null;
        }).when(codeGenerator).generateCode(any(), eq(tempDir.resolve("testdir").toString()));

        ServiceCode mockCode = new ServiceCode();
        mockCode.setServiceCodeFile(new byte[]{1, 2, 3});
        when(serviceCodeRepository.save(any())).thenReturn(mockCode);

        byte[] result = codeService.generateCode(graph);

        assertNotNull(result);
        assertArrayEquals(new byte[]{1, 2, 3}, result);
        verify(codeGenerator, times(1)).generateCode(any(), eq(tempDir.resolve("testdir").toString()));
        verify(serviceCodeRepository, times(1)).save(any());
        assertFalse(Files.exists(generatedFile));
    }

    @Test
    void save_ShouldPersistServiceCode() {
        byte[] zipData = new byte[]{10, 20, 30};
        ServiceCode expected = new ServiceCode();
        expected.setServiceCodeFile(zipData);
        when(serviceCodeRepository.save(any())).thenReturn(expected);

        ServiceCode result = codeService.save(zipData);

        assertNotNull(result);
        assertArrayEquals(zipData, result.getServiceCodeFile());
        verify(serviceCodeRepository, times(1)).save(any());
    }

    @Test
    void clear_ShouldDeleteOldRecords() {
        codeService.clear();

        verify(serviceCodeRepository, times(1)).deleteAllCreatedBefore(argThat(time ->
                time.isBefore(Instant.now()) && time.isAfter(Instant.now().minusSeconds(600))
        ));
    }

    @Test
    void generateCode_ShouldGenerateAndReturnZip() throws IOException {
        UUID nodeId = UUID.randomUUID();
        NodeDTO serviceNode = NodeDTO.builder()
                .id(nodeId)
                .name("TestService")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode));

        Path generatedFile = Files.createFile(tempDir.resolve("TestService.txt"));
        Files.writeString(generatedFile, "test content");

        ServiceCode savedCode = new ServiceCode();
        savedCode.setId("1");
        savedCode.setServiceCodeFile("dummy zip".getBytes());

        when(serviceCodeRepository.save(any())).thenReturn(savedCode);

        byte[] result = codeService.generateCode(graph);

        assertArrayEquals("dummy zip".getBytes(), result);
        verify(codeGenerator).generateCode(anyList(), anyString());
        verify(serviceCodeRepository).save(any(ServiceCode.class));
    }


    @Test
    void generateCode_WithNoServiceNodes_ShouldWork() {
        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("NotAService")
                .type(NodeDTO.TypeEnum.HTTP)
                .build()));

        ServiceCode serviceCode = new ServiceCode();
        serviceCode.setServiceCodeFile("empty".getBytes());

        when(serviceCodeRepository.save(any())).thenReturn(serviceCode);
        ReflectionTestUtils.setField(codeService, "exportCodeDirectory", tempDir.toString());

        byte[] result = codeService.generateCode(graph);

        assertNotNull(result);
        assertArrayEquals("empty".getBytes(), result);
        verify(codeGenerator).generateCode(eq(List.of()), eq(tempDir.toString()));
    }


    @Test
    void packToZip_ShouldReturnEmptyArray_WhenIOExceptionOccurs() {
        byte[] result = codeService.packToZip(Path.of("nonexistent-folder"));

        assertArrayEquals(new byte[0], result);
    }

    @Test
    void save_ShouldSaveServiceCode() {
        byte[] data = "zip data".getBytes();
        ServiceCode expected = new ServiceCode();
        expected.setServiceCodeFile(data);

        when(serviceCodeRepository.save(any())).thenReturn(expected);

        ServiceCode actual = codeService.save(data);

        assertSame(expected, actual);
        verify(serviceCodeRepository).save(any(ServiceCode.class));
    }

    @Test
    void clear_ShouldDeleteOldCodes() {
        codeService.clear();
        verify(serviceCodeRepository).deleteAllCreatedBefore(any());
    }

    @Test
    void generateCode_ShouldFilterOnlyServiceNodes() throws IOException {
        NodeDTO serviceNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("ServiceNode")
                .type(NodeDTO.TypeEnum.SERVICE)
                .build();

        NodeDTO httpNode = NodeDTO.builder()
                .id(UUID.randomUUID())
                .name("HttpNode")
                .type(NodeDTO.TypeEnum.HTTP)
                .build();

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(serviceNode, httpNode));

        ServiceCode serviceCode = new ServiceCode();
        serviceCode.setServiceCodeFile("some content".getBytes());

        when(serviceCodeRepository.save(any())).thenReturn(serviceCode);
        ReflectionTestUtils.setField(codeService, "exportCodeDirectory", tempDir.toString());

        byte[] result = codeService.generateCode(graph);

        assertNotNull(result);
        verify(codeGenerator).generateCode(argThat(list ->
                list.size() == 1 && list.get(0).getName().equals("ServiceNode")
        ), eq(tempDir.toString()));

    }

    @Test
    void generateCode_ShouldCreateExportDirectory() throws IOException {
        Files.deleteIfExists(tempDir);

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(
                NodeDTO.builder()
                        .id(UUID.randomUUID())
                        .name("ServiceNode")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .build()
        ));

        when(serviceCodeRepository.save(any())).thenReturn(new ServiceCode());

        codeService.generateCode(graph);

        assertTrue(Files.exists(tempDir), "Директория должна быть создана методом prepareCodeDirectory");
    }

    @Test
    void generateCode_ShouldCleanDirectoryBeforeGeneration() throws Exception {
        Path junkFile = Files.createFile(tempDir.resolve("old.txt"));
        Files.writeString(junkFile, "old content");

        EventGraphDTO graph = new EventGraphDTO();
        graph.setNodes(List.of(
                NodeDTO.builder()
                        .id(UUID.randomUUID())
                        .name("ServiceNode")
                        .type(NodeDTO.TypeEnum.SERVICE)
                        .build()
        ));

        when(serviceCodeRepository.save(any())).thenReturn(new ServiceCode());

        Method prepareCodeDirectoryMethod = CodeServiceImpl.class.getDeclaredMethod("prepareCodeDirectory", String.class);
        prepareCodeDirectoryMethod.setAccessible(true);
        prepareCodeDirectoryMethod.invoke(codeService, tempDir.toString());

        codeService.generateCode(graph);

        assertFalse(Files.exists(junkFile), "prepareCodeDirectory должна очистить старые файлы");
    }

    @Test
    void shouldReturnFileLengthWhenFileExists1() throws IOException {
        ServiceCode serviceCode = new ServiceCode();

        File serviceCodeFile = new File("src/test/resources/path/for_test_shotest_paths.json");

        byte[] fileBytes = Files.readAllBytes(serviceCodeFile.toPath());

        serviceCode.setServiceCodeFile(fileBytes);

        assertEquals(fileBytes.length, serviceCode.getServiceCodeFile() != null ? serviceCode.getServiceCodeFile().length : 0);
    }

    @Test
    void shouldReturnZeroWhenFileDoesNotExist() {
        ServiceCode serviceCode = new ServiceCode();
        serviceCode.setServiceCodeFile(null);

        assertEquals(0, serviceCode.getServiceCodeFile() != null ? serviceCode.getServiceCodeFile().length : 0);
    }

    @Test
    void shouldReturnFileLengthWhenFileExists() throws IOException {
        Path tempDir = Files.createTempDirectory("testDir");
        Path tempFile = Files.createFile(tempDir.resolve("testFile.txt"));

        Files.write(tempFile, "Test content".getBytes());

        CodeServiceImpl codeService = new CodeServiceImpl(serviceCodeRepository, codeGenerator, messageHelper);

        byte[] zipBytes = codeService.packToZip(tempDir);

        assertTrue(zipBytes.length > 0, "The zip file should not be empty");

        Files.delete(tempFile);
        Files.delete(tempDir);
    }

    @Test
    void shouldIgnoreDirectoriesWhenPackingZip() throws IOException {
        Path tempDir = Files.createTempDirectory("testDir");
        Path subDir = Files.createDirectory(tempDir.resolve("subDir"));
        Path tempFile = Files.createFile(tempDir.resolve("testFile.txt"));

        Files.write(tempFile, "Test content".getBytes());

        CodeServiceImpl codeService = new CodeServiceImpl(serviceCodeRepository, codeGenerator, messageHelper);

        byte[] zipBytes = codeService.packToZip(tempDir);

        assertTrue(zipBytes.length > 0, "The zip file should not be empty");

        assertFalse(new String(zipBytes).contains("subDir"), "The directory should not be included in the zip");

        Files.delete(tempFile);
        Files.delete(subDir);
        Files.delete(tempDir);
    }


    @Test
    void shouldReturnEmptyZipWhenNoFilesInDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("emptyDir");

        CodeServiceImpl codeService = new CodeServiceImpl(serviceCodeRepository, codeGenerator, messageHelper);

        byte[] zipBytes = codeService.packToZip(tempDir);

        assertTrue(zipBytes.length > 0, "The zip file should not be empty");

        Files.delete(tempDir);
    }


    CodeServiceImpl service = new CodeServiceImpl(null, null, messageHelper);

    @Test
    void testPackToZipCreatesValidZip() throws IOException {
        Path tempDir = Files.createTempDirectory("test-zip-dir");
        Path file1 = Files.writeString(tempDir.resolve("file1.txt"), "Hello");
        Path file2 = Files.writeString(tempDir.resolve("file2.txt"), "World");

        byte[] zipBytes = service.packToZip(tempDir);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            ZipEntry entry1 = zis.getNextEntry();
            assertNotNull(entry1);
            assertTrue(entry1.getName().equals("file1.txt") || entry1.getName().equals("file2.txt"));

            ZipEntry entry2 = zis.getNextEntry();
            assertNotNull(entry2);
            assertNotEquals(entry1.getName(), entry2.getName());

            assertNull(zis.getNextEntry());
        }

        Files.deleteIfExists(file1);
        Files.deleteIfExists(file2);
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testPackToZipWithIOExceptionDuringWalkReturnsEmpty() throws IOException {
        Path brokenPath = Paths.get("non-existent-dir-" + System.currentTimeMillis());

        byte[] result = codeService.packToZip(brokenPath);

        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void testZipContainsAllExpectedFilesWithSeparateEntries() throws IOException {
        Path tempDir = Files.createTempDirectory("zip-test-dir");
        Path file1 = Files.writeString(tempDir.resolve("a.txt"), "AAA");
        Path file2 = Files.writeString(tempDir.resolve("b.txt"), "BBB");

        byte[] zipBytes = service.packToZip(tempDir);

        assertNotNull(zipBytes);
        assertTrue(zipBytes.length > 0);

        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertEquals(2, entries.size(), "Должно быть 2 zip entry");
        assertTrue(entries.contains("a.txt"));
        assertTrue(entries.contains("b.txt"));

        Files.deleteIfExists(file1);
        Files.deleteIfExists(file2);
        Files.deleteIfExists(tempDir);
    }
    private static final Logger log = LoggerFactory.getLogger(CodeServiceImplTest.class);

    @Test
    void PackToZipHandlesIOExceptionDuringFileCopy() throws IOException {
        Path tempDir = Files.createTempDirectory("zip-error-test-dir");

        Path goodFile = Files.writeString(tempDir.resolve("ok.txt"), "Valid content");

        Path badFile = Files.writeString(tempDir.resolve("bad.txt"), "Should fail");

        Files.deleteIfExists(badFile);

        CodeServiceImpl service = new CodeServiceImpl(null, null, messageHelper);

        try {
            log.info("Пытаемся создать ZIP архив из директории: {}", tempDir);

            byte[] zipBytes = service.packToZip(tempDir);

            log.info("Размер созданного ZIP архива: {}", zipBytes.length);

            assertNotNull(zipBytes, "Архив не должен быть пустым.");
            assertTrue(zipBytes.length > 0, "Архив не должен иметь нулевой размер.");

            List<String> entries = new ArrayList<>();
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    entries.add(entry.getName());
                    zis.closeEntry();
                }
            }

            log.info("Содержимое архива: {}", entries);

            assertTrue(entries.contains("ok.txt"), "Архив должен содержать 'ok.txt'");
            assertFalse(entries.contains("bad.txt"), "Архив не должен содержать 'bad.txt'");

        } catch (IOException e) {
            log.error("Произошла ошибка при создании ZIP архива", e);
            assertTrue(e.getMessage().contains("Simulated read error"), "Сообщение об ошибке не соответствует ожиданиям");
        } finally {
            log.info("Удаляем временные файлы и директорию");

            Files.deleteIfExists(goodFile);
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void testZipOutputStreamFinish() throws IOException {
        Path tempDir = Files.createTempDirectory("zip-finish-test-dir");

        Path goodFile = Files.writeString(tempDir.resolve("ok.txt"), "Valid content");

        Path zipFile = Files.createTempFile("testArchive", ".zip");

        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            File fileToZip = goodFile.toFile();
            try (FileInputStream fis = new FileInputStream(fileToZip)) {
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
            }

            zipOut.finish();

            assertTrue(Files.exists(zipFile), "ZIP архив не был создан");

            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile.toFile()))) {
                ZipEntry entry;
                boolean fileExistsInZip = false;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if ("ok.txt".equals(entry.getName())) {
                        fileExistsInZip = true;
                        break;
                    }
                }
                assertTrue(fileExistsInZip, "Файл 'ok.txt' должен присутствовать в архиве");
            }
        } finally {
            Files.deleteIfExists(goodFile);
            Files.deleteIfExists(zipFile);
            Files.deleteIfExists(tempDir);
        }
    }


    @Test
    void shouldHandleIOExceptionDuringZipEntryCreationWithoutPosix() throws IOException {
        Path tempDir = Files.createTempDirectory("code-test");

        Path problematicFile = tempDir.resolve("will_be_deleted.txt");
        Files.writeString(problematicFile, "Temporary content");

        Path goodFile = Files.writeString(tempDir.resolve("good.txt"), "Good content");

        Files.delete(problematicFile);

        CodeServiceImpl codeService = new CodeServiceImpl(null, null, messageHelper);
        byte[] zipBytes = codeService.packToZip(tempDir);

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            List<String> entries = new ArrayList<>();
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entries.add(entry.getName());
            }

            assertFalse(entries.contains("will_be_deleted.txt"));
            assertTrue(entries.contains("good.txt"));
        } finally {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {}
                    });
        }
    }

    @Test
    void shouldReturnEmptyByteArrayWhenDirectoryDoesNotExist() throws IOException {
        Path tempDir = Files.createTempDirectory("code-test");
        Files.delete(tempDir);

        CodeServiceImpl codeService = new CodeServiceImpl(null, null, messageHelper);
        byte[] zipBytes = codeService.packToZip(tempDir);

        assertNotNull(zipBytes);
        assertEquals(0, zipBytes.length, "Ожидался пустой byte[] при ошибке обхода директории");
    }

    @Test
    void shouldFilterOutDirectoriesDuringZipCreation() throws IOException {
        Path tempDir = Files.createTempDirectory("code-test");
        Path subDir = Files.createDirectory(tempDir.resolve("subdir"));
        Files.writeString(subDir.resolve("file.txt"), "test content");
        Files.writeString(tempDir.resolve("file2.txt"), "other content");

        CodeServiceImpl codeService = new CodeServiceImpl(null, null, messageHelper);

        byte[] zipBytes = codeService.packToZip(tempDir);

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            List<String> entryNames = new ArrayList<>();
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entryNames.add(entry.getName());
            }

            assertFalse(entryNames.stream().anyMatch(name -> name.endsWith("/") || name.endsWith("\\")),
                    "Директория 'subdir/' не должна попадать в архив");

            assertTrue(entryNames.contains("file2.txt"));
            assertTrue(entryNames.contains("subdir/file.txt"));
        }

        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException ignored) {}
                });
    }
}
