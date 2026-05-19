package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

@Component
public class ProtvarClient {

    private final RestClient restClient;

    public ProtvarClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public String get(String uriTemplate, Object... uriVars) {
        try {
            return restClient.get()
                    .uri(uriTemplate, uriVars)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return "No data available (404 Not Found)";
        } catch (RestClientResponseException e) {
            return "API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        }
    }

    public String get(Function<UriBuilder, URI> uriFunction) {
        try {
            return restClient.get()
                    .uri(uriFunction)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return "No data available (404 Not Found)";
        } catch (RestClientResponseException e) {
            return "API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        }
    }

    public String post(String uriTemplate, MediaType contentType, String body) {
        try {
            return restClient.post()
                    .uri(uriTemplate)
                    .contentType(contentType)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (HttpClientErrorException.NotFound e) {
            return "No data available (404 Not Found)";
        } catch (RestClientResponseException e) {
            return "API error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        }
    }
}
