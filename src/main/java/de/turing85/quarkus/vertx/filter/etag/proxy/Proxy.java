package de.turing85.quarkus.vertx.filter.etag.proxy;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.httpproxy.HttpProxy;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class Proxy {
  private final Vertx vertx;
  private final int port;

  @Inject
  public Proxy(@SuppressWarnings("CdiInjectionPointsInspection") Vertx vertx,
      @ConfigProperty(name = "quarkus.http.port") int port) {
    this.vertx = vertx;
    this.port = port;
  }

  void observe(@Observes StartupEvent event) {
    HttpClient proxyClient = vertx.createHttpClient();
    HttpProxy proxy = HttpProxy.reverseProxy(proxyClient);
    proxy.origin(port, "localhost");
    proxy.addInterceptor(new ETagInterceptor(vertx));

    vertx.createHttpServer().requestHandler(proxy).listen(8888);
    Log.infof("vert.x proxy started on port %d", 8888);
  }
}
