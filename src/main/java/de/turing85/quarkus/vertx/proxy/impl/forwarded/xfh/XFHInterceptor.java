package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfh;

import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFHInterceptor implements ProxyInterceptor {
  public static final String HEADER_NAME = "X-Forwarded-Host";

  @Override
  public Future<ProxyResponse> handleProxyRequest(final ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfhHeader = headers.get(HEADER_NAME);
    final String updatedXfhHeader;
    if (Optional.ofNullable(xfhHeader).orElse("").isEmpty()) {
      updatedXfhHeader = context.request().proxiedRequest().authority().host();
      headers.add(HEADER_NAME, updatedXfhHeader);
    } else {
      updatedXfhHeader = xfhHeader;
    }
    context.set(HEADER_NAME, updatedXfhHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_NAME)).orElse("").isEmpty()) {
      headers.add(HEADER_NAME, context.get(HEADER_NAME, String.class));
    }
    return context.sendResponse();
  }
}
