package pro.axenix_innovation.axenapi.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.exception.AxenApiException;
import pro.axenix_innovation.axenapi.web.exception.NotServiceNode;
import pro.axenix_innovation.axenapi.web.exception.OpenAPISpecParseException;
import pro.axenix_innovation.axenapi.web.generate.DocxSpecificationDbHandler;
import pro.axenix_innovation.axenapi.web.generate.PdfSpecificationDbHandler;
import pro.axenix_innovation.axenapi.web.generate.SpecificationGenerator;
import pro.axenix_innovation.axenapi.web.mapper.GitRepoMapper;
import pro.axenix_innovation.axenapi.web.model.*;
import pro.axenix_innovation.axenapi.web.repository.DocxSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.MarkdownSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.PdfSpecificationRepository;
import pro.axenix_innovation.axenapi.web.repository.SpecificationRepository;
import pro.axenix_innovation.axenapi.web.service.*;
import pro.axenix_innovation.axenapi.web.service.git.GitServiceCommand;
import pro.axenix_innovation.axenapi.web.util.ProcessingFiles;
import pro.axenix_innovation.axenapi.web.validate.CalculateAllPathsValidator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.*;

@RestController
@RequiredArgsConstructor
public class AxenAPIController implements DefaultApi {

    private final SpecificationGenerator specificationGenerator;
    private final SpecificationRepository specificationRepository;
    private final MarkdownSpecificationRepository markdownSpecificationRepository;
    private final DocxSpecificationRepository docxSpecificationRepository;
    private final PdfSpecificationRepository pdfSpecificationRepository;
    private final CodeService codeService;
    private final SpecService specService;
    private final MarkdownSpecService markdownSpecService;
    private final DocxSpecificationDbHandler docxSpecificationDbHandler;
    private final PdfSpecificationDbHandler pdfSpecificationDbHandler;
    private final JsonToSchemaGenerationService jsonToSchemaGenerationService;
    private final PdfGenerationService pdfGenerationService;
    private final DocxGenerationService docxGenerationService;
    private final DownloadService downloadService;
    private final MarkdownService markdownService;
    private final MessageHelper messageHelper;
    private final AllServicePdfGenerationService allServicePdfGenerationService;
    private final GitServiceCommand gitServiceCommand;
    private final GitRepoMapper gitRepoMapper;
    private final GitRepositoryService gitRepositoryService;


    private static final Logger log = LoggerFactory.getLogger(AxenAPIController.class);

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    @Override
    public ResponseEntity generateSpecPost(
            @Valid @RequestBody EventGraphDTO eventGraphDTO,
            @RequestParam(value = "format", required = false, defaultValue = "json") String format
    ) {
        try {
            GenerateSpecPost200Response response = specService.validateAndGenerateSpec(eventGraphDTO, format);

            if (response == null) {
                log.error(messageHelper.getMessage(ERROR_RESP_FROM_SPEC_NULL));
                BaseResponse errorResponse = new BaseResponse();
                errorResponse.setStatus("ERROR");
                errorResponse.setCode(RESP_ERROR_EMPTY_RESP_SPEC_GEN.getCode());
                errorResponse.setMessage(messageHelper.getMessage(RESP_ERROR_EMPTY_RESP_SPEC_GEN.getMessageKey()));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

            if ("OK".equalsIgnoreCase(response.getStatus())) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_PROCESS_REQUEST), e);
            BaseResponse errorResponse = new BaseResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setCode(RESP_ERROR_FAIL_PROCESS_GRAPH.getCode());
            errorResponse.setMessage(messageHelper.getMessage(RESP_ERROR_FAIL_PROCESS_GRAPH.getMessageKey(), e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Uploads a list of files and returns the event graph.
     * files should be json or yaml (yml) file. Content - openapi specification >= 3.0.0 version.
     * Specification should be in AxenAPI format
     *
     * @param files (required)
     * @return graph by specifications
     */
    @Override
    public ResponseEntity uploadPost(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            log.warn(messageHelper.getMessage(WARN_NO_FILES_PROVIDED));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        EventGraphDTO eventGraphDTO;
        try {
            eventGraphDTO = ProcessingFiles.processFiles(files);
        } catch (OpenAPISpecParseException e) {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    BaseResponse.builder()
                            .code(RESP_UNEXPECTED_ERROR.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_READ_FILE.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_READ_FILE.getMessageKey(),
                                    e.getMessage()))
                            .build()
            );
        }
        log.info(messageHelper.getMessage("axenapi.info.success.processed.file", files.size()));
        return ResponseEntity.ok(eventGraphDTO);
    }

    @Override
    public ResponseEntity<EventGraphDTO> addServiceToGraphPost(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("eventGraph") @Valid EventGraphDTO eventGraph) {

        EventGraphDTO updatedGraph = EventGraphService.addServiceToGraph(files, eventGraph);

        if (!updatedGraph.getErrors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updatedGraph);
        }
        return ResponseEntity.ok(updatedGraph);
    }

    @Override
    public ResponseEntity<EventGraphDTO> updateServiceSpecificationPost(
            @Valid @RequestBody UpdateServiceSpecificationPostRequest request) {

        EventGraphDTO updatedGraph;
        try {
            updatedGraph = EventGraphService.updateServiceSpecification(request);
        } catch (OpenAPISpecParseException e) {
            updatedGraph = request.getEventGraph();
            ErrorDTO error = ErrorDTO.builder()
                    .fileName(e.getFileName())
                    .errorMessage(e.getMessage())
                    .build();
            updatedGraph.addErrorsItem(error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updatedGraph);
        } catch (NotServiceNode e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (!updatedGraph.getErrors().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(updatedGraph);
        }
        return ResponseEntity.ok(updatedGraph);
    }

    @Override
    public ResponseEntity<Resource> generateCodePost(
            @RequestBody EventGraphDTO eventGraph) {
        List<String> nodeNames = eventGraph.getNodes().stream()
                .map(NodeDTO::getName)
                .toList();

        log.info(messageHelper.getMessage("axenapi.info.obtain.graph.node.link",
                        eventGraph.getNodes().size(),
                        eventGraph.getLinks().size(),
                        nodeNames));

        byte[] generatedCode = codeService.generateCode(eventGraph);

        if (generatedCode == null || generatedCode.length == 0) {
            log.warn(messageHelper.getMessage(WARN_EMPTY_ARCHIVE_GEN));
            return ResponseEntity.noContent().build();
        }

        log.info(messageHelper.getMessage("axenapi.info.method.generate.code.return.archive", generatedCode.length));

        ByteArrayResource resource = new ByteArrayResource(generatedCode);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        httpHeaders.setContentDispositionFormData("attachment", "archive.zip");

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(resource);
    }

    @Override
    public ResponseEntity calculateAllPathsPost(
            @Valid CalculateAllPathsPostRequest request
    ) {
        if (CalculateAllPathsValidator.isInvalid(request)) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .message(messageHelper.getMessage(RESP_ERROR_INVALID_REQ_PARAMS.getMessageKey()))
                            .code(RESP_ERROR_INVALID_REQ_PARAMS.getCode())
                            .build()
            );
        }

        GenerateSpecPost200Response validationResponse = specService.validateAndGenerateSpec(request.getEventGraph(), "json");

        if (!"OK".equals(validationResponse.getStatus())) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .message(validationResponse.getMessage())
                            .code(validationResponse.getCode())
                            .build()
            );
        }

        try {
            List<List<LinkDTO>> paths = PathsService.findAllPaths(
                    request.getEventGraph(),
                    request.getFrom(),
                    request.getTo()
            );

            Set<String> uniqueTags = PathsService.extractUniqueTags(paths);

            ;
            AppCodeMessage codeMessage = paths.isEmpty()
                    ? RESP_OK_PATH_NO_FOUND_FROM_TO.withArgs(request.getFrom(), request.getTo())
                    : RESP_OK_PATH_FOUND_FROM_TO.withArgs(paths.size(), request.getFrom(), request.getTo());

            return ResponseEntity.ok(
                    CalculateAllPathsPost200Response.builder()
                            .paths(paths)
                            .uniqueTags(uniqueTags)
                            .code(codeMessage.getEnumItem().getCode())
                            .message(messageHelper.getMessage(codeMessage.getEnumItem().getMessageKey(),
                                    codeMessage.getArgs()))
                            .build()
            );

        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_CALCULATE_PATH), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_CALCULATE_PATH.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_CALCULATE_PATH.getMessageKey(),
                                    e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity getServiceSpecificationPost(
            @Valid GetServiceSpecificationPostRequest getServiceSpecificationPostRequest) {
        EventGraphDTO eventGraph = getServiceSpecificationPostRequest.getEventGraph();
        UUID serviceNodeId = getServiceSpecificationPostRequest.getServiceNodeId();
        try {
            String specByServiceId = specService.getSpecByServiceId(eventGraph, serviceNodeId);
            return ResponseEntity.status(HttpStatus.OK).body(GetServiceSpecificationPost200Response.builder()
                            .serviceNodeId(serviceNodeId)
                            .specification(specByServiceId)
                    .build());
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @Override
    public ResponseEntity<List<GitRepoDto>> gitGet() {
        List<GitRepoDto> dtos = gitRepositoryService.getList()
                .stream()
                .map(gitRepoMapper::toGitRepoDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity gitIdDelete(String id) {
        try {
            gitRepositoryService.delete(id);
            return ResponseEntity.ok().build();
        } catch (AxenApiException.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_GIT_REPOSITORY_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity gitIdGet(String id) {
        try {
            return ResponseEntity.ok(
                    gitRepoMapper.toGitRepoDto(
                            gitRepositoryService.getById(id)
                    )
            );
        } catch (AxenApiException.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_GIT_REPOSITORY_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity gitIdPut(String id, GitRepoDto gitRepoDto) {
        try {
            return ResponseEntity.ok(
                    gitRepoMapper.toGitRepoDto(
                            gitRepositoryService.put(id, gitRepoDto)
                    )
            );
        } catch (AxenApiException.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_GIT_REPOSITORY_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    BaseResponse.builder()
                            .code(RESP_UNEXPECTED_ERROR.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity gitPost(@Valid @RequestBody GitRepoDto gitRepoDto) {
        try {
            return ResponseEntity.ok(
                    gitRepoMapper.toGitRepoDto(
                            gitRepositoryService.create(gitRepoDto)
                    )
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    BaseResponse.builder()
                            .code(RESP_UNEXPECTED_ERROR.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity generateJsonExamplePost(
            @Valid @RequestBody GenerateJsonExamplePostRequest request) {
        try {
            log.info(messageHelper.getMessage("axenapi.info.received.json.schema", request.getJsonSchema()));

            JsonNode schemaNode = new ObjectMapper().readTree(request.getJsonSchema());

            String generatedJson = SchemaToJsonService.generateJsonFromSchema(schemaNode);

            log.info(messageHelper.getMessage("axenapi.info.gen.json.example", generatedJson));

            GenerateJsonExamplePost200Response response = new GenerateJsonExamplePost200Response();
            response.setJsonExample(generatedJson);
            return ResponseEntity.ok(response);

        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_INVALID_INPUT_DATA.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_INVALID_INPUT_DATA.getMessageKey(), e.getMessage()))
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_PROCESSING_JSON_SCHEMA.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_PROCESSING_JSON_SCHEMA.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity generateJsonSchemaPost(@Valid @RequestBody GenerateJsonSchemaPostRequest request) {
        try {
            String generatedSchema = jsonToSchemaGenerationService.generateSchema(request.getJsonInput());

            GenerateJsonSchemaPost200Response response = new GenerateJsonSchemaPost200Response();
            response.setSchema(generatedSchema);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_INVALID_INPUT_DATA.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_INVALID_INPUT_DATA.getMessageKey(),
                                    e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    BaseResponse.builder()
                            .code(RESP_UNEXPECTED_ERROR.getCode())
                            .message(messageHelper.getMessage(RESP_UNEXPECTED_ERROR.getMessageKey(),
                                    e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<SaveMarkdownPost201Response> saveMarkdownPost(
            @RequestHeader("Authorization") String gitToken,
            @Parameter(name = "SaveMarkdownPostRequest", required = true)
            @Valid @RequestBody SaveMarkdownPostRequest saveMarkdownPostRequest) {
        try {
            SaveMarkdownPost201Response response = markdownService.saveMarkdownToFile(
                    saveMarkdownPostRequest.getMarkdown(),
                    saveMarkdownPostRequest.getFilePath()
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            BaseResponse error = new BaseResponse();
            error.setMessage(e.getMessage());
            return new ResponseEntity(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            BaseResponse error = new BaseResponse();
            error.setMessage(e.getMessage());
            return new ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity generateMarkdownPost(
            @Valid @RequestBody EventGraphDTO graph,
            @RequestParam(value = "serviceIds", required = false) List<UUID> serviceIds) {

        if (serviceIds == null) {
            serviceIds = Collections.emptyList();
        }

        EventGraphDTO graphToProcess;
        if (serviceIds.isEmpty()) {
            graphToProcess = graph;
        } else {
            graphToProcess = MarkdownSpecService.filterByServiceUUIDs(graph, new HashSet<>(serviceIds));
        }

        GenerateMarkdownPost200Response response = markdownSpecService.generateFullMarkdown(graphToProcess);

        if ("OK".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .status("ERROR")
                            .code(RESP_ERROR_FAIL_GEN_MD_DOC.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_FAIL_GEN_MD_DOC.getMessageKey()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity generateDocxPost(
            @Valid @RequestBody EventGraphDTO graph,
            @RequestParam(value = "serviceIds", required = false) List<UUID> serviceIds) {

        if (serviceIds == null) {
            serviceIds = Collections.emptyList();
        }

        EventGraphDTO graphToProcess;
        if (serviceIds.isEmpty()) {
            graphToProcess = graph;
        } else {
            graphToProcess = MarkdownSpecService.filterByServiceUUIDs(graph, new HashSet<>(serviceIds));
        }

        GenerateDocxPost200Response response = docxGenerationService.generateDocxFromEventGraph(graphToProcess);

        if ("OK".equalsIgnoreCase(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_FAIL_GEN_DOCX_DOC.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_FAIL_GEN_DOCX_DOC.getMessageKey()))
                            .status("ERROR")
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity generatePdfPost(
            @Valid @RequestBody EventGraphDTO graph,
            @RequestParam(value = "serviceIds", required = false) List<UUID> serviceIds) {

        GeneratePdfPost200Response response = new GeneratePdfPost200Response();

        if (serviceIds == null) {
            serviceIds = Collections.emptyList();
        }

        EventGraphDTO graphToProcess;
        if (serviceIds.isEmpty()) {
            graphToProcess = graph;
        } else {
            graphToProcess = MarkdownSpecService.filterByServiceUUIDs(graph, new HashSet<>(serviceIds));
        }

        try {
            GeneratePdfPost200Response genResponse = pdfGenerationService.generatePdfFromEventGraph(graphToProcess);
            if ("OK".equalsIgnoreCase(genResponse.getStatus())) {
                return ResponseEntity.ok(genResponse);
            } else {
                response.setMessage("");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        BaseResponse.builder()
                                .code(RESP_ERROR_FAIL_GEN_PDF_DOC.getCode())
                                .message(messageHelper.getMessage(RESP_ERROR_FAIL_GEN_PDF_DOC.getMessageKey()))
                                .status("ERROR")
                                .build()
                );
            }
        } catch (Exception e) {
            response.setStatus("ERROR");
            response.setMessage(": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .status("ERROR")
                            .code(RESP_ERROR_SERVER_GEN_PDF_DOC.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_SERVER_GEN_PDF_DOC.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity<Resource> generateAllServicePdfPost(@Valid @RequestBody EventGraphDTO eventGraphDTO) {
        try {
            AllServicePdf allPdf = allServicePdfGenerationService.generateAllServicesPDF(eventGraphDTO);
            byte[] pdfBytes = allPdf.getCombinedPdfBytes();

            String filename = "documentation_" + UUID.randomUUID() + ".pdf";

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body(new ByteArrayResource(e.getMessage().getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header("Content-Type", "application/json")
                    .body(new ByteArrayResource(("Internal error: " + e.getMessage()).getBytes()));
        }
    }




    @Override
    public ResponseEntity downloadDocxFileIdDocxGet(String fileId) {
        log.info(messageHelper.getMessage("axenapi.info.received.req.download.docx.spec", fileId));

        try {
            byte[] docxBytes = downloadService.getDocxFileBytes(fileId);
            ByteArrayResource resource = new ByteArrayResource(docxBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".docx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .contentLength(docxBytes.length)
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.warn(messageHelper.getMessage(WARN_FILE_NOT_FOUND, fileId, e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_FILE_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(WARN_FILE_NOT_FOUND.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (SQLException e) {
            log.error(messageHelper.getMessage(ERROR_SQL_READING_DOCX_BLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_SQL_READING_DOCX_BLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_SQL_READING_DOCX_BLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_DOCX_BLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_UNEXPECTED_ERROR_READING_DOCX_BLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_DOCX_BLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity downloadPdfFileIdPdfGet(String fileId) {
        log.info(messageHelper.getMessage("axenapi.info.received.req.download.pdf.spec", fileId));

        try {
            byte[] pdfBytes = downloadService.getPdfFileBytes(fileId);
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.warn(messageHelper.getMessage(WARN_FILE_NOT_FOUND, fileId, e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_FILE_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(WARN_FILE_NOT_FOUND.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (SQLException e) {
            log.error(messageHelper.getMessage(ERROR_SQL_READING_PDF_BLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_SQL_READING_PDF_BLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_SQL_READING_PDF_BLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_PDF_BLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_UNEXPECTED_ERROR_READING_PDF_BLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_PDF_BLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity downloadMarkdownFileIdMdGet(String fileId) {
        log.info(messageHelper.getMessage("axenapi.info.received.req.download.md.spec", fileId));

        try {
            byte[] markdownBytes = downloadService.getMarkdownFileBytes(fileId);
            ByteArrayResource resource = new ByteArrayResource(markdownBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".md\"")
                    .contentType(MediaType.parseMediaType("text/markdown"))
                    .body(resource);
        } catch (FileNotFoundException e) {
            log.warn(messageHelper.getMessage(WARN_FILE_NOT_FOUND, fileId, e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_FILE_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(WARN_FILE_NOT_FOUND.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (SQLException e) {
            log.error(messageHelper.getMessage(ERROR_SQL_READING_CLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_SQL_READING_CLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_SQL_READING_CLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_CLOB, fileId, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_UNEXPECTED_ERROR_READING_CLOB.getCode())
                            .message(messageHelper.getMessage(ERROR_UNEXPECTED_ERROR_READING_CLOB.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity downloadSpecsFileIdJsonGet(String fileId) {
        try {
            byte[] jsonBytes = downloadService.getJsonSpecBytes(fileId);
            ByteArrayResource resource = new ByteArrayResource(jsonBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.warn(messageHelper.getMessage(WARN_FILE_NOT_FOUND, fileId, e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_FILE_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(WARN_FILE_NOT_FOUND.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_DOWNLOAD_JSON_SPEC, fileId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_DOWNLOAD_JSON_SPEC.getCode())
                            .message(messageHelper.getMessage(ERROR_DOWNLOAD_JSON_SPEC.getMessageKey(), fileId))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity downloadSpecsFileIdYamlGet(String fileId) {
        try {
            byte[] yamlBytes = downloadService.getYamlSpecBytes(fileId);
            ByteArrayResource resource = new ByteArrayResource(yamlBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".yaml\"")
                    .contentType(MediaType.parseMediaType("application/x-yaml"))
                    .body(resource);

        } catch (FileNotFoundException e) {
            log.warn(messageHelper.getMessage(WARN_FILE_NOT_FOUND, fileId, e.getMessage()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    BaseResponse.builder()
                            .code(WARN_FILE_NOT_FOUND.getCode())
                            .message(messageHelper.getMessage(WARN_FILE_NOT_FOUND.getMessageKey(), fileId, e.getMessage()))
                            .build()
            );
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_DOWNLOAD_YMAL_SPEC, fileId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    BaseResponse.builder()
                            .code(ERROR_DOWNLOAD_YMAL_SPEC.getCode())
                            .message(messageHelper.getMessage(ERROR_DOWNLOAD_YMAL_SPEC.getMessageKey(), fileId))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity cloneDocumentationPost() {
        try {
            log.info("axenapi.info.clone.doc.start");
            String cloneUrl = gitServiceCommand.cloneProject();
            log.info(messageHelper.getMessage("axenapi.info.clone.doc.success", cloneUrl));
            return ResponseEntity.ok().body(cloneUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.builder()
                            .code(RESP_ERROR_CLONE_DOC_REP.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_CLONE_DOC_REP.getMessageKey(), e.getMessage()))
                            .build());
        }
    }

    @Override
    public ResponseEntity addDocumentationPost(@RequestBody String docPath) {
        try {
            log.info("axenapi.info.add.doc.start");
            gitServiceCommand.addFile(docPath);
            log.info(messageHelper.getMessage("axenapi.info.add.doc.success", docPath));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error(messageHelper.getMessage(RESP_ERROR_ADD_DOC.getMessageKey(), e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.OK).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_ADD_DOC.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_ADD_DOC.getMessageKey(), e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity commitDocumentationPost() {
        try {
            log.info("axenapi.info.commit.doc.start");
            gitServiceCommand.createCommit();
            log.info("axenapi.info.commit.doc.success");
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            log.error(messageHelper.getMessage(RESP_ERROR_COMMIT_DOC, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.OK).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_COMMIT_DOC.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_COMMIT_DOC, e.getMessage()))
                            .build()
            );
        }
    }

    @Override
    public ResponseEntity createMRDocumentationPost() {
        log.info(messageHelper.getMessage("axenapi.info.merge.request.doc.start"));
        try {
            String titleUUID = UUID.randomUUID().toString();
            String url = gitServiceCommand.createMergeRequest(titleUUID);
            if (url == null || url.isEmpty()) {
                log.error(messageHelper.getMessage(ERROR_DOC_CREATE_MR, "url = null"));
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        BaseResponse.builder()
                                .code(RESP_ERROR_DOC_CREATE_MR.getCode())
                                .message(messageHelper.getMessage(RESP_ERROR_DOC_CREATE_MR.getMessageKey(), "url = null"))
                                .build()
                );
            }
            log.info("axenapi.info.merge.request.doc.success", titleUUID, url);
            return ResponseEntity.ok()
                    .body(url);
        } catch (Exception e) {
            log.error(messageHelper.getMessage(ERROR_DOC_CREATE_MR, e.getMessage()), e);
            return ResponseEntity.status(HttpStatus.OK).body(
                    BaseResponse.builder()
                            .code(RESP_ERROR_DOC_CREATE_MR.getCode())
                            .message(messageHelper.getMessage(RESP_ERROR_DOC_CREATE_MR, e.getMessage()))
                            .build()
            );
        }
    }
    @Override
    public ResponseEntity<Void> healthGet() {
        return ResponseEntity.ok().build();
    }
}
