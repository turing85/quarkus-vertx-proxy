package de.turing85.quarkus.vertx.filter.etag.proxy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.quarkus.logging.Log;
import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;

class HashingWriteStream implements WriteStream<Buffer>, AutoCloseable {
  private final MessageDigest digest;
  private final Path tempFile;
  private final FileReadStream fileReadStream;

  public HashingWriteStream(String algorithm, Vertx vertx)
      throws NoSuchAlgorithmException, IOException {
    this.digest = MessageDigest.getInstance(algorithm);
    this.tempFile = Files.createTempFile("vertx-etag-hash-", ".tmp").toAbsolutePath();
    fileReadStream = new FileReadStream(vertx, tempFile);
  }

  public byte[] digest() {
    return digest.digest();
  }

  public FileReadStream content() {
    return fileReadStream;
  }

  @Override
  public WriteStream<Buffer> exceptionHandler(Handler<Throwable> handler) {
    return this;
  }

  @Override
  public Future<Void> write(Buffer data) {
    try {
      write(data, null);
      return Future.succeededFuture();
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  @Override
  public void write(Buffer data, Handler<AsyncResult<Void>> handler) {
    digest.update(data.getBytes());
    try {
      Files.write(tempFile, data.getBytes(), StandardOpenOption.APPEND);
    } catch (IOException e) {
      Log.errorf("unable to write to %s", tempFile, e);
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void end(Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  @Override
  public WriteStream<Buffer> setWriteQueueMaxSize(int maxSize) {
    return this;
  }

  @Override
  public boolean writeQueueFull() {
    return false;
  }

  @Override
  public WriteStream<Buffer> drainHandler(@Nullable Handler<Void> handler) {
    return this;
  }

  @Override
  public void close() {
    fileReadStream.close();
    try {
      Files.delete(tempFile);
    } catch (IOException e) {
      Log.errorf("failed to delete temporary file %s", tempFile, e);
    }
  }
}
