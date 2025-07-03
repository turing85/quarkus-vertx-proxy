package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfproto;

import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;

@ApplicationScoped
public class XFProtoInterceptorProvider implements ProxyInterceptorProvider {
  @Override
  public ProxyInterceptor createInterceptor(Vertx vertx) {
    return new XFProtoInterceptor();
  }
}
