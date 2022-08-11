import Controller.Initializers;
import Model.Territory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        List<Territory> territories = Initializers.buildBoard(1);
        for (Territory territory : territories)  {
            session.persist(territory);
        }
        session.getTransaction().commit();
    }
}
