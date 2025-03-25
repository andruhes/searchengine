package searchengine.exception;

public class PageProcessingException extends RuntimeException {
    public PageProcessingException(String message) {
        super(message);
    }
    
    public PageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}