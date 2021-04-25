import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ThreadCrawlers {
    private static List<String> links = new ArrayList<>();
    private static int numberOfThreads = 10;
    private static int lastLinkIndex = numberOfThreads-1;

    public static void main(String[] args) throws SQLException {

        Scanner sc = new Scanner(System.in);
        List<String> urlList = new ArrayList<>();
        System.out.print("Enter the number of URLs to add to the repository (Type 0 if you don't want to add a URL): ");
        int numberOfCrawlers = sc.nextInt();
        System.out.print("Enter the URls (e.g. url1 url2 url3): ");

        for (int i = 0; i < numberOfCrawlers; i++) {
            urlList.add(sc.next());
        }

        new addToRepository(urlList);

        links = getUrlsFromRepository();

        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        if(links.size()<numberOfThreads) numberOfThreads = links.size();
        for (int i = 1; i <= numberOfThreads; i++) {
            crawlers.add(new WebCrawler(links.get(i-1), i));
        }

        for (WebCrawler w : crawlers) {
            try {
                System.out.println(java.lang.Thread.activeCount());
                w.getThread().join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getNewUrl() {
        if(links.size()>numberOfThreads){
            lastLinkIndex++;
            return links.get(lastLinkIndex);
        }
        return null;
    }

    private static List<String> getUrlsFromRepository() throws SQLException {
        String connectionUrl = "jdbc:postgresql://localhost:5432/webcrawler";
        Connection conn = DriverManager.getConnection(connectionUrl, "postgres", "1234");
        String query = "SELECT * FROM repository WHERE is_crawled = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setBoolean(1, false);
        ResultSet rs = pstmt.executeQuery();
        ArrayList <String> urls = new ArrayList<String>();
        while(rs.next()){
            urls.add(rs.getString("seed_url"));
        }
        conn.close();

        return urls;
    }

}
