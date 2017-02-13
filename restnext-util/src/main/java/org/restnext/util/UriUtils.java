package org.restnext.util;

import java.util.Optional;
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
        return Optional.ofNullable(uri)
                .map(UriUtils::addFirstSlash)
                .map(UriUtils::removeLastSlash)
                .orElse(uri);
    }

    public static String addFirstSlash(String uri) {
        return Optional.ofNullable(uri)
                .filter(u -> u.length() >= 1 && u.charAt(0) != '/')
                .map(u -> "/" + u)
                .orElse(uri);
    }

    public static String removeLastSlash(String uri) {
        return Optional.ofNullable(uri)
                .filter(u -> u.length() > 1 && u.lastIndexOf('/') == (u.length() - 1))
                .map(u -> u.substring(0, u.lastIndexOf('/')))
                .orElse(uri);
    }

    public static boolean isPathParamUri(final String uri) {
        return uri != null && PATH_PARAM_URI.matcher(uri).matches();
    }
}
