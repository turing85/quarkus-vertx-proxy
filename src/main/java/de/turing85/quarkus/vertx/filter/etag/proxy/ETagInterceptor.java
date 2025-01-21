package de.turing85.quarkus.vertx.filter.etag.proxy;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.DatatypeConverter;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.ReadStream;
import io.vertx.httpproxy.Body;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;

import static java.util.function.Predicate.not;

class ETagInterceptor implements ProxyInterceptor {
  private static final Set<String> ALLOWED_METHODS = Set.of(HttpMethod.GET, HttpMethod.PUT);

  private final Vertx vertx;

  public ETagInterceptor(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public Future<Void> handleProxyResponse(ProxyContext context) {
    if (!ALLOWED_METHODS.contains(context.request().getMethod().name())) {
      return context.sendResponse();
    }
    // @formatter:off
    return Optional.ofNullable(context.response().etag())
        .map(eTag -> handleETag(context, eTag))
        .orElseGet(() -> generateAndHandleETag(context));
    // @formatter:on
  }

  private static Future<Void> handleETag(ProxyContext context, String eTag) {
    if (extractIfNoneMatchHeader(context).contains(eTag)) {
      setReturnAsNotModified(context);
    }
    return context.sendResponse();
  }

  private Future<Void> generateAndHandleETag(ProxyContext context) {
    ReadStream<Buffer> bodyStream = context.response().getBody().stream().pause();
    // @formatter:off
    return HashingHelper.of("MD5", vertx)
        .compose(helper -> helper
            .process(bodyStream)
            .compose(unused-> handleETagFromHelper(context, helper))
            .onComplete(unused -> helper.close()));
    // @formatter:on
  }

  private Future<Void> handleETagFromHelper(ProxyContext context, HashingHelper helper) {
    final String eTag =
        "\"%s\"".formatted(DatatypeConverter.printHexBinary(helper.digest()).toLowerCase());
    context.response().putHeader(HttpHeaders.ETAG, eTag);
    final Set<String> ifNoneMatch = extractIfNoneMatchHeader(context);
    if (ifNoneMatch.contains(eTag)) {
      setReturnAsNotModified(context);
    } else {
      context.response().setBody(Body.body(helper.file()));
    }
    return context.sendResponse();
  }

  private static Set<String> extractIfNoneMatchHeader(ProxyContext context) {
    // @formatter:off
    return Arrays.stream(
            Optional.ofNullable(context.request().headers().get(HttpHeaders.IF_NONE_MATCH))
                .orElse("").split(","))
        .map(String::trim)
        .filter(not(String::isBlank))
        .collect(Collectors.toSet());
    // @formatter:on
  }

  private static void setReturnAsNotModified(ProxyContext context) {
    // @formatter:off
    context.response()
        .setBody(Body.body(Buffer.buffer()))
        .setStatusCode(Response.Status.NOT_MODIFIED.getStatusCode())
        .setStatusMessage(Response.Status.NOT_MODIFIED.getReasonPhrase())
        .headers()
            .remove(HttpHeaders.CONTENT_LENGTH)
            .remove(HttpHeaders.CONTENT_TYPE)
            .remove(HttpHeaders.CONTENT_ENCODING);
    // @formatter:on
  }
}
