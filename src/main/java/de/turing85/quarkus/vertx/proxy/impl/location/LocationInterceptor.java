package de.turing85.quarkus.vertx.proxy.impl.location;

import java.net.URI;
import java.util.Optional;

import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;

import static java.util.function.Predicate.not;

class LocationInterceptor implements ProxyInterceptor {
  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    // @formatter:off
    Optional.ofNullable(context.response().headers().get(HttpHeaders.LOCATION))
        .map(URI::create)
        .map(originalLocation -> rewriteLocation(originalLocation, context ))
        .ifPresent(newLocation ->
            context.response().headers().set(HttpHeaders.LOCATION, newLocation));
    // @formatter:on
    return context.sendResponse();
  }

  private static String rewriteLocation(final URI originalLocation, final ProxyContext context) {
    final URI requestURI = URI.create(context.request().absoluteURI());
    final String schema = requestURI.getScheme();
    final String host = requestURI.getHost();
    final int port = requestURI.getPort();
    // @formatter:off
    return Optional.ofNullable(originalLocation.getPath())
        .map(path -> path.replaceAll("^/", ""))
        .filter(not(String::isEmpty))
        .map(path -> "%s://%s:%d/%s".formatted(schema, host, port, path))
        .orElseGet(() -> "%s://%s:%d".formatted(schema, host, port));
    // @formatter:on
  }
}
