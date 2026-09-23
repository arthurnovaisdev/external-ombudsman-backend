package com.mbfreire.employee_reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
@ConditionalOnProperty(name = "attachments.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final RestClient supabaseRestClient;

    @Value("${supabase.storage.bucket}")
    private String bucketName;

    public void upload(
            String storedFileName,
            byte[] content,
            String contentType
    ) {

        try {

            supabaseRestClient
                    .post()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .pathSegment(
                                            "storage",
                                            "v1",
                                            "object",
                                            bucketName,
                                            storedFileName
                                    )
                                    .build()
                    )
                    .contentType(
                            MediaType.parseMediaType(contentType)
                    )
                    .header(
                            "x-upsert",
                            "false"
                    )
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientResponseException e) {

            log.error(
                    "Falha no upload para o storage. Status HTTP: {}",
                    e.getStatusCode().value()
            );

            throw new IllegalStateException(
                    "Falha ao armazenar o arquivo.",
                    e
            );
        }
    }

    public byte[] download(
            String storedFileName
    ) {

        try {

            byte[] content =
                    supabaseRestClient
                            .get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .pathSegment(
                                                    "storage",
                                                    "v1",
                                                    "object",
                                                    "authenticated",
                                                    bucketName,
                                                    storedFileName
                                            )
                                            .build()
                            )
                            .accept(MediaType.ALL)
                            .retrieve()
                            .body(byte[].class);

            if (content == null) {
                throw new IllegalStateException(
                        "Arquivo não encontrado ou indisponível."
                );
            }

            return content;

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().value() == 404) {
                throw new IllegalStateException(
                        "Arquivo não encontrado ou indisponível."
                );
            }

            log.error(
                    "Falha ao baixar arquivo do storage. Status HTTP: {}",
                    e.getStatusCode().value()
            );

            throw new IllegalStateException(
                    "Não foi possível carregar o arquivo.",
                    e
            );
        }
    }

    public boolean delete(
            String storedFileName
    ) {

        try {

            supabaseRestClient
                    .delete()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .pathSegment(
                                            "storage",
                                            "v1",
                                            "object",
                                            bucketName,
                                            storedFileName
                                    )
                                    .build()
                    )
                    .retrieve()
                    .toBodilessEntity();

            return true;

        } catch (RestClientResponseException e) {

            if (e.getStatusCode().value() == 404) {
                return false;
            }

            log.warn(
                    "Não foi possível remover arquivo do storage. Status HTTP: {}",
                    e.getStatusCode().value()
            );

            return false;

        } catch (RuntimeException e) {

            log.warn(
                    "Não foi possível remover arquivo do storage."
            );

            return false;
        }
    }
}
