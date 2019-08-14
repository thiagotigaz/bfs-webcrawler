package br.com.supercloud;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class WebCrawler {

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36";
    private final URI startPageUri;
    private final Queue<URI> queue = new LinkedList<>();
    private final Map<URI, Boolean> visited = new HashMap<>();
    private final Set<URI> resources = new TreeSet<>();

    public WebCrawler(URI startPageUri) {
        this.startPageUri = startPageUri;
    }

    /*
        Uses Breadth-First-Search to navigate through all the pages within the same domain and retrieve all the static
         assets.
     */
    public Set<URI> fetch() {
        visited.put(startPageUri, false);
        queue.add(startPageUri);
        while (!queue.isEmpty()) {
            URI uri = queue.poll();
            if (visited.containsKey(uri) && !visited.get(uri)) {
                try {
                    Document document = Jsoup.connect(uri.toString())
                            .userAgent(USER_AGENT)
                            .get();
                    // Mark page as visited so that we prevent it from being crawled again
                    visited.put(startPageUri, true);
                    resources.add(uri); // Add the fetched uri to the list of resources
                    resources.addAll(fetchStaticAssets(document)); // Add all extract static assets from current page
                    Set<URI> links = fetchContainedLinks(document); // Extract links that will later be crawled
                    links.forEach(l -> {
                        // Only add links that haven't been added yet, this way we can prevent cycles
                        if (!visited.containsKey(l)) {
                            visited.put(l, false);
                            queue.add(l);
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return resources;
    }

    /*
        Responsible to fetch all the links present in the page and return a set containing all them in order and
        without duplicates.
     */
    private Set<URI> fetchContainedLinks(Document document) {
        return document.select("a[href]").stream()
                .map(l -> l.attr("abs:href").replaceAll("\\#.*$", "")) // Remove anchors to prevent duplicates
                .map(this::toUri)
                .filter(this::filterByOriginDomain)
                .collect(Collectors.toSet());
    }

    /*
        Filter uris that are null, does not contain a host(empty) or are not within the same domain of the starting uri.
     */
    private boolean filterByOriginDomain(URI uri) {
        return uri != null
                && uri.getHost() != null
                && uri.getHost().equals(startPageUri.getHost());
    }

    /*
        Try to parse the given string into a URI and return null in case it fails.
     */
    private URI toUri(String url) {
        try {
            return URI.create(url);
        } catch (IllegalArgumentException ex) {
            // invalid urls like mailTo: thiagotigaz@gmail.com
            ex.printStackTrace();
            return null;
        }
    }

    /*
        Fetch all the static assets from the given page. Looks for link's href tag and src which is present in static
        assets (<audio>, <embed>, <iframe>, <img>, <input>, <script>, <source>, <track>, <video>).
     */
    private Set<URI> fetchStaticAssets(Document document) {
        Set<URI> results = new TreeSet<>();
        Elements imgAssets = document.select("[src]");
        results.addAll(imgAssets.stream()
                .map(l -> URI.create(l.attr("abs:src")))
                .collect(Collectors.toSet())
        );

        Elements staticAssets = document.select("link[href]");
        results.addAll(staticAssets.stream()
                .map(l -> URI.create(l.attr("abs:href")))
                .collect(Collectors.toSet())
        );

        return results;
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args.length > 1) {
            System.out.println("usage: java -jar build/libs/webcrawler-1.0-SNAPSHOT.jar http://urltobecrawled.com");
            System.exit(0);
        }
        WebCrawler webCrawler = new WebCrawler(URI.create(args[0]));
        Set<URI> fetchResources = webCrawler.fetch();
        System.out.println(String.format("-------------- The total number of fetched resources are %d --------------",
                fetchResources.size()));
        fetchResources.forEach(System.out::println);
    }
}
