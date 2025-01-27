package de.turing85.quarkus.vertx.proxy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.DatatypeConverter;

import de.turing85.quarkus.vertx.proxy.resource.ProxyTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;

@QuarkusTest
class EndpointTest extends ProxyTest {

  @Test
  void get() throws NoSuchAlgorithmException {
    // @formatter:off
    RestAssured
        .when().get("/hello")

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, eTagFor("Hello"))
            .body(is("Hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETag() throws NoSuchAlgorithmException {
    final String helloETag = eTagFor("Hello");
    // @formatter:off
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of(eTagFor("otherOne"), helloETag, eTagFor("otherTwo"))))

        .when().get("/hello")

        .then()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .contentType(is(emptyString()))
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "0")
            .header(HttpHeaders.ETAG, helloETag)
            .body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETag() throws NoSuchAlgorithmException {
    // @formatter:off
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of(eTagFor("otherOne"), eTagFor("otherTwo"))))

        .when().get("/hello")

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, eTagFor("Hello"))
            .body(is("Hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETagFromBackend() throws NoSuchAlgorithmException {
    // @formatter:off
    String eTag = "\"hello\"";
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of(eTagFor("otherOne"), eTag, eTagFor("otherTwo"))))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get("/hello")

        .then()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .contentType(is(emptyString()))
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "0")
            .header(HttpHeaders.ETAG, eTag)
            .body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETagFromBackend() throws NoSuchAlgorithmException {
    // @formatter:off
    String eTag = "\"hello\"";
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of(eTagFor("otherOne"), eTagFor("otherTwo"))))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get("/hello")

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, eTag)
            .body(is("Hello"));
    // @formatter:on
  }

  private static String eTagFor(String value) throws NoSuchAlgorithmException {
    return "\"%s\"".formatted(DatatypeConverter
        .printHexBinary(
            MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)))
        .toLowerCase());
  }

}
