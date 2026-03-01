package com.smartmobility.billingservice.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.codec.Decoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * FeignConfig — décodeur personnalisé pour extraire le champ "data"
 * des réponses ApiResponse<T> du user-service.
 *
 * user-service retourne : { "status": 200, "message": "...", "data": { ... } }
 * On extrait automatiquement "data" et on le désérialise dans le type attendu.
 */
@Configuration
public class FeignConfig {

    @Bean
    public Decoder feignDecoder() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        return (response, type) -> {
            String body = new BufferedReader(
                new InputStreamReader(response.body().asInputStream(), StandardCharsets.UTF_8))
                .lines().collect(Collectors.joining("\n"));

            JsonNode root = mapper.readTree(body);

            // Si la réponse a un champ "data", on extrait ce champ
            if (root.has("data") && !root.get("data").isNull()) {
                return mapper.treeToValue(root.get("data"),
                        mapper.constructType(type));
            }

            // Sinon on désérialise directement (fallback)
            return mapper.readValue(body,
                    mapper.constructType(type));
        };
    }
}
