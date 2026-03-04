package com.zentora.nike_x.util;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory;
    static {
        try {
            Configuration configuration = new Configuration().configure();

            // Inject secure database credentials dynamically
            configuration.setProperty("hibernate.connection.url", Env.get("DB_URL"));
            configuration.setProperty("hibernate.connection.username", Env.get("DB_USERNAME"));
            configuration.setProperty("hibernate.connection.password", Env.get("DB_PASSWORD"));

            sessionFactory = configuration.buildSessionFactory();
        } catch (HibernateException e) {
            throw new ExceptionInInitializerError("SessionFactory creation failed: " + e.getMessage());
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        sessionFactory.close();
    }
}