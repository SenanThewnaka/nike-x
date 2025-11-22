package com.zentora.nike_x;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {


        private static final int SERVER_PORT = 8080;
        public static void main(String[] args) {
            try {
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(SERVER_PORT);
            tomcat.getConnector();
            tomcat.addWebapp("/nike-x", new File("src/main/webapp").getAbsolutePath());
            tomcat.start();
                System.out.println("App URL: http://localhost:" + SERVER_PORT + "/nike-x");
            tomcat.getServer().await();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
        }

}
