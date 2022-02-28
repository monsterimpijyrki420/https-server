package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.json.JSONObject;

public class CoordinateDatabase {
    private static CoordinateDatabase coordinate_db = null;
    private Connection db_connection;

    private CoordinateDatabase(String path, String db_name) {
        try {
            open(path, db_name);
        } catch (SQLException e) {
            System.out.println("Something went wrong with databases");
        }
    }

    public static CoordinateDatabase getInstance(String path, String db_name) {
        if (coordinate_db == null) {
            coordinate_db = new CoordinateDatabase(path, db_name);
        }
        return coordinate_db;
    }

    private void open(String path, String db_name) throws SQLException  {
    
        //checks if database exists
        File db_file = new File(path + db_name);
        boolean does_exist = db_file.exists() && !db_file.isDirectory();

        String db_path = "jdbc:sqlite:" + path + db_name;
        db_connection = DriverManager.getConnection(db_path);

        //creates database if there isnt one
        if (!does_exist) {
            System.out.println("Creating database...");
            if (initializeDatabase()) {
                System.out.println("Database successfully created");
            } else {
                System.out.println("Database creation failed");
            }
        } else {
            System.out.println("Database detected");
        }

    }

    private boolean initializeDatabase() throws SQLException {
        if (null != db_connection) {
            String user_table = "CREATE TABLE Users (Username VARCHAR(50) NOT NULL, Password VARCHAR(50) NOT NULL, Email VARCHAR(50), PRIMARY KEY(Username))";
            String coordinate_table = "CREATE TABLE Coordinates (Username VARCHAR(50) NOT NULL, Longitude VARCHAR(50) NOT NULL, Latitude VARCHAR(50) NOT NULL, Timestamp NUMERIC NOT NULL, PRIMARY KEY(Username, Timestamp))";
            Statement create_statement = db_connection.createStatement();
            create_statement.executeUpdate(user_table);
            create_statement.executeUpdate(coordinate_table);
            create_statement.close();
            return true;
        }
        return false;
       }

    public boolean checkCredentials(String username, String given_password) throws SQLException {
        String get_message = "SELECT Password FROM Users WHERE Username = " + "'" + username + "'";
        Statement query_statement = db_connection.createStatement();
		ResultSet rs = query_statement.executeQuery(get_message);
        if (rs.getString("Password").equals(given_password)) {
            return true;
        }
        return false;
    }

    public boolean addUser(JSONObject user) {
        try {
            String new_user = "INSERT INTO Users " + "VALUES('"
					+ user.getString("username") + "','" + user.getString("password") + "','" + user.getString("email") + "')";
            Statement create_statment = db_connection.createStatement();
            create_statment.executeUpdate(new_user);
            create_statment.close();
            return true;
        }
        catch(Exception e) { //username already exists
            return false;
        }
    }

    public void addCoordinates(String nick, String longitude, String latitude, long timestamp) throws SQLException{
        String new_coordinates = "INSERT INTO Coordinates " + "VALUES('" 
                + nick + "','" + longitude + "','" + latitude + "','" + timestamp + "')";
        Statement create_statment = db_connection.createStatement();
        create_statment.executeUpdate(new_coordinates);
        create_statment.close();
    }

    public ResultSet getCoordinates() throws SQLException {
        String get_message = "SELECT Username, Longitude, Latitude, Timestamp FROM Coordinates";
        Statement query_statement = db_connection.createStatement();
		ResultSet rs = query_statement.executeQuery(get_message);
        return rs;
    }
}
