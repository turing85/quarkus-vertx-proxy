package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfp;

import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

@ApplicationScoped
public class XFPInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(Vertx vertx) {
    return new XFPInterceptor();
  }
}
