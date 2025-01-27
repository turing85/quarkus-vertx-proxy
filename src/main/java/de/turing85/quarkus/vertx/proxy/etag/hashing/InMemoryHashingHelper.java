package de.turing85.quarkus.vertx.proxy.etag.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.httpproxy.Body;

public class InMemoryHashingHelper implements HashingHelper {
  private final MessageDigest digest;
  private final Buffer buffer;

  private InMemoryHashingHelper(MessageDigest digest, Buffer buffer) {
    this.digest = digest;
    this.buffer = buffer;
  }

  public static Future<InMemoryHashingHelper> of(String algorithm) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      return Future.failedFuture(e);
    }
    return Future.succeededFuture(new InMemoryHashingHelper(digest, Buffer.buffer()));
  }

  @Override
  public Body body() {
    return Body.body(buffer);
  }

  @Override
  public byte[] digest() {
    return digest.digest();
  }

  @Override
  public void handleBuffer(Buffer data, ReadStream<Buffer> bodyStream) {
    digest.update(data.getBytes());
    buffer.appendBuffer(data);
  }
}
