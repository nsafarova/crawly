import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;



public class WebCrawler implements Runnable {
    private static final int MAX_DEPTH = 3;
    private Thread thread;
    private String first_link;
    private ArrayList<String> visitedLinks = new ArrayList<String>();
    private long ID;
    String connectionUrl = "jdbc:postgresql://localhost:5432/webcrawler";
    java.sql.Connection conn = null;

    public WebCrawler(String link, int num) {
        System.out.println();
        first_link = link;
        ID = num;

        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {

        try {
            conn = DriverManager.getConnection(connectionUrl, "postgres", "");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        crawl(first_link);

        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    private void crawl(String url) {
            try {
                String query = "SELECT * FROM records where url LIKE ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1,"%"+url+"%");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() != false) {
                    do {
                        printCrawler(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4));
                    }
                    while(rs.next());
                } else {

                    int level = 1;
                    request(level, url);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void printCrawler(Long id, String url, String title, String text) {
        System.out.println("\n**Crawler " + id + ": Received webpage at " + url);
        System.out.println(title);
        System.out.println(text); // text extracting
    }

    private void request(int level, String url) {
        try {
                Connection con = Jsoup.connect(url);
                Document doc = con.get();

                if (con.response().statusCode() == 200) {
                    String title = doc.title();
                    String text = doc.body().text();

                    printCrawler(ID, url, title, text);
                    sleep(3); // niceness delay

                    String query = "INSERT INTO records (url, website_title, crawled_text) VALUES(?,?,?)";
                    PreparedStatement pstmt = conn.prepareStatement(query);
                    pstmt.setString(1, url);
                    pstmt.setString(2, title);
                    pstmt.setString(3, text);

                    pstmt.executeUpdate();
                    //if (level <= MAX_DEPTH) {
                        for (Element link : doc.select("a[href]")) {
                            String next_link = link.absUrl("href");
                            request(level++, next_link);
                        }
                    }

                //}
            } 
            
            catch (Exception e) {
                e.printStackTrace();
            }

    }

    public Thread getThread() {
        return thread;
    }

    protected void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        }
        catch (InterruptedException e) {
            e.getMessage();
        }
    }

}
