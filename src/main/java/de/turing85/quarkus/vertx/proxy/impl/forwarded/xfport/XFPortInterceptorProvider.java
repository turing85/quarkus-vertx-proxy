package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfport;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class XFPortInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(final Vertx vertx) {
    return new XFPortInterceptor();
  }
}
