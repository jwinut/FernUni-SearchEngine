package com.fernuni.searchengine.SearchEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fernuni.searchengine.RESTController;
import org.apache.catalina.Store;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 5/24/2016 AD.
 * Indexer is a singleton class, it provides methods to index files and a little management about the index.
 */

public class Indexer implements Runnable {
    //Log related instances.
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.SearchEngine.Indexer");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }

    private static Indexer indexer;
    private IndexWriter iwriter;
    private File indexDir;
    private ArrayList<File> dataDirs;
    private DirectoryHandler directoryHandler;
    private FilesParser parser = new FilesParser();
    private Tika tika = new Tika();
    private final String SUPPORT_TYPE = "text/html\n" + //File MIME type support list.
                                    "text/plain\n" +
                                    "application/rtf\n" +
                                    "application/xml\n" +
                                    "application/msword\n" +
                                    "application/vnd.ms-excel\n" +
                                    "application/vnd.ms-powerpoint\n" +
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\n" +
                                    "application/vnd.openxmlformats-officedocument.presentationml.presentation\n" +
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document\n" +
                                    "application/vnd.openxmlformats-officedocument.presentationml.slideshow\n" +
                                    "application/pdf";
    private int numOfFilesIndexed = 0;

    /**
     * Normal constructor.
     */
    private Indexer(){
        directoryHandler = DirectoryHandler.getDirectoryHandler();
        indexDir = directoryHandler.getIndexDirectory();
        dataDirs = directoryHandler.getDataDirectories();
    }

    /**
     * @return  A Singleton object of Indexer.
     */
    public static Indexer getIndexer(){
        if(indexer != null)
            return indexer;
        else{
            indexer = new Indexer();
            return indexer;
        }
    }

    /**
     * Index every files in directory and in sub-directory entered to system.
     */
    public void run(){
        //Delete old index data before re-index again.
        IndexManager indexManager = new IndexManager();
        indexManager.deleteIndex();

        int numOfFilesIndexed = 0;

        //This is the index directory object.
        File indexDir = getIndexDir();

        //Create IndexWriter.
        Directory directory;
        try {
            //Open index directory for assign with IndexWriter.
            directory = FSDirectory.open(indexDir.toPath());
        } catch (IOException e) {
            logger.warning("Index directory incorrect.");
            return;
        } catch (NullPointerException e) {
            logger.severe("Cannot open index directory.");
            return;
        }
        iwriter = getIndexWriter(directory);

        //Get all data directory.
        ArrayList<File> files = getDataDirs();

        //Get everything checked.
        if(files.size() == 0){
            logger.severe("There's no data to be indexed.");
            return;
        }

        //Index every files in directory and within subdirectory.
        for(File file : files){
            numOfFilesIndexed += index(file, iwriter);
        }
        try {
            iwriter.close();
            directory.close();
            logger.info("Total files indexed: " + numOfFilesIndexed);
            indexer.setNumOfFilesIndexed(numOfFilesIndexed);
        } catch (IOException e) {
            logger.severe("IndexWriter or Directory is null.");
        }
    }

    /**
     * add every files inside a directory to be indexed
     * @param directory File obj that is a directory.
     * @param iwriter   IndexWriter obj with StandardAnalyzer.
     */
    public int index(File directory, IndexWriter iwriter){
        /**
         * numOfFile for keep tracking the number of file indexed.
         */
        int numOfFile = 0;

        /**
         * Check again if the file really is a directory.
         */
        if(directory.isDirectory()){
            File[] files = directory.listFiles();           //Get list of files inside a directory.
            if (files != null) {
                for(File file : files){
                    if(file.isDirectory())
                        numOfFile += index(file, iwriter);  //Recursively index a directory out of files.
                    else{
                        numOfFile += indexfile(file, iwriter);   //Index a file
                    }
                }
            }
        }
        else{
            indexfile(directory, iwriter);   //Index a file
            numOfFile++;
        }
        return numOfFile;
    }

    /**
     * Index a single file.
     * @param file  A file to be indexed.
     * @param iwriter   A indexer obj with StandardAnalyzer.
     */
    private int indexfile(File file, IndexWriter iwriter){
        logger.info("Indexing..." + file.getAbsolutePath());
        String contents;
        Document doc;
        try {
            String file_type = tika.detect(file);
            if(!SUPPORT_TYPE.contains(file_type)) {
                logger.info("file: " + file.getName() + " file type: " + file_type + " is not supported.");
                return 0;
            }
            else{
                contents = parser.parseToString(file);

                //Create new document add to index.
                doc = new Document();
                doc.add(new StringField("path", file.getAbsolutePath(), Field.Store.YES));
                doc.add(new StringField("file_name", file.getName(), Field.Store.YES));
                doc.add(new StringField("pre_contents", getNWords(contents), IndexManager.getPre_contents_store()));
                doc.add(new Field("contents", contents, IndexManager.getContents_store()));
                doc.add(new StringField("type", file_type, Field.Store.YES));

                //use Indexwriter to add a document and index it.
                iwriter.addDocument(doc);
                return 1;
            }
        }
        catch (IOException | TikaException | SAXException e){
            logger.info("file: " + file.getName() + " is not supported.");
            return 0;
        }

    }

    /**
     * Get a registered index directory.
     * @return  Index directory instance.
     */
    private File getIndexDir() {
        indexDir = directoryHandler.getIndexDirectory();
        return indexDir;
    }

    /**
     * Get all registered data directory.
     * @return  ArrayList of data directories.
     */
    private ArrayList<File> getDataDirs() {
        dataDirs = directoryHandler.getDataDirectories();
        return dataDirs;
    }

    /**
     * Create an instance of IndexWriter.
     * @param directory An instance of directory that should be close, So I pass it anyway.
     * @return  IndexWriter instance.
     */
    public IndexWriter getIndexWriter(Directory directory){
        //Create IndexWriter config and create IndexWriter.
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter iwriter = null;
        try {
            iwriter = new IndexWriter(directory, config);
        } catch (IOException e) {
            logger.severe("Cannot access to index directory, Please enter the correct full path...");
            System.exit(1);
        }
        return iwriter;
    }

    /**
     * Getter of contents_store field.
     * @return  (FieldType) contents_store.
     */
    private FieldType getContents_store() {
        return IndexManager.getContents_store();
    }

    /**
     * Getter of pre_contents_store field.
     * @return  (FieldType) pre_contents_store.
     */
    private Field.Store getPre_contents_store() {
        return IndexManager.getPre_contents_store();
    }

    /**
     * Get words equal to PRE_CONTENT_SIZE value, to store as a pre-content, an example of each file.
     * @param contents  Contents of each file as String.
     * @return  first N words extract from String contents.
     */
    private String getNWords(String contents){
        if (contents == null) return "";    //If there's nothing inside or can't read contents, then return nothing.
        //Split words by a space to array with size of PRE_CONTENT_SIZE (100 by default).
        String[] pre_contents = contents.split(" ", IndexManager.PRE_CONTENT_SIZE);
        //If contents have more words than PRE_CONTENT_SIZE then replace "..." to the last words.
        if(pre_contents.length == IndexManager.PRE_CONTENT_SIZE) pre_contents[IndexManager.PRE_CONTENT_SIZE-1] = "...";
        StringBuilder builder = new StringBuilder();
        for(String s : pre_contents) {
            s = s.replace("\n", " ");
            builder.append(s);
            builder.append(" ");
        }
        return builder.toString();
    }

    /**
     * Getter of numOfFilesIndexed.
     * @return  number of total file has been indexed.
     */
    public int getNumOfFilesIndexed() {
        return numOfFilesIndexed;
    }

    /**
     * Setter of numOfFilesIndexed.
     * @param numOfFilesIndexed New value.
     */
    void setNumOfFilesIndexed(int numOfFilesIndexed) {
        this.numOfFilesIndexed = numOfFilesIndexed;
    }

}
