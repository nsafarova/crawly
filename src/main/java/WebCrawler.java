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
    java.sql.Connection conn = null;

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
            conn = DriverManager.getConnection(connectionUrl, "postgres", "1234");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        crawl(first_link);

        try {
            saveAsCrawled(first_link);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        ThreadCrawlers anotherUrl = new ThreadCrawlers();
        String newUrl = anotherUrl.getNewUrl();
        if(newUrl != null){
            try {
                new WebCrawler(newUrl, (int) ID);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }

        try {
            conn.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void saveAsCrawled(String url) throws SQLException {
        String updateQuery = "UPDATE repository SET is_crawled=? WHERE seed_url=?";
        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
        updateStmt.setBoolean(1, true);
        updateStmt.setString(2, url);
        updateStmt.executeUpdate();
    }

    private void crawl(String url) {
            try {
                boolean newLink = true;
                String query = "SELECT * FROM records where url LIKE ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setString(1,"%"+url+"%");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() != false) {
                    newLink = false;
                    System.out.println("Found previous crawl records of '" + url +"'. Printing them...");
                    do {
                        printCrawler(ID, rs.getString("url"), rs.getString("website_title"), rs.getString("crawled_text"), rs.getString("record_date"));
                        visitedLinks.add(rs.getString("url"));
                    }
                    while(rs.next());
                }

                int level = 1;
                request(level, url, newLink);

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private void printCrawler(Long id, String url, String title, String text, String date) {
        System.out.println("\n**Crawler " + id + ": Received webpage at " + url);
        System.out.println(title);
        System.out.println(text); // text extracting
        System.out.println(date);

        try {

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

    private void request(int level, String url, boolean isNewLink) {
        try {
                Connection con = Jsoup.connect(url).ignoreContentType(true);
                Document doc = con.get();

            if(isNewLink) {
                if (con.response().statusCode() == 200) {
                    String title = doc.title();
                    String text = doc.body().text();

                    String crawlTime = new Date().toString();
                    printCrawler(ID, url, title, text, crawlTime);
                    visitedLinks.add(url);
                    sleep(2); // niceness delay

                    String query = "INSERT INTO records (url, website_title, crawled_text, record_date, crawled_text_size, url_depth) VALUES(?,?,?,?,?,?)";
                    PreparedStatement pstmt = conn.prepareStatement(query);
                    pstmt.setString(1, url);
                    pstmt.setString(2, title);
                    pstmt.setString(3, text);
                    pstmt.setString(4, crawlTime);
                    pstmt.setLong(5, text.length());
                    pstmt.setLong(6, level);

                    pstmt.executeUpdate();
                }
            }
                if (level <= MAX_DEPTH) {
                    for (Element link : doc.select("a[href]")) {
                        String next_link = link.absUrl("href");
                        if(next_link.length()>0) {
                            if (!visitedLinks.contains(next_link)) {
                                request(level++, next_link, true);
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
