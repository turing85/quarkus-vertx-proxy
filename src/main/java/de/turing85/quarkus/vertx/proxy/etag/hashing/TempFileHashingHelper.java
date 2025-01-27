package de.turing85.quarkus.vertx.proxy.etag.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import io.vertx.httpproxy.Body;

public class TempFileHashingHelper implements HashingHelper {
  private final MessageDigest digest;
  private final Vertx vertx;
  private final String path;
  private final AsyncFile file;

  private TempFileHashingHelper(MessageDigest digest, Vertx vertx, String path, AsyncFile file) {
    this.digest = digest;
    this.vertx = vertx;
    this.path = path;
    this.file = file;
  }

  public static Future<TempFileHashingHelper> of(String algorithm, Vertx vertx) {
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
            .map(asyncFile -> new TempFileHashingHelper(digest, vertx, path, asyncFile)));
    // @formatter:on
  }

  @Override
  public Body body() {
    return Body.body(file);
  }

  @Override
  public byte[] digest() {
    return digest.digest();
  }

  @Override
  public void handleBuffer(Buffer data, ReadStream<Buffer> bodyStream) {
    digest.update(data.getBytes());
    file.write(data);
    if (file.writeQueueFull()) {
      bodyStream.pause();
      file.drainHandler(v -> bodyStream.resume());
    }
  }

  @Override
  public void close() {
    file.close().andThen(v -> vertx.fileSystem().delete(path));
  }
}
