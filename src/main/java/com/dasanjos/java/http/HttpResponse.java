package com.dasanjos.java.http;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import paris.boris.java.http.util.CacheUtils;

/**
 * HttpResponse class defines the HTTP Response Status Line (method, URI,
 * version) and Headers http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
 */
public class HttpResponse {

    private static final Logger LOG = Logger.getLogger(HttpResponse.class);

    public static final String VERSION = "HTTP/1.0";

    List<String> headers = new ArrayList<>();

    byte[] body;

    public HttpResponse(HttpRequest req) throws IOException {

        switch (req.method) {
            case HEAD:
                fillHeaders(Status._200);
                break;
            case GET:
                try {
                    File file = new File(req.rootPath + req.uri);

                    // TODO fix dir bug http://localhost:8080/src/test                    
                    if (file.isDirectory()) {
                        fillHeaders(Status._200);
                        headers.add(ContentType.HTML.toString());
                        StringBuilder result = new StringBuilder("<html><head><title>Index of ");
                        result.append(req.uri);
                        result.append("</title></head><body><h1>Index of ");
                        result.append(req.uri);
                        result.append("</h1><hr><pre>");

                        // TODO add Parent Directory
                        File[] files = file.listFiles();
                        for (File subfile : files) {
                            result.append(" <a href=\"")
                                    .append(subfile.getPath())
                                    .append("\">")
                                    .append(subfile.getPath())
                                    .append("</a>\n");
                        }
                        result.append("<hr></pre></body></html>");

                        fillResponse(result.toString());
                    } else if (file.exists()) {
                        /*
                        @boris paris:
                        http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

                        If none of the entity tags match, or if "*" is given and no 
                        current entity exists, the server MUST NOT perform the 
                        requested method, and MUST return a 412 
                        (Precondition Failed) response.
                         */
                        if (!req.ifMatch(CacheUtils.generateETag(file))
                                || (req.isIfMatchWildcard() && !file.exists())) {
                            fillHeaders(Status._412);
                            break;
                        }
                        /*
                        @boris paris:
                        http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

                        The If-Modified-Since request-header field is used with a 
                        method to make it conditional: if the requested variant has 
                        not been modified since the time specified in this field, an 
                        entity will not be returned from the server; instead, a 304 
                        (not modified) response will be returned without any message-body.                    
                         */
                        if (!req.ifModifiedSince(CacheUtils.generateLastModified(file))) {
                            fillHeaders(Status._304);
                            break;
                        }
                        /*
                        @boris paris:
                        
                        http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html

                        If any of the entity tags match the entity tag of the entity 
                        that would have been returned in the response to a similar 
                        GET request (without the If-None-Match header) on that 
                        resource, or if "*" is given and any current entity exists 
                        for that resource, then the server MUST NOT perform the 
                        requested method, unless required to do so because the 
                        resource's modification date fails to match that supplied 
                        in an If-Modified-Since header field in the request. 
                        Instead, if the request method was GET or HEAD, the server 
                        SHOULD respond with a 304 (Not Modified) response, including 
                        the cache- related header fields (particularly ETag) of one 
                        of the entities that matched.
                         */
                        if (req.ifNoneMatch(CacheUtils.generateETag(file))
                                || (req.isIfNoneMatchWildcard() && file.exists())) {
                            fillHeaders(Status._304);
                            headers.add("ETag: " + CacheUtils.generateETag(file));
                            break;
                        }
                        
                        fillHeaders(Status._200);
                        // @boris paris: Add headers for Last-Modified, and ETag
                        headers.add("Last-Modified: " + CacheUtils.generateLastModified(file));
                        headers.add("ETag: " + CacheUtils.generateETag(file));
                        setContentType(req.uri, headers);
                        fillResponse(getBytes(file));
                    } else {
                        LOG.info("File not found:" + req.uri);
                        fillHeaders(Status._404);
                        fillResponse(Status._404.toString());
                    }
                } catch (Exception e) {
                    LOG.error("Response Error", e);
                    fillHeaders(Status._400);
                    fillResponse(Status._400.toString());
                }

                break;
            case UNRECOGNIZED:
                fillHeaders(Status._400);
                fillResponse(Status._400.toString());
                break;
            default:
                fillHeaders(Status._501);
                fillResponse(Status._501.toString());
        }

    }

    private byte[] getBytes(File file) throws IOException {
        int length = (int) file.length();
        byte[] array = new byte[length];
        // @boris paris: 'in' wasnt closed properly after usage
        try (InputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < length) {
                int count = in.read(array, offset, (length - offset));
                offset += count;
            }
        }
        return array;
    }

    /**
     * @boris paris: Added 'Date' in RFC_1123 format.
     */
    private void fillHeaders(Status status) {
        headers.add(HttpResponse.VERSION + " " + status.toString());
        headers.add("Date: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        headers.add("Server: SimpleWebServer");
    }

    private void fillResponse(String response) {
        body = response.getBytes();
    }

    private void fillResponse(byte[] response) {
        body = response;
    }

    public void write(OutputStream os) throws IOException {
        DataOutputStream output = new DataOutputStream(os);
        for (String header : headers) {
            output.writeBytes(header + "\r\n");
        }
        output.writeBytes("\r\n");
        if (body != null) {
            output.write(body);
        }
        output.writeBytes("\r\n");
        output.flush();
    }

    private void setContentType(String uri, List<String> list) {
        try {
            String ext = uri.substring(uri.indexOf(".") + 1);
            list.add(ContentType.valueOf(ext.toUpperCase()).toString());
        } catch (Exception e) {
            LOG.error("ContentType not found: " + e, e);
        }
    }
}
