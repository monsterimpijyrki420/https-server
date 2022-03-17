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

public class CommentHandler implements HttpHandler {
    private final CoordinateDatabase db_handler;

    public CommentHandler(CoordinateDatabase db_handler) {
        this.db_handler = db_handler;
    }

    @Override
    public void handle(HttpExchange t) throws IOException {
        System.out.println("");
        System.out.println("Comment add request handled in thread " + Thread.currentThread().getId()); 
        Headers headers = t.getRequestHeaders();
        int code;
        String response = "";

        if (t.getRequestMethod().equalsIgnoreCase("POST")) {

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

                //saves data to db, sends error if data wasnt sufficent
                try {
                    JSONObject received_obj = new JSONObject(message);
                    int id = received_obj.getInt("id");
                    String comment = received_obj.getString("comment");
                    String timestamp = received_obj.getString("sent");

                    db_handler.addComment(id, comment, timestamp);
                    code = 200;
                    System.out.println("Comment added successfully");
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
            //Informs user that only POST functions are supported and send an error code
            System.out.println("Fail: Bad HTTP method");
            code = 400;
            response = "This HTTP method is not supported";
        }

        //sends the message
        if (response.isEmpty()) { //no message needed
            t.sendResponseHeaders(code, -1);
        } else {
            OutputStream os = t.getResponseBody();
            byte[] response_bytes = response.getBytes("UTF-8");
            t.sendResponseHeaders(code, response_bytes.length);
            os.write(response_bytes);
            os.flush();
            os.close();
        }
    }
}
