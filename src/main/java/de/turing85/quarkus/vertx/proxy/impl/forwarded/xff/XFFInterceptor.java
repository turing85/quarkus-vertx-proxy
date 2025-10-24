package de.turing85.quarkus.vertx.proxy.impl.forwarded.xff;

import java.util.Optional;

import de.turing85.quarkus.vertx.proxy.config.ProxyConfig;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFFInterceptor implements ProxyInterceptor {
  public static final String HEADER_NAME = "X-Forwarded-For";

  private final String proxyHost;

  XFFInterceptor(ProxyConfig proxyConfig) {
    this.proxyHost = proxyConfig.hostName();
  }

  @Override
  public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xffHeader = headers.get(HEADER_NAME);
    final String updatedXffHeader;
    if (Optional.ofNullable(xffHeader).orElse("").isEmpty()) {
      final String client = context.request().proxiedRequest().remoteAddress().host();
      updatedXffHeader = String.join(", ", client, proxyHost);
    } else {
      updatedXffHeader = String.join(", ", xffHeader, proxyHost);
    }
    context.set(HEADER_NAME, updatedXffHeader);
    headers.set(HEADER_NAME, updatedXffHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_NAME)).orElse("").isEmpty()) {
      headers.set(HEADER_NAME, context.get(HEADER_NAME, String.class));
    }
    return context.sendResponse();
  }
}
