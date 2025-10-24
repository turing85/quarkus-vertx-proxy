package de.turing85.quarkus.vertx.proxy.impl.etag;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

@Priority(ProxyInterceptorProvider.DEFAULT_PRIORITY + 1)
@ApplicationScoped
class ETagInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(final Vertx vertx) {
    return new ETagInterceptor(vertx);
  }
}
