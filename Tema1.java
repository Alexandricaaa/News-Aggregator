import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Tema1 {

    public static int numOfThreads;
    public static int numOfFiles;
    public static String[] files;
    public static String[] auxiliaryFiles;
    public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> languages =  new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> categoriesMap =  new ConcurrentHashMap<>();
    public static HashSet<String> linkingWordsSet = new HashSet<>();
    public static ConcurrentHashMap<String, AtomicInteger> keywordsCountMap =  new ConcurrentHashMap<>();

    public static AtomicInteger duplicatesFound = new AtomicInteger(0);
    public static AtomicInteger uniqueArticlesFound = new AtomicInteger(0);
    public static ConcurrentHashMap<String, AtomicInteger> best_authorMap =  new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, AtomicInteger> top_LanguageMap =  new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, AtomicInteger> top_CategoriesMap =  new ConcurrentHashMap<>();
    //uuid - <yyyy...
    public static ConcurrentHashMap<String, String> processedArticles =  new ConcurrentHashMap<>();

    public static MyThread[] threads;
    public static int[] ids;
    public static ConcurrentHashMap<String,Article> differentTitle = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Article> differentUuidMap = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,Article> finaleArticleMap = new ConcurrentHashMap<>();
    public static Article[] articlesArray;

    public static CyclicBarrier barrier;
    public static ConcurrentHashMap<String, AtomicInteger> titleCounts = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, AtomicInteger> uuidCounts = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        numOfThreads = Integer.parseInt(args[0]);

        threads = new MyThread[numOfThreads];
        ids = new int[numOfThreads];

        String fisierArticole =  args[1];
        String inputCategories = args[2];

        File fArticole = new File(fisierArticole);
        File dirArticole = fArticole.getParentFile();

        MyScanner scanner = new MyScanner(new FileReader(fArticole));

        numOfFiles = scanner.nextInt();
        files = new String[numOfFiles];

        for (int i = 0; i < numOfFiles; i++) {
            String fileName = scanner.next();
            if (dirArticole != null) {
                files[i] = new File(dirArticole, fileName).getPath();
            } else {
                files[i] = fileName;
            }
        }

        File fAux = new File(inputCategories);
        File dirAux = fAux.getParentFile();

        MyScanner scannerCategories = new  MyScanner(new FileReader(fAux));
        int numOfAuxiliaryFiles = scannerCategories.nextInt();
        auxiliaryFiles = new String[numOfAuxiliaryFiles];

        for (int i = 0; i < numOfAuxiliaryFiles; i++) {
            String fileName = scannerCategories.next();
            if (dirAux != null) {
                auxiliaryFiles[i] = new File(dirAux, fileName).getPath();
            } else {
                auxiliaryFiles[i] = fileName;
            }
        }

        MyScanner scannerLanguages = new MyScanner(new FileReader(auxiliaryFiles[0]));
        int numOfLang = scannerLanguages.nextInt();
        for (int i = 0; i < numOfLang; i++) {
            languages.put(scannerLanguages.next(), new ConcurrentLinkedQueue<>());
        }

        MyScanner scannerCat = new MyScanner(new FileReader(auxiliaryFiles[1]));
        int numOfCat = scannerCat.nextInt();
        for (int i = 0; i < numOfCat; i++) {
            String categoryName = scannerCat.nextLine();
            if (categoryName != null && !categoryName.trim().isEmpty()) {
                categoriesMap.put(categoryName.trim(), new ConcurrentLinkedQueue<>());
            }
        }

        MyScanner english_words = new MyScanner(new FileReader(auxiliaryFiles[2]));
        int numOfWords = english_words.nextInt();
        for (int i = 0; i < numOfWords; i++) {
            linkingWordsSet.add(english_words.next());
        }

        for (int i = 0; i < numOfThreads; i++) {
            ids[i] = i;
        }
        barrier =  new CyclicBarrier(numOfThreads);

        for (int i = 0; i < numOfThreads; i++) {
            threads[i] = new MyThread(i, numOfFiles, numOfThreads, files);
            threads[i].start();
        }

        for (int i = 0; i < numOfThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}