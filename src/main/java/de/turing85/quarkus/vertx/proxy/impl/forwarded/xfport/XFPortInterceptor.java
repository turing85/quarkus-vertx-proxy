package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfport;

import java.net.URI;
import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFPortInterceptor implements ProxyInterceptor {
  public static final String HEADER_XF_PORT = "X-Forwarded-Port";

  @Override
  public Future<ProxyResponse> handleProxyRequest(final ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfPortHeader = headers.get(HEADER_XF_PORT);
    final String updatedXfPortHeaders;
    if (Optional.ofNullable(xfPortHeader).orElse("").isEmpty()) {
      int port = URI.create(context.request().absoluteURI()).getPort();
      updatedXfPortHeaders = Integer.toString(port);
      if (port != -1) {
        headers.add(HEADER_XF_PORT, updatedXfPortHeaders);
      }
    } else {
      updatedXfPortHeaders = xfPortHeader;
    }
    context.set(HEADER_XF_PORT, updatedXfPortHeaders);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_XF_PORT)).orElse("").isEmpty()) {
      headers.add(HEADER_XF_PORT, context.get(HEADER_XF_PORT, String.class));
    }
    return context.sendResponse();
  }
}
