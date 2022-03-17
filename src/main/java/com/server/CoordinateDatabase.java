package com.server;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

public class CoordinateDatabase {
    private static CoordinateDatabase coordinate_db = null;
    private Connection connection;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    private CoordinateDatabase(String path, String db_name) {
        try {
            open(path, db_name);
        } catch (SQLException e) {
            System.out.println("Something went wrong with databases");
        }
    }

    public static synchronized CoordinateDatabase getInstance(String path, String db_name) {
        if (coordinate_db == null) {
            coordinate_db = new CoordinateDatabase(path, db_name);
        }
        return coordinate_db;
    }

    private void open(String path, String coordinate_db_name) throws SQLException  {
    
        //checks if database exists
        File coordinate_file = new File(path + coordinate_db_name);
        boolean coordinate_exists = coordinate_file.exists() && !coordinate_file.isDirectory();

        //makes the connection to database
        String full_path = "jdbc:sqlite:" + path + coordinate_db_name;
        connection = DriverManager.getConnection(full_path);

        //creates database if there isnt one
        if (!coordinate_exists) {
            System.out.println("Creating coordinate database...");
            if (initializeDatabase()) {
                System.out.println("Coordinate database successfully created");
            } else {
                System.out.println("Coordinate database creation failed");
            }
        } else {
            System.out.println("Coordinate Database detected");
        }

    }

    private boolean initializeDatabase() throws SQLException {
        if (null != connection) {
            String coordinate_table = "CREATE TABLE Coordinates (ID INT PRIMARY KEY, Username VARCHAR(50), Longitude DOUBLE," +
                                                    " Latitude DOUBLE, Timestamp NUMERIC, Description VARCHAR(1024)," +
                                                    " Modified Numeric, Sender VARCHAR)";
            String comment_table = "CREATE TABLE Comments (ID INT, Comment VARCHAR, Timestamp NUMERIC, " +
                                        "PRIMARY KEY(ID, Comment, Timestamp), FOREIGN KEY(ID) REFERENCES Coordinates(ID))";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(coordinate_table);
            stmt.executeUpdate(comment_table);
            stmt.close();
            return true;
        }
        return false;
    }

    //handles all adding and editing of coordinates. keeps database safe (with synchronized)
    private synchronized void editCoordDB(String sql_command) throws SQLException {
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql_command);
        stmt.close();
    }

    public void addCoordinates(String nick, double longitude, double latitude,
                            String sent, String description, String sender) throws SQLException{

        int id = getNewID();

        String new_coordinates = "INSERT INTO Coordinates(ID, Username, Longitude, Latitude, Timestamp, Description, Sender) " + 
                                    "VALUES('" + id + "','" + nick + "','" + longitude + "','" + latitude + "','" + 
                                    dateAsInt(sent) + "','" + description + "','" + sender + "')";
        editCoordDB(new_coordinates);
    }

    public void addCoordinates(String nick, double longitude, double latitude,
                            String sent, String description, String sender, String comment) throws SQLException{

        int id = getNewID();
        String new_coordinates = "INSERT INTO Coordinates(ID, Username, Longitude, Latitude, Timestamp, Description, Sender) " + 
                                    "VALUES('" + id + "','" + nick + "','" + longitude + "','" + latitude + "','" + 
                                    dateAsInt(sent) + "','" + description + "','" + sender + "')";
        editCoordDB(new_coordinates);
        addComment(id, comment, sent);
    }

    public synchronized void addComment(int id, String comment, String sent) throws SQLException {
        String new_comment = "INSERT INTO Comments VALUES('" + id + "','" + comment + "','" + dateAsInt(sent) + "')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(new_comment);
        stmt.close();
    }

    public JSONArray getCoordinates() throws SQLException {
        String get_msg = "SELECT ID, Username, Longitude, Latitude, Timestamp, Description, Modified FROM Coordinates";
        return actuallyGetCoordinates(get_msg);
    }

    //for username query
    public JSONArray getCoordinates(String value) throws SQLException {
        String get_msg = "SELECT ID, Username, Longitude, Latitude, Timestamp, Description, Modified FROM Coordinates "
                            + "WHERE Username = '" + value + "'";
        return actuallyGetCoordinates(get_msg);
    }

    //for time query
    public JSONArray getCoordinates(String time_start, String time_end) throws SQLException {
        String get_msg = "SELECT ID, Username, Longitude, Latitude, Timestamp, Description, Modified FROM Coordinates "
                        + "WHERE Timestamp > '" + dateAsInt(time_start) + "' AND Timestamp < '" + dateAsInt(time_end) + "'";
        return actuallyGetCoordinates(get_msg);
    }

    //for location query
    public JSONArray getCoordinates(double max_long, double min_long, double max_lat, double min_lat) throws SQLException {
        String get_msg = "SELECT ID, Username, Longitude, Latitude, Timestamp, Description, Modified FROM Coordinates "
                        + "WHERE Longitude > " + min_long + " AND Longitude < " + max_long + ""
                        + " AND Latitude > " + min_lat + " AND Latitude < " + max_lat + "";
        return actuallyGetCoordinates(get_msg);
    }

    private JSONArray actuallyGetCoordinates(String get_msg) throws SQLException {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(get_msg);

        JSONArray message_array = new JSONArray();
        //gets every entity of the database
        while(rs.next()) {

            //makes sure coordinates has not been deleted
            if(!rs.getString("Latitude").equals("<deleted>")) {

                int id = rs.getInt("ID");
                String username = rs.getString("Username");
                double latitude = rs.getDouble("Latitude");
                double longitude = rs.getDouble("Longitude");
                LocalDateTime timestamp = setTimestamp(rs.getLong("Timestamp"));
                long edited_ts = rs.getLong("modified");

                JSONObject coordinate_obj = new JSONObject();
                coordinate_obj.put("id", id);
                coordinate_obj.put("username", username);
                coordinate_obj.put("latitude", latitude);
                coordinate_obj.put("longitude", longitude);

                //puts the utc version of time to obj
                coordinate_obj.put("sent", ZonedDateTime.of(timestamp, ZoneOffset.UTC));

                //doesnt add 'nodata' -description
                String description = rs.getString("Description");
                if (!description.equals("nodata")) {
                    coordinate_obj.put("description", description);
                }

                //adds possible comments
                JSONArray comments = getComments(id);
                if (!comments.toString().equals("[]")) {
                    coordinate_obj.put("comments", comments);
                }
                
                //adds possible modifed-tag
                if (edited_ts != 0) {
                    LocalDateTime modified = setTimestamp(edited_ts);
                    coordinate_obj.put("modified", ZonedDateTime.of(modified, ZoneOffset.UTC));
                }

                message_array.put(coordinate_obj);
            }
        }
        return message_array;
    }

    //edits coordinate data, returns true if success
    public boolean editCoordinates(int id, String nick, double longitude, double latitude,
                    String sent, String description, String sender) throws SQLException {

        boolean permission = authorizeEditing(id, sender);
        if (permission) {
            String update_msg = "UPDATE Coordinates SET Username = '" + nick + "', Longitude = '" + longitude + "', Latitude = '" + 
                        latitude + "', Description = '" + description + "', Modified = '" + dateAsInt(sent) + "' WHERE ID = '" + id + "'";
            editCoordDB(update_msg);
        }
        return permission;
    }

    //"delets" the data -> edits longitude and latitude to '<delted>'
    //returns true if deletion was succesful
    public boolean deleteData(int id, String sender) throws SQLException {
        if (authorizeEditing(id, sender)) {

            //gets current time
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            String date_text = now.format(formatter); 
            long timestamp = dateAsInt(date_text);

            String update_msg = "UPDATE Coordinates SET Longitude = '<deleted>', Latitude = '<deleted>', " +
                                "Timestamp = '" + timestamp + "' WHERE ID = '" + id + "'";
            editCoordDB(update_msg);
            return true;
        }
        return false;
    }

    private JSONArray getComments(int id) throws SQLException {
        Statement stmt = connection.createStatement();
        String get_msg = "SELECT Comment, Timestamp FROM Comments WHERE ID = '" + id + "'";
        ResultSet rs = stmt.executeQuery(get_msg);

        JSONArray comment_array = new JSONArray();
        while (rs.next()){
            JSONObject comment_obj = new JSONObject();
            String comment = rs.getString("comment");
            LocalDateTime timestamp = setTimestamp(rs.getLong("Timestamp"));

            comment_obj.put("id", id);
            comment_obj.put("comment", comment);
            comment_obj.put("sent", ZonedDateTime.of(timestamp, ZoneOffset.UTC));
            comment_array.put(comment_obj);
        }
        return comment_array;
    }

    //gets the biggest id and makes one bigger
    private synchronized int getNewID() throws SQLException {
        int id;
        String get_msg = "SELECT ID FROM Coordinates ORDER BY ID DESC";
        Statement stmt = connection.createStatement();

        try {
            ResultSet rs = stmt.executeQuery(get_msg);
            id = rs.getInt("ID") + 1;
        }
        catch (SQLException e) { //db empty -> starts with id 1
            id = 1;
        }
        return id;
    }

    //tests if user is allowed to edit/delete -> checks id match and that entry isnt deleted
    private boolean authorizeEditing(int id, String sender) throws SQLException {
        String get_msg = "SELECT Sender, Latitude FROM Coordinates WHERE ID = '" + id + "'";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(get_msg);
        String og_sender = rs.getString("Sender");
        String latitude = rs.getString("Latitude");
        if (sender.equals(og_sender) && !latitude.equals("<deleted>")) {
            return true;
        }
        return false;
    }

    //makes time a numeric value for databases
    private long dateAsInt(String sent) {
        
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        LocalDateTime timestamp = LocalDateTime.parse(sent, formatter);
    
        //transforms time to numeral for database
        return timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    //transforms time back from numeral
    private LocalDateTime setTimestamp(long epoch) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    public void closeDB() throws SQLException {
		if (null != connection) {
			connection.close();
            System.out.println("Closing coordinate database connection");
			connection = null;
		}
    }
}
