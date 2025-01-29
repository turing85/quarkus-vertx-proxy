package de.turing85.quarkus.vertx.proxy.impl.forwarded.xff;

import jakarta.enterprise.context.ApplicationScoped;

import de.turing85.quarkus.vertx.proxy.ProxyInterceptorProvider;
import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.vertx.core.Vertx;
import io.vertx.httpproxy.ProxyInterceptor;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class XFFInterceptorProvider implements ProxyInterceptorProvider {
  private final ProxyConfig proxyConfig;

  @Override
  public ProxyInterceptor createInterceptor(Vertx vertx) {
    return new XFFInterceptor(proxyConfig);
  }
}
