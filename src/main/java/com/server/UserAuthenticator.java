package com.server;

import com.sun.net.httpserver.BasicAuthenticator;

class UserAuthenticator extends BasicAuthenticator {
    private CoordinateDatabase db_handler;

    public UserAuthenticator (String realm, CoordinateDatabase db_handler){
        super(realm);
        this.db_handler = db_handler;
    }

    public boolean checkCredentials(String username, String password) {
        System.out.println("");
        System.out.println("Checking credentials...");

        //checks password matches to name from database
        try {
            if (db_handler.checkCredentials(username, password)) {
                System.out.println("Authentication confirmed");
                return true;
            } else {
                System.out.println("Wrong password");
                return false;
            }
        }
        catch (Exception e) {
            System.out.println("User does not exist");
            return false;
        }
    }
}