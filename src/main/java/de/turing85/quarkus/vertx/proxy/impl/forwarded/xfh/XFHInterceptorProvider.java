package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfh;

import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

@ApplicationScoped
public class XFHInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(final Vertx vertx) {
    return new XFHInterceptor();
  }
}
