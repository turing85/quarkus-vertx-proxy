package de.turing85.quarkus.vertx.proxy.impl.etag;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.DatatypeConverter;

import de.turing85.quarkus.vertx.proxy.impl.etag.hashing.HashingHelper;
import de.turing85.quarkus.vertx.proxy.impl.etag.hashing.InMemoryHashingHelper;
import de.turing85.quarkus.vertx.proxy.impl.etag.hashing.TempFileHashingHelper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.streams.ReadStream;
import io.vertx.httpproxy.Body;
import io.vertx.httpproxy.ProxyContext;
import io.vertx.httpproxy.ProxyInterceptor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static java.util.function.Predicate.not;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class ETagInterceptor implements ProxyInterceptor {
  private static final Set<String> CAN_RETURN_NOT_MODIFIED = Set.of(HttpMethod.GET, HttpMethod.PUT);
  private static final int ONE_KILOBYTE = 1024;

  private final Vertx vertx;

  @Override
  public Future<Void> handleProxyResponse(final ProxyContext context) {
    // @formatter:off
    return Optional.ofNullable(context.response().etag())
        .map(eTag -> handleETag(context, eTag))
        .orElseGet(() -> generateAndHandleETag(context));
    // @formatter:on
  }

  private static Future<Void> handleETag(final ProxyContext context, final String eTag) {
    if (shouldReturnNotModified(context, eTag)) {
      setReturnAsNotModified(context);
    }
    return context.sendResponse();
  }

  private Future<Void> generateAndHandleETag(final ProxyContext context) {
    final long length = context.response().getBody().length();
    final ReadStream<Buffer> bodyStream = context.response().getBody().stream().pause();
    // @formatter:off
    return getHelper(length).compose(helper -> helper
        .process(bodyStream)
        .compose(unused-> handleETagFromHelper(context, helper))
        .onComplete(unused -> helper.close()));
    // @formatter:on
  }

  private Future<? extends HashingHelper> getHelper(final long length) {
    final String algorithm = "MD5";
    if (length < ONE_KILOBYTE) {
      return InMemoryHashingHelper.of(algorithm);
    } else {
      return TempFileHashingHelper.of(algorithm, vertx);
    }
  }

  private Future<Void> handleETagFromHelper(final ProxyContext context,
      final HashingHelper helper) {
    final String eTag = "\"%s\""
        .formatted(DatatypeConverter.printHexBinary(helper.digest()).toLowerCase(Locale.ROOT));
    context.response().putHeader(HttpHeaders.ETAG, eTag);
    if (shouldReturnNotModified(context, eTag)) {
      setReturnAsNotModified(context);
    } else {
      context.response().setBody(helper.body());
    }
    return context.sendResponse();
  }

  private static boolean shouldReturnNotModified(final ProxyContext context, final String eTag) {
    return CAN_RETURN_NOT_MODIFIED.contains(context.request().getMethod().name())
        && extractIfNoneMatchHeader(context).contains(eTag);
  }

  private static Set<String> extractIfNoneMatchHeader(final ProxyContext context) {
    // @formatter:off
    return Arrays
        .stream(Optional.ofNullable(context.request().headers().get(HttpHeaders.IF_NONE_MATCH))
            .orElse("").split(","))
        .map(String::trim)
        .filter(not(String::isBlank))
        .collect(Collectors.toSet());
    // @formatter:on
  }

  private static void setReturnAsNotModified(final ProxyContext context) {
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
