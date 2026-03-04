package com.zentora.nike_x;

import com.zentora.nike_x.config.AppConfig;
import com.zentora.nike_x.listener.ContextPathListener;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11NioProtocol;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final int SERVER_PORT = 8080;
    private static final String CONTEXT_PATH = "/nike-x";

    public static void main(String[] args) {
        try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(SERVER_PORT);
            org.apache.catalina.connector.Connector connector = tomcat.getConnector();

            // Production Tuning for Tomcat Connector
            Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
            protocol.setMaxThreads(400); // Max concurrent threads (Default 200)
            protocol.setMaxConnections(10000); // Max concurrent connections (Default 8192)
            protocol.setAcceptCount(200); // Max queue length for incoming connection requests (Default 100)
            protocol.setConnectionTimeout(20000); // 20s timeout for reading request URI/Headers

            // Max Post Size (e.g., for file uploads like Reviews/Products) - 50MB
            connector.setMaxPostSize(50 * 1024 * 1024);

            Context context = tomcat.addWebapp(CONTEXT_PATH, new File("src/main/webapp").getAbsolutePath());
            Tomcat.addServlet(context, "JerseyServlet", new ServletContainer(new AppConfig()));
            context.addServletMappingDecoded("/api/*", "JerseyServlet");

            context.addApplicationListener(ContextPathListener.class.getName());

            tomcat.start();
            logger.info("Nike-X Production API Gateway Online at: http://localhost:{}{}", SERVER_PORT, CONTEXT_PATH);
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            logger.error("Tomcat Embedded Server loading failed!", e);
            throw new RuntimeException("Tomcat Embedded Server loading failed: " + e.getMessage());
        }
    }

}
