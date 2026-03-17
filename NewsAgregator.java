import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class NewsAgregator {

    static ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // parsez continutul JSON in obiecte de tip Article
    // in differentTitle si differentUuidMap retin de cate ori s-a repetat un titlu, respectiv un uuid
    public static void parseUniqueArt(String file) throws IOException {
        Article[] articleFromFile = mapper.readValue(new File(file), Article[].class);

        for (Article article : articleFromFile) {
            Tema1.differentTitle.putIfAbsent(article.title(), article);
            AtomicInteger atomicTitleCount = Tema1.titleCounts.putIfAbsent(article.title(), new AtomicInteger(1));
            if (atomicTitleCount != null) {
                atomicTitleCount.addAndGet(1);
            }
            Tema1.differentUuidMap.putIfAbsent(article.uuid(), article);
            AtomicInteger atomicUuidCount = Tema1.uuidCounts.putIfAbsent(article.uuid(), new AtomicInteger(1));
            if (atomicUuidCount != null) {
                atomicUuidCount.addAndGet(1);
            }
        }
    }

    //metoda folosita pentru a elimina duplicatele in functie de titlu si uuid
    //articolele unice sunt salvate in articlesArray
    public static void writeFinalResults() throws IOException {
        List<Article> finalArticles = new ArrayList<>();
        for (Article art : Tema1.differentTitle.values()) {
            AtomicInteger tCountVal = Tema1.titleCounts.get(art.title());
            int tCount;

            if (tCountVal != null) {
                tCount = tCountVal.get();
            } else {
                tCount = 0;
            }
            if (tCount > 1) {
                Tema1.duplicatesFound.addAndGet(tCount);
                continue;
            }

            AtomicInteger uCountVal = Tema1.uuidCounts.get(art.uuid());
            int uCount;
            if (uCountVal != null) {
                uCount = uCountVal.get();
            } else {
                uCount = 0;
            }
            if (uCount > 1) {
                Tema1.duplicatesFound.addAndGet(uCount);
                continue;
            }
            finalArticles.add(art);
            Tema1.finaleArticleMap.put(art.uuid(), art);
            Tema1.uniqueArticlesFound.addAndGet(1);
        }
        Tema1.articlesArray = finalArticles.toArray(new Article[0]);
    }

    //metoda pentru normalizarea textului: intai toate literele le fac lowerCase,
    //sparg textul in tokeni de cuvinte prin separatorul spatiu
    //fiecare caracter care nu e din alfabet este eliminat
    public static List<String> normalizeText(String text) {
        String lowerText = text.toLowerCase();
        String[] words = lowerText.split("\\s+");
        List<String> result = new ArrayList<>();
        for (String word : words) {
            String finalWord = word.replaceAll("[^a-z]", "");
            if (!finalWord.isEmpty()) {
                result.add(finalWord);
            }
        }
        return result;
    }

    /**
     * salvez uuid-ul articolului in cazul in care valoarea campului language se regaseste in inputul dat
     * daca limba este engleza, incrementez pentru cuvintele de interes numarul de aparitii in articole
     * */
    public static void organizeByLanguage(Article article) {
        if (Tema1.languages.containsKey(article.language())) {
            Tema1.languages.get(article.language()).add(article.uuid());
        }
        if (article.language().equals("english")) {
            List<String> textArticle = normalizeText(article.text());
            Set<String> uniqueWords = new HashSet<>(textArticle);
            for (String word : uniqueWords) {
                if (!Tema1.linkingWordsSet.contains(word)) {
                    AtomicInteger wordCount = Tema1.keywordsCountMap.putIfAbsent(word, new AtomicInteger(1));
                    if (wordCount != null) {
                        wordCount.addAndGet(1);
                    }
                }
            }
        }
    }

    /**
     * Retin uuid-ul articolului a carui categorie este data in input
     */
    public static void organizeByCategories(Article article) {
        for (String category : article.categories()) {
            if (Tema1.categoriesMap.containsKey(category)) {
                Tema1.categoriesMap.get(category).add(article.uuid());
                AtomicInteger categoryAtomic = Tema1.top_CategoriesMap.putIfAbsent(category, new AtomicInteger(1));
                if (categoryAtomic != null) {
                    categoryAtomic.addAndGet(1);
                }
            }
        }
    }

    /**
     * contorizez cate articole sunt scrise de catre un autor (cate aparitii ale unui nume sunt in campul "author"
     */
    public static void bestAuthorMethod(Article article) {
        AtomicInteger count_bestAuth_atomic = Tema1.best_authorMap.putIfAbsent(article.author(),new AtomicInteger(1));
        if (count_bestAuth_atomic != null) {
            count_bestAuth_atomic.addAndGet(1);
        }
    }

    public static void topLanguage(Article article) {
        AtomicInteger top_lang_atomic = Tema1.top_LanguageMap.putIfAbsent(article.language(), new AtomicInteger(1));
        if (top_lang_atomic != null) {
            top_lang_atomic.addAndGet(1);
        }
    }

    /**
     * Salvez articolul intr-o structura ConcurrentHashMap<String, String>
     * unde cheia este uuid, iar valoarea anul publicarii
     * ma voi folosi de aceasta structura pentru a afisa outputul all_articles.txt
     */
    public static void globalArticles(Article article) {
        Tema1.processedArticles.put(article.uuid(), article.published());
    }

    /**
     * aceasta metoda ia ca parametru ConcurrentHashMap<String, String> map
     * dau override la metoda compare din interfata Comparator pentru a sorta descrescator dupa valoare,
     * iar la egalitate sortez crescator dupa cheie
     */
    public static Map<String, String> sortPublishedArticles(ConcurrentHashMap<String, String> map) {

        List<Map.Entry<String, String>> list = new ArrayList<>(map.entrySet());
        list.sort(new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
                String v1 = "";
                if (e1.getValue() != null) {
                    v1 = e1.getValue();
                }
                String v2 = "";
                if (e2.getValue() != null) {
                    v2 = e2.getValue();
                }
                int valCompare = v2.compareTo(v1);
                if (valCompare != 0) {
                    return valCompare;
                }
                return e1.getKey().compareTo(e2.getKey());
            }
        });

        Map<String, String> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static ArrayList<String> sortLexicographic(ConcurrentLinkedQueue<String> queue) {
        ArrayList<String> sorted = new ArrayList<>(queue);
        Collections.sort(sorted);
        return sorted;
    }

    public static String normalize_Title_Category(String categoryTitle) {
        return categoryTitle.replace(",", "")
                .trim().replaceAll("\\s+", "_");
    }

    /**
     * aceasta metoda ia ca parametru ConcurrentHashMap<String, AtomicInteger> map
     * dau override la metoda compare din interfata Comparator pentru a sorta descrescator dupa valoare,
     * iar la egalitate sortez crescator dupa cheie
     */
    public static Map<String, AtomicInteger> sortMapDescending(ConcurrentHashMap<String, AtomicInteger> map) {
    List<Map.Entry<String, AtomicInteger>> list = new ArrayList<>(map.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, AtomicInteger>>() {
        @Override
        public int compare(Map.Entry<String, AtomicInteger> e1, Map.Entry<String, AtomicInteger> e2) {
            int v1 = 0;
            if (e1.getValue() != null) {
                v1 = e1.getValue().get();
            }
            int v2 = 0;
            if (e2.getValue() != null) {
                v2 = e2.getValue().get();
            }
            int valCompare = Integer.compare(v2, v1);
            if (valCompare != 0) {
                return valCompare;
            }
            return e1.getKey().compareTo(e2.getKey());
        }
    });

    Map<String, AtomicInteger> sortedMap = new LinkedHashMap<>();
    for (Map.Entry<String, AtomicInteger> entry : list) {
        sortedMap.put(entry.getKey(), entry.getValue());
    }

    return sortedMap;
}

    public static void writeUuid(String fileName, ArrayList<String> sorted) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (String uuid :  sorted) {
                if (uuid != null) {
                    writer.write(uuid);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write_keywordCount(String fileName,  Map<String, AtomicInteger> map) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, AtomicInteger> entry : map.entrySet()) {
                int val;
                if (entry.getValue() == null) {
                    val = 0;
                } else {
                    val = entry.getValue().get();
                }
                writer.write(entry.getKey() + " " + val);
                writer.newLine();
            }
        } catch  (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write_globalArticles(String fileName, Map<String, String> map) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String val;
                if (entry.getValue() == null) {
                    val = "";
                } else {
                    val = entry.getValue();
                }
                writer.write(entry.getKey() + " " + val);
                writer.newLine();
            }
        } catch  (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * aceasta metoda este folosita pentru crearea si scrierea fisierului reports.txt
     */
    public static void writeReports() {
        String top_keyword = "";
        String top_keyword_len = "0";
        String best_author = "";
        String best_author_len = "0";
        String top_language = "";
        String top_language_len = "0";
        String category = "";
        String category_len = "0";
        String publishedDate = "";
        String url = "";

        Map<String, AtomicInteger> keywordMap = sortMapDescending(Tema1.keywordsCountMap);
        for (Map.Entry<String, AtomicInteger> entry : keywordMap.entrySet() ) {
            top_keyword = entry.getKey();
            top_keyword_len = String.valueOf(entry.getValue());
            break;
        }

        Map<String, AtomicInteger> best_authorReport = sortMapDescending(Tema1.best_authorMap);
        for (Map.Entry<String, AtomicInteger> entry : best_authorReport.entrySet()) {
            best_author = entry.getKey();
            best_author_len = String.valueOf(entry.getValue());
            break;
        }

        Map<String, AtomicInteger> top_lang_report = sortMapDescending(Tema1.top_LanguageMap);
        for (Map.Entry<String, AtomicInteger> entry : top_lang_report.entrySet()) {
            top_language = entry.getKey();
            top_language_len = String.valueOf(entry.getValue());
            break;
        }

        Map<String, AtomicInteger> topCategory_report = sortMapDescending(Tema1.top_CategoriesMap);
        for (Map.Entry<String, AtomicInteger> entry : topCategory_report.entrySet()) {
            category = entry.getKey();
            category_len = String.valueOf(entry.getValue());
            break;
        }

        Map<String, String> publishedArticlesReport = sortPublishedArticles(Tema1.processedArticles);
        for (Map.Entry<String, String> entry : publishedArticlesReport.entrySet()) {
            String key = entry.getKey();
            Article art = Tema1.finaleArticleMap.get(key);
            if (art != null) {
                url = art.url();
                publishedDate = String.valueOf(entry.getValue());
            }
            break;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("reports.txt"))) {
            writer.write("duplicates_found - " + Tema1.duplicatesFound);
            writer.newLine();

            writer.write("unique_articles - " + Tema1.uniqueArticlesFound);
            writer.newLine();

            writer.write("best_author - " + best_author + " " + best_author_len);
            writer.newLine();

            writer.write("top_language - " + top_language + " " + top_language_len);
            writer.newLine();

            writer.write("top_category - " + normalize_Title_Category(category) + " " + category_len);
            writer.newLine();

            writer.write("most_recent_article - " + publishedDate + " " + url);
            writer.newLine();

            writer.write("top_keyword_en - " + top_keyword + " " + top_keyword_len);
            writer.newLine();

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}