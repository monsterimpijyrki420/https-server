package com.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class UserCoordinate {
    private String nick, latitude, longitude;
    private LocalDateTime timestamp;
    private CoordinateDatabase db_handler;

    public UserCoordinate(String nick, String longitude, String latitude,
                String timestamp, CoordinateDatabase db_handler) {
        this.nick = nick;
        this.latitude = latitude;
        this.longitude = longitude;
        this.db_handler = db_handler;

        //transforms time from string to localdatetime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        this.timestamp = LocalDateTime.parse(timestamp, formatter);
    }


    public void addToDB() throws SQLException {
        db_handler.addCoordinates(nick, longitude, latitude, dateAsInt());
    }

    public JSONObject getFromDB() throws SQLException {
        ResultSet rs = db_handler.getCoordinates();
        JSONObject coordinate_obj = new JSONObject();
        coordinate_obj.put("username", rs.getString("Username"));
        coordinate_obj.put("latitude", rs.getString("Latitude"));
        coordinate_obj.put("longitude", rs.getString("Longitude"));

        setTimestamp(rs.getLong("timestamp"));
        coordinate_obj.put("sent", timestamp);

        return coordinate_obj;
    }
    
    private long dateAsInt() {
        //transforms time to numeral for database
        return timestamp.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    private void setTimestamp(long epoch) {
        //transforms time back from numeral
        timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    } 
}
    