package org.restnext.server;

import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

/**
 * Created by thiago on 29/03/17.
 */
public class CustomReadTimeoutHandler extends ReadTimeoutHandler {

  @Deprecated
  public CustomReadTimeoutHandler(int timeoutSeconds) {
    super(timeoutSeconds);
  }

  public CustomReadTimeoutHandler(long timeoutSeconds) {
    this(timeoutSeconds, TimeUnit.SECONDS);
  }

  public CustomReadTimeoutHandler(long timeout, TimeUnit unit) {
    super(timeout, unit);
  }
}
