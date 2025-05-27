package com.nav.work.controller; // Sørg for at dette er riktig pakkenavn for din controller

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nav.work.models.AnnonserPrUke;
import com.nav.work.services.AnnonseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
public class JobbAnnonseController
{

    private final AnnonseService annonseService;
    private final ObjectMapper objectMapper;

    public JobbAnnonseController(AnnonseService annonseService)
    {
        this.annonseService = annonseService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Sikrer at ZonedDateTime håndteres korrekt - tips funnet på nettet
    }

    @Operation(summary = "Henter annonser pr uke for de siste månedene bestemt i application.yaml",
            description = "Lager en liste over antall jobbannonser med Java- og Kotlin per uke for en bestemt tidsperiode bestemt i application.yaml.",
            responses =
                    {
                        @ApiResponse(
                                responseCode = "200", description = "Vellykket henting av ukentlige tall",
                                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = AnnonserPrUke.class, type = "array"))),
                        @ApiResponse(responseCode = "500", description = "Intern serverfeil"),
                        @ApiResponse(responseCode = "503", description = "Tjenesten er utilgjengelig"),
                        @ApiResponse(responseCode = "504", description = "Gateway Timeout")
                    })
    @GetMapping(value = "/javaKotlinPrUke", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getWeeklyLanguageCounts()
    {
        // legg merke til at jeg her bruker ResponseEntity<?>
        // dette gjør at metoden kan levere nøyaktig hvilken type verdi den vil
        // f.eks en String, et JSON objekt, en exception osv osv..
        try
        {
            Collection<AnnonserPrUke> annonserPrUke = annonseService.hentAnnonserHalvtAarTilbakeITid()
                    .get(120, TimeUnit.SECONDS);

            // Pretty print av JSON til konsollen
            System.out.println(System.lineSeparator());
            System.out.println("--- Endelig Rapport (JSON) ---");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(annonserPrUke));
            System.out.println("---------------------------");
            System.out.println(System.lineSeparator());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(annonserPrUke);

        }
        catch (InterruptedException e)
        {
            String error = "Annonsehenting avbrutt: " + e.getMessage();
            Thread.currentThread().interrupt(); // frigjør tråden ettersom den nå er "blokkert"
            System.err.println(error);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
        catch (ExecutionException e)
        {
            String error = "Feil under sjekk av annonser: " + e.getCause().getMessage();
            System.err.println(error);
            e.getCause().printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
        catch (TimeoutException e)
        {
            String error = "Feil under sjekk av annonser: " + e.getCause().getMessage();
            System.err.println(error);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
        }
        catch (Exception e)
        {
            String error = "Feil under sjekk av annonser: " + e.getCause().getMessage();
            System.err.println(error);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}