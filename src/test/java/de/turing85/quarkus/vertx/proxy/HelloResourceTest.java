package de.turing85.quarkus.vertx.proxy;

import java.util.List;

import de.turing85.quarkus.vertx.proxy.impl.forwarded.xfport.XFPortInterceptor;
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

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.endsWith;
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
    final String helloETag = postEntityAndGetETag();
    // @formatter:off
    RestAssured
        .when().get()

        .then().assertThat()
            .statusCode(Response.Status.OK.getStatusCode())
            .and().contentType(MediaType.TEXT_PLAIN)
            .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
            .and().header(HttpHeaders.CONTENT_LENGTH, "5")
            .and().header(HttpHeaders.ETAG, helloETag)
            .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
            .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
            .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
            .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
            .and().body(is("hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETag() {
    final String helloETag = postEntityAndGetETag();
    // @formatter:off
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of("\"otherOne\"", helloETag, "\"otherTwo\"")))

        .when().get()

        .then().assertThat()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .and().contentType(emptyString())
            .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
            .and().header(HttpHeaders.CONTENT_LENGTH, "0")
            .and().header(HttpHeaders.ETAG, helloETag)
            .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
            .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
            .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
            .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
            .and().body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETag() {
    final String helloETag = postEntityAndGetETag();
    // @formatter:off
    RestAssured
        .given().header(
            HttpHeaders.IF_NONE_MATCH,
            String.join(",",List.of("\"otherOne\"", "\"otherTwo\"")))

        .when().get()

        .then().assertThat()
            .statusCode(Response.Status.OK.getStatusCode())
            .and().contentType(MediaType.TEXT_PLAIN)
            .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
            .and().header(HttpHeaders.CONTENT_LENGTH, "5")
            .and().header(HttpHeaders.ETAG, helloETag)
            .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
            .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
            .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
            .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
            .and().body(is("hello"));
    // @formatter:on
  }

  @Test
  void getWithMatchingETagFromBackend() {
    final String eTag = "\"hello\"";
    // @formatter:off
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of("\"otherOne\"", eTag, "\"otherTwo\"")))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get()

        .then().assertThat()
            .statusCode(Response.Status.NOT_MODIFIED.getStatusCode())
            .and().contentType(emptyString())
            .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
            .and().header(HttpHeaders.CONTENT_LENGTH, "0")
            .and().header(HttpHeaders.ETAG, eTag)
            .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
            .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
            .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
            .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
            .and().body(is(emptyString()));
    // @formatter:on
  }

  @Test
  void getWithMismatchingETagFromBackend() {
    final String eTag = "\"hello\"";
    // @formatter:off
    RestAssured
        .given()
            .header(
                HttpHeaders.IF_NONE_MATCH,
                String.join(",",List.of("\"otherOne\"", "\"otherTwo\"")))
            .queryParam("eTag", eTag.replace("\"", ""))

        .when().get()

        .then().assertThat()
            .statusCode(Response.Status.OK.getStatusCode())
            .and().contentType(MediaType.TEXT_PLAIN)
            .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
            .and().header(HttpHeaders.CONTENT_LENGTH, "5")
            .and().header(HttpHeaders.ETAG, eTag)
            .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
            .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
            .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
            .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
            .and().body(is("hello"));
    // @formatter:on
  }

  private String postEntityAndGetETag() {
    // @formatter:off
    return RestAssured
        .when().post()
        .then()
            .assertThat()
                .statusCode(Response.Status.CREATED.getStatusCode())
                .and().contentType(MediaType.TEXT_PLAIN)
                .and().header(HttpHeaders.LOCATION, getProxyUri(HelloResource.PATH).toString())
                .and().header(HttpHeaders.CONTENT_ENCODING, nullValue())
                .and().header(HttpHeaders.CONTENT_LENGTH, "5")
                .and().header(HttpHeaders.ETAG, notNullValue())
                .and().header(HttpHeaders.ETAG, not(emptyString()))
                .and().header(XFPInterceptor.HEADER_XFP, getProxyUri().getScheme())
                .and().header(XFHInterceptor.HEADER_XFH, getProxyUri().getHost())
                .and().header(XFPortInterceptor.HEADER_XFPORT, String.valueOf(getProxyUri().getPort()))
                .and().header(XFFInterceptor.HEADER_XFF, endsWith(", quarkus-vertx-proxy"))
                .and().body(is("hello"))
            .extract().header(HttpHeaders.ETAG);
    // @formatter:on
  }
}
