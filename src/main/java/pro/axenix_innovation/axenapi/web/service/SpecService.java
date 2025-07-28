package pro.axenix_innovation.axenapi.web.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.axenix_innovation.axenapi.web.entity.AppCodeMessage;
import pro.axenix_innovation.axenapi.web.generate.SpecificationGenerator;
import pro.axenix_innovation.axenapi.web.graph.EventGraphFacade;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;
import pro.axenix_innovation.axenapi.web.model.GenerateSpecPost200Response;
import pro.axenix_innovation.axenapi.web.util.OpenAPIGenerator;
import pro.axenix_innovation.axenapi.web.validate.EventGraphDTOValidator;

import java.util.Map;
import java.util.UUID;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_UNEXPECTED_DURING_SPEC_GEN;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_WHILE_GET_OPEN_API_SPEC_SERVICE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_DIR_ERROR;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_FILE_ALREADY_EXISTS;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_SPEC_GEN_NULL;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_UNEXPECTED_ERROR_SPEC_GEN;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.RESP_ERROR_UNSUPPORTED_FORMAT;

@Service
@Slf4j
public class SpecService {

    private final SpecificationGenerator specificationGenerator;
    private final ObjectMapper objectMapper;
    private final MessageHelper messageHelper;

    @Autowired
    public SpecService(SpecificationGenerator specificationGenerator, MessageHelper messageHelper) {
        this.specificationGenerator = specificationGenerator;
        this.messageHelper = messageHelper;
        this.objectMapper = new ObjectMapper();
    }


    public GenerateSpecPost200Response validateAndGenerateSpec(EventGraphDTO eventGraph, String format) {
        GenerateSpecPost200Response response = new GenerateSpecPost200Response();

        try {
            if (eventGraph == null) {
                log.error("Input EventGraphDTO is null");
                response.setStatus("ERROR");
                response.setMessage("Input graphDTO is null");
                return response;
            }

            log.info(messageHelper.getMessage("axenapi.info.spec.gen.start.graph", eventGraph.getName()));

            // 1. Валидация общего состояния графа
            AppCodeMessage validationError = EventGraphDTOValidator.validateEventGraph(eventGraph);
            if (validationError != null) {
                log.error(messageHelper.getMessage(validationError.getEnumItem(), validationError.getArgs()));
                response.setStatus("ERROR");
                response.setCode(validationError.getEnumItem().getCode());
                response.setMessage(messageHelper.getMessage(validationError.getEnumItem().getMessageKey(),
                        validationError.getArgs()));
                return response;
            }

            // 2. Валидация связей (должны иметь eventId)
            AppCodeMessage eventLinkError = EventGraphDTOValidator.validateLinksHaveEvents(eventGraph);
            if (eventLinkError != null) {
                log.error(messageHelper.getMessage(eventLinkError.getEnumItem(), eventLinkError.getArgs()));
                response.setStatus("ERROR");
                response.setCode(eventLinkError.getEnumItem().getCode());
                response.setMessage(messageHelper.getMessage(eventLinkError.getEnumItem().getMessageKey(),
                        eventLinkError.getArgs()));
                return response;
            }

            // 3. Генерация спецификации
            log.info(messageHelper.getMessage("axenapi.info.spec.call.gen", format));
            Map<String, String> result = specificationGenerator.generate(eventGraph, format);
            log.info(messageHelper.getMessage("axenapi.info.spec.gen.result.keys",
                    result != null ? result.keySet() : "null"));

            if (result == null || result.isEmpty()) {
                log.error(messageHelper.getMessage(RESP_ERROR_SPEC_GEN_NULL));
                response.setStatus("ERROR");
                response.setCode(RESP_ERROR_SPEC_GEN_NULL.getCode());
                response.setMessage(messageHelper.getMessage(RESP_ERROR_SPEC_GEN_NULL.getMessageKey()));
                return response;
            }

            if (result.containsKey("directory_error")) {
                String msg = result.get("directory_error");
                log.error(messageHelper.getMessage(RESP_ERROR_DIR_ERROR, msg));
                response.setStatus("ERROR");
                response.setCode(RESP_ERROR_DIR_ERROR.getCode());
                response.setMessage(messageHelper.getMessage(RESP_ERROR_DIR_ERROR.getMessageKey(), msg));
                return response;
            }

            if (!"json".equalsIgnoreCase(format) && !"yaml".equalsIgnoreCase(format)) {
                log.error(messageHelper.getMessage(RESP_ERROR_UNSUPPORTED_FORMAT, format));
                response.setStatus("ERROR");
                response.setCode(RESP_ERROR_UNSUPPORTED_FORMAT.getCode());
                response.setMessage(messageHelper.getMessage(RESP_ERROR_UNSUPPORTED_FORMAT.getMessageKey(), format) );
                return response;
            }

            for (Map.Entry<String, String> entry : result.entrySet()) {
                if ("service1".equals(entry.getKey()) && entry.getValue().contains("File already exists")) {
                    log.error(messageHelper.getMessage(RESP_ERROR_FILE_ALREADY_EXISTS, entry.getValue()));
                    response.setStatus("ERROR");
                    response.setCode(RESP_ERROR_FILE_ALREADY_EXISTS.getCode());
                    response.setMessage(messageHelper.getMessage(RESP_ERROR_FILE_ALREADY_EXISTS.getMessageKey(),
                            entry.getValue()));
                    return response;
                }
            }

            log.info(messageHelper.getMessage("axenapi.info.spec.gen.complete.success"));
            response.setStatus("OK");
            response.setDownloadLinks(result);
            return response;

        } catch (Exception e) {
            log.error(MessageHelper.getStaticMessage(ERROR_UNEXPECTED_DURING_SPEC_GEN), e);
            response.setStatus("ERROR");
            response.setCode(RESP_ERROR_UNEXPECTED_ERROR_SPEC_GEN.getCode());
            response.setMessage(messageHelper.getMessage(RESP_ERROR_UNEXPECTED_ERROR_SPEC_GEN.getMessageKey(),
                    e.getMessage()));
            return response;
        }
    }


    public String getSpecByServiceId(EventGraphDTO eventGraphDTO, UUID serviceID) throws JsonProcessingException {
        EventGraphFacade facade = new EventGraphFacade(eventGraphDTO);
        OpenAPI openAPISpecByServiceId = OpenAPIGenerator.getOpenAPISpecByServiceId(facade, serviceID);
        if(openAPISpecByServiceId == null) {
            log.error(MessageHelper.getStaticMessage(ERROR_WHILE_GET_OPEN_API_SPEC_SERVICE, serviceID));
            return "";
        }
        // Serialize the entire OpenAPI object to JSON string
        return Json.pretty(openAPISpecByServiceId);
    }
}