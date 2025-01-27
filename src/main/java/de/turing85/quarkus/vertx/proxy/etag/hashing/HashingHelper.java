package de.turing85.quarkus.vertx.proxy.etag.hashing;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.httpproxy.Body;

public interface HashingHelper {
  byte[] digest();

  Body body();

  void handleBuffer(Buffer data, ReadStream<Buffer> bodyStream);

  default Future<Void> process(ReadStream<Buffer> bodyStream) {
    final Promise<Void> promise = Promise.promise();
    // @formatter:off
    bodyStream
        .exceptionHandler(promise::tryFail)
        .endHandler(promise::tryComplete)
        .handler(data -> handleBuffer(data, bodyStream));
    // @formatter:on
    bodyStream.resume();
    return promise.future();
  }

  default void close() {}
}
