import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;


public class WebCrawler implements Runnable {
    private static final int MAX_DEPTH = 3;
    private final Thread thread;
    private final String first_link;
    private final ArrayList<String> visitedLinks = new ArrayList<String>();
    private final long ID;
    String connectionUrl = "jdbc:postgresql://localhost:5432/webcrawler";
    java.sql.Connection conn = DriverManager.getConnection(connectionUrl, "postgres", "");

    public WebCrawler(String link, int num) throws SQLException {
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

        try {
            seedURL(first_link);
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

    private void seedURL(String url) throws SQLException {
        String query = "INSERT INTO repository (seed_url, is_crawled, date_and_time) VALUES(?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, url);
        pstmt.setBoolean(2, false);
        pstmt.setString(3, new Date().toString());

        pstmt.executeUpdate();
    }


    private void crawl(String url) {
            try {
                String query = "SELECT * FROM records where url LIKE ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1,"%"+url+"%");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() != false) {
                    do {
                        printCrawler(ID, rs.getString(2), rs.getString(3), rs.getString(4));
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

        try {

            //conn = DriverManager.getConnection(connectionUrl, "postgres", "");
            CopyManager cm = new CopyManager((BaseConnection) conn);

            String fileName = "src/main/resources/records.csv";

            try (FileOutputStream fos = new FileOutputStream(fileName);
                 OutputStreamWriter osw = new OutputStreamWriter(fos,
                         StandardCharsets.UTF_8)) {

                cm.copyOut("COPY records (url, website_title, crawled_text, record_date, crawled_text_size, url_depth) TO STDOUT WITH CSV HEADER", osw);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void request(int level, String url) {
        try {
                Connection con = Jsoup.connect(url).ignoreContentType(true);
                Document doc = con.get();

                if (con.response().statusCode() == 200) {
                    String title = doc.title();
                    String text = doc.body().text();

                    printCrawler(ID, url, title, text);
                    System.out.println("level " + level);
                    visitedLinks.add(url);
                    sleep(2); // niceness delay

                    String query = "INSERT INTO records (url, website_title, crawled_text, record_date, crawled_text_size, url_depth) VALUES(?,?,?,?,?,?)";
                    PreparedStatement pstmt = conn.prepareStatement(query);
                    pstmt.setString(1, url);
                    pstmt.setString(2, title);
                    pstmt.setString(3, text);
                    pstmt.setString(4, new Date().toString());
                    pstmt.setLong(5, text.length());
                    pstmt.setLong(6, level);

                    pstmt.executeUpdate();
                    if (level <= MAX_DEPTH) {
                        for (Element link : doc.select("a[href]")) {
                            String next_link = link.absUrl("href");
                            if (!visitedLinks.contains(next_link)) {
                                request(level++, next_link);
                            }
                        }
                    }
                }
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
            Thread.sleep(seconds * 1000L);
        }
        catch (InterruptedException e) {
            e.getMessage();
        }
    }


}
