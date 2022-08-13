package Controller;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.security.auth.login.LoginException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DuneBot {



    public static void main(String[] args) throws LoginException, SQLException {

        ResultSet rs = ConnectionHandler.runSQL("select * from game");

        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();

        Bot.run("MTAwNTUzODI2NjQ0OTE5MDk0Mg.GvY98f.28Tl-Bzeaqy9_ssjFbci1hQWt849sqxlhWOPw4", rs, session);
        }
}

