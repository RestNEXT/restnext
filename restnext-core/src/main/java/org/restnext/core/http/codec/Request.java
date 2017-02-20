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
package org.restnext.core.http.codec;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.restnext.core.http.EntityTag;
import org.restnext.core.http.MediaType;
import org.restnext.core.http.MultivaluedMap;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public interface Request extends Message, Headers {

    FullHttpRequest getFullHttpRequest();

    URI getBaseURI();

    URI getURI();

    Method getMethod();

    MultivaluedMap<String, String> getParams();

    byte[] getContent();

    boolean hasContent();

    int getLength();

    boolean isKeepAlive();

    Date getDate();

    MediaType getMediaType();

    Response.Builder evaluatePreconditions(EntityTag eTag);

    Response.Builder evaluatePreconditions(Date lastModified);

    Response.Builder evaluatePreconditions(Date lastModified, EntityTag eTag);

    Response.Builder evaluatePreconditions();

    //============================
    //     STATIC METHODS
    //============================

    static Request fromRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        return new RequestImpl(ctx, request);
    }

    //============================
    //     HTTP METHOD
    //============================

    enum Method {

        GET, POST, PUT, DELETE;

        private static final Map<String, Method> methodMap = new HashMap<>();

        static {
            methodMap.put(GET.toString(), GET);
            methodMap.put(POST.toString(), POST);
            methodMap.put(PUT.toString(), PUT);
            methodMap.put(DELETE.toString(), DELETE);
        }

        @Override
        public String toString() {
            return name();
        }

        public static Method of(HttpMethod method) {
            if (method == null) return null;
            return of(method.toString());
        }

        public static Method of(String method) {
            if (method == null) return null;
            return methodMap.getOrDefault(method, null);
        }
    }

}
