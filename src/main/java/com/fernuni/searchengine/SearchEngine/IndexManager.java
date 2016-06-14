package com.fernuni.searchengine.SearchEngine;

import com.fernuni.searchengine.RESTController;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/2/2016 AD.
 * IndexManager meant to be the manager of index.
 */
public class IndexManager {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private DirectoryHandler directoryHandler;
    public static final int PRE_CONTENT_SIZE = 100;

    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.SearchEngine.DirectoryHandler");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }
    /**
     * Default constructor.
     */
    public IndexManager() {
    }

    /**
     * Delete old index with the path corresponding to DirectoryHandler.
     * @return  successful, or not
     */
    public boolean deleteIndex(){
        directoryHandler = DirectoryHandler.getDirectoryHandler();
        try {
            File indexDir = directoryHandler.getIndexDirectory();
            Directory directory = FSDirectory.open(indexDir.toPath());
            /**
             * Create IndexWriter config and create IndexWriter.
             */
            IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            try {
                IndexWriter iwriter = new IndexWriter(directory, config);
                logger.info("Deleting the index at: " + indexDir.getAbsolutePath());
                iwriter.deleteAll();
                iwriter.close();
                Indexer.getIndexer().setNumOfFilesIndexed(0);
                return true;
            } catch (IOException e) {
                logger.severe("Cannot access to index directory, Please enter the correct full path...");
                return false;
            }
        } catch (IOException e) {
            logger.warning("No index founded, Cannot delete old index files.");
            return false;
        } catch (NullPointerException e) {
            logger.warning("No index has been registered, Cannot delete old index files.");
            return false;
        }
    }

    public static FileHandler getFileHandler(String logfile_str, int count){
        try{
            FileHandler fileHandler = new FileHandler(logfile_str, true);
            return fileHandler;
        }
        catch (IOException e){
            logger.warning("Cannot log to file " + logfile_str);
            return null;
        }
    }
}
