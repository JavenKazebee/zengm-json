package com.krazytar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONArray;

public class Main {
    // App name
    private static final String APPLICATION_NAME = "ZenGMJSON";
    // Directory to store user credentials
    private static final File  DATA_STORE_DIR = new File(System.getProperty("user.home"), ".credentials/sheets.googleapis.com-java-zengmjson");
    // Global instance of DataStoreFactory
    private static DataStoreFactory DATA_STORE_FACTORY;
    // Global instance of JSONFactory
    private static final com.google.api.client.json.JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    // Global Instance of HTTPTransport
    private static HttpTransport HTTP_TRANSPORT;
    
    static int numOfFA = 0;
    
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
    
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS);
    
    public static List<List<Object>> parse(String s) {
        JSONParser parser = new JSONParser();
        List<List<Object>> freeAgents = new ArrayList();
        
        try {
            Object obj = parser.parse(new FileReader(s));
            JSONObject jobj = (JSONObject) obj;
            
            JSONArray players = (JSONArray) jobj.get("players");
            for (Object o : players) {
                freeAgents.add(parsePlayerObject((JSONObject) o));
            }
        } catch (IOException | ParseException ioe) {
            
        }
        return freeAgents;
    }
    
    
    public static void main(String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        System.out.println("Please enter your spreadsheet ID:");
        String spreadsheetID = in.nextLine();
        List<List<Object>> freeAgents = parse("data.json");
        List<Object> test = new ArrayList();
        Sheets service = getSheetsService();

        ValueRange body;

        body = new ValueRange().setValues(freeAgents);
        UpdateValuesResponse result = service.spreadsheets().values().update(spreadsheetID, "Sheet9!A:E", body).setValueInputOption("RAW").execute();
        System.out.println(numOfFA + " players added to spreadsheet.");
        
    }
    
    public static List<Object> parsePlayerObject(JSONObject obj) {
        JSONArray ratings = (JSONArray) obj.get("ratings");
        JSONObject born = (JSONObject) obj.get("born");
        List<Object> players = new ArrayList();
        JSONObject rat = null;
        int ovr, pot;
        for (Object r : ratings) {
            rat = (JSONObject) r;
        }
        if(obj.get("tid").equals(new Long(-1))) {
            players.add(obj.get("name"));
            players.add(obj.get("pos"));
            players.add(born.get("year"));
            players.add(rat.get("ovr"));
            players.add(rat.get("pot"));
            numOfFA++;
        }
        return players;
    }
    
    
    public static Credential authorize() throws IOException {
        InputStream in = Main.class.getResourceAsStream("client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        
        System.out.println("Credentials saved to: " + DATA_STORE_DIR.getAbsolutePath());
        
        return credential;
    }
    
    public static Sheets getSheetsService() throws IOException{
        Credential credential = authorize();
        return new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
}
