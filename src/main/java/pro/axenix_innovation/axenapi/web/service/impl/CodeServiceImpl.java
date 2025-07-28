package pro.axenix_innovation.axenapi.web.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.axenix_innovation.axenapi.web.entity.ServiceCode;
import pro.axenix_innovation.axenapi.web.generate.CodeGenerator;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.NodeDTO;
import pro.axenix_innovation.axenapi.web.model.ServiceInfo;
import pro.axenix_innovation.axenapi.web.repository.ServiceCodeRepository;
import pro.axenix_innovation.axenapi.web.service.CodeService;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_ADDING_FILE_TO_ARCHIVE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_CREATE_ZIP_ARCHIVE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_DELETING_PATH;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_READ_FILE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_TRAVERSING_FILES_DIRECTORY;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.FAIL_TO_PREPARE_DIR;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_COUL_NOT_DELETE_FILE;

@Service
@RequiredArgsConstructor
public class CodeServiceImpl implements CodeService {

    private static final Logger log = LoggerFactory.getLogger(CodeServiceImpl.class);

    private final ServiceCodeRepository serviceCodeRepository;
    private final CodeGenerator codeGenerator;
    private final MessageHelper messageHelper;

    @Value("${export.code.folder:src/main/resources/code}")
    String exportCodeDirectory;

    @Override
    @Transactional
    public byte[] generateCode(EventGraphDTO eventGraph) {
        List<NodeDTO> services = eventGraph.getNodes().stream()
                .filter(node -> "SERVICE".equals(node.getType().getValue()))
                .toList();

        AtomicInteger counter = new AtomicInteger(88);

        List<ServiceInfo> serviceInfoList = services.stream()
                .map(service -> new ServiceInfo(
                        service.getName(),
                        String.valueOf(counter.getAndIncrement()),
                        service.getName(),
                        "localhost:2181"
                ))
                .toList();

        prepareCodeDirectory(exportCodeDirectory);
        codeGenerator.generateCode(serviceInfoList, exportCodeDirectory);

        byte[] zip = packToZip(Path.of(exportCodeDirectory));
        log.info(messageHelper.getMessage("axenapi.info.gen.byte.code", zip.length));

        ServiceCode serviceCode = save(zip);
        log.info(messageHelper.getMessage("axenapi.info.saved.service.code.id.size",
                serviceCode.getId(),
                serviceCode.getServiceCodeFile() != null ? serviceCode.getServiceCodeFile().length : 0));


        cleanExportDir(exportCodeDirectory);

        return serviceCode.getServiceCodeFile();
    }

    @Override
    public ServiceCode save(byte[] zip) {
        ServiceCode serviceCode = new ServiceCode();
        serviceCode.setServiceCodeFile(zip);
        return serviceCodeRepository.save(serviceCode);
    }

    @Override
    @Scheduled(fixedRate = 60 * 1000)
    @Transactional
    public void clear() {
        Instant fiveMinutesAgo = Instant.now().minus(5, ChronoUnit.MINUTES);
        serviceCodeRepository.deleteAllCreatedBefore(fiveMinutesAgo);
    }
    public byte[] packToZip(Path exportCodeDirectory) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        List<Path> successfulFiles = new ArrayList<>();

        try {
            Files.walk(exportCodeDirectory)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(file -> {
                        if (Files.exists(file) && Files.isReadable(file)) {
                            try (InputStream in = Files.newInputStream(file)) {
                                successfulFiles.add(file);
                            } catch (IOException e) {
                                log.error(messageHelper.getMessage(ERROR_READ_FILE, file, e.getMessage()));
                            }
                        }
                    });
        } catch (IOException e) {
            log.error(messageHelper.getMessage(ERROR_TRAVERSING_FILES_DIRECTORY, exportCodeDirectory, e.getMessage()));
            return new byte[0];
        }



        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (Path file : successfulFiles) {
                try (InputStream in = Files.newInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(
                            exportCodeDirectory.relativize(file).toString().replace("\\", "/")
                    );

                    zipOutputStream.putNextEntry(zipEntry);
                    in.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                } catch (IOException e) {

                    log.error(messageHelper.getMessage(ERROR_ADDING_FILE_TO_ARCHIVE, file), e);
                }
            }

            zipOutputStream.finish();
        } catch (IOException e) {
            log.error(messageHelper.getMessage(ERROR_CREATE_ZIP_ARCHIVE), e);
            return new byte[0];
        }

        return byteArrayOutputStream.toByteArray();
    }


    private static void prepareCodeDirectory(String directoryPath) {
        Path path = Path.of(directoryPath);

        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info(MessageHelper.getStaticMessage("axenapi.info.dir.created.success", directoryPath));
            }

            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(path))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                                log.debug("Deleted file or directory: {}", p);
                            } catch (IOException e) {
                                log.warn(MessageHelper.getStaticMessage(WARN_COUL_NOT_DELETE_FILE, p), e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error(MessageHelper.getStaticMessage(FAIL_TO_PREPARE_DIR, directoryPath), e);
            throw new RuntimeException("Failed to prepare export directory", e);
        }
    }


    private static void cleanExportDir(String directoryPath) {
        Path path = Paths.get(directoryPath);
        if (Files.exists(path)) {
            try (Stream<Path> walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.info(MessageHelper.getStaticMessage(ERROR_DELETING_PATH, p, e.getMessage()));
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean directory", e);
            }
        }
    }

}
