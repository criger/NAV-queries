package com.nav.work.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.work.helpers.NavHelper;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AnnonseService
{

    private final HttpClient httpClient;
    private final TokenService tokenService;
    private final NavHelper navHelper;
    private final String baseUrl;
    private final String strippetBaseURI;
    private final long fetchDelayMs;
    private final int monthsBackInTime;
    private final int timeOutLimit;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss XXXX", Locale.US) // RFC 1123
            .withZone(ZoneId.of("Europe/Oslo"));


    public AnnonseService(HttpClient httpClient,
                          TokenService tokenService,
                          NavHelper navHelper,
                          @Value("${nav.baseURI}") String baseUrl,
                          @Value("${nav.strippetBaseURI}") String strippetBaseURI,
                          @Value("${nav.rateLimitDelay}") long fetchDelayMs,
                          @Value("${nav.monthsBackInTime}") int monthsBackInTime,
                          @Value("${nav.timeOutLimit}") int timeOutLimit
    ) {
        this.httpClient = httpClient;
        this.tokenService = tokenService;
        this.navHelper = navHelper;
        this.baseUrl = baseUrl;
        this.strippetBaseURI = strippetBaseURI;
        this.fetchDelayMs = fetchDelayMs;
        this.monthsBackInTime = monthsBackInTime;
        this.timeOutLimit = timeOutLimit;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public CompletableFuture<Collection<AnnonserPrUke>> hentAnnonserHalvtAarTilbakeITid()
    {
        ZonedDateTime sixMonthsAgoInOslo = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(monthsBackInTime);
        String ifModifiedSinceHeader = sixMonthsAgoInOslo.format(HTTP_DATE_FORMATTER);

        System.out.println("Henter annonser opprettet etter: " + sixMonthsAgoInOslo + " (bruker If-Modified-Since: " + ifModifiedSinceHeader + ")");

        return tokenService.fetchPublicToken()
                .thenCompose(token ->
                {
                    if (token == null)
                    {
                        return CompletableFuture.failedFuture(new RuntimeException("Klarte ikke hente token. Jobben avbrytes!"));
                    }
                    System.out.println("Token mottat, fortsetter videre");

                    return hentSiderRekursivt(strippetBaseURI + "/api/v1/feed", token, ifModifiedSinceHeader, new ArrayList<>());
                })
                .thenApply(allFeedLines ->
                {
                    ZonedDateTime currentSixMonthsAgoInOslo = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(monthsBackInTime);

                    // Denne filtreringen gjøres i tilfelle header i kallet ikke klarte å filtrere på dato
                    // I tillegg tar vi kun med de som er ACTIVE
                    List<FeedLine> eligibleFeedLines = allFeedLines.stream()
                            .filter(feedLine -> feedLine.get_feed_entry() != null && "ACTIVE".equalsIgnoreCase(feedLine.get_feed_entry().getStatus()))
                            .filter(feedLine -> feedLine.getDate_modified() != null && feedLine.getDate_modified().isAfter(currentSixMonthsAgoInOslo))
                            .collect(Collectors.toList());


                    // gjør en ekstra filtrering her da vi av ytelsesårsaker sjekker kun Title feltet for Java og/eller Kotlin.
                    // "utvikler" og "backend" er tatt med da dette var en del av den initielle sjekken der jeg sjekket hver enkelt annonse som matchet filteret.
                    // utfordringen med dette er at annonser IKKE er indeksert, og dermed tar dette kallet vesentlig lenger tid enn det opprinnelige kallet.
                    List<FeedLine> filteredFeedLines = new ArrayList<>();
                    StringBuilder titleLower = new StringBuilder(); // stringbuilder gjør at vi kan operere på kun ET objekt hele veien

                    for (FeedLine line : eligibleFeedLines)
                    {
                        titleLower.setLength(0); // nuller ut sb-objektet, klar til bruk på nytt i neste loop...
                        titleLower.append(line.getTitle().toLowerCase()); // denne kommer vi ikke unna.. toLowerCase() oppretter et String objekt..

                        if (titleLower.indexOf("java") != -1 ||
                            titleLower.indexOf("kotlin") != -1 ||
                            titleLower.indexOf("utvikler") != -1 ||
                            titleLower.indexOf("backend") != -1
                        ) // bruken av indexOf gjør at vi sjekker mot byteverdier lagret i sb-objektet istedenfor å kalle på toString().toLowerCase() som ville opprettet to immutable string objekter.
                            // objektene ville levd en stund i minnet før GC finner ut at den trenger å frigjøre minne
                        {
                            filteredFeedLines.add(line);
                        }
                    }

                    System.out.println("Antall annonser etter status og dato filtering: " + filteredFeedLines.size());
                    System.out.println("Fullført filtering. Behandler titler med match på Java og/eller Kotlin.");

                    return navHelper.processFeedLines(filteredFeedLines).values();
                })
                .exceptionally(ex ->
                {
                    System.err.println("En feil skjedde under sjekk av annonser: " + ex.getMessage());
                    ex.printStackTrace();
                    return new ArrayList<>();
                });
    }

    private CompletableFuture<List<FeedLine>> hentSiderRekursivt(String currentUrl, String token, String ifModifiedSinceHeader, List<FeedLine> collectedLines)
    {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(currentUrl))
                .header("Authorization", "Bearer " + token);


        if (ifModifiedSinceHeader != null && !ifModifiedSinceHeader.isEmpty())
        {
            System.out.println("RFC 1123 dato: " + ifModifiedSinceHeader);
            requestBuilder.header("If-Modified-Since", ifModifiedSinceHeader);
        }

        HttpRequest request = requestBuilder
                .timeout(java.time.Duration.ofSeconds(timeOutLimit))
                .build();

        return CompletableFuture.supplyAsync(() ->
                {
                    try
                    {
                        if (fetchDelayMs > 0)
                        {
                            TimeUnit.MILLISECONDS.sleep(fetchDelayMs);
                        }
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted during delay", e);
                    }
                    return null;
                })
                .thenCompose(ignored -> httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .thenCompose(response ->
                {
                    if (response.statusCode() == 200)
                    {
                        try
                        {
                            Feed feed = objectMapper.readValue(response.body(), Feed.class);
                            System.out.println("Antall items: " + feed.getItems().size());

                            if (feed.getItems() != null)
                            {
                                collectedLines.addAll(feed.getItems());
                            }

                            if (feed.getNextUrl() != null && !feed.getNextUrl().isEmpty())
                            {
                                System.out.println("Hentet side. Nåværende antall annonser: " + collectedLines.size() + ". Henter neste fra: " + feed.getNextUrl());

                                String domainBaseUrl = baseUrl.substring(0, baseUrl.indexOf(".no") + ".no".length());

                                URI nextFullUri = URI.create(domainBaseUrl).resolve(feed.getNextUrl());

                                return hentSiderRekursivt(nextFullUri.toString(), token, ifModifiedSinceHeader, collectedLines);
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


}