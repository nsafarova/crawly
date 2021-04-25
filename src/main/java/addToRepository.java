import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class addToRepository {

    public addToRepository(List<String> url) throws SQLException {
        seedURL(url);
    }

    String connectionUrl = "jdbc:postgresql://localhost:5432/webcrawler";
    Connection conn = DriverManager.getConnection(connectionUrl, "postgres", "1234");

    private void seedURL(List<String> url) throws SQLException {

        for (String s : url) {
            String query = "INSERT INTO repository (seed_url, is_crawled, date_and_time) VALUES(?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, s);
            pstmt.setBoolean(2, false);
            pstmt.setString(3, new Date().toString());
            pstmt.executeUpdate();
        }

        conn.close();
    }
}
