package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfp;

import java.net.URI;
import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFPInterceptor implements ProxyInterceptor {
  public static final String HEADER_XFP = "X-Forwarded-Proto";

  @Override
  public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfpHeader = headers.get(HEADER_XFP);
    final String updatedXfpHeader;
    if (Optional.ofNullable(xfpHeader).orElse("").isEmpty()) {
      updatedXfpHeader = URI.create(context.request().absoluteURI()).getScheme();
      headers.add(HEADER_XFP, updatedXfpHeader);
    } else {
      updatedXfpHeader = xfpHeader;
    }
    context.set(HEADER_XFP, updatedXfpHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_XFP)).orElse("").isEmpty()) {
      headers.add(HEADER_XFP, context.get(HEADER_XFP, String.class));
    }
    return context.sendResponse();
  }
}
