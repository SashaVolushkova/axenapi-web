package pro.axenix_innovation.axenapi.web.exception;

public class OpenAPISpecParseException extends Exception {
    private String fileName;

    public OpenAPISpecParseException(String fileName, String s) {
        super(s);
        this.fileName = fileName;
    }


    public String getFileName() {
        return fileName;
    }
    public OpenAPISpecParseException(String s) {
        super(s);
    }
}
