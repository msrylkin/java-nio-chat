package nio.model;

import org.hibernate.Session;

import java.util.List;

/**
 * Created by user on 13.12.2015.
 */
public class DAOMessage{
    @SuppressWarnings("unchecked")
    public List<Message> getAllMessages(){
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        List<Message> messages =  session.createQuery("from nio.model.Message").list();
        session.close();
        return messages;
    }

    public void persist(Message message) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        session.save(message);
        session.getTransaction().commit();
        session.close();
    }
}
