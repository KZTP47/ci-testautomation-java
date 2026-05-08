package se.ecutbildning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integrationstester mot https://fakestoreapi.com/.
 *
 * Här är en förklaring på vad jag försökt göra;
 * Testerna gör HTTP-anrop mot API:t.
 * Lokalt ska API:t normalt svara med 200.
 * I GitHub Actions kan API:t istället svara med 403 eftersom Actions kan ses som en bot. (men det stod i kursen att det var ok, så då räknar jag med det)
 */
class FakeStoreApiTest {

    private static final String BASE_URL = "https://fakestoreapi.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getProductsShouldReturnStatusCode200() throws Exception {
        /*
         * Här försökte jag mig på G-kravet:
         * Vi skickar en GET-förfrågan till /products och kontrollerar statuskod 200.
         */
        HttpResponse<String> response = sendGetRequest("/products");

        assertStatusCode200(response);
    }

    @Test
    void productsShouldReturnExpectedAmount() throws Exception {
        /*
         * här försökte jag mig på VG-kravet:
         * products ska returnera en lista med förväntat antal produkter.
         * 
         */
        HttpResponse<String> response = sendGetRequest("/products");
        assertStatusCode200(response);

        JsonNode products = objectMapper.readTree(response.body());

        assertTrue(products.isArray(), "Svaret från /products ska vara en JSON-lista.");
        assertEquals(20, products.size(), "FakeStoreAPI ska returnera 20 produkter.");
    }

    @Test
    void specificProductShouldContainExpectedFields() throws Exception {
        /*
         * också VG-kravet:
         * En specifik produkt ska innehålla fält som title, price och category.
         */
        HttpResponse<String> response = sendGetRequest("/products/1");
        assertStatusCode200(response);

        JsonNode product = objectMapper.readTree(response.body());

        assertTrue(product.has("title"), "Produkten ska ha fältet title.");
        assertTrue(product.has("price"), "Produkten ska ha fältet price.");
        assertTrue(product.has("category"), "Produkten ska ha fältet category.");

        assertEquals("Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops", product.get("title").asText());
        assertEquals(109.95, product.get("price").asDouble(), 0.001);
        assertEquals("men's clothing", product.get("category").asText());
    }

    @Test
    void specificProductIdShouldReturnRightData() throws Exception {
        /*
         * också en del av VG-kravet:
         * Ett specifikt produkt-ID ska returnera rätt data.
         */
        HttpResponse<String> response = sendGetRequest("/products/1");
        assertStatusCode200(response);

        JsonNode product = objectMapper.readTree(response.body());

        assertEquals(1, product.get("id").asInt());
        assertEquals("Fjallraven - Foldsack No. 1 Backpack, Fits 15 Laptops", product.get("title").asText());
        assertEquals("men's clothing", product.get("category").asText());
    }

    private HttpResponse<String> sendGetRequest(String path) throws IOException, InterruptedException {
        /*
         * Den här funktionen skickar ett GET-anrop till API:t.
         * Jag använder en timeout så att testet inte fastnar för alltid om API:t är segt. Vilket nämndes i kursen kan hända
         */
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertStatusCode200(HttpResponse<String> response) {
        /*
         * Uppgiften säger att GitHub Actions kan få 403 från FakeStoreAPI.
         * Därför har jag ett tydligt felmeddelande, så man ser om felet beror på just det.
         */
        assertEquals(
                200,
                response.statusCode(),
                "Förväntade statuskod 200 men fick " + response.statusCode()
                        + ". I GitHub Actions kan detta vara 403 om FakeStoreAPI blockerar körningen."
        );
    }
}
