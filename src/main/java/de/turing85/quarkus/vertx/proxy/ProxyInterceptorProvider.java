package de.turing85.quarkus.vertx.proxy;

import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

public interface ProxyInterceptorProvider {
  int DEFAULT_PRIORITY = 5000;

  ProxyInterceptor createInterceptor(Vertx vertx);
}
