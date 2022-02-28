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
import java.util.stream.Collectors;
import org.json.JSONObject;

public class RegistrationHandler implements HttpHandler {
    private CoordinateDatabase db_handler;

    public RegistrationHandler (CoordinateDatabase db_handler){
        this.db_handler = db_handler;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        Headers headers = t.getRequestHeaders();
        String response = "";
        int code; //error code

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {

            System.out.println("");
            System.out.println("Registration request received");

            //checks for content type
            if (!headers.containsKey("Content-Type")) {
                System.out.println("Fail: No content type");
                code = 411;
                response = "No content type";
            }

            //checks for CORRECT content type
            else if (headers.get("Content-Type").get(0) 
                    .equalsIgnoreCase("application/json")) {

                //receive data
                InputStream is = t.getRequestBody();
                String message = new BufferedReader(new InputStreamReader(is,
                    StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
                is.close();

                //handle data
                try {
                    JSONObject user_obj = new JSONObject(message);

                    //checks the data exists
                    String username = user_obj.getString("username");
                    String password = user_obj.getString("password");
                    String email = user_obj.getString("email");
                    if (username.isEmpty() || password.isEmpty() || email.isEmpty()){
                        throw new Exception("Empty string");
                    }
                
                    //adds user if possible
                    if (db_handler.addUser(user_obj)) {
                        code = 200;
                        System.out.println("User added to database");
                        System.out.println("Registration successful");
                    } else { //username was already registered
                        code = 405;
                        response = "User already registered";
                        System.out.println("User already exist");
                    }
                }
                catch (Exception e) { //not right key-values or not json format or smth
                    System.out.println("Fail: Bad data");
                    code = 403;
                    response = "Data was not ok";
                }
            } else {
                System.out.println("Fail: Bad content type");
                response = "Bad content type";
                code = 411;
            }

        } else {
            System.out.println("Fail: Bad HTTP method");
            code = 400;
            response = "This HTTP method is not supported";
        }
        //sends the response
        if (response.isEmpty()) { //no message needed
            t.sendResponseHeaders(200, -1);
        } else { //something went wrong
            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders(code, response.length());
            os.write(response.getBytes());
            os.flush();
            os.close();
        }
    }
}