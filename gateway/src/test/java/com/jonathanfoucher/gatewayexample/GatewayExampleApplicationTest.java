package com.jonathanfoucher.gatewayexample;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.Setter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

import java.time.LocalDate;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static org.apache.http.HttpHeaders.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock
@ActiveProfiles("test")
class GatewayExampleApplicationTest {
    @LocalServerPort
    private int port;

    private static final String API_PATH = "/movie-api";
    private static final String GATEWAY_PATH = "/gateway-example";
    private static final String MOVIES_BY_ID_PATH = "/movies/15";
    private static final String MOVIES_PATH = "/movies";
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String API_KEY = "some-api-key";
    private static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String CORRELATION_ID = "some-correlation-id";
    private static final Pattern CORRELATION_ID_REGEX_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final String JWT = "some-jwt";

    private static final Long ID = 15L;
    private static final String TITLE = "Some movie";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2022, 7, 19);

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }

    @BeforeEach
    void init() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @Test
    void findMovieById() throws JsonProcessingException {
        // GIVEN
        MovieDto movie = initMovie();

        stubFor(WireMock.get(API_PATH + MOVIES_BY_ID_PATH)
                .willReturn(ok().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(movie))));

        // WHEN / THEN
        given().header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, JWT)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .when()
                .get(GATEWAY_PATH + API_PATH + MOVIES_BY_ID_PATH)
                .then()
                .statusCode(HTTP_OK)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(Matchers.equalTo(objectMapper.writeValueAsString(movie)));

        verify(WireMock.getRequestedFor(urlEqualTo(API_PATH + MOVIES_BY_ID_PATH))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(API_KEY_HEADER, equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(CORRELATION_ID))
                .withHeader(AUTHORIZATION, absent())
        );
    }

    @Test
    void findMovieByIdWithoutGivenCorrelationId() throws JsonProcessingException {
        // GIVEN
        MovieDto movie = initMovie();

        stubFor(WireMock.get(API_PATH + MOVIES_BY_ID_PATH)
                .willReturn(ok().withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(movie))));

        // WHEN / THEN
        Response response = given().header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, JWT)
                .when()
                .get(GATEWAY_PATH + API_PATH + MOVIES_BY_ID_PATH);

        response.then()
                .statusCode(HTTP_OK)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(CORRELATION_ID_HEADER, Matchers.matchesRegex(CORRELATION_ID_REGEX_PATTERN))
                .body(Matchers.equalTo(objectMapper.writeValueAsString(movie)));

        String generatedCorrelationId = response.getHeader(CORRELATION_ID_HEADER);

        verify(WireMock.getRequestedFor(urlEqualTo(API_PATH + MOVIES_BY_ID_PATH))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(API_KEY_HEADER, equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(generatedCorrelationId))
                .withHeader(AUTHORIZATION, absent())
        );
    }

    @Test
    void findMovieByIdWithNotFoundResponse() {
        // GIVEN
        stubFor(WireMock.get(API_PATH + MOVIES_BY_ID_PATH)
                .willReturn(notFound()));

        // WHEN / THEN
        given().header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, JWT)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .when()
                .get(GATEWAY_PATH + API_PATH + MOVIES_BY_ID_PATH)
                .then()
                .statusCode(HTTP_NOT_FOUND)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(Matchers.emptyString());

        verify(WireMock.getRequestedFor(urlEqualTo(API_PATH + MOVIES_BY_ID_PATH))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(API_KEY_HEADER, equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(CORRELATION_ID))
                .withHeader(AUTHORIZATION, absent())
        );
    }

    @Test
    void saveMovie() throws JsonProcessingException {
        // GIVEN
        MovieDto movie = initMovie();

        stubFor(WireMock.post(API_PATH + MOVIES_PATH).willReturn(ok()));

        // WHEN / THEN
        given().header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, JWT)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(objectMapper.writeValueAsString(movie))
                .when()
                .post(GATEWAY_PATH + API_PATH + MOVIES_PATH)
                .then()
                .statusCode(HTTP_OK)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(Matchers.emptyString());

        verify(WireMock.postRequestedFor(urlEqualTo(API_PATH + MOVIES_PATH))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                .withHeader(API_KEY_HEADER, equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(CORRELATION_ID))
                .withHeader(AUTHORIZATION, absent())
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(movie)))
        );
    }

    @Test
    void saveMovieWithAServerError() throws JsonProcessingException {
        // GIVEN
        MovieDto movie = initMovie();

        stubFor(WireMock.post(API_PATH + MOVIES_PATH).willReturn(serverError()));

        // WHEN / THEN
        given().header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, JWT)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(objectMapper.writeValueAsString(movie))
                .when()
                .post(GATEWAY_PATH + API_PATH + MOVIES_PATH)
                .then()
                .statusCode(HTTP_INTERNAL_ERROR)
                .header(CORRELATION_ID_HEADER, CORRELATION_ID)
                .body(Matchers.emptyString());

        verify(WireMock.postRequestedFor(urlEqualTo(API_PATH + MOVIES_PATH))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE + ";charset=UTF-8"))
                .withHeader(API_KEY_HEADER, equalTo(API_KEY))
                .withHeader(CORRELATION_ID_HEADER, equalTo(CORRELATION_ID))
                .withHeader(AUTHORIZATION, absent())
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(movie)))
        );
    }

    @Getter
    @Setter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class MovieDto {
        private Long id;
        private String title;
        private LocalDate releaseDate;
    }

    private MovieDto initMovie() {
        MovieDto movie = new MovieDto();
        movie.setId(ID);
        movie.setTitle(TITLE);
        movie.setReleaseDate(RELEASE_DATE);
        return movie;
    }
}
