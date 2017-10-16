package io.pivotal.gcp.cloudsql;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

@RestController
public class CloudsqlController {

    private String instanceConnectionName;
    private String databaseName;
    private String username;
    private String password;

    private Connection connection;

    private static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";
    private static final String GCP_CREDENTIALS_FILE_NAME = "GCP_credentials.json";

    private void parseVcapServices() {
        // 1. Get the MySQL details.
        JSONObject mySqlCred = getCredObj("google-cloudsql-mysql");
        String projectId = null; // Get this later, from a different service binding
        String region = System.getenv("CLOUDSQL_REGION"); // FIXME: Get this from CloudSQL service binding
        String instanceName = mySqlCred.getString("instance_name");
        this.databaseName = mySqlCred.getString("database_name");
        this.username = mySqlCred.getString("Username");
        this.password = mySqlCred.getString("Password");

        // 2. Get remaining parts from Storage binding.
        JSONObject storageCred = getCredObj("google-storage");
        projectId = storageCred.getString("ProjectId");
        this.instanceConnectionName = String.join(":", projectId, region, instanceName);

        // 3. Write out the GOOGLE_APPLICATION_CREDENTIALS file and set up environment variable.
        String privateKeyData = storageCred.getString("PrivateKeyData");
        setupCredentials(privateKeyData);
    }

    private JSONObject getCredObj (String vcapKey) {
        String env = System.getenv("VCAP_SERVICES");
        JSONObject json = new JSONObject(env);
        JSONArray root = json.getJSONArray(vcapKey);
        JSONObject obj0 = root.getJSONObject(0);
        return obj0.getJSONObject("credentials");
    }

    private void setupCredentials(String privateKey) {
        InputStream in = new ByteArrayInputStream(Base64.getDecoder().decode(privateKey));
        File gcpJsonFile = new File(System.getProperty("java.io.tmpdir"), GCP_CREDENTIALS_FILE_NAME);
        writeInputStreamToFile(in, gcpJsonFile);

        Map<String, String> replEnv = new HashMap<>();
        replEnv.put(GOOGLE_APPLICATION_CREDENTIALS, gcpJsonFile.getPath());
        setEnv(replEnv);
    }

    private static void writeInputStreamToFile (InputStream is, File outFile) {
        try {
            FileCopyUtils.copy(is, new FileOutputStream(outFile));
        } catch (IOException e) {
            throw new RuntimeException("Failed while creating " + GCP_CREDENTIALS_FILE_NAME + " file", e);
        }
    }

    private static void setEnv(Map<String, String> newenv) {
        try {
            Class<?>[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class<?> cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed while setting " + GOOGLE_APPLICATION_CREDENTIALS + " environment variable.", e);
        }
    }

    public CloudsqlController() {
        super();
        System.out.println("In the constructor");
        parseVcapServices();
        String jdbcUrl = String.format(
                "jdbc:mysql://google/%s?cloudSqlInstance=%s&"
                        + "socketFactory=com.google.cloud.sql.mysql.SocketFactory",
                databaseName,
                instanceConnectionName);
        try {
            connection = DriverManager.getConnection(jdbcUrl, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping("/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @RequestMapping("/showTables")
    public List<String> showTables() throws SQLException {

        List<String> rv = new ArrayList<>();
        rv.add("Greetings from showTables");
    
        try (Statement statement = connection.createStatement()) {
          ResultSet resultSet = statement.executeQuery("SHOW TABLES");
          while (resultSet.next()) {
            rv.add(resultSet.getString(1));
          }
        }
        return rv;
    }

    @RequestMapping("/now")
    public List<String> now() throws SQLException {

        List<String> rv = new ArrayList<>();
        rv.add("Greetings from now");

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT NOW()");
            while (resultSet.next()) {
                //System.out.println(resultSet.getString(1));
                rv.add(resultSet.getString(1));
            }
        }
        return rv;
    }

}
