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

import io.netty.handler.codec.http.HttpVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by thiago on 06/09/16.
 */
public interface Message {

    Version getVersion();

    //============================
    //        HTTP VERSION
    //============================

    enum Version {

        HTTP_1_0(HttpVersion.HTTP_1_0),
        HTTP_1_1(HttpVersion.HTTP_1_1);

        private static class Holder {
            static Map<HttpVersion, Version> MAP = new HashMap<>();
        }

        private final HttpVersion nettyVersion;

        Version(HttpVersion nettyVersion) {
            this.nettyVersion = nettyVersion;
            Holder.MAP.put(nettyVersion, this);
        }

        public HttpVersion getNettyVersion() {
            return nettyVersion;
        }

        @Override
        public String toString() {
            return name();
        }

        public static Version of(HttpVersion version) {
            if (version == null) return null;
            return Holder.MAP.getOrDefault(version, null);
        }
    }
}
