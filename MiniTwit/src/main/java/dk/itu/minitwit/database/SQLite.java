package dk.itu.minitwit.database;

import dk.itu.minitwit.controller.MiniTwitController;
import dk.itu.minitwit.domain.Register;
import dk.itu.minitwit.domain.SimData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

@Component
public class SQLite {


//    private final String DATABASE_URL = "minitwit.db";


    private final String DATABASE_URL = "mysql://db-minitwit-do-user-13625042-0.b.db.ondigitalocean.com:25060/defaultdb?ssl-mode=REQUIRED";
    private final boolean DEBUG = true;
    private final String SECRET_KEY = "development key";

    @Autowired
    private PasswordEncoder passwordEncoder;

    Logger logger = LoggerFactory.getLogger(MiniTwitController.class);


     private Connection connectDb() throws SQLException, ClassNotFoundException {
//         Class.forName("com.mysql.cj.jdbc.Driver");
         return DriverManager.getConnection("jdbc:" + DATABASE_URL, "doadmin", "AVNS_W7vkzWZhBrw3fUsgp71");
     }



//    public void initDb() throws SQLException {
//        try (Connection conn = connectDb()) {
//            List<String> schemaStatements = readSchemaStatements();
//            Statement statement = conn.createStatement();
//            for (String schemaStatement : schemaStatements) {
//                statement.executeUpdate(schemaStatement);
//            }
//            conn.commit();
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private List<String> readSchemaStatements() {
//        List<String> schemaStatements = new ArrayList<>();
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("schema.sql")))) {
//            StringBuilder statement = new StringBuilder();
//            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
//                if (!line.endsWith(";")) {
//                    statement.append(line).append("\n");
//                } else {
//                    statement.append(line);
//                    schemaStatements.add(statement.toString());
//                    statement = new StringBuilder();
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to read the schema.sql file", e);
//        }
//        return schemaStatements;
//    }

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
        } catch (ClassNotFoundException e) {
            logger.error("Encountered error connecting to database: " +e.getMessage() + "\n"+ Arrays.toString(e.getStackTrace()));
        }
        return result;
    }

    public int updateDb(String query, List<Object> args) throws SQLException {
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < args.size(); i++) {
                stmt.setObject(i + 1, args.get(i));
            }
            return stmt.executeUpdate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public void insertMessage(int userId, SimData data) throws SQLException, ClassNotFoundException {
        String query = "INSERT INTO message (author_id, text, pub_date, flagged) VALUES (?, ?, ?, 0)";
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, data.getContent());
            stmt.setLong(3, System.currentTimeMillis()/1000);
            stmt.executeUpdate();
        }
    }

    public void register(Register register) throws SQLException {
        String query = "insert into user (username, email, pw_hash) values (?, ?, ?)";
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, register.getUsername());
            stmt.setString(2, register.getEmail());
            stmt.setString(3, passwordEncoder.encode(register.getPwd()));
            stmt.executeUpdate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void unfollow(int userId, int unfollowsUserId) throws SQLException, ClassNotFoundException {
        String query = "DELETE FROM follower WHERE who_id=? AND whom_id=?";
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, unfollowsUserId);

            stmt.executeUpdate();
        }
    }

    public int follow(int userId, int followUserId) throws SQLException {
        String query = "INSERT INTO follower (who_id, whom_id) VALUES (?, ?)";
        try (Connection conn = connectDb();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, followUserId);
            return stmt.executeUpdate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public int getUserId(String username) throws SQLException, ClassNotFoundException {
        int userId = -1;
        List<Map<String, Object>> results = queryDb("select user_id from user where username = ?", List.of(username));
        if (!results.isEmpty()) {
            userId = (int) results.get(0).getOrDefault("user_id", -1);
        }
        return userId;
    }

}
