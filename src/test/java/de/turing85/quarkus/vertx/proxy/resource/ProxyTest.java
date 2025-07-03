package de.turing85.quarkus.vertx.proxy.resource;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.test.common.WithTestResource;
import io.restassured.RestAssured;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

@WithTestResource(ProxyPortResource.class)
public abstract class ProxyTest {
  @InjectProxy
  @Nullable
  URI proxyUri;

  @BeforeEach
  final void setupRestAssured() {
    RestAssured.baseURI = Objects.requireNonNull(proxyUri).toString();
  }

  protected URI getProxyUri(String... pathElements) {
    URI uri = getProxyUri();
    for (String pathElement : Optional.ofNullable(pathElements).orElse(new String[0])) {
      uri = uri.resolve(pathElement);
    }
    return uri;
  }

  protected URI getProxyUri() {
    return Objects.requireNonNull(proxyUri);
  }

}
