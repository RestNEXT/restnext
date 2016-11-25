package org.restnext.util;

import java.util.regex.Pattern;

/**
 * Created by thiago on 10/18/16.
 */
public final class UriUtils {

    /**
     * Regex to validate uris with or without parameters.
     * E.g: /test/1/{id}/sub/{2}/{name}
     */
    public static final Pattern PATH_PARAM_URI = Pattern.compile("^([/])(([/\\w])+(/\\{[\\w]+\\})*)*([?])?$");

    private UriUtils() {
        throw new AssertionError();
    }

    public static String normalize(String uri) {
        if (uri != null) {
            // adds initial slash in the uri if not exists.
            if (uri.length() >= 1 && uri.charAt(0) != '/') uri = "/" + uri;
            // removes the latest slash from the uri if exists.
            if (uri.length() > 1 && uri.lastIndexOf('/') == (uri.length()-1)) uri = uri.substring(0, uri.lastIndexOf('/'));
        }
        return uri;
    }

    public static boolean isPathParamUri(final String uri) {
        return uri != null && PATH_PARAM_URI.matcher(uri).matches();
    }
}
