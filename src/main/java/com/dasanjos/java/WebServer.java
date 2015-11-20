package com.dasanjos.java;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * Class <code>WebServer</code> - Main class that starts the Web Server Thread Pool in port 8080 (default)
 * 
 * @boris paris: Changed thread pooling from fixed threads to cached. Let the
 * jvm handle the threads seems more efficient to me.
 */
public class WebServer extends Thread {

	private static Logger log = Logger.getLogger(WebServer.class);

	private static final int DEFAULT_PORT = 8080;

	public static void main(String args[]) {
		try {
			new WebServer().start(getValidPortParam(args));
		} catch (Exception e) {
			log.error("Startup Error", e);
		}
	}

	public void start(int port) throws IOException {
		ServerSocket s = new ServerSocket(port);
		System.out.println("Web server listening on port " + port + " (press CTRL-C to quit)");
		ExecutorService executor = Executors.newCachedThreadPool();
		while (true) {
			executor.submit(new RequestHandler(s.accept()));
		}
	}

	/**
	 * Parse command line arguments (string[] args) for valid port number
	 * 
         * @boris paris: Changed port numbers from 0-65335 to 1024-65535, since
         * ports from 0-1023 are reserved.
         * 
	 * @return int valid port number or default value (8080)
	 */
	static int getValidPortParam(String args[]) throws NumberFormatException {
		if (args.length > 0) {
			int port = Integer.parseInt(args[0]);
			if (port > 1023 && port < 65535) {
				return port;
			} else {
				throw new NumberFormatException("Invalid port! Port value is a number between 1024 and 65535");
			}
		}
		return DEFAULT_PORT;
	}
}
