import java.util.Set;

public record Article(String uuid, String title, String author, String url,
                      String text, String published, String language, Set<String> categories) {


}
