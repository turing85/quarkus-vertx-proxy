package de.turing85.quarkus.vertx.proxy.impl.forwarded.xfproto;

import java.net.URI;
import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import io.vertx.httpproxy.ProxyResponse;

public class XFProtoInterceptor implements ProxyInterceptor {
  public static final String HEADER_XF_PROTO = "X-Forwarded-Proto";

  @Override
  public Future<ProxyResponse> handleProxyRequest(ProxyContext context) {
    final MultiMap headers = context.request().headers();
    final String xfProtoHeader = headers.get(HEADER_XF_PROTO);
    final String updatedXfProtoHeader;
    if (Optional.ofNullable(xfProtoHeader).orElse("").isEmpty()) {
      updatedXfProtoHeader = URI.create(context.request().absoluteURI()).getScheme();
      headers.add(HEADER_XF_PROTO, updatedXfProtoHeader);
    } else {
      updatedXfProtoHeader = xfProtoHeader;
    }
    context.set(HEADER_XF_PROTO, updatedXfProtoHeader);
    return context.sendRequest();
  }

  @Override
  public Future<Void> handleProxyResponse(ProxyContext context) {
    final MultiMap headers = context.response().headers();
    if (Optional.ofNullable(headers.get(HEADER_XF_PROTO)).orElse("").isEmpty()) {
      headers.add(HEADER_XF_PROTO, context.get(HEADER_XF_PROTO, String.class));
    }
    return context.sendResponse();
  }
}
