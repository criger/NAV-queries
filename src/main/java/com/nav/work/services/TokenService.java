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
    private final String tokenUrl; // Gjort final da den nå injiseres
    private final int timeOutLimit; // Gjort final da den nå injiseres

    // Spring vil injisere HttpClient og verdiene fra application.yaml automatisk
    public TokenService(HttpClient httpClient,
                        @Value("${nav.tokenURI}") String tokenUrl,     // Injiserer tokenUrl fra application.yaml
                        @Value("${nav.timeOutLimit}") int timeOutLimit) // Injiserer timeOutLimit fra application.yaml
    {
        this.httpClient = httpClient;
        this.tokenUrl = tokenUrl;
        this.timeOutLimit = timeOutLimit;
    }

    /**
     * Henter en public token fra det angitte endepunktet.
     * Den forventer at tokenet er på den andre linjen av responsen.
     *
     * @return A CompletableFuture that completes with the token string, or null if an error occurs.
     */
    public CompletableFuture<String> fetchPublicToken() {
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

                            String token = reader.readLine(); // nå er vi på linje 2
                            if (token != null)
                            {
                                System.out.println("Successfully fetched public token.");
                                System.out.println("Token: " + token.trim());
                                return token.trim(); // fjern alle spaces før og etter tokenet
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
