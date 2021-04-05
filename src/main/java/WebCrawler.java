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

    java.sql.Connection conn;
    {
        try {
            conn = DriverManager.getConnection(connectionUrl, "postgres", "");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    public WebCrawler(String link, int num) {
        System.out.println();
        first_link = link;
        ID = num;

        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        crawl(1, first_link);
    }

    private void crawl(int level, String url) {
        if (level <= MAX_DEPTH) {
                    try {
                        String query = "SELECT * FROM records where url LIKE ?";
                        PreparedStatement pstmt = conn.prepareStatement(query);
                        pstmt.setString(1,"%"+url+"%");
                        ResultSet rs = pstmt.executeQuery();

                        if (rs.next() == false) {
                            request(url);
                        } else {
                            do {
                                printCrawler(rs.getLong(1), rs.getString(2),
                                        rs.getString(3), rs.getString(4));
                            }
                                while(rs.next());
                        }
                        conn.close();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

    private void printCrawler(Long id, String url, String title, String text) {
        System.out.println("\n**Crawler " + id + ": Received webpage at " + url);
        System.out.println(title);
        System.out.println(text); // text extracting
    }

    private Document request(String url) {
        try {
                Connection con = Jsoup.connect(url);
                Document doc = con.get();

                if (con.response().statusCode() == 200) {
                    String title = doc.title();
                    String text = doc.body().text();

                    printCrawler(ID, url, title, text);

                    //visitedLinks.add(url);
                    sleep(3); // niceness delay

                    String query = "INSERT INTO records (url, website_title, crawled_text) VALUES(?,?,?)";
                    PreparedStatement pstmt = conn.prepareStatement(query);
                    pstmt.setString(1, url);
                    pstmt.setString(2, title);
                    pstmt.setString(3, text);

                    pstmt.executeUpdate();

                    return doc;
                }
            } 
            
            catch (Exception e) {
                e.printStackTrace();
            }
        return null;
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
