import java.io.FileReader;
import java.util.Properties;

public class Configuration {
    public static void main(String[] args) {
        // this code sets up the File Reader Object and point to config.properties file.
        try(FileReader reader =  new FileReader("config.properties")) {
            Properties properties = new Properties();
            properties.load(reader);

            String MAX_DEPTH = properties.getProperty("MAX_DEPTH");

            System.out.println(MAX_DEPTH);

        }catch (Exception e) {;
            e.printStackTrace();
        }
    }
}
