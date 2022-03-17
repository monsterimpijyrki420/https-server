package com.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class Server {

    private Server() {
    }

    private static SSLContext coordinateServerSSLContext(String path, String password) throws Exception {
        char[] passphrase = password.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(path), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }

    public static void main(String[] args) { // throws Exception
        try {
            System.out.println("Starting up the server...");
            System.out.println("");
            String db_path = ""; //for gitlab
            String register_db_name = "RegisterDB.db";
            String coordinate_db_name = "CoordinateDB.db";

            CoordinateDatabase coord_db_handler = CoordinateDatabase.getInstance(db_path, coordinate_db_name);
            RegisterDatabase reg_db_handler = RegisterDatabase.getInstance(db_path, register_db_name);
            UserAuthenticator authchecker = new UserAuthenticator("coordinates", reg_db_handler);

            //create the http server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            //create contexts
            HttpContext context = server.createContext("/coordinates", new CoordinatesHandler(coord_db_handler));
            context.setAuthenticator(authchecker);

            HttpContext commentContext = server.createContext("/comment", new CommentHandler(coord_db_handler));
            commentContext.setAuthenticator(authchecker);

            HttpContext regContext = server.createContext("/registration", new RegistrationHandler(reg_db_handler));

            SSLContext sslContext = coordinateServerSSLContext(args[0], args[1]);

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            // creates a default executor
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            //handles closing down the server
            boolean server_on = true;
            Scanner scanner = new Scanner(System.in);
            while (server_on == true){
                String quit = scanner.nextLine();
                if (quit.equals("/quit")) {
                    server_on = false;
                }
            }
            coord_db_handler.closeDB();
            reg_db_handler.closeDB();
            System.out.println("Closing the server");
            server.stop(3);

        } catch (FileNotFoundException e) {
            // Certificate file not found!
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
