package com.zentora.nike_x.config;

import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {
    public AppConfig(){
        packages("com.zentora.nike-x.controller");
        packages("com.zentora.nike-x.middleware");
        register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
    }
}
