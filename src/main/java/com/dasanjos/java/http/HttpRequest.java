package com.dasanjos.java.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * HttpRequest class parses the HTTP Request Line (method, URI, version) and
 * Headers http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html
 */
public class HttpRequest {

    private static Logger log = Logger.getLogger(HttpRequest.class);

    List<String> headers = new ArrayList<>();
    
    String body;
    
    String rootPath;

    Method method;

    String uri;

    String version;

    public HttpRequest(InputStream is, String rootPath) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String str = reader.readLine();
        parseRequestLine(str);
        this.rootPath = rootPath;

        while (!str.equals("")) {
            str = reader.readLine();
            parseRequestHeader(str);
        }
        
        // @boris paris:
        // If the request was a POST request check for a body and read it.
        if (method.equals(Method.POST)) {
            StringBuilder builder = new StringBuilder();
            do {
                str = reader.readLine();
                log.info(str);
                builder.append(str);
            } while(!str.equals(""));
            body = builder.toString();
        }
    }

    private void parseRequestLine(String str) {
        log.info(str);
        String[] split = str.split("\\s+");
        try {
            method = Method.valueOf(split[0]);
        } catch (Exception e) {
            method = Method.UNRECOGNIZED;
        }
        uri = split[1];
        version = split[2];
    }

    private void parseRequestHeader(String str) {
        log.info(str);
        headers.add(str);
    }

    /**
     * @boris paris: Added for handling the ETag(If-Match, If-Non-Match) and
     * If-Modified-Since.
     */
        
    private boolean hasHeader(String name) {
        return headers.stream().filter(h -> h.startsWith(name)).findAny().isPresent();
    }

    private String getHeaderValue(String name) {
        String modifiedSinceHeader = headers.stream().filter(h -> h.startsWith(name)).findFirst().get();
        return modifiedSinceHeader.substring(modifiedSinceHeader.indexOf(":") + 1).trim();
    }

    /*
        If-Modified-Since
    */
    private boolean hasIfModifiedSince() {
        return hasHeader("If-Modified-Since");
    }

    private String getIfModifiedSince() {
        return getHeaderValue("If-Modified-Since");
    }

    /**
     * Exact comparsion of the If-Modified-Since string.
     * 
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     * 
     * Note: When handling an If-Modified-Since header field, some
     * servers will use an exact date comparison function, rather than a
     * less-than function, for deciding whether to send a 304 (Not
     * Modified) response. To get best results when sending an If-
     * Modified-Since header field for cache validation, clients are
     * advised to use the exact date string received in a previous Last-
     * Modified header field whenever possible.
     * 
     * @param date as a String
     * @return true if the String is the exact same in this request. Everything
     * else is invalid and returns to a normal request.
     */
    public boolean ifModifiedSince(String date) {
        if (hasIfModifiedSince()) {
            return getIfModifiedSince().equals(date);
        }
        return true;
    }

    /*
        If-Match
    */
    private boolean hasIfMatch() {
        return hasHeader("If-Match");
    }

    private List<String> getIfMatch() {
        return Arrays.asList(getHeaderValue("If-Match").split(","));
    }
    
    public boolean ifMatch(String tag) {
        if (hasIfMatch()) {
            return getIfMatch().contains(tag);
        }
        return true;
    }
    
    public boolean isIfMatchWildcard() {
       if (hasIfMatch()) {
           return getHeaderValue("If-Match").equals("*");
       }
       return false;
    }

    /*
        If-None-Match
    */
    private boolean hasIfNoneMatch() {
        return hasHeader("If-None-Match");
    }

    private List<String> getIfNoneMatch() {
        return Arrays.asList(getHeaderValue("If-None-Match").split(","));
    }
    
    public boolean ifNoneMatch(String tag) {
        if (hasIfNoneMatch()) {
            return getIfNoneMatch().contains(tag);
        }
        return false;
    }
    
    public boolean isIfNoneMatchWildcard() {
        if (hasIfNoneMatch()) {
           return getHeaderValue("If-None-Match").equals("*");
       }
       return false;
    }
    
    /*
        Keep-Alive
    */
    public boolean isKeepAlive() {
        // Assume a persistent connection unless the client sends a close-token
        if (version.equals("HTTP/1.1")) {
            return !(hasHeader("Connection") &&
                    getHeaderValue("Connection").toLowerCase().equals("close"));
        }
        // To keep persistent connections in HTTP 1.0 there has to be a
        // keep-alive-token in the request.
        else if (version.equals("HTTP/1.0")) {
           return hasHeader("Connection") &&
                   getHeaderValue("Connection").toLowerCase().equals("keep-alive");
        }
        return false;
    }
}
