package com.dasanjos.java;

import java.net.Socket;

import org.apache.log4j.Logger;

import com.dasanjos.java.http.HttpRequest;
import com.dasanjos.java.http.HttpResponse;

/**
    * Class <code>RequestHandler</code> - class that answer the requests in
 * the socket
 */
public class RequestHandler {

    private static final Logger log = Logger.getLogger(RequestHandler.class);

    public boolean handleConnection(Socket socket, String rootPath) {
        try {
            HttpRequest req = new HttpRequest(socket.getInputStream(), rootPath);
            HttpResponse res = new HttpResponse(req);
            res.write(socket.getOutputStream());
            return req.isKeepAlive();
        } catch (Exception e) {
            log.error("Runtime Error", e);
            return false;
        }
    }
}
