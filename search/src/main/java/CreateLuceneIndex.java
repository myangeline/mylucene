import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Created by Administrator on 2015-10-11.
 *
 */
public class CreateLuceneIndex {

    public static void main(String[] args) throws IOException {
        if (args == null || args.length <= 0) {
            args = new String[]{
                    "-index", "F:\\Program Files (x86)\\JetBrains\\workspace\\Javase\\index", "-docs", "F:\\opensource\\lucene-5.3.1\\docs\\demo"
            };
        }
        // 索引文件保存的路径
        String indexPath = "";
        // 资源文件所在目录
        String docsPath = null;

        boolean create = true;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    indexPath = args[++i];
                    break;
                case "-docs":
                    docsPath = args[++i];
                    break;
                case "-update":
                    create = false;
                    break;
                default:
                    break;
            }
        }

        if (docsPath == null) {
            System.err.println("必须指定资源文件所在目录！");
            System.exit(1);
        }
        final File docDir = new File(docsPath);
        if (!docDir.exists() || !docDir.canRead()) {
            System.err.println("资源文件目录不存在或者不可读！");
            System.exit(1);
        }

        Date start = new Date();
        System.out.println("正在创建索引文件到 '" + indexPath + "'...");
        Directory directory = FSDirectory.open(new File(indexPath).toPath());
        Analyzer analyzer = new EnglishAnalyzer();
//        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        if (create) {
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        indexDocs(indexWriter, docDir);
        indexWriter.close();
        Date end = new Date();
        System.out.println((end.getTime() - start.getTime()) + "total milliseconds...");
    }

    private static void indexDocs(IndexWriter indexWriter, File file) throws IOException {
        if (file.isDirectory()) {
            String[] files = file.list();
            for (String f : files) {
                indexDocs(indexWriter, new File(file, f));
            }
        } else {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);

                Document doc = new Document();
                Field pathField = new StringField("path", file.getPath(), Field.Store.YES);
                doc.add(pathField);
                doc.add(new LongField("modified", file.lastModified(), Field.Store.NO));
                doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))));

                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    System.out.println("adding " + file + " ...");
                    indexWriter.addDocument(doc);
                } else {
                    System.out.println("updating " + file + " ...");
                    indexWriter.updateDocument(new Term("path", file.getPath()), doc);
                }
            } finally {
                if (fis != null) {
                    fis.close();
                }
            }
        }
    }

}
