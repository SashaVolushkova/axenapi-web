package pro.axenix_innovation.axenapi.web.exception;

public class AxenApiException {

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

}
