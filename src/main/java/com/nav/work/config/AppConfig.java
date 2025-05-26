package com.nav.work.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public HttpClient httpClient()
    {

        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2) // <-- bedre og sikrere enn den eldre HTTP 1.1 protokollen. Direkte koblet til TLS versjon.
                .followRedirects(HttpClient.Redirect.ALWAYS) // følg alle redirecter uansett, selv om det går fra https til http.
                .build();
    }
}