package com.fernuni.searchengine.SearchEngine;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/2/2016 AD.
 */
public class IndexManager {
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private DirectoryHandler directoryHandler;
    public static final int PRE_CONTENT_SIZE = 100;

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
                IndexWriter iwriter = new IndexWriter(
                        directory,
                        config);
                System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                                    "Deleting the index at: " + indexDir.getAbsolutePath());
                iwriter.deleteAll();
                iwriter.close();
                Indexer.getIndexer().setNumOfFilesIndexed(0);
                return true;
            } catch (IOException e) {
                System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                        "\t Cannot access to index directory.\n" +
                        sdf.format(Calendar.getInstance().getTime()) +
                        "\t Please enter the correct full path...");
                return false;
            }
        } catch (IOException e) {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t No index founded, Cannot delete old index files.");
            return false;
        } catch (NullPointerException e) {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t No index has been registered, Cannot delete old index files.");
            return false;
        }
    }
}
