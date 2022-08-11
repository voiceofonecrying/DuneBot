package Controller;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionHandler {

    public static ResultSet runSQL(String SQL) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Dune", "root", "root");
            return conn.createStatement().executeQuery(SQL);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "A fatal error has occurred: this application made a query to the database that contained an error. This could be because of a change in the structure of the backend database, or " +
                    "because some required fields are missing data. Please correct the error and try again.");
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}