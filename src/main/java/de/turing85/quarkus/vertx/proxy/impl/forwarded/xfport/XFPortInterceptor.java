package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfport;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

import java.util.Optional;

public class XFPortInterceptor implements ProxyInterceptor {
  public static final String HEADER_XFPORT = "X-Forwarded-Port";

  @Override
  public Future<ProxyResponse> handleProxyRequest(final ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfhHeader = headers.get(HEADER_XFPORT);
    final String updatedXfhHeader;
    if (Optional.ofNullable(xfhHeader).orElse("").isEmpty()) {
      updatedXfhHeader = String.valueOf(context.request().proxiedRequest().authority().port());
      headers.add(HEADER_XFPORT, updatedXfhHeader);
    } else {
      updatedXfhHeader = xfhHeader;
    }
    context.set(HEADER_XFPORT, updatedXfhHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_XFPORT)).orElse("").isEmpty()) {
      headers.add(HEADER_XFPORT, context.get(HEADER_XFPORT, String.class));
    }
    return context.sendResponse();
  }
}
