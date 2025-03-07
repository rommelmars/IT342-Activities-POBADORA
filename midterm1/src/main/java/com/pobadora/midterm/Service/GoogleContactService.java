package com.pobadora.midterm.Service;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoogleContactService {

    private final OAuth2AuthorizedClientService clientService;

    public GoogleContactService(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    private String getAccessToken(OAuth2User oAuth2User) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", oAuth2User.getAttribute("sub"));
        if (client == null) {
            throw new IllegalStateException("No authorized client found for Google");
        }
        return client.getAccessToken().getTokenValue();
    }

    public Map<String, Object> getContacts(OAuth2User oAuth2User) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", oAuth2User.getAttribute("sub"));
        if (client == null) {
            throw new IllegalStateException("No authorized client found for Google");
        }
    
        String accessToken = client.getAccessToken().getTokenValue();
        String url = "https://people.googleapis.com/v1/people/me/connections"
                + "?personFields=names,emailAddresses,phoneNumbers";
    
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
    
        return response.getBody();
    }

    public List<Map<String, String>> formatContacts(Map<String, Object> contacts) {
        List<Map<String, Object>> connections = (List<Map<String, Object>>) contacts.getOrDefault("connections", List.of());
        return connections.stream()
                .map(contact -> Map.of(
                        "name", getValue(contact, "names"),
                        "email", getValue(contact, "emailAddresses"),
                        "phone", getValue(contact, "phoneNumbers"),
                        "resourceName", (String) contact.getOrDefault("resourceName", "N/A")
                ))
                .collect(Collectors.toList());
    }

    private String getValue(Map<String, Object> contact, String key) {
        List<Map<String, Object>> values = (List<Map<String, Object>>) contact.get(key);
        if (values != null && !values.isEmpty()) {
            if (key.equals("names")) {
                return (String) values.get(0).getOrDefault("displayName", "N/A");
            } else if (key.equals("emailAddresses")) {
                return (String) values.get(0).getOrDefault("value", "N/A");
            } else if (key.equals("phoneNumbers")) {
                return (String) values.get(0).getOrDefault("value", "N/A");
            }
        }
        return "N/A";
    }  

    public void addContact(OAuth2User oAuth2User, String name, String email, String phone) {
        if (oAuth2User == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        String accessToken = getAccessToken(oAuth2User);
        String url = "https://people.googleapis.com/v1/people:createContact";

        Map<String, Object> body = Map.of(
            "names", List.of(Map.of("givenName", name)),
            "emailAddresses", List.of(Map.of("value", email)),
            "phoneNumbers", List.of(Map.of("value", phone))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        new RestTemplate().postForEntity(url, entity, String.class);
    }

    public void updateContact(OAuth2User oAuth2User, String resourceName, String name, String email, String phone) {
        System.out.println("Updating contact with resourceName: " + resourceName);
        String accessToken = getAccessToken(oAuth2User);
    
        Map<String, Object> contact = getContactDetails(oAuth2User, resourceName);
        String etag = (String) contact.get("etag");
        Map<String, Object> metadata = (Map<String, Object>) contact.get("metadata");
        List<Map<String, Object>> sources = metadata != null ? (List<Map<String, Object>>) metadata.get("sources") : List.of();
    
        if (etag == null || sources.isEmpty()) {
            throw new IllegalStateException("No valid etag or contact source found.");
        }
    
        System.out.println("Fetched etag: " + etag);
        System.out.println("Fetched sources: " + sources);
    
        Map<String, Object> person = new HashMap<>();
        person.put("etag", etag);
        person.put("metadata", Map.of("sources", sources));
        person.put("names", List.of(Map.of("givenName", name))); 
        person.put("emailAddresses", List.of(Map.of("value", email)));
        person.put("phoneNumbers", List.of(Map.of("value", phone))); 
    
        try {
            System.out.println("Request Body: " + new ObjectMapper().writeValueAsString(person));
        } catch (Exception e) {
            System.err.println("Failed to convert request body to JSON: " + e.getMessage());
        }
    
        WebClient webClient = WebClient.builder()
                .baseUrl("https://people.googleapis.com/v1/")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("If-Match", etag)
                .build();
    
        webClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path(resourceName + ":updateContact")
                        .queryParam("updatePersonFields", "names,emailAddresses,phoneNumbers")
                        .build())
                .body(BodyInserters.fromValue(person))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        responseBody -> System.out.println("Contact updated successfully."),
                        error -> {
                            if (error instanceof WebClientResponseException) {
                                WebClientResponseException webClientError = (WebClientResponseException) error;
                                System.err.println("Failed to update contact: " + webClientError.getRawStatusCode() + " " + webClientError.getStatusText());
                                System.err.println("Response body: " + webClientError.getResponseBodyAsString());
                            } else {
                                System.err.println("Failed to update contact: " + error.getMessage());
                            }
                        }
                );
    }
    
    private Map<String, Object> getContactDetails(OAuth2User oAuth2User, String resourceName) {
        String accessToken = getAccessToken(oAuth2User);
        String url = "https://people.googleapis.com/v1/" + resourceName + "?personFields=names,emailAddresses,phoneNumbers,metadata";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken); 
        HttpEntity<String> entity = new HttpEntity<>(headers);
    
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        Map<String, Object> body = response.getBody();
    
        System.out.println("Full contact response: " + body);
        return body != null ? body : Map.of();
    }

    public void deleteContact(OAuth2User oAuth2User, String resourceName) {
        System.out.println("Deleting contact with resourceName: " + resourceName);
        String accessToken = getAccessToken(oAuth2User);
        String url = "https://people.googleapis.com/v1/" + resourceName + ":deleteContact";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        new RestTemplate().exchange(url, HttpMethod.DELETE, entity, String.class);
    }
}