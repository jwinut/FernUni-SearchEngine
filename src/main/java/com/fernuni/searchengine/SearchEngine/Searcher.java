package com.fernuni.searchengine.SearchEngine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fernuni.searchengine.RESTController;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/1/2016 AD.
 * This is a search module, it searches through the index and return results as a Result instance.
 */
public class Searcher {
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.SearchEngine.Searcher");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }
    private IndexSearcher indexSearcher = null;
    private QueryParser parser = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static int numOfWordsBeforeMatchedWord = 3;

    /**
     * When initialize, automatically ready up for search, use isCorrupt to check readily.
     */
    public Searcher(){
        //Get a same directory as Indexer.
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        File indexDir = directoryHandler.getIndexDirectory();
        try {
            Directory directory = FSDirectory.open(indexDir.toPath());
            //Assign a index directory to IndexSearcher.
            indexSearcher = new IndexSearcher(DirectoryReader.open(directory));
            /**
             * @param   content     This specify a field that going to be searched.
             * @param   Analyzer    Use the same analyzer as the Indexer.
             */
            parser = new QueryParser("contents", new StandardAnalyzer());
        }
        catch (IOException e){
            logger.severe("Index does not exist.");
            return;
        }
        catch (NullPointerException e){
            logger.severe("Index directory is incorrect, Cannot search without setup the indexer.");
            return;
        }
    }

    /**
     * Check readily of the Searcher.
     * @return  True when ready to use.
     */
    private boolean isCorrupted(){
        if(indexSearcher != null){
            if(parser != null){
                return false;
            }
        }
        return true;
    }

    /**
     * Search through index.
     * @param searchStr Keywords to use for search.
     * @return  Result instances of results.
     */
    public ArrayList<Result> run(String searchStr){
        ArrayList<Result> results = new ArrayList<>();
        logger.info("======================\n" +
                "\tSearching...\n");

        if(!isCorrupted()) {
            String searchString = searchStr;    //Input string to be search for.
            int numOfResult = 100;              //Number of top results limit.
            try {
                //get top 'numOfResult' matching documents list for the query 'searchString'
                TopDocs topDocs = performSearch(searchString, numOfResult); //This is a kind of result report that contains a list of files.
                ScoreDoc[] hits = topDocs.scoreDocs;    //Get files from the list and store in array.
                logger.info("Number of file(s) matched: [" + hits.length + "]");

                //Get detail out of ScoreDoc which is a document user is looking for.
                int counter = 1;
                for(ScoreDoc hit : hits){
                    Document doc = getDocument(hit.doc);
                    String filename = doc.get("file_name");
                    String filepath = doc.get("path");
                    String filecontent = doc.get("contents");
                    String filetype = doc.get("type");
                    //If user hasn't saved pre_contents then use contents instead.
                    if(filecontent != null){
                        logger.info("Using file contents as a pre content.");
                        //Get 100 words around matched word.
                        filecontent = getNWordsMatched(filecontent, searchStr);
                        if(filecontent.length() == 0) filecontent = "[No matched exact word from content]";
                    }
                    else{
                        filecontent = doc.get("pre_contents");
                        if(filecontent == null) filecontent = "[No content]";
                    }
                    //Report to a terminal.
                    logger.info("\t[" + counter++ + "]\n" + "\tFound: " + filename + "\n\t@[" + filepath +
                        "]\n\t-----CONTENT-----\n\t" + filecontent + "\n");
                    results.add(new Result(filename, filepath, filecontent, filetype));    //add Result obj to a list for output.
                }
            }
            catch (IOException e){
                logger.severe("QueryParser or IndexSearcher instance error.\n");
            }
            catch (ParseException e) {
                logger.severe("Parser cannot complete the task.\n");
            }
        }
        else{
            logger.severe("Searcher object is corrupted.");
        }
        logger.info("======================");
        return results;
    }

    /**
     * Get Document's instance from IndexSearcher.
     * @param docId Id
     * @return  Document from index.
     * @throws IOException
     */
    public Document getDocument(int docId) throws IOException {
        return getIndexSearcher().doc(docId);
    }

    /**
     * Do search with keywords.
     * @param queryString   Keywords use for search.
     * @param n             Number of results returned.
     * @return              A list of result documents.
     * @throws IOException
     * @throws ParseException
     */
    public TopDocs performSearch(String queryString, int n) throws IOException, ParseException {
        Query query = getParser().parse(queryString);
        return getIndexSearcher().search(query, n);
    }

    /**
     * Getter of IndexSearcher.
     * @return  The instance of IndexSearcher.
     */
    private IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    /**
     * Getter of QueryParser.
     * @return  The instance of QueryParser.
     */
    private QueryParser getParser() {
        return parser;
    }

    /**
     * This method will find the keyword and get some of contents around that keyword.
     * @param filecontent   File contents.
     * @param searchStr     Keyword.
     * @return  Number of words according to IndexManager.PRE_SIZE_CONTENT and numOfWordsBeforeMatchedWord around the first found keyword.
     */
    private String getNWordsMatched(String filecontent, String searchStr){
        String searchString = searchStr;
        ArrayList<String> arr_str = new ArrayList<>();
        int matched_index = -1;

        String n_filecontent = filecontent.replaceAll("\n", " ");
        String[] strs = n_filecontent.split(" ");

        for(String str : strs) {
            if(str.length() > 0){
                arr_str.add(str);
                if(str.equals(searchString)) matched_index = arr_str.indexOf(str);
            }
        }

        //Cannot find a matched word in contents.
        if(matched_index == -1) return "";

        //Adjust the index to make a string.
        if(matched_index > numOfWordsBeforeMatchedWord) matched_index -= numOfWordsBeforeMatchedWord;
        else matched_index = 0;

        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < IndexManager.PRE_CONTENT_SIZE && i < arr_str.size(); i++) {
            stringBuilder.append(arr_str.get(matched_index + i) + " ");
        }
        if(arr_str.size() > matched_index + IndexManager.PRE_CONTENT_SIZE) stringBuilder.append("...");
        return stringBuilder.toString();
    }
}
