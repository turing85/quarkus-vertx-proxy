package de.turing85.quarkus.vertx.proxy;

import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.turing85.quarkus.resource.HelloResource;
import de.turing85.quarkus.vertx.proxy.impl.forwarded.xff.XFFInterceptor;
import de.turing85.quarkus.vertx.proxy.impl.forwarded.xfh.XFHInterceptor;
import de.turing85.quarkus.vertx.proxy.impl.forwarded.xfp.XFPInterceptor;
import de.turing85.quarkus.vertx.proxy.resource.ProxyTest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class HelloResourceTest extends ProxyTest {
  @BeforeEach
  void setup() {
    RestAssured.baseURI = "%s/%s".formatted(RestAssured.baseURI, HelloResource.PATH);
  }

  @Test
  void get() {
    // @formatter:off
    final String helloETag = postEntityAndGetETag();

    RestAssured
        .when().get()

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, helloETag)
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is("hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETag() {
    // @formatter:off
    final String helloETag = postEntityAndGetETag();
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of("\"otherOne\"", helloETag, "\"otherTwo\"")))

        .when().get()

        .then()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .contentType(is(emptyString()))
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "0")
            .header(HttpHeaders.ETAG, helloETag)
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETag() {
    // @formatter:off
    final String helloETag = postEntityAndGetETag();
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of("\"otherOne\"", "\"otherTwo\"")))

        .when().get()

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, helloETag)
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is("hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETagFromBackend() {
    // @formatter:off
    final String eTag = "\"hello\"";
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of("\"otherOne\"", eTag, "\"otherTwo\"")))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get()

        .then()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .contentType(is(emptyString()))
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "0")
            .header(HttpHeaders.ETAG, eTag)
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETagFromBackend() {
    // @formatter:off
    final String eTag = "\"hello\"";
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of("\"otherOne\"", "\"otherTwo\"")))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get()

        .then()
            .statusCode(Response.Status.OK.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, eTag)
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is("hello"));
    // @formatter:on
  }

  private String postEntityAndGetETag() {
    // @formatter:off
    return RestAssured
        .when().post()
        .then()
            .statusCode(Response.Status.CREATED.getStatusCode())
            .contentType(MediaType.TEXT_PLAIN)
            .header(HttpHeaders.LOCATION, getProxyUri(HelloResource.PATH).toString())
            .header(HttpHeaders.CONTENT_ENCODING, is(nullValue()))
            .header(HttpHeaders.CONTENT_LENGTH, "5")
            .header(HttpHeaders.ETAG, is(notNullValue()))
            .header(HttpHeaders.ETAG, is(not(emptyString())))
            .header(XFPInterceptor.HEADER_XFP, is(getProxyUri().getScheme()))
            .header(XFHInterceptor.HEADER_XFH, is(getProxyUri().getAuthority()))
            .header(XFFInterceptor.HEADER_XFF, containsString(", "))
            .body(is("hello"))
            .extract().header(HttpHeaders.ETAG);
    // @formatter:on
  }
}
