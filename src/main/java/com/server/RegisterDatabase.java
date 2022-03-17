package com.server;

import java.util.Base64;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.SecureRandom;
import org.apache.commons.codec.digest.Crypt;
import org.json.JSONObject;

public class RegisterDatabase {
    private static RegisterDatabase register_db = null;
    private Connection connection;
    private SecureRandom secure_random = new SecureRandom();

    private RegisterDatabase(String path, String db_name) {
        try {
            open(path, db_name);
        } catch (SQLException e) {
            System.out.println("Something went wrong with databases");
        }
    }

    public static synchronized RegisterDatabase getInstance(String path, String db_name) {
        if (register_db == null) {
            register_db = new RegisterDatabase(path, db_name);
        }
        return register_db;
    }

    private void open(String path, String db_name) throws SQLException  {

        //checks if database exists
        File register_file = new File(path + db_name);
        boolean register_exists = register_file.exists() && !register_file.isDirectory();

        //makes the connection to database
        String full_path = "jdbc:sqlite:" + path + db_name;
        connection = DriverManager.getConnection(full_path);

        //creates database if there isnt one
        if (!register_exists) {
            System.out.println("Creating register database...");
            if (initializeDatabase()) {
                System.out.println("Register database successfully created");
            } else {
                System.out.println("Register database creation failed");
            }
        } else {
            System.out.println("Register Database detected");
        }
    }

    private boolean initializeDatabase() throws SQLException {
        if (null != connection) {
            String user_table = "CREATE TABLE Users (Username VARCHAR(50) NOT NULL, Password VARCHAR(50) NOT NULL, " +
                                                        "Email VARCHAR(50), Salt VARCHAR, PRIMARY KEY(Username))";
            Statement create_statement = connection.createStatement();
            create_statement.executeUpdate(user_table);
            create_statement.close();
            return true;
        }
        return false;
       }

    public boolean checkCredentials(String username, String given_password) throws SQLException {
        String get_message = "SELECT Password, Salt FROM Users WHERE Username = " + "'" + username + "'";
        Statement query_statement = connection.createStatement();
		ResultSet rs = query_statement.executeQuery(get_message);
        String hashedPassword = Crypt.crypt(given_password, rs.getString("Salt"));
        if (rs.getString("Password").equals(hashedPassword)) {
            return true;
        }
        return false;
    }

    public synchronized boolean addUser(JSONObject user) {
        //preps the salt
        byte bytes[] = new byte[13];
        secure_random.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes; 

        //saves the data to db
        try {
            String password = user.getString("password");
            String hashedPassword = Crypt.crypt(password, salt);
            String new_user = "INSERT INTO Users " + "VALUES('"
					+ user.getString("username") + "','" + hashedPassword + "','" + user.getString("email") + "','" + salt + "')";
            Statement create_statment = connection.createStatement();
            create_statment.executeUpdate(new_user);
            create_statment.close();
            return true;
        }
        catch(Exception e) { //username already exists
            return false;
        }
    }

    public void closeDB() throws SQLException {
		if (null != connection) {
			connection.close();
            System.out.println("Closing register database connection");
			connection = null;
		}
    }
}
