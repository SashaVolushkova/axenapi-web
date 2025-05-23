package pro.axenix_innovation.axenapi.web.util;

import io.swagger.v3.oas.models.PathItem;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility for extracting tags from OpenAPI PathItem.
 */
public class TagExtractor {
    public static Set<String> extractTags(PathItem path) {
        if (path == null) {
            return Collections.emptySet();
        }
        Set<String> allTags = new HashSet<>();
        if (path.getGet() != null && path.getGet().getTags() != null) {
            allTags.addAll(path.getGet().getTags());
        }
        if (path.getPost() != null && path.getPost().getTags() != null) {
            allTags.addAll(path.getPost().getTags());
        }
        if (path.getPut() != null && path.getPut().getTags() != null) {
            allTags.addAll(path.getPut().getTags());
        }
        if (path.getDelete() != null && path.getDelete().getTags() != null) {
            allTags.addAll(path.getDelete().getTags());
        }
        if (path.getPatch() != null && path.getPatch().getTags() != null) {
            allTags.addAll(path.getPatch().getTags());
        }
        if (path.getOptions() != null && path.getOptions().getTags() != null) {
            allTags.addAll(path.getOptions().getTags());
        }
        if (path.getHead() != null && path.getHead().getTags() != null) {
            allTags.addAll(path.getHead().getTags());
        }
        if (path.getTrace() != null && path.getTrace().getTags() != null) {
            allTags.addAll(path.getTrace().getTags());
        }
        return new HashSet<>(allTags);
    }

    /**
     * Extracts all tags from the given OpenAPI PathItem, aggregating tags from all HTTP methods.
     * This is an alias for extractTags method for backward compatibility.
     *
     * @param path the PathItem to extract tags from
     * @return a set of tags found in the path's operations
     */
    public static Set<String> getTagsFromPath(PathItem path) {
        return extractTags(path);
    }
}
