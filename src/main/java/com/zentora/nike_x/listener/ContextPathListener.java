package com.zentora.nike_x.listener;

import com.zentora.nike_x.provider.MailServiceProvider;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class ContextPathListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MailServiceProvider.getMailServiceProvider().start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        MailServiceProvider.getMailServiceProvider().shutdown();
    }
}
