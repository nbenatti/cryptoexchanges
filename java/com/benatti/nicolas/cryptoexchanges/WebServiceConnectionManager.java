package com.benatti.nicolas.cryptoexchanges;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

//TODO: dovrà diventare a tutti gli effetti un wrapper delle funzionalità del web service
public class WebServiceConnectionManager {

    private final String USER_AGENT = "Mozilla/5.0";

    private ArrayList<String> responses;    // JSON data returned by the lasy executed query

    private static final WebServiceConnectionManager ourInstance = new WebServiceConnectionManager();

    public static WebServiceConnectionManager getInstance() {
        return ourInstance;
    }

    private WebServiceConnectionManager() {
        responses = new ArrayList<>();
    }

    public void sendGetRequest(String url) {

        URL obj = null;
        HttpsURLConnection conn = null;

        try {
            obj = new URL(url);
        }
        catch (MalformedURLException e) {

            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }

        try {
            conn = (HttpsURLConnection) obj.openConnection();
        }
        catch(IOException e) {

            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }

        try {
            conn.setRequestMethod("GET");
        }
        catch (ProtocolException e) {

            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }

        conn.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = 0;

        try {
            responseCode = conn.getResponseCode();
        }
        catch (IOException e) {

            System.out.println("ERROR: " + e.getMessage());
            //System.exit(1);
        }

        System.out.println("response code from " + url + ": " + responseCode);

        BufferedReader buf = null;
        String inputLine = null;
        StringBuffer response = null;

        try {
            buf = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            response = new StringBuffer(); // JSON coming from the server

            while( (inputLine = buf.readLine()) != null) {

                response.append(inputLine);
            }
        }
        catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }


        try {
            buf.close();
        }
        catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            System.exit(1);
        }

        this.responses.add(response.toString());
    }

    /*private void getMarketStatus() {


    }*/

    // cancella tutti i risultati rimasti
    public void clearCache() {

        responses.clear();
    }

    public ArrayList<String> getResponses() {

        return responses;
    }

    /*public String getQueryResult() {
        return returnedJSON;
    }*/
}
