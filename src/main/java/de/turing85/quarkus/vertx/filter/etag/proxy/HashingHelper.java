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

  static Future<HashingHelper> create(String algorithm, Vertx vertx) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      return Future.failedFuture(e);
    }
    return vertx.fileSystem().createTempFile("vertx-etag-hash-", ".tmp")
        .compose(path -> vertx.fileSystem().open(path, new OpenOptions().setDeleteOnClose(true))
            .map(asyncFile -> new HashingHelper(digest, vertx, path, asyncFile)));
  }

  AsyncFile file() {
    return file;
  }

  byte[] digest() {
    return digest.digest();
  }

  Future<Void> process(ReadStream<Buffer> bodyStream) {
    Promise<Void> promise = Promise.promise();

    // @formatter:off
        bodyStream
                .exceptionHandler(promise::tryFail)
                .endHandler(promise::tryComplete)
                .handler(data -> {
                    digest.update(data.getBytes());
                    file.write(data);
                    if (file.writeQueueFull()) {
                        bodyStream.pause();
                        file.drainHandler(v -> bodyStream.resume());
                    }
                });
        // @formatter:on

    bodyStream.resume();

    return promise.future();
  }

  void close() {
    file.close().andThen(v -> vertx.fileSystem().delete(path));
  }
}
