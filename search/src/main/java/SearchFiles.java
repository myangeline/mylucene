import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Created by Administrator on 2015-10-11.
 *
 */
public class SearchFiles {
    public static void main(String[] args) throws IOException, ParseException {
        args = new String[]{
                "-index",
                "F:\\Program Files (x86)\\JetBrains\\workspace\\Javase\\index"
        };

        String index = "index";
        String field = "contents";
        String queries = null;
        int repeat = 0;
        boolean raw = false;
        String queryString = null;
        int hitsPerPage = 10;

        for (int i=0;i<args.length; i++){
            switch (args[i]){
                case "-index":
                    index = args[++i];
                    break;
                case  "-field":
                    field = args[++i];
                    break;
                case "-queries":
                    queries = args[++i];
                    break;
                case "-query":
                    queryString = args[++i];
                    break;
                case "-repeat":
                    repeat = Integer.parseInt(args[++i]);
                    break;
                case "-raw":
                    raw = true;
                    break;
                case "-paging":
                    hitsPerPage = Integer.parseInt(args[++i]);
                    if (hitsPerPage <= 0){
                        System.err.println("at least 1 data per page!");
                        System.exit(1);
                    }
                    break;
                default:
                    break;
            }
        }

        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index).toPath()));
        IndexSearcher searcher = new IndexSearcher(reader);

//      这里的 Analyzer 需要和前面创建索引的时候使用的一致，不然可能会出现找不到的情况
//        Analyzer analyzer = new StandardAnalyzer();
        Analyzer analyzer = new EnglishAnalyzer();
        BufferedReader in;
        in = null;
        if (queries != null){
            in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), StandardCharsets.UTF_8));
        }else {
            in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }

        QueryParser parser = new QueryParser(field, analyzer);
        while (true){
            if (queries == null && queryString == null){
                System.out.println("请输入查询关键字:");
            }
            String line = queryString!=null ?queryString:in.readLine();
            if (line == null || line.trim().length() <= 0){
                break;
            }
            Query query = parser.parse(line);
            System.out.println("Searching for: " + query.toString(field));
            if (repeat>0){
                Date start = new Date();
                for (int i=0; i<repeat; i++){
                    searcher.search(query, 100);
                }
                Date end = new Date();
                System.out.println("Time: " + (end.getTime() - start.getTime()) + "ms");
            }

            doPageSearch(in, searcher, query, hitsPerPage, raw, queries==null&&queryString==null);
            if (queryString!=null){
                break;
            }
        }
        reader.close();
    }

    public static void doPageSearch(BufferedReader in, IndexSearcher searcher, Query query,
                                    int hitsPerPage, boolean raw, boolean interactive) throws IOException {

        TopDocs results = searcher.search(query, 5*hitsPerPage);
        ScoreDoc[] hits = results.scoreDocs;

        int totalHits = results.totalHits;

        System.out.println(totalHits+" total matching documents");

        int start = 0;
        int end = Math.min(totalHits, hitsPerPage);

        while (true){
            if (end > hits.length){
                System.out.println("Only results 1 - " + hits.length + " of " + totalHits + " total matching documents collected.");
                System.out.println("Collect more (y/n) ?");
                String line = in.readLine();
                if (line.length() == 0 || line.charAt(0) == 'n'){
                    break;
                }
                hits = searcher.search(query, totalHits).scoreDocs;
            }

            end = Math.min(hits.length, start+hitsPerPage);

            for (int i = start; i < end; i++) {
                if (raw){
                    System.out.println("doc=" + hits[i].doc + " score=" + hits[i].score);
                }else{
                    Document doc = searcher.doc(hits[i].doc);
                    String path = doc.get("path");
                    if (path != null) {
                        System.out.println((i + 1) + ". " + path);
                        String title = doc.get("title");
                        if (title != null) {
                            System.out.println("   Title: " + doc.get("title"));
                        }
                    } else {
                        System.out.println((i + 1) + ". "
                                + "No path for this document");
                    }
                }
            }

            if (!interactive || end == 0) {
                break;
            }

            if (totalHits >= end) {
                boolean quit = false;
                while (true) {
                    System.out.print("Press ");
                    if (start - hitsPerPage >= 0) {
                        System.out.print("(p)revious page, ");
                    }
                    if (start + hitsPerPage < totalHits) {
                        System.out.print("(n)ext page, ");
                    }
                    System.out
                            .println("(q)uit or enter number to jump to a page.");

                    String line = in.readLine();
                    if (line.length() == 0 || line.charAt(0) == 'q') {
                        quit = true;
                        break;
                    }
                    if (line.charAt(0) == 'p') {
                        start = Math.max(0, start - hitsPerPage);
                        break;
                    } else if (line.charAt(0) == 'n') {
                        if (start + hitsPerPage < totalHits) {
                            start += hitsPerPage;
                        }
                        break;
                    } else {
                        int page = Integer.parseInt(line);
                        if ((page - 1) * hitsPerPage < totalHits) {
                            start = (page - 1) * hitsPerPage;
                            break;
                        } else {
                            System.out.println("No such page");
                        }
                    }
                }
                if (quit)
                    break;
                end = Math.min(totalHits, start + hitsPerPage);
            }
        }
    }
}
