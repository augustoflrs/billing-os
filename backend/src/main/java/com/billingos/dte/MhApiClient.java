package com.billingos.dte;

import com.billingos.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends signed DTE payloads to the Ministerio de Hacienda API.
 *
 * Returns a typed {@link MhResponse} — never throws; network/server errors
 * surface as {@link MhResponse.Kind#TRANSIENT_ERROR} so the caller can retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MhApiClient {

    private final RestClient.Builder restClientBuilder;
    private final AppProperties      appProperties;
    private final ObjectMapper       objectMapper;

    /**
     * Submits a signed DTE to MH.
     *
     * @param dteJson   the complete signed dteJson (with firma)
     * @param companyNit NIT of the emitting company
     */
    @SuppressWarnings("unchecked")
    public MhResponse submit(Map<String, Object> dteJson, String companyNit) {
        AppProperties.Dte cfg = appProperties.getDte();
        String url = cfg.getMhApiUrl() + cfg.getMhApiPath();
        String certPassword = cfg.getCertificatePassword();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nit",         companyNit);
        body.put("activo",      true);
        body.put("passwordPri", certPassword);
        body.put("dteJson",     dteJson);

        RestClient client = restClientBuilder
                .baseUrl(cfg.getMhApiUrl())
                .build();

        try {
            Map<String, Object> responseMap = client.post()
                    .uri(cfg.getMhApiPath())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return parseMhResponse(responseMap, body);

        } catch (HttpClientErrorException e) {
            // 4xx — MH rejected the request itself (malformed envelope, auth error)
            log.warn("MH returned 4xx for NIT {}: {} {}", companyNit, e.getStatusCode(), e.getResponseBodyAsString());
            Map<String, Object> errMap = safeParseJson(e.getResponseBodyAsString());
            return MhResponse.rejected(
                    errMap.getOrDefault("codigoMsg", "4XX").toString(),
                    errMap.getOrDefault("descripcionMsg", e.getMessage()).toString(),
                    body, errMap);

        } catch (HttpServerErrorException e) {
            log.warn("MH returned 5xx for NIT {}: {}", companyNit, e.getStatusCode());
            return MhResponse.transientError("MH HTTP " + e.getStatusCode(), body);

        } catch (ResourceAccessException e) {
            log.warn("MH unreachable for NIT {}: {}", companyNit, e.getMessage());
            return MhResponse.transientError("MH unreachable: " + e.getMessage(), body);

        } catch (Exception e) {
            log.error("Unexpected error calling MH for NIT {}: {}", companyNit, e.getMessage(), e);
            return MhResponse.transientError("Unexpected: " + e.getMessage(), body);
        }
    }

    @SuppressWarnings("unchecked")
    private MhResponse parseMhResponse(Map<String, Object> raw, Map<String, Object> requestBody) {
        if (raw == null) return MhResponse.transientError("Empty response from MH", requestBody);

        String estado = String.valueOf(raw.getOrDefault("estado", ""));
        String codigo = String.valueOf(raw.getOrDefault("codigoMsg", ""));
        String desc   = String.valueOf(raw.getOrDefault("descripcionMsg", ""));
        String sello  = String.valueOf(raw.getOrDefault("selloRecibido", ""));

        if ("PROCESADO".equalsIgnoreCase(estado)) {
            return MhResponse.accepted(sello, codigo, desc, requestBody, raw);
        } else if ("RECHAZADO".equalsIgnoreCase(estado)) {
            return MhResponse.rejected(codigo, desc, requestBody, raw);
        } else {
            log.warn("Unknown MH estado '{}': {}", estado, raw);
            return MhResponse.transientError("Unknown estado: " + estado, requestBody);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeParseJson(String body) {
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            return Map.of("raw", body);
        }
    }
}
