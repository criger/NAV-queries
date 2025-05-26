package com.nav.work.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nav.work.models.Feed;
import com.nav.work.models.AnnonserPrUke;
import com.nav.work.models.FeedLine;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AnnonseService
{

    private final HttpClient httpClient;
    private final TokenService tokenService;
    private final String baseUrl;
    private final long fetchDelayMs;
    private final int monthsBackInTime;
    private final int timeOutLimit;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US) // RFC 1123
            .withZone(ZoneId.of("Europe/Oslo")); //

    public AnnonseService(HttpClient httpClient,
                          TokenService tokenService,
                          @Value("${nav.baseURI}") String baseUrl,
                          @Value("${nav.rateLimitDelay}") long fetchDelayMs,
                          @Value("${nav.monthsBackInTime}") int monthsBackInTime,
                          @Value("${nav.timeOutLimit}") int timeOutLimit)
    {
        this.httpClient = httpClient;
        this.tokenService = tokenService;
        this.baseUrl = baseUrl;
        this.fetchDelayMs = fetchDelayMs;
        this.monthsBackInTime = monthsBackInTime;
        this.timeOutLimit = timeOutLimit;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public CompletableFuture<Collection<AnnonserPrUke>> hentAnnonserHalvtAarTilbakeITid()
    {
        // ZonedDateTime kan brukes for å konvertere mellom tidssoner. F.eks kan et gitt tidspunkt i Oslo på en gitt dato konverteres til tilsvarende tid i f.eks Los Angeles
        ZonedDateTime sixMonthsAgoInOslo = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(monthsBackInTime);

        String ifModifiedSinceHeader = sixMonthsAgoInOslo.format(HTTP_DATE_FORMATTER); // Formatterer dato til RFC 1123

        System.out.println("Henter annonser opprettet etter: " + sixMonthsAgoInOslo + " (bruker If-Modified-Since: " + ifModifiedSinceHeader + ")"); // Updated print

        return tokenService.fetchPublicToken()
                .thenCompose(token ->
                {
                    if (token == null)
                    {
                        return CompletableFuture.failedFuture(new RuntimeException("Klarte ikke hente token. Jobben avbrytes!"));
                    }
                    System.out.println("Token mottat, fortsetter videre");

                    // Pass the If-Modified-Since header to the recursive fetching method
                    return hentSiderRekursivt(baseUrl + "feed", token, ifModifiedSinceHeader, new ArrayList<>());
                })
                .thenApply(allFeedLines ->
                {
                    // filtrerer bort KUN de som er eldre enn 6 mnd
                    // merk at datofiltreringen som gjøres her er i tilfelle NAV bestemmer seg for å ikke lenger støtte filtrering i header
                    List<FeedLine> filteredFeedLines = allFeedLines.stream()
                            .filter(feedLine -> feedLine.getDate_modified() != null && feedLine.getDate_modified().isAfter(sixMonthsAgoInOslo))
                            .collect(Collectors.toList());

                    System.out.println("Fullført henting, antall annonser funnet: " + filteredFeedLines.size());
                    return processFeedLines(filteredFeedLines).values();
                })
                .exceptionally(ex -> {
                    System.err.println("Error during ad analysis: " + ex.getMessage());
                    ex.printStackTrace();
                    return new ArrayList<>(); // Return empty list on error
                });
    }

    /**
     * Henter rekursivt alle feedsidene ved hjelp av HttpClient.sendAsync.
     * En liten forsinkelse er lagt inn mellom hver forespørsel for å unngå å overbelaste API-et og dermed få en HTTP 429 - Too Many Request.
     *
     * @param currentUrl URL-en til den nåværende siden som skal hentes.
     * @param token token for autentisering.
     * @param ifModifiedSinceHeader Den formatert datostrengen for If-Modified-Since header
     * @param collectedLines Liste med alle FeedLine-objekter som er funnet
     * @return CompletableFuture som fullføres med en liste av alle FeedLine-objekter.
     */
    private CompletableFuture<List<FeedLine>> hentSiderRekursivt(String currentUrl, String token, String ifModifiedSinceHeader, List<FeedLine> collectedLines)
    { // Updated signature
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(currentUrl))
                .header("Authorization", "Bearer " + token);


        if (ifModifiedSinceHeader != null && !ifModifiedSinceHeader.isEmpty())
        {
            requestBuilder.header("If-Modified-Since", ifModifiedSinceHeader);
        }

        HttpRequest request = requestBuilder
                .timeout(java.time.Duration.ofSeconds(timeOutLimit))
                .build();

        // Introduce a delay before making the request
        return CompletableFuture.supplyAsync(() ->
                {
                    try
                    {
                        if (fetchDelayMs > 0)
                        {
                            // her skjer forsinkelsen som skal forsøke å forhindre HTTP 429 - Too Many Requests
                            TimeUnit.MILLISECONDS.sleep(fetchDelayMs);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted during delay", e);
                    }
                    return null; // brukes kun for å returnere en verdi i supplyAsync. IKKE i bruk, kun for å få kode til å kjøre :-)

                }).thenCompose(ignored -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenCompose(response ->
                {
                    if (response.statusCode() == 200)
                    {
                        try
                        {
                            Feed feed = objectMapper.readValue(response.body(), Feed.class);
                            if (feed.getFeedLines() != null)
                            {
                                collectedLines.addAll(feed.getFeedLines());
                            }

                            if (feed.getNextUrl() != null && !feed.getNextUrl().isEmpty())
                            {
                                // Log current page and next page
                                System.out.println("Hentet side. Nåværende antall annonser: " + collectedLines.size() + ". Henter neste fra: " + feed.getNextUrl());

                                String originalUri = "https://pam-stilling-feed.nav.no/api/v1/";
                                String target = ".no";

                                int index = originalUri.indexOf(target);

                                if (index != -1) {
                                    // If ".no" is found, take the substring from the beginning up to the end of ".no"
                                    String strippedUri = originalUri.substring(0, index + target.length());
                                    System.out.println(strippedUri); // Output: https://pam-stilling-feed.nav.no
                                    return hentSiderRekursivt(strippedUri + feed.getNextUrl(), token, ifModifiedSinceHeader, collectedLines);
                                } else {
                                    // Handle the case where ".no" is not found in the string
                                    System.out.println("'.no' not found in the URI.");
                                    return null;
                                }


                            }
                            else
                            {
                                System.out.println("Siste side nådd. Ferdig med henting av annonser.");
                                return CompletableFuture.completedFuture(collectedLines);
                            }
                        }
                        catch (Exception e)
                        {
                            System.err.println("En feil skjedde. Noe er ØLvørlig galt..! " + currentUrl + ": " + e.getMessage());
                            return CompletableFuture.failedFuture(e);
                        }
                    }
                    else if (response.statusCode() == 304)
                    {
                        System.out.println("Feed side " + currentUrl + " ikke endret siden If-Modified-Since datoen. Stopper paginering.");
                        return CompletableFuture.completedFuture(collectedLines);
                    }
                    else
                    {
                        System.err.println("En feil skjedde på side: " + currentUrl + ". HTTP Status: " + response.statusCode() + ", Response: " + response.body());
                        return CompletableFuture.failedFuture(new RuntimeException("Kunne ikke hente side. Http status kode: " + response.statusCode()));
                    }
                });
    }

    private TreeMap<String, AnnonserPrUke> processFeedLines(List<FeedLine> feedLines) {
        TreeMap<String, AnnonserPrUke> weeklyCounts = new TreeMap<>();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (FeedLine line : feedLines)
        {
            String content = line.getContent_text() != null ? line.getContent_text().toLowerCase(Locale.ROOT) : "";
            String title = line.getTitle() != null ? line.getTitle().toLowerCase(Locale.ROOT) : "";
            ZonedDateTime date = line.getDate_modified();

            if (date == null)
            {
                continue;
            }

            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());

            String weekKey = String.format("%d-%02d", year, weekOfYear);

            weeklyCounts.putIfAbsent(weekKey, new AnnonserPrUke(weekKey,0, 0));
            AnnonserPrUke count = weeklyCounts.get(weekKey);

            // legger opp to if'er her for de annonsene som har både Kotlin og Java i seg
            if (content.contains("kotlin") || title.contains("kotlin"))
            {
                count.setAntallKotlin(count.getAntallKotlin() + 1);
            }
            if (content.contains("java") || title.contains("java"))
            {
                count.setAntallJava(count.getAntallJava() + 1);
            }
        }
        return weeklyCounts;
    }
}