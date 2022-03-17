package com.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;

public class CoordinatesHandler implements HttpHandler {
    private final CoordinateDatabase db_handler;

    public CoordinatesHandler(CoordinateDatabase db_handler) {
        this.db_handler = db_handler;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        System.out.println("");
        System.out.println("Coordinate request handled in thread " + Thread.currentThread().getId()); 
        Headers headers = t.getRequestHeaders();
        int code;
        String response = "";

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {

            System.out.println("");
            System.out.println("POST coordinates message received");

            //checks for header
            if (!headers.containsKey("Content-Type")) {
                System.out.println("Fail: No content type");
                code = 411;
                response = "No content type";
            }

            //checks for correct header
            else if (headers.get("Content-Type").get(0) 
                .equalsIgnoreCase("application/json")) {

                //receives data
                InputStream is = t.getRequestBody();
                String message = new BufferedReader(new InputStreamReader(is,
                        StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                message = message + "\n";
                is.close();

                JSONObject received_coordinates = new JSONObject(message);

                if (received_coordinates.has("query")) { //query post
                    try {
                        String parameter = received_coordinates.getString("query");

                        if (parameter.equals("user")) {
                            String username = received_coordinates.getString("nickname");
                            JSONArray coord_array = db_handler.getCoordinates(username);
                            response = coord_array.toString();
                            code = 200;
                            System.out.println("User query handled succesfully");
                        }

                        else if (parameter.equals("time")) {
                            String time_start = received_coordinates.getString("timestart");
                            String time_end = received_coordinates.getString("timeend");
                            JSONArray coord_array = db_handler.getCoordinates(time_start, time_end);
                            response = coord_array.toString();
                            code = 200;
                            System.out.println("Time query handled succesfully");
                        }

                        else if (parameter.equals("location")) {
                            double max_long = received_coordinates.getDouble("uplongitude");
                            double min_long = received_coordinates.getDouble("downlongitude");
                            double max_lat = received_coordinates.getDouble("uplatitude");
                            double min_lat = received_coordinates.getDouble("downlatitude");
                            JSONArray coord_array = db_handler.getCoordinates(max_long, min_long, max_lat, min_lat);
                            response = coord_array.toString();
                            code = 200;
                            System.out.println("Location query handled succesfully");
                        }

                        else {
                            code = 413;
                            response = "Parameter not supported";
                            System.out.println("Parameter not supported");
                        }
                    }
                    catch (Exception e) {
                        System.out.println("Error: " + e.toString());
                        code = 403;
                        response = "Data was not ok";
                    }
                }

                else{ //not query

                    //saves data to db, sends error if data wasnt sufficent
                    try {
                        String nick = received_coordinates.getString("username");
                        double longitude = received_coordinates.getDouble("longitude");
                        double latitude = received_coordinates.getDouble("latitude");
                        String timestamp = received_coordinates.getString("sent");
                        String description = "nodata";
                        String sender = getSendersName(headers);

                        try {
                            description = received_coordinates.getString("description");
                        }
                        catch (Exception e) {}

                        if (received_coordinates.has("id")) { //edit post
                            try {
                                int id = received_coordinates.getInt("id");
                                if (db_handler.editCoordinates(id, nick, longitude, latitude, timestamp, description, sender)) {
                                    code = 200;
                                    System.out.println("Coordinates edited successfully");
                                }
                                else {
                                    code = 403;
                                    response = "No right to edit";
                                    System.out.println("Edit request failed, no right to edit");
                                }
                            } catch (SQLException e) {
                                code = 403;
                                response = "ID does not exist";
                                System.out.println("Edit request failed, ID doesn't exist");
                            }
                        }

                        else { //'normal' adding post
                            try {
                                String comment = received_coordinates.getString("comment");
                                db_handler.addCoordinates(nick, longitude, latitude, timestamp, description, sender, comment);
                            }
                            catch (Exception e) {
                                db_handler.addCoordinates(nick, longitude, latitude, timestamp, description, sender);
                            }
    
                            code = 200;
                            System.out.println("Coordinates received successfully");
                        }

                    } 
                    catch (Exception e){ //not correct data
                        System.out.println("Bad data: " + e.toString());
                        code = 403;
                        response = "Data was not ok";
                    }
                }

            } else {
                System.out.println("Fail: Bad content type");
                response = "Bad content type";
                code = 411;
            }

        } else if (t.getRequestMethod().equalsIgnoreCase("GET")) {
            
            System.out.println("");
            System.out.println("GET coordinates message received");

            //checks for header
            if (!headers.containsKey("Content-Type")) {
                System.out.println("Fail: No content type");
                code = 411;
                response = "No content type";
            }

            //checks for correct header
            else if (headers.get("Content-Type").get(0) 
            .equalsIgnoreCase("application/json")) {

                try {
                    JSONArray message_array = new JSONArray(); //jsonarray that will be sent
                    message_array = db_handler.getCoordinates();

                    if (message_array.toString().equals("[]")) {
                        System.out.println("Empty coordinates list requested");
                        code = 204;
                    }
                    else {
                        response = message_array.toString();
                        code = 200;
                        System.out.println("Coordinates sent successfully");
                    }
                }
                catch (SQLException e) {
                    response = "Failed to get coordinates";
                    code = 407;
                    System.out.println("Failed to get coordinates");
                }

            } else {
                System.out.println("Fail: Bad content type");
                response = "Bad content type";
                code = 411;
            }

        } else if (t.getRequestMethod().equalsIgnoreCase("DELETE")) {

            System.out.println("");
            System.out.println("DELETE coordinates message received");

            //checks for header
            if (!headers.containsKey("Content-Type")) {
                System.out.println("Fail: No content type");
                code = 411;
                response = "No content type";
            }

            //checks for correct header
            else if (headers.get("Content-Type").get(0) 
                .equalsIgnoreCase("application/json")) {

                //receives data
                InputStream is = t.getRequestBody();
                String message = new BufferedReader(new InputStreamReader(is,
                        StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                message = message + "\n";
                is.close();

                try {
                    JSONObject delete_obj = new JSONObject(message);
                    int id = delete_obj.getInt("id");
                    String sender = getSendersName(headers);

                    if (db_handler.deleteData(id, sender)){
                        code = 200;
                        System.out.println("Coordinates deleted successfully");
                    } else {
                        System.out.println("No user match, deletion denied");
                        code = 409;
                        response = "No right to delete";
                    }
                    
                } 
                catch (Exception e){ //not correct data
                    System.out.println("Bad data: " + e.toString());
                    code = 403;
                    response = "Data was not ok";
                }

            } else {
                System.out.println("Fail: Bad content type");
                response = "Bad content type";
                code = 411;
            }


        } else {
            //Inform user here that only POST and GET functions are supported and send an error code
            System.out.println("Fail: Bad HTTP method");
            code = 400;
            response = "This HTTP method is not supported";
        }

        //sends the message
        if (response.isEmpty()) { //no message needed
            t.sendResponseHeaders(code, -1);
        } else { //something went wrong
            OutputStream os = t.getResponseBody();
            byte[] response_bytes = response.getBytes("UTF-8");
            t.sendResponseHeaders(code, response_bytes.length);
            os.write(response_bytes);
            os.flush();
            os.close();
        }
    }

    //gets the senders username (the one from registration)
    private String getSendersName(Headers header) {
        String auth = header.getFirst ("Authorization");
        byte[] b = Base64.getDecoder().decode(auth.substring(auth.indexOf (' ') + 1));
        String userpass = new String (b);
        int colon = userpass.indexOf (':');
        String username = userpass.substring (0, colon);
        return username;
    }
}
