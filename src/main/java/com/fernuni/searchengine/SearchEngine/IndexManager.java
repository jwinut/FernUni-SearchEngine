package com.fernuni.searchengine.SearchEngine;

import com.fernuni.searchengine.RESTController;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/2/2016 AD.
 * IndexManager meant to be the manager of index.
 */
public class IndexManager {
    //Log related instances.
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.SearchEngine.DirectoryHandler");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }
    static final int PRE_CONTENT_SIZE = 100;
    private static FieldType contents_store = TextField.TYPE_NOT_STORED;
    private static FieldType pre_contents_store = TextField.TYPE_STORED;
    private Indexer indexer;

    /**
     * Default constructor.
     */
    public IndexManager() {
        indexer = Indexer.getIndexer();
    }

    /**
     * Delete old index with the path corresponding to DirectoryHandler.
     * @return  successful, or not
     */
    public boolean deleteIndex(){
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
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

    /**
     * Report status of the system.
     * @return  System report.
     */
    public static String statusReport(){
        Indexer indexer = Indexer.getIndexer();
        String status;
        logger.info("Status report was called.");

        //If indexer is not null, it will pass this line.
        status = "\tIndexer is not NULL\n";

        //Get index directory and contents_store for return, display.
        File indexDir = DirectoryHandler.getDirectoryHandler().getIndexDirectory();
        String indexDir_str = (indexDir != null) ? "\n\t\t" + indexDir.getAbsolutePath() : "\tnot found";

        //Get data directory and contents_store for return, display.
        String dataDir;
        ArrayList<File> dataDirs = DirectoryHandler.getDirectoryHandler().getDataDirectories();
        /**
         * List all directory on the system.
         */
        if (dataDirs == null) dataDir = "\tnot found.\n";
        else if (dataDirs.size() > 0) {
            String tmp = "\n";
            for (File dir : dataDirs) {
                tmp += "\t\t" + dir.getAbsolutePath() + "\n";
            }
            dataDir = tmp;
        }
        else if (dataDirs.size() == 0) {
            dataDir = "\tNo directory entered yet.\n";
        }
        else dataDir = "Directory error.\n";

        //Get status of pre contents, contents.
        boolean contentsStoreStatus = (getContents_store() == TextField.TYPE_STORED);
        boolean preContentsStoreStatus = (getPre_contents_store() == TextField.TYPE_STORED);
        String contentsStoreStatus_str = contentsStoreStatus ? "\tContent is stored.\n" : "\tContent is not stored.\n";
        String preContentsStoreStatus_str = preContentsStoreStatus ? "\tPre content is stored.\n" : "\tPre content is not stored.\n";
        String fileContentsStatus = "\tPreview content using ";
        if(getContents_store() == TextField.TYPE_STORED)
            fileContentsStatus += "100 matched content words.\n";
        else if(getPre_contents_store() == TextField.TYPE_STORED)
            fileContentsStatus += "100 first words from the file\n";
        else
            fileContentsStatus += "[White Space], (No preview)\n";

        //Assemble String.
        status += "\tIndex directory:" + indexDir_str + "\n" + "\tData directory(s):" + dataDir + contentsStoreStatus_str +
                preContentsStoreStatus_str + fileContentsStatus;
        logger.info("Reporting status...\n" + "======================\n" +
                status + "======================");
        return status;
    }

    /**
     * Set value of contents_store to TextField.TYPE_STORE to store contents of files in index.
     * @return  true, but if something bad happens, it returns false.
     */
    public static boolean setContentStoreTrue() {
        contents_store = TextField.TYPE_STORED;
        return contents_store == TextField.TYPE_STORED;
    }

    /**
     * Set value of contents_store to TextField.TYPE_NOT_STORE to NOT store contents of files in index.
     * @return  true, but if something bad happens, it returns false.
     */
    public static boolean setContentStoreFalse(){
        contents_store = TextField.TYPE_NOT_STORED;
        return contents_store == TextField.TYPE_NOT_STORED;
    }

    /**
     * Set value of pre_contents_store to TextField.TYPE_STORE to store contents of files in index.
     * @return  true, but if something bad happens, it returns false.
     */
    public static boolean setPreContentStoreTrue() {
        pre_contents_store = TextField.TYPE_STORED;
        return pre_contents_store == TextField.TYPE_STORED;
    }

    /**
     * Set value of pre_contents_store to TextField.TYPE_NOT_STORE to NOT store contents of files in index.
     * @return  true, but if something bad happens, it returns false.
     */
    public static boolean setPreContentStoreFalse(){
        pre_contents_store = TextField.TYPE_NOT_STORED;
        return pre_contents_store == TextField.TYPE_NOT_STORED;
    }

    /**
     * Getter of contents_store field.
     * @return  (FieldType) contents_store.
     */
    public static FieldType getContents_store() {
        return contents_store;
    }

    /**
     * Getter of pre_contents_store field.
     * @return  (FieldType) pre_contents_store.
     */
    public static FieldType getPre_contents_store() {
        return pre_contents_store;
    }

    /**
     * Add a file to index, if it is a directory, all file will be added.
     * @param file  File to be added into the index.
     * @throws IOException
     */
    public void addFileToIndex(File file) throws IOException{
        IndexWriter indexWriter = indexer.getIndexWriter(FSDirectory.open(DirectoryHandler.getDirectoryHandler().getIndexDirectory().toPath()));
        int numOfFilesIndexed = indexer.index(file, indexWriter);
        indexWriter.close();
        logger.info("Total file just added to index: " + numOfFilesIndexed);
    }
}
