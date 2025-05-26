package com.nav.work.controller; // Sørg for at dette er riktig pakkenavn for din controller

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.work.models.AnnonserPrUke; // Bruker ditt nye modellnavn
import com.nav.work.services.AnnonseService; // Bruker ditt nye servicenavn
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/annonser")
public class JobbAnnonseController {

    private final AnnonseService annonseService; // Oppdatert til AnnonseService
    private final ObjectMapper objectMapper; // For pen utskrift til konsoll

    public JobbAnnonseController(AnnonseService annonseService) { // Injisering av AnnonseService
        this.annonseService = annonseService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Sikrer at ZonedDateTime håndteres korrekt
    }

    @GetMapping(value = "/weekly-language-counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Collection<AnnonserPrUke>> getWeeklyLanguageCounts() { // Oppdatert til AnnonserPrUke
        try {
            // Blokker og vent på at CompletableFuture fullføres, med en timeout
            Collection<AnnonserPrUke> counts = annonseService.hentAnnonserHalvtAarTilbakeITid() // Kaller den nye metoden
                    .get(120, TimeUnit.SECONDS); // Maks 2 minutter for å hente og behandle

            // Skriv ut til konsoll (pent formatert JSON)
            System.out.println("\n--- Endelig Rapport (JSON) ---");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(counts));
            System.out.println("---------------------------\n");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(counts);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Gjenopprett interrupt-status
            System.err.println("Analyse avbrutt: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
        } catch (ExecutionException e) {
            System.err.println("Feil under utførelse av annonseanalyse: " + e.getCause().getMessage());
            e.getCause().printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        } catch (TimeoutException e) {
            System.err.println("Analyse av annonser tok for lang tid: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(null);
        } catch (Exception e) {
            System.err.println("Uventet feil under annonseanalyse: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}