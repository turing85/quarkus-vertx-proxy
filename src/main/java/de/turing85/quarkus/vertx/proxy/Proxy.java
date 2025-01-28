package de.turing85.quarkus.vertx.proxy;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import de.turing85.quarkus.vertx.proxy.etag.ETagInterceptor;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.quarkus.vertx.http.HttpServerStart;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.httpproxy.HttpProxy;
import org.eclipse.microprofile.config.ConfigProvider;

@Singleton
public class Proxy {
  private final Vertx vertx;
  private final int proxyHttpPort;
  private final LaunchMode launchMode;

  @Inject
  public Proxy(@SuppressWarnings("CdiInjectionPointsInspection") final Vertx vertx,
      final ProxyConfig proxyConfig, final LaunchMode launchMode) {
    this.vertx = vertx;
    this.proxyHttpPort = proxyConfig.httpPort();
    this.launchMode = launchMode;
  }

  void observe(@ObservesAsync final HttpServerStart ignored) {
    final HttpClient proxyClient = vertx.createHttpClient();
    final HttpProxy proxy = HttpProxy.reverseProxy(proxyClient);
    final String portPropertyName = launchMode == LaunchMode.TEST ? "test-port" : "port";
    final int httpPort = ConfigProvider.getConfig()
        .getValue("quarkus.http.%s".formatted(portPropertyName), Integer.class);
    proxy.origin(httpPort, "localhost");
    proxy.addInterceptor(new ETagInterceptor(vertx));
    // @formatter:off
    vertx.createHttpServer().requestHandler(proxy).listen(proxyHttpPort)
        .onSuccess(unused -> Log.infof(
            "vert.x proxy started on  http://localhost:%d, forwarding to http://localhost:%d",
            proxyHttpPort, httpPort))
        .onFailure(cause -> {
          Log.errorf("Failed to start proxy.", cause);
          Quarkus.asyncExit(1);
        });
    // @formatter:on
  }
}
