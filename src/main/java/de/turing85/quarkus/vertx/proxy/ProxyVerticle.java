package de.turing85.quarkus.vertx.proxy;

import de.turing85.quarkus.vertx.proxy.etag.ETagInterceptor;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.Quarkus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClient;
import io.vertx.httpproxy.HttpProxy;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.config.ConfigProvider;

@RequiredArgsConstructor
public class ProxyVerticle extends AbstractVerticle {
  private final int proxyHttpPort;
  private final LaunchMode launchMode;

  @Override
  public void start() {
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
