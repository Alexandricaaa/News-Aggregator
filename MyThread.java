import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThread extends Thread {

    private final int id;
    private final int n;
    private final int p;
    private final String[] articlesName;

    MyThread(int id, int n, int p, String[] articlesName) {
        this.id = id;
        this.n = n;
        this.p = p;
        this.articlesName = articlesName;
    }

    @Override
    public void run() {
        int start = id * n / p;
        int end = min((id + 1) * n / p, n);

        for (int i = start; i < end; i++) {
            try {
                NewsAgregator.parseUniqueArt(articlesName[i]);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        if (id == 0) {
            try {
                NewsAgregator.writeFinalResults();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        int articlesLen = Tema1.articlesArray.length;
        int newStart = (int)(id * (double)articlesLen / p);
        int newEnd = min((int) ((id + 1) * (double)articlesLen / p), articlesLen);

        for(int i = newStart; i < newEnd; i++) {
            NewsAgregator.organizeByLanguage(Tema1.articlesArray[i]);
            NewsAgregator.organizeByCategories(Tema1.articlesArray[i]);
            NewsAgregator.bestAuthorMethod(Tema1.articlesArray[i]);
            NewsAgregator.topLanguage(Tema1.articlesArray[i]);
            NewsAgregator.globalArticles(Tema1.articlesArray[i]);
        }

        try {
            Tema1.barrier.await();
        } catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        }

        if (id == 0) {
            for (Map.Entry<String, ConcurrentLinkedQueue<String>> entry : Tema1.categoriesMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    String fileName = NewsAgregator.normalize_Title_Category(entry.getKey()) + ".txt";
                    ArrayList<String> sorted = NewsAgregator.sortLexicographic(entry.getValue());
                    NewsAgregator.writeUuid(fileName, sorted);
                }
            }

            for (Map.Entry<String, ConcurrentLinkedQueue<String>> entry : Tema1.languages.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    String fileName = entry.getKey() + ".txt";
                    ArrayList<String> sorted = NewsAgregator.sortLexicographic(entry.getValue());
                    NewsAgregator.writeUuid(fileName, sorted);
                }
            }

            Map<String, AtomicInteger> keywordsCountSorted = NewsAgregator.sortMapDescending(Tema1.keywordsCountMap);
            NewsAgregator.write_keywordCount("keywords_count.txt", keywordsCountSorted);

            Map<String, String> publishedArticlesReport = NewsAgregator.sortPublishedArticles(Tema1.processedArticles);
            NewsAgregator.write_globalArticles("all_articles.txt", publishedArticlesReport);

            NewsAgregator.writeReports();
        }
    }

    private int min(int i, int n) {
        if (i < n) return i;
        return n;
    }
}