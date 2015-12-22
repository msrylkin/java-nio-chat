package nio.model;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.*;
import org.hibernate.boot.MetadataSources;

import javax.imageio.spi.ServiceRegistry;

/**
 * Created by user on 10.12.2015.
 */
public class HibernateUtil {
    public static SessionFactory sessionFactory = null;

    static {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        } catch (Throwable e){
            System.out.println("oops "+e);
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
