package de.turing85.quarkus.vertx.filter.etag.proxy;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;

class HashingHelper {
  private final MessageDigest digest;
  private final Vertx vertx;
  private final String path;
  private final AsyncFile file;

  private HashingHelper(MessageDigest digest, Vertx vertx, String path, AsyncFile file) {
    this.digest = digest;
    this.vertx = vertx;
    this.path = path;
    this.file = file;
  }

  static Future<HashingHelper> of(String algorithm, Vertx vertx) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      return Future.failedFuture(e);
    }

    // @formatter:off
    return vertx.fileSystem()
        .createTempFile("vertx-etag-hash-", ".tmp")
        .compose(path -> vertx
            .fileSystem()
            .open(path, new OpenOptions())
            .map(asyncFile -> new HashingHelper(digest, vertx, path, asyncFile)));
    // @formatter:on
  }

  AsyncFile file() {
    return file;
  }

  byte[] digest() {
    return digest.digest();
  }

  Future<Void> process(ReadStream<Buffer> bodyStream) {
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

  private void handleBuffer(Buffer data, ReadStream<Buffer> bodyStream) {
    digest.update(data.getBytes());
    file.write(data);
    if (file.writeQueueFull()) {
      bodyStream.pause();
      file.drainHandler(v -> bodyStream.resume());
    }
  }

  void close() {
    file.close().andThen(v -> vertx.fileSystem().delete(path));
  }
}
