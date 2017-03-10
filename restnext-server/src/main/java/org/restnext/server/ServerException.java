/*
 * Copyright (C) 2016 Thiago Gutenberg Carvalho da Costa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.restnext.server;

import java.util.Optional;

import org.restnext.core.http.Response;

/**
 * Created by thiago on 04/08/16.
 */
public class ServerException extends RuntimeException {

  private static final long serialVersionUID = 9222578952610045310L;

  private final Response.Status responseStatus;

  public ServerException() {
    this((Throwable) null, Response.Status.INTERNAL_SERVER_ERROR);
  }

  public ServerException(Throwable cause, Response.Status status) {
    this(computeExceptionMessage(status), cause, status);
  }

  public ServerException(String message, Throwable cause, Response.Status status) {
    super(message, cause);
    this.responseStatus = Optional.ofNullable(status).orElse(Response.Status.INTERNAL_SERVER_ERROR);
  }

  private static String computeExceptionMessage(Response.Status status) {
    Response.Status statusInfo = Optional.ofNullable(status)
        .orElse(Response.Status.INTERNAL_SERVER_ERROR);
    return "HTTP " + statusInfo.getStatusCode() + ' ' + statusInfo.getReasonPhrase()
        + ' ' + statusInfo.getFamily();
  }

  public ServerException(String message) {
    this(message, null, Response.Status.INTERNAL_SERVER_ERROR);
  }

  public ServerException(Throwable cause) {
    this(computeExceptionMessage(Response.Status.INTERNAL_SERVER_ERROR), cause,
        Response.Status.INTERNAL_SERVER_ERROR);
  }

  public ServerException(Response.Status status) {
    this((Throwable) null, status);
  }

  public ServerException(String message, Throwable cause) {
    this(message, cause, Response.Status.INTERNAL_SERVER_ERROR);
  }

  public ServerException(String message, Response.Status status) {
    this(message, null, status);
  }

  public Response.Status getResponseStatus() {
    return responseStatus;
  }

}
