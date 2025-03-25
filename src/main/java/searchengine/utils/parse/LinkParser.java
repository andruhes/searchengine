package searchengine.utils.parse;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.config.UserAgents;
import searchengine.model.Site;
import org.jsoup.Connection;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class LinkParser {
    private final Site site;
    private final String url;
    private final Set<String> allLinksOfPage = new HashSet<>();
    private final UserAgents userAgent;
    private String path;
    private int code;
    private String content;

    public void parse() throws IOException {
        String siteUrl = site.getUrl().replace("www.", "");
        siteUrl = siteUrl.endsWith("/") ? siteUrl : siteUrl + "/";
        path = "/" + url.replaceAll(siteUrl,"");

        Random random = new Random();
        Connection connection = Jsoup.connect(url)
                .userAgent(userAgent.getUser(random.nextInt(userAgent.getUsers().size())))
                .referrer(userAgent.getReferrer())
                .ignoreHttpErrors(true)
                .followRedirects(true);

        code = connection.execute().statusCode();
        content = code == 200 ? connection.get().html() : "";

        Document document = connection.get();
        Elements elements = document.select("body").select("a");
        elements.forEach(e -> {
            String link = e.absUrl("href");
            if (link.contains(url) && !link.contains("#") && !link.isEmpty()
                    && isLink(link) && !isFile(link)){
                allLinksOfPage.add(link);
            }
        });
    }

    private boolean isLink(String link) {
        return link.matches("^(https?|ftp)://[-\\w+&@#/%?=~_|!:,.;]*[-\\w+&@#/%=~_|]");
    }

    private boolean isFile(String link) {
        link = link.toLowerCase();
        return link.matches(".*\\.(jpg|jpeg|png|gif|webp|pdf|eps|xlsx|doc|pptx|docx)$") 
               || link.contains("?_ga");
    }
}