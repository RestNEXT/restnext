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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

import java.util.Arrays;
import java.util.List;

import org.restnext.core.http.MediaType;

public class CustomHttpContentCompressor extends HttpContentCompressor {

  private final int compressionContentLength;
  private final MediaType[] compressibleTypes;

  private boolean skipCompression;

  /**
   * Creates a new handler with the specified compression level, default
   * window size (<tt>15</tt>) and default memory level (<tt>8</tt>).
   *
   * @param compressionLevel
   *        {@code 1} yields the fastest compression and {@code 9} yields the
   *        best compression.  {@code 0} means no compression.  The default
   *        compression level is {@code 6}.
   * @param compressionContentLength minimum content length for compression
   * @param compressibleTypes compressible media types
   */
  public CustomHttpContentCompressor(
      int compressionLevel,
      int compressionContentLength,
      MediaType... compressibleTypes) {

    super(compressionLevel);
    this.compressionContentLength = compressionContentLength;
    this.compressibleTypes = compressibleTypes;
  }

  @Override
  protected Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception {
    if (skipCompression) {
      return null;
    }
    return super.beginEncode(headers, acceptEncoding);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
      throws Exception {
    if (msg instanceof HttpResponse) {
      HttpResponse res = (HttpResponse) msg;

      skipCompression = false;

      // if an "content-encoding: identity" header was set, we do not compress
      if (skipCompression = res.headers().containsValue(
          HttpHeaderNames.CONTENT_ENCODING,
          HttpHeaderValues.IDENTITY,
          true)) {
        // remove header as one should not send Identity as content encoding
        res.headers().remove(HttpHeaderNames.CONTENT_ENCODING);
      } else {
        CharSequence mimeType = HttpUtil.getMimeType(res);
        // skip compression if the media type is not compressible by the server
        skipCompression = mimeType != null && !isCompressable(MediaType.parse(mimeType.toString()));

        // skip compression if the content length is less than expected by the server
        int contentLength = res.headers().getInt(HttpHeaderNames.CONTENT_LENGTH, 0);
        skipCompression = contentLength > 0 && contentLength < compressionContentLength;
      }
    }

    super.encode(ctx, msg, out);
  }

  private boolean isCompressable(MediaType type) {
    return Arrays.stream(compressibleTypes)
        .anyMatch(m -> {
          boolean match = m.isSimilar(type) || m.isCompatible(type);
          if (match) {
            System.out.println(m);
          }
          return match;
        });
  }
}
