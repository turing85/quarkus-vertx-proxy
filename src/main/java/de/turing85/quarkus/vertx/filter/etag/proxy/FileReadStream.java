package de.turing85.quarkus.vertx.filter.etag.proxy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

import io.quarkus.logging.Log;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.impl.InboundBuffer;

public class FileReadStream implements ReadStream<Buffer> {
  public static final int DEFAULT_READ_BUFFER_SIZE = 8192;

  private final ReadableByteChannel ch;
  private final Vertx vertx;

  private boolean closed;
  private boolean readInProgress;

  private Handler<Buffer> dataHandler;
  private Handler<Void> endHandler;
  private Handler<Throwable> exceptionHandler;
  private final InboundBuffer<Buffer> queue;

  private long readPos;

  public FileReadStream(Vertx vertx, Path file) throws FileNotFoundException {
    this.vertx = vertx;
    this.ch = Channels.newChannel(new FileInputStream(file.toFile()));
    this.queue = new InboundBuffer<>(vertx.getOrCreateContext(), 0);
    queue.handler(buff -> {
      if (buff.length() > 0) {
        handleData(buff);
      } else {
        handleEnd();
      }
    });
    queue.drainHandler(v -> doRead());
  }

  public void close() {
    synchronized (this) {
      check();
      closed = true;
      doClose();
    }
  }

  @Override
  public synchronized FileReadStream endHandler(Handler<Void> endHandler) {
    check();
    this.endHandler = endHandler;
    return this;
  }

  @Override
  public synchronized FileReadStream exceptionHandler(Handler<Throwable> exceptionHandler) {
    check();
    this.exceptionHandler = exceptionHandler;
    return this;
  }

  @Override
  public synchronized FileReadStream handler(Handler<Buffer> handler) {
    check();
    this.dataHandler = handler;
    if (this.dataHandler != null && !this.closed) {
      this.doRead();
    } else {
      queue.clear();
    }
    return this;
  }

  @Override
  public synchronized FileReadStream pause() {
    check();
    queue.pause();
    return this;
  }

  @Override
  public synchronized FileReadStream resume() {
    check();
    if (!closed) {
      queue.resume();
    }
    return this;
  }

  @Override
  public ReadStream<Buffer> fetch(long amount) {
    queue.fetch(amount);
    return this;
  }

  private void check() {
    if (this.closed) {
      throw new IllegalStateException("ReaderStream is closed");
    }
  }

  private void doClose() {
    try {
      ch.close();
    } catch (IOException e) {
      Log.error("error closing FileReadStream", e);
    }
  }

  private void doRead() {
    check();
    doRead(ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE));
  }

  private synchronized void doRead(ByteBuffer bb) {
    if (!readInProgress) {
      readInProgress = true;
      Buffer buff = Buffer.buffer(DEFAULT_READ_BUFFER_SIZE);
      doRead(buff, 0, bb, readPos, ar -> {
        if (ar.succeeded()) {
          readInProgress = false;
          Buffer buffer = ar.result();
          readPos += buffer.length();
          // Empty buffer represents end of file
          if (queue.write(buffer) && buffer.length() > 0) {
            doRead(bb);
          }
        } else {
          handleException(ar.cause());
        }
      });
    }
  }

  private void doRead(Buffer writeBuff, int offset, ByteBuffer buff, long position,
      Handler<AsyncResult<Buffer>> handler) {

    // ReadableByteChannel doesn't have a completion handler, so we wrap it into
    // an executeBlocking and use the future there
    vertx.executeBlocking(() -> {
      try {
        int bytesRead = ch.read(buff);
        // Do the completed check
        if (bytesRead == -1) {
          // End of file
          vertx.getOrCreateContext().runOnContext(ignored -> {
            buff.flip();
            writeBuff.setBytes(offset, buff);
            buff.compact();
            handler.handle(Future.succeededFuture(writeBuff));
          });
        } else if (buff.hasRemaining()) {
          long pos = position;
          pos += bytesRead;
          // resubmit
          doRead(writeBuff, offset, buff, pos, handler);
        } else {
          // It's been fully written

          vertx.getOrCreateContext().runOnContext(v -> {
            buff.flip();
            writeBuff.setBytes(offset, buff);
            buff.compact();
            handler.handle(Future.succeededFuture(writeBuff));
          });
        }
        return bytesRead;
      } catch (Exception e) {
        Log.error(e);
        throw e;
      }
    });
  }

  private void handleData(Buffer buff) {
    Handler<Buffer> handler;
    synchronized (this) {
      handler = this.dataHandler;
    }
    if (handler != null) {
      handler.handle(buff);
    }
  }

  private synchronized void handleEnd() {
    Handler<Void> localEndHandler;
    synchronized (this) {
      dataHandler = null;
      localEndHandler = endHandler;
    }
    if (localEndHandler != null) {
      localEndHandler.handle(null);
    }
  }

  private void handleException(Throwable t) {
    if (exceptionHandler != null && t instanceof Exception) {
      exceptionHandler.handle(t);
    } else {
      Log.error("unhandled exception", t);
    }
  }

}
