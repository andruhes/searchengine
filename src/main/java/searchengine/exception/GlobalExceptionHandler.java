package searchengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public IndexingResponse handleException(Exception ex) {
        logError("Internal server error", ex);
        return new IndexingResponse("Внутренняя ошибка сервера: " + ex.getMessage());
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IndexingResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        logError("Invalid request", ex);
        return new IndexingResponse("Некорректный запрос: " + ex.getMessage());
    }

    @ExceptionHandler(SearchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public SearchResponse handleSearchException(SearchException ex) {
        logError("Search error", ex);
        return new SearchResponse("Ошибка поиска: " + ex.getMessage());
    }

    @ExceptionHandler(IndexingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IndexingResponse handleIndexingException(IndexingException ex) {
        logError("Indexing error", ex);
        return new IndexingResponse("Ошибка индексации: " + ex.getMessage());
    }

    @ExceptionHandler(PageProcessingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public IndexingResponse handlePageProcessingException(PageProcessingException ex) {
        logError("Page processing error", ex);
        return new IndexingResponse("Ошибка обработки страницы: " + ex.getMessage());
    }

    @ExceptionHandler(SiteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public IndexingResponse handleSiteNotFoundException(SiteNotFoundException ex) {
        logError("Site not found", ex);
        return new IndexingResponse("Сайт не найден: " + ex.getMessage());
    }

    private void logError(String message, Exception ex) {
        log.error("{}: {}", message, ex.getMessage(), ex);
    }
}