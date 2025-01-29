package de.turing85.quarkus.vertx.proxy;

import java.util.List;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.vertx.core.AbstractVerticle;
import io.vertx.httpproxy.HttpProxy;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class ProxyVerticle extends AbstractVerticle {
  private final int proxyHttpPort;
  private final int httpPort;
  private final List<ProxyInterceptorProvider> prioritySortedProviders;

  @Override
  public void start() {
    // @formatter:off
    final HttpProxy proxy = HttpProxy
        .reverseProxy(vertx.createHttpClient())
        .origin(httpPort, "localhost");
    for (ProxyInterceptorProvider provider : prioritySortedProviders) {
      proxy.addInterceptor(provider.createInterceptor(vertx));
    }
    vertx.createHttpServer().requestHandler(proxy).listen(proxyHttpPort)
        .onFailure(cause -> {
          Log.errorf("Failed to start proxy.", cause);
          Quarkus.asyncExit(1);
        });
    // @formatter:on
  }
}
