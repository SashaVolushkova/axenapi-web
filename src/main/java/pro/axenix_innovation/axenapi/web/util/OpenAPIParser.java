package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.extern.slf4j.Slf4j;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.ERROR_PARSING_OPEN_API_SPEC;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_DURING_OPEN_API_PARSE;
import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_EMPTY_NULL_SPEC;

@Slf4j
public class OpenAPIParser {
    
    public static OpenAPI parseSpecification(String specification) {
        if (specification == null || specification.trim().isEmpty()) {
            log.warn(MessageHelper.getStaticMessage(WARN_EMPTY_NULL_SPEC));
            return null;
        }

        try {
            SwaggerParseResult result = new OpenAPIV3Parser().readContents(specification);
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                log.warn(MessageHelper.getStaticMessage(WARN_DURING_OPEN_API_PARSE, result.getMessages()));
            }
            return result.getOpenAPI();
        } catch (Exception e) {
            log.error(MessageHelper.getStaticMessage(ERROR_PARSING_OPEN_API_SPEC), e);
            return null;
        }
    }
} 