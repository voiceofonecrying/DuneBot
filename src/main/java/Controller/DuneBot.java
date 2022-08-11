package Controller;

import Model.Game;
import Model.Player;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.management.openmbean.KeyAlreadyExistsException;
import javax.security.auth.login.LoginException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DuneBot {



    public static void main(String[] args) throws LoginException, SQLException {

        ResultSet rs = ConnectionHandler.runSQL("select * from game");
        List<Player> players = new ArrayList<>();
        players.add(new Player("voiceofonecrying", "James"));
        players.add(new Player("voiceofonecrying2", "Jamesy"));
        players.add(new Player("voiceofonecrying3", "Jameson"));
        players.add(new Player("voiceofonecrying4", "Jim"));
        players.add(new Player("voiceofonecrying5", "Jimbo"));
        players.add(new Player("voiceofonecrying6", "Jimmy"));


        Game newGame = new Game(players);
        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();

        Bot.run("botkey", rs, session);
        }
}

