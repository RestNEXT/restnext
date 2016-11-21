/**************************************************************************************************
 * Copyright (c) 2004, Federal University of So Carlos                                            *
 *                                                                                                *
 * All rights reserved.                                                                           *
 *                                                                                                *
 * Redistribution and use in source and binary forms, with or without modification, are permitted *
 * provided that the following conditions are met:                                                *
 *                                                                                                *
 *     * Redistributions of source code must retain the above copyright notice, this list of      *
 *       conditions and the following disclaimer.                                                 *
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of   *
 *     * conditions and the following disclaimer in the documentation and/or other materials      *
 *     * provided with the distribution.                                                          *
 *     * Neither the name of the Federal University of So Carlos nor the names of its             *
 *     * contributors may be used to endorse or promote products derived from this software       *
 *     * without specific prior written permission.                                               *
 *                                                                                                *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS                            *
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT                              *
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR                          *
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR                  *
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,                          *
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,                            *
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR                             *
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF                         *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING                           *
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS                             *
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                   *
 **************************************************************************************************/
/*
 * Created on Oct 6, 2004
 */
package org.restnext.core.classpath;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Useful class for dynamically changing the classpath, adding classes during runtime.
 */
public class ClasspathRegister {

    /**
     * Parameters of the method to add an URL to the System classes.
     */
    private static final Class<?>[] parameters = new Class[]{URL.class};

    /**
     * Adds a file to the classpath.
     *
     * @param s a String pointing to the file
     */
    public static void addPath(String s) {
        addPath(Paths.get(s));
    }

    /**
     * Adds a file to the classpath
     *
     * @param path the file to be added
     */
    public static void addPath(Path path) {
        try {
            addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error, could not get path URL", e);
        }
    }

    /**
     * Adds the content pointed by the URL to the classpath.
     *
     * @param u the URL pointing to the content to be added
     * @throws RuntimeException if something goes wrong
     */
    public static void addURL(URL u) {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<?> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", parameters);
            method.setAccessible(true);
            method.invoke(sysloader, u);
        } catch (Throwable t) {
            throw new RuntimeException("Error, could not add URL to system classloader", t);
        }
    }
}