package de.turing85.quarkus.vertx.proxy.impl.location;

import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

@ApplicationScoped
public class LocationInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(final Vertx vertx) {
    return new LocationInterceptor();
  }
}
