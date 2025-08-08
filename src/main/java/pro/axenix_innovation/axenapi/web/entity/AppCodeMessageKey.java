package pro.axenix_innovation.axenapi.web.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Справочник с кодом ошибки/предупреждения и ключом сообщения из messages.properties
 */
@AllArgsConstructor
@Getter
public enum AppCodeMessageKey {

//////////////////////////////////////////--- ERRORS ---/////////////////////////////////////////
    ERROR_PARSE_OPEN_API_SPEC_FAIL(50001, "axenapi.error.parse.open.api.spec.fail"),
    ERROR_WHILE_PARSING_OPEN_API_SPEC(50002, "axenapi.error.while.parse.open.api.spec"),
    ERROR_PROCESSING_SCHEMA(50003, "axenapi.error.processing.schema"),
    ERROR_PARSING_OPEN_API_SPEC(50004, "axenapi.error.parse.open.api.spec"),
    ERROR_MD_CONTENT_NOT_FOUND(50005, "axenapi.error.md.content.not.found"),
    ERROR_DOWNLOAD_LINK_DOCX_NULL(50006, "axenapi.error.download.link.docx.null"),
    ERROR_DURING_DOCX_GEN(50007, "axenapi.error.during.docx.gen"),
    FAIL_FIND_NODES_FOR_LINK(50008, "axenapi.error.fail.find.nodes.link"),
    FAIL_FIND_NODES_FOR_LINK_G2(50009, "axenapi.error.fail.find.nodes.link.g2"),
    ERROR_READ_FILE(50010, "axenapi.error.read.file"),
    ERROR_TRAVERSING_FILES_DIRECTORY(50011, "axenapi.error.traversing.files.directory"),
    ERROR_ADDING_FILE_TO_ARCHIVE(50012, "axenapi.error.adding.file.to.archive"),
    ERROR_CREATE_ZIP_ARCHIVE(50013, "axenapi.error.create.zip.archive"),
//    FAIL_TO_PREPARE_DIR также используется в тестах, при изменении записи или сообщения в messages.properties поправить тесты MessageHelperTest
    FAIL_TO_PREPARE_DIR(50014, "axenapi.error.fail.prepare.dir"),
    ERROR_DELETING_PATH(50015, "axenapi.error.deleting.path"),
    ERROR_INPUT_GRAPHDTO_NULL(50016, "axenapi.error.input.graph.dto.null"),
    FAIL_GEN_YAML_SPEC(50017, "axenapi.error.fail.gen.yaml.spec"),
    FAIL_READ_YAML_CONTENT(50018, "axenapi.error.fail.read.yaml.content"),
    ERROR_NO_YAML_CONTENT(50019, "axenapi.error.no.yaml.content"),
    ERROR_MD_FILE_NOT_GEN(50020, "axenapi.error.md.file.not.gen"),
    ERROR_DURING_MD_GEN_YAML(50021, "axenapi.error.during.md.gen.yaml"),
    ERROR_NO_MD_FILES_GEN(50022, "axenapi.error.no.md.files.gen"),
    ERROR_OCCRUED_DURING_MD_GEN(50023, "axenapi.error.occrued.during.md.gen"),
    ERROR_UNEXPECTED_ERROR_MD_GEN(50024, "axenapi.error.unexpected.error.md.gen"),
    ERROR_RESP_FROM_SPEC_NULL(50025, "axenapi.error.resp.from.spec.null"),
    ERROR_PROCESS_REQUEST(50026, "axenapi.error.process.request"),
    ERROR_CALCULATE_PATH(50027, "axenapi.error.calculate.path"),
    ERROR_SQL_READING_DOCX_BLOB(50028, "axenapi.error.sql.reading.docx.blob"),
    ERROR_UNEXPECTED_ERROR_READING_DOCX_BLOB(50029, "axenapi.error.unexpected.error.reading.docx.blob"),
    ERROR_SQL_READING_PDF_BLOB(50030, "axenapi.error.sql.reading.pdf.blob"),
    ERROR_UNEXPECTED_ERROR_READING_PDF_BLOB(50031, "axenapi.error.unexpected.error.reading.pdf.blob"),
    ERROR_SQL_READING_CLOB(50032, "axenapi.error.sql.reading.clob"),
    ERROR_UNEXPECTED_ERROR_READING_CLOB(50033, "axenapi.error.unexpected.error.reading.clob"),
    ERROR_DOWNLOAD_JSON_SPEC(50034, "axenapi.error.download.json.spec"),
    ERROR_DOWNLOAD_YMAL_SPEC(50035, "axenapi.error.download.yaml.spec"),
    ERROR_DOWNLOAD_LINK_PDF_NULL(50036, "axenapi.error.download.link.pdf.null"),
    ERROR_DURING_PDF_GEN(50037, "axenapi.error.during.pdf.gen"),
    ERROR_UNKNOWN_ERROR_PROC_FILE(50038, "axenapi.error.unknown.error.proc.file"),
    ERROR_UNKNOWN_ERROR_PROC_SPEC(50039, "axenapi.error.unknown.error.proc.spec"),
    ERROR_READING_CLOB(50040, "axenapi.error.reading.clob"),
    ERROR_UNEXPECTED_DURING_SPEC_GEN(50041, "axenapi.error.unexpected.during.spec.gen"),
    ERROR_WHILE_GET_OPEN_API_SPEC_SERVICE(50042, "axenapi.error.while.get.open.api.spec.service"),
    ERROR_MD_GEN_FAIL_OR_EMTY_LINK(50043, "axenapi.error.md.fail.or.emty.link"),
    ERROR_EXTRACT_FILE_ID_NULL_EMPTY(50044, "axenapi.error.extract.file.id.null.empty"),
    ERROR_EXTRACTED_MD_LINK_NULL_EMPTY(50045, "axenapi.error.extracted.md.link.null.empty"),
    ERROR_FILE_ID_EXTRACTED_URL_NULL_BLANK(50046, "axenapi.error.extracted.url.null.blank"),
    ERROR_TEMPLATES_MD_NOT_FOUND(50047, "axenapi.error.templates.md.not.found"),
    ERROR_COMMIT_DOC_NOT_CHANGES(50048, "axenapi.error.commit_doc_not_changes"),
    ERROR_DOC_CREATE_MR(50049, "axenapi.error.merge.request.doc"),

//////////////////////////////////////////--- WARNINGS ---/////////////////////////////////////////

    WARN_NO_SCHEMAS_FOUND(60001, "axenapi.warn.no.schemas.found"),
    WARN_NO_EXTENSIONS_IN_SCHEMA(60002, "axenapi.warn.no.extensions.in.schema"),
    WARN_SKIPPING_LINK(60003, "axenapi.warn.skipping.link"),
    WARN_NO_OPEN_API_SPEC_FOUND_SKIP_INC(60004, "axenapi.warn.no.open.api.spec.found.skip.inc"),
    WARN_NO_OPEN_API_SPEC_FOUND_SKIP_OUT(60005, "axenapi.warn.no.open.api.spec.found.skip.out"),
    WARN_NO_OPEN_API_SPEC_FOUND_SKIP_HTTP(60006, "axenapi.warn.no.open.api.spec.found.skip.http"),
    WARN_HTTP_URL_SKIP_LINK(60007, "axenapi.warn.http.url.skip.link"),
    WARN_NODE_NOT_FOUND(60008, "axenapi.warn.node.not.found"),
    WARN_EMPTY_NULL_SPEC(60009, "axenapi.warn.empty.null.spec"),
    WARN_DURING_OPEN_API_PARSE(60010, "axenapi.warn.during.open.api.parse"),
    WARN_COUL_NOT_DELETE_FILE(60011, "axenapi.warn.could.not.delete.file"),
    WARN_FAIL_DELETE_TEMP_FILES(60012, "axenapi.warn.fail.delete.temp.files"),
    WARN_DEFAULT_API_NOT_FOUND(60013, "axenapi.warn.default.api.not.found"),
    WARN_FAIL_DELETE_FILE_OR_DIR(60014, "axenapi.warn.fail.delete.file.or.dir"),
    WARN_FAIL_WALK_DIR(60015, "axenapi.warn.fail.walk.dir"),
    WARN_NO_FILES_PROVIDED(60016, "axenapi.warn.no.files.provided"),
    WARN_EMPTY_ARCHIVE_GEN(60017, "axenapi.warn.empty.archive.gen"),
    WARN_FILE_NOT_FOUND(60018, "axenapi.warn.file.not.found"),
    WARN_PARSE_OPEN_API_NO_EVENTS(60019, "axenapi.warn.parse.open.api.no.events"),
    WARN_PARSE_JSON_NO_EVENTS(60020, "axenapi.warn.parse.json.no.events"),
    WARN_MERGED_GRAPH_NO_EVENTS(60021, "axenapi.warn.merged.graph.no.events"),
    WARN_FAIL_EXTRACT_FROM_TOPIC(60022, "axenapi.warn.fail.extract.from.topic"),
    WARN_READ_ME_NOT_FOUND(60023, "axenapi.warn.read.me.not.found"),
    WARN_MD_GEN_RUNNING(60024, "axenapi.warn.md.gen.running"),
    WARN_SERVICE_INFO_LIST_NULL(60025, "axenapi.warn.service.info.list.null"),
    WARN_INVALID_OPEN_API_FORMAT_TITLE(60026, "axenapi.warn.invalid.open.api.format.title"),
    WARN_INVALID_JSON_FORMAT_SPEC(60027, "axenapi.warn.invalid.json.format.spec"),
    WARN_ALL_ITEMS_HAD_ERRORS(60028, "axenapi.warn.all.items.had.errors"),
    WARN_FOUND_UNUSED_EVENTS(60029, "axenapi.warn.found.unused.events"),
    WARN_TOPIC_NODE_NOT_FOUND_DOC_LINK(60030, "axenapi.warn.topic.node.not.found.doc.link"),
    WARN_GIT_REPOSITORY_NOT_FOUND(60031, "axenapi.warn.git.repository.not.found"),

    //////////////////////////////////////////--- RESPONSES ---/////////////////////////////////////////

    RESP_ERROR_EMPTY_RESP_SPEC_GEN(70001, "axenapi.resp.error.empty.resp.spec.gen"),
    RESP_ERROR_FAIL_PROCESS_GRAPH(70002, "axenapi.resp.error.fail.process.graph"),
    RESP_ERROR_VALID_EVENT_GRAPH_NULL(70003, "axenapi.resp.error.valid.event.graph.null"),
    RESP_ERROR_VALID_GRAPH_NULL_NODES(70004, "axenapi.resp.error.valid.graph.null.nodes"),
    RESP_ERROR_VALID_GRAPH_NAME_NULL(70005, "axenapi.resp.error.valid.graph.name.null"),
    RESP_ERROR_VALID_INVALID_NODE_TYPE(70006, "axenapi.resp.error.valid.invalid.node.type"),
    RESP_ERROR_VALID_INVALID_NODE_BROKER_TYPE(70007, "axenapi.resp.error.valid.invalid.broker.type"),
    RESP_ERROR_VALID_NODE_NAME_NULL(70008, "axenapi.resp.error.valid.node.name.null"),
    RESP_ERROR_VALID_NODE_IN_NULL(70009, "axenapi.resp.error.valid.node.in.null"),
    RESP_ERROR_VALID_NODE_ID_DUPLICATE(70010, "axenapi.resp.error.valid.node.id.duplicate"),
    RESP_ERROR_VALID_EVENT_ID_NULL(70011, "axenapi.resp.error.valid.event.id.null"),
    RESP_ERROR_VALID_EVENT_ID_DUPLICATE(70012, "axenapi.resp.error.valid.event.id.duplicate"),
    RESP_ERROR_VALID_EVENT_NAME_NULL(70013, "axenapi.resp.error.valid.event.name.null"),
    RESP_ERROR_VALID_EVENT_SCHEMA_NULL(70014, "axenapi.resp.error.valid.event.schema.null"),
    RESP_ERROR_VALID_LINK_ID_FROM_NULL(70015, "axenapi.resp.error.valid.link.id.from.null"),
    RESP_ERROR_VALID_LINK_ID_TO_NULL(70016, "axenapi.resp.error.valid.link.id.to.null"),
    RESP_ERROR_VALID_NODE_ID_FROM_NULL(70017, "axenapi.resp.error.valid.node.id.from.null"),
    RESP_ERROR_VALID_NODE_ID_TO_NULL(70018, "axenapi.resp.error.valid.node.id.to.null"),
    RESP_ERROR_VALID_LINKS_NULL_EVENTS(70019, "axenapi.resp.error.valid.links.null.events"),
    RESP_ERROR_SPEC_GEN_NULL(70020, "axenapi.resp.error.spec.gen"),
    RESP_ERROR_UNSUPPORTED_FORMAT(70021, "axenapi.resp.error.unsupported.format"),
    RESP_ERROR_DIR_ERROR(70022, "axenapi.resp.error.dir.error"),
    RESP_ERROR_FILE_ALREADY_EXISTS(70023, "axenapi.resp.error.file.already.exists"),
    RESP_ERROR_UNEXPECTED_ERROR_SPEC_GEN(70024, "axenapi.resp.error.unexpected.error.spec.gen"),
    RESP_ERROR_INVALID_REQ_PARAMS(70025, "axenapi.resp.error.invalid.req.params"),
    RESP_OK_PATH_NO_FOUND_FROM_TO(70026, "axenapi.resp.ok.path.not.found.from.to"),
    RESP_OK_PATH_FOUND_FROM_TO(70027, "axenapi.resp.ok.path.found.from.to"),
    RESP_ERROR_CALCULATE_PATH(70028, "axenapi.resp.error.calculate.path"),
    RESP_ERROR_INVALID_INPUT_DATA(70029, "axenapi.resp.error.input.data"),
    RESP_ERROR_PROCESSING_JSON_SCHEMA(70030, "axenapi.resp.error.processing.json.schema"),
    RESP_UNEXPECTED_ERROR(70031, "axenapi.resp.error.unexpected.error"),
    RESP_ERROR_READ_FILE(70032, "axenapi.resp.error.read.file"),
    RESP_ERROR_FAIL_GEN_MD_DOC(70033, "axenapi.resp.error.gen.md.doc"),
    RESP_ERROR_FAIL_GEN_DOCX_DOC(70033, "axenapi.resp.error.gen.docx.doc"),
    RESP_ERROR_FAIL_GEN_PDF_DOC(70033, "axenapi.resp.error.gen.pdf.doc"),
    RESP_ERROR_SERVER_GEN_PDF_DOC(70033, "axenapi.resp.server.error.gen.pdf.doc"),
    RESP_OK_MD_FILE_SAVED(70034, "axenapi.resp.ok.md.file.saved.success"),
    RESP_ERROR_CLONE_DOC_REP(70035, "axenapi.resp.error.clone.doc.rep"),
    RESP_ERROR_COMMIT_DOC(70036, "axenapi.resp.error.commit.doc"),
    RESP_ERROR_DOC_CREATE_MR(70038, "axenapi.resp.error.merge.request.doc"),
    RESP_ERROR_ADD_DOC(70039, "axenapi.resp.error.add.doc");
    private final int code;
    private final String messageKey;

    /**
     * Фабричный метод для обертки с параметрами
     */
    public AppCodeMessage withArgs(Object... args) {
        return new AppCodeMessage(this, args);
    }
}
