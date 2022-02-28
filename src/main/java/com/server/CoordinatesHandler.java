package com.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONArray;

public class CoordinatesHandler implements HttpHandler {
    private CoordinateDatabase db_handler;

    //this list keeps track of which coordinates have not been sent after GET-method
    private ArrayList<UserCoordinate> coordinates_list = new ArrayList<UserCoordinate>();

    public CoordinatesHandler(CoordinateDatabase db_handler) {
        this.db_handler = db_handler;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
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

                //saves data, sends error if data wasnt sufficent
                try {
                    JSONObject received_coordinates = new JSONObject(message);
                    String nick = received_coordinates.getString("username");
                    String longitude = received_coordinates.getString("longitude");
                    String latitude = received_coordinates.getString("latitude");
                    String timestamp = received_coordinates.getString("sent");
                    UserCoordinate new_uc = new UserCoordinate(nick, longitude, latitude, 
                            timestamp, db_handler);

                    new_uc.addToDB(); //throws exception if data is bad

                    coordinates_list.add(new_uc);
                    code = 200;
                    System.out.println("Coordinates received successfully");
                } 
                catch (Exception e){  //not correct data
                    System.out.println("Bad data: " + e.toString());
                    code = 403;
                    response = "Data was not ok";
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

                //puts data (if any) into JSONArray
                if (coordinates_list.isEmpty()) {
                    System.out.println("Empty coordinates list requested");
                    code = 204;
                } else {
                    JSONArray message_array = new JSONArray(); //jsonarray that is sent
                    for (UserCoordinate data : coordinates_list) {

                        JSONObject coordinate_obj = new JSONObject();
                        try {
                            System.out.println("");
                            System.out.println("Requesting coordinates from database...");
                            coordinate_obj = data.getFromDB();
                        }
                        catch(Exception e) {
                            System.out.println("Failed to get coordinates");
                        }
                        message_array.put(coordinate_obj);
                    }
                    coordinates_list.clear();

                    response = message_array.toString();
                    code = 200;
                    System.out.println("Coordinates sent successfully");
                }
            } else {
                System.out.println("Fail: Bad content type");
                response = "Bad content type";
                code = 411;
            }

        } else {
            // Inform user here that only POST and GET functions are supported and send an error code
            System.out.println("Fail: Bad HTTP method");
            code = 400;
            response = "This HTTP method is not supported";
        }

        //sends the message
        if (response.isEmpty()) { //no message needed
            t.sendResponseHeaders(code, -1);
        } else { //something went wrong
            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders(code, response.length());
            os.write(response.getBytes());
            os.flush();
            os.close();
        }
    }
}