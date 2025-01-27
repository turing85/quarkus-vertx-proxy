package de.turing85.quarkus.vertx.proxy.resource;

import java.util.Objects;

import io.quarkus.test.common.WithTestResource;
import io.restassured.RestAssured;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;

@WithTestResource(ProxyPortResource.class)
public abstract class ProxyTest {
  @InjectProxyUrl
  @Nullable
  String proxyUrl;

  @BeforeEach
  final void setupRestAssured() {
    RestAssured.baseURI = Objects.requireNonNull(proxyUrl);
  }
}
