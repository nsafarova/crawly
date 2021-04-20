import java.util.ArrayList;
import java.util.Scanner;

public class ThreadCrawlers {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        System.out.print("Enter the number of Crawlers: ");
        int numberOfCrawlers = sc.nextInt();
        System.out.print("Enter the URls (e.g. url1 url2 url3): ");
        for (int i = 1; i <= numberOfCrawlers; i++) {
            crawlers.add(new WebCrawler(sc.next(), i));
        }

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
