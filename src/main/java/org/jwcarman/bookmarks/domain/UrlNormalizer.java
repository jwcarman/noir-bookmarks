package org.jwcarman.bookmarks.domain;

import java.util.Objects;

/**
 * Turns absolute {@code http} and {@code https} URLs into a canonical form.
 *
 * <p>The canonical form is the input with these changes, and no others:
 *
 * <ol>
 *   <li>The scheme and the host are lowercased (ASCII only, independent of the default locale).
 *   <li>A default port is removed: {@code :80} for {@code http} and {@code :443} for {@code https}.
 *       Any other port is kept.
 *   <li>The fragment ({@code #...}) is removed.
 *   <li>An empty path becomes {@code /}, and a path longer than {@code /} loses one trailing
 *       {@code /}.
 * </ol>
 *
 * <p>The path keeps its case, and the query ({@code ?...}) and percent-encoding are kept exactly as
 * given.
 */
public final class UrlNormalizer {

  private static final String SCHEME_SEPARATOR = "://";

  private UrlNormalizer() {}

  /**
   * Returns the canonical form of {@code url}.
   *
   * @param url an absolute {@code http} or {@code https} URL with a host
   * @return the canonical form
   * @throws NullPointerException if {@code url} is {@code null}
   * @throws IllegalArgumentException if {@code url} is not an absolute {@code http} or {@code
   *     https} URL with a non-empty host
   */
  public static String normalize(String url) {
    Objects.requireNonNull(url, "url");
    rejectWhitespace(url);
    int schemeEnd = url.indexOf(SCHEME_SEPARATOR);
    if (schemeEnd < 0) {
      throw invalid(url);
    }
    String scheme = asciiLower(url.substring(0, schemeEnd));
    String defaultPort = defaultPort(scheme, url);
    String rest = stripFragment(url.substring(schemeEnd + SCHEME_SEPARATOR.length()));
    int authorityEnd = indexOfAny(rest, "/?");
    String authority = normalizeAuthority(rest.substring(0, authorityEnd), defaultPort, url);
    String pathAndQuery = rest.substring(authorityEnd);
    int queryStart = indexOfAny(pathAndQuery, "?");
    String path = normalizePath(pathAndQuery.substring(0, queryStart));
    return scheme + SCHEME_SEPARATOR + authority + path + pathAndQuery.substring(queryStart);
  }

  private static void rejectWhitespace(String url) {
    if (url.chars().anyMatch(c -> c <= ' ' || c == 0x7f)) {
      throw invalid(url);
    }
  }

  private static String defaultPort(String scheme, String url) {
    return switch (scheme) {
      case "http" -> "80";
      case "https" -> "443";
      default -> throw invalid(url);
    };
  }

  private static String stripFragment(String s) {
    int hash = s.indexOf('#');
    return hash < 0 ? s : s.substring(0, hash);
  }

  private static int indexOfAny(String s, String chars) {
    for (int i = 0; i < s.length(); i++) {
      if (chars.indexOf(s.charAt(i)) >= 0) {
        return i;
      }
    }
    return s.length();
  }

  private static String normalizeAuthority(String authority, String defaultPort, String url) {
    int at = authority.lastIndexOf('@');
    String userInfo = authority.substring(0, at + 1);
    String hostPort = authority.substring(at + 1);
    int portSep = portSeparator(hostPort);
    String host = hostPort.substring(0, portSep);
    String port = hostPort.substring(portSep);
    if (!isValidHost(host) || !isValidPort(port)) {
      throw invalid(url);
    }
    if (port.equals(":" + defaultPort)) {
      port = "";
    }
    return userInfo + asciiLower(host) + port;
  }

  private static int portSeparator(String hostPort) {
    int colon = hostPort.lastIndexOf(':');
    int bracket = hostPort.lastIndexOf(']');
    return colon > bracket ? colon : hostPort.length();
  }

  private static boolean isValidHost(String host) {
    return !host.isEmpty() && (host.startsWith("[") || host.indexOf(':') < 0);
  }

  private static boolean isValidPort(String port) {
    return port.chars().skip(1).allMatch(c -> c >= '0' && c <= '9');
  }

  private static String normalizePath(String path) {
    if (path.isEmpty()) {
      return "/";
    }
    if (path.length() > 1 && path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    }
    return path;
  }

  private static String asciiLower(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      sb.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
    }
    return sb.toString();
  }

  private static IllegalArgumentException invalid(String url) {
    return new IllegalArgumentException("Not an absolute http or https URL with a host: " + url);
  }
}
