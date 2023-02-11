package dk.itu.minitwit.database;

import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SQLite {

//    private final String DATABASE_URL = "/tmp/minitwit.db";
    private final String DATABASE_URL = "minitwit.db";
    private final boolean DEBUG = true;
    private final String SECRET_KEY = "development key";

    private Connection connectDb() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DATABASE_URL);
    }


    public void initDb() throws SQLException {
        try (Connection conn = connectDb()) {
            List<String> schemaStatements = readSchemaStatements();
            Statement statement = conn.createStatement();
            for (String schemaStatement : schemaStatements) {
                statement.executeUpdate(schemaStatement);
            }
            conn.commit();
        }
    }

    private List<String> readSchemaStatements() {
        List<String> schemaStatements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("schema.sql")))) {
            StringBuilder statement = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.endsWith(";")) {
                    statement.append(line).append("\n");
                } else {
                    statement.append(line);
                    schemaStatements.add(statement.toString());
                    statement = new StringBuilder();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read the schema.sql file", e);
        }
        return schemaStatements;
    }

    public List<Map<String, Object>> queryDb(String query, List<Object> args) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                stmt.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        }
        return result;
    }


    private Integer getUserId(String username) throws SQLException {
        Integer userId = null;
        List<Map<String, Object>> results = queryDb("select user_id from user where username = ?", List.of(username));
        if (!results.isEmpty()) {
            userId = (Integer) results.get(0).get("user_id");
        }
        return userId;
    }

}
