import java.util.ArrayList;
import java.util.Scanner;

public class ThreadCrawlers {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        System.out.print("Enter the URls (e.g. url1 url2 url3): ");
        crawlers.add(new WebCrawler(sc.next(), 1));
        //crawlers.add(new WebCrawler(sc.next(), 2));
        //crawlers.add(new WebCrawler(sc.next(), 3));

        /*
        crawlers.add(new WebCrawler("https://www.nytimes.com/", 1));
        crawlers.add(new WebCrawler("https://medium.com/", 2));
        crawlers.add(new WebCrawler("https://www.investopedia.com/", 3));
        */

        for (WebCrawler w : crawlers) {
            try {
                w.getThread().join();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
