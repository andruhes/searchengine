package searchengine.exception;

public class SiteNotFoundException extends RuntimeException {
    public SiteNotFoundException(String message) {
        super(message);
    }
    
    public SiteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}