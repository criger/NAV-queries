package com.nav.work.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class TokenService
{

    private final HttpClient httpClient;
    private final String tokenUrl;
    private final int timeOutLimit;

    public TokenService(HttpClient httpClient,
                        @Value("${nav.tokenURI}") String tokenUrl,
                        @Value("${nav.timeOutLimit}") int timeOutLimit)
    {
        this.httpClient = httpClient;
        this.tokenUrl = tokenUrl;
        this.timeOutLimit = timeOutLimit;
    }

    /**
     * Henter en public token.
     * Metoden forventer at tokenet finnes på andre linje i responsen.
     *
     * @return CompletableFuture med tokenet eller en nullverdi om den ikke fant token eller en feil skjedde
     */
    public CompletableFuture<String> fetchPublicToken()
    {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(tokenUrl))
                .timeout(Duration.ofSeconds(timeOutLimit))
                .build();

        System.out.println("Prøver å hente token fra: " + tokenUrl);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response ->
                {
                    if (response.statusCode() == 200)
                    {
                        String responseBody = response.body();
                        try (BufferedReader reader = new BufferedReader(new StringReader(responseBody)))
                        {
                            // Hopp over første linje. Tokenet er lagret i linje 2
                            reader.readLine();

                            String token = reader.readLine(); // nå er vi på linje 2.
                            if (token != null && token.startsWith("ey"))
                            {
                                // gjør en nullsjekk først, i tilfelle den ikke klarte å hente noen verdi fra linje 2
                                // deretter sjekker den at teksten begynner med 'ey' da alle token begynner med 'ey'
                                System.out.println("Token: " + token);
                                return token.trim(); // trimmer token slik at evt mellomrom fjernes før og etter tekstverdien
                            }
                            else
                            {
                                System.err.println("Ingen linje 2 her... ooops..!");
                                return null;
                            }
                        }
                        catch (Exception e)
                        {
                            System.err.println("En feil skjedde under lesing av responsen: " + e.getMessage());
                            return null;
                        }
                    }
                    else
                    {
                        // alt annet enn HTTP 200
                        System.err.println("Klarte ikke hente token. HTTP status kode: " + response.statusCode());
                        System.err.println("Response body: " + response.body());
                        return null;
                    }
                })
                .exceptionally(ex ->
                {
                    System.err.println("Noe ØLvørlig galt skjedde under henting av token: " + ex.getMessage());
                    return null;
                });
    }
}
