package de.turing85.quarkus.vertx.proxy;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import de.turing85.quarkus.vertx.proxy.etag.ETagInterceptor;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
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
  public Proxy(@SuppressWarnings("CdiInjectionPointsInspection") Vertx vertx,
      ProxyConfig proxyConfig, LaunchMode launchMode) {
    this.vertx = vertx;
    this.proxyHttpPort = proxyConfig.httpPort();
    this.launchMode = launchMode;
  }

  void observe(@ObservesAsync HttpServerStart ignored) {
    HttpClient proxyClient = vertx.createHttpClient();
    HttpProxy proxy = HttpProxy.reverseProxy(proxyClient);
    String portPropertyName = launchMode == LaunchMode.TEST ? "test-port" : "port";
    int httpPort = ConfigProvider.getConfig()
        .getValue("quarkus.http.%s".formatted(portPropertyName), Integer.class);
    proxy.origin(httpPort, "localhost");
    proxy.addInterceptor(new ETagInterceptor(vertx));

    vertx.createHttpServer().requestHandler(proxy).listen(proxyHttpPort);
    Log.infof("vert.x proxy started on  http://localhost:%d, forwarding to http://localhost:%d",
        proxyHttpPort, httpPort);
  }
}
