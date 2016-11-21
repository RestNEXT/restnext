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
package org.restnext.util;

/**
 * Created by thiago on 24/08/16.
 */
public final class AnsiUtils {

    private AnsiUtils() {
        throw new AssertionError();
    }

    public static void install() {
        org.fusesource.jansi.AnsiConsole.systemInstall();
    }

    public static void uninstall() {
        org.fusesource.jansi.AnsiConsole.systemUninstall();
    }

    public static String info(String message) {
        return createAnsi(message, org.fusesource.jansi.Ansi.Color.GREEN, false);
    }

    public static String warn(String message) {
        return createAnsi(message, org.fusesource.jansi.Ansi.Color.YELLOW, true);
    }

    public static String error(String message) {
        return createAnsi(message, org.fusesource.jansi.Ansi.Color.RED, true);
    }

    public static String createAnsi(String message, org.fusesource.jansi.Ansi.Color color, boolean bold) {
        org.fusesource.jansi.Ansi ansi = org.fusesource.jansi.Ansi.ansi().fg(color);
        if (bold) {
            ansi.bold();
        }
        return ansi.a(message).reset().toString();
    }

}
