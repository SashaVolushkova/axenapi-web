package pro.axenix_innovation.axenapi.web.service;

import lombok.Getter;
import java.util.Map;

@Getter
public class AllServicePdf {
    private final Map<String, String> individualMdFiles;
    private final byte[] combinedPdfBytes;

    public AllServicePdf(Map<String, String> individualMdFiles, byte[] combinedPdfBytes) {
        this.individualMdFiles = individualMdFiles;
        this.combinedPdfBytes = combinedPdfBytes;
    }
}
