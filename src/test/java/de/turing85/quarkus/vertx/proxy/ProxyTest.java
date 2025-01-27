package de.turing85.quarkus.vertx.proxy;

import de.turing85.quarkus.vertx.proxy.resource.ProxyPortResource;
import io.quarkus.test.common.WithTestResource;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;

@WithTestResource(ProxyPortResource.class)
public abstract class ProxyTest {
  @BeforeEach
  final void restAssuredSetup() {
    RestAssured.baseURI = ProxyPortResource.proxyUrl();
  }
}
