package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfh;

import java.net.URI;
import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFHInterceptor implements ProxyInterceptor {
  public static final String HEADER_XFH = "X-Forwarded-Host";

  @Override
  public Future<ProxyResponse> handleProxyRequest(final ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfhHeader = headers.get(HEADER_XFH);
    final String updatedXfhHeader;
    if (Optional.ofNullable(xfhHeader).orElse("").isEmpty()) {
      updatedXfhHeader = URI.create(context.request().absoluteURI()).getAuthority();
      headers.add(HEADER_XFH, updatedXfhHeader);
    } else {
      updatedXfhHeader = xfhHeader;
    }
    context.set(HEADER_XFH, updatedXfhHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_XFH)).orElse("").isEmpty()) {
      headers.add(HEADER_XFH, context.get(HEADER_XFH, String.class));
    }
    return context.sendResponse();
  }
}
