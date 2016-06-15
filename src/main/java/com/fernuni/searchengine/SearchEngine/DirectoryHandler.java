package com.fernuni.searchengine.SearchEngine;

import com.fernuni.searchengine.FileWatcher.WatchService;
import com.fernuni.searchengine.RESTController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 5/30/2016 AD.
 * This class has all the information about where to get files and where to store other files including the index location.
 */
public class DirectoryHandler {
    //Log related instances.
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.SearchEngine.DirectoryHandler");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }
    private File indexDirectory;
    private ArrayList<File> dataDirectories;
    private static DirectoryHandler directoryHandler;
    private Thread watcher_thread = null;
    private WatchService watcher_service = null;

    /**
     * Singleton pattern constructor.
     */
    private DirectoryHandler() {
    }

    /**
     * Remove a data directory from the list, if input is '*' then remove all data directory.
     * Bug!!! when clear bigger directory, no more watcher for sub-directory.
     * I had an idea to re-check every sometime for every directory or every time this method is called.
     * @param path  Data directory path to be remove.
     * @return  True if success, otherwise, false.
     */
    public boolean clearDataDir(String path){
        logger.info("clearDataDir is called, and processing.");
        logger.info("Status report before remove data directory.");
        IndexManager.statusReport();
        ArrayList<File> dataDirs = getDataDirectories();
        if (path.equals("*")) {
            int size = dataDirs.size();
            for(int i = size - 1; i >= 0; i--){
                Path removed_path = this.dataDirectories.remove(i).toPath();
                unWatchDir(removed_path);
            }
            if(dataDirs.size() == 0){
                return true;
            }
            else {
                logger.warning("Remove all data directory error. Cannot clear all directory.");
                return false;
            }
        } else {
            Iterator<File> fileIterator = dataDirs.iterator();
            Path dir_path;
            Path file_path = Paths.get(path);
            //Iterate through a list of paths, If matched, remove.
            while(fileIterator.hasNext()){
                dir_path = Paths.get(fileIterator.next().getAbsolutePath());
                if(dir_path.equals(file_path)){  //If it is matched.
                    fileIterator.remove();  //remove the path that is matched.
                    //Report to a terminal.
                    logger.fine("Following path has been removed: " + dir_path);
                    IndexManager.statusReport(); //Log
                    unWatchDir(file_path);
                    return true;
                }
            }
            logger.info("No directory match.");
            IndexManager.statusReport();
            return false;
        }
    }

    /**
     * Get instance of a DirectoryHandler.
     * @return  An instance of DirectoryHandler.
     */
    public static DirectoryHandler getDirectoryHandler() {
        if(directoryHandler == null){
            directoryHandler = new DirectoryHandler();
            return directoryHandler;
        }
        else return directoryHandler;
    }

    /**
     * Getter of indexDirectory.
     * @return  indexDirectory field.
     */
    File getIndexDirectory() {
        return indexDirectory;
    }

    /**
     * Set a index directory path.
     * @param path  Path to the index directory.
     * @return  True when new index directory has been set, otherwise false.
     */
    public boolean setIndexDirectory(String path) {
        if(path != null && path.length() != 0) {
            this.indexDirectory = new File(path);
            return true;
        }
        else {
            logger.info("No new path has been set for index directory.");
            return false;
        }
    }

    /**
     * Getter of dataDirectories.
     * @return  dataDirectories.
     */
    ArrayList<File> getDataDirectories() {
        if(dataDirectories == null){
            dataDirectories = new ArrayList<>();
            return dataDirectories;
        }
        else return dataDirectories;
    }

    /**
     * Get all data directory as a String.
     * @return  A String contain all data directory.
     */
    public String getDataDirectoriesString() {
        ArrayList dataDirs = this.getDataDirectories();
        String dataDirs_str = "";

        File dir;
        for(Iterator var3 = dataDirs.iterator(); var3.hasNext(); dataDirs_str = dataDirs_str + dir.getAbsolutePath() + "\n") {
            dir = (File)var3.next();
        }

        return dataDirs_str;
    }

    /**
     * Get an index directory as a String.
     * @return  A String of index directory path.
     */
    public String getIndexDirectoryString() {
        File indexDir = this.getIndexDirectory();
        return indexDir != null?indexDir.getAbsolutePath():null;
    }

    /**
     * Add a data directory path to the system.
     * @param path  A new data directory path.
     * @return  String report of successfully added path.
     */
    public String addDataDir(String path){
        ArrayList<File> dataDir = getDataDirectories();

        String tmp = "These are paths to be indexed.\n";

        //Check input(s).
        if(path.isEmpty() || path.length() == 0) return "Input error.";

        //Split a string into many paths with ';'
        String[] paths = path.split(";");

        for(String dir : paths) {
            Path dir_path = Paths.get(dir);
            if(Files.exists(dir_path)) {
                if (!contains(dataDir, dir_path)) {    //If path hasn't been added.
                    dataDir.add(dir_path.toFile()); //Add a path into a list of directory to be indexed.
                    watchDir(dir_path);
                    tmp += dir_path.toString() + "\n";
                }
                else {
                    tmp += dir + " is already added.\n";
                    logger.info(dir + " is a duplicated path.");
                }
            }
            else {
                tmp += dir + " is an incorrect path, Please enter an exist path.\n";
                logger.warning(dir + " is an incorrect path, Please enter an exist path.");
            }
        }
        return tmp; //Report back what path had been added.
    }

    /**
     * Assign a new thread to watch a data directory.
     * @param path  Path to be watched.
     */
    private void watchDir(Path path){
        if(watcher_service != null && watcher_thread != null) {
            watcher_service.registerAll(path);
        }
        else{
            try {
                watcher_service = new WatchService(path, true);
                watcher_thread = new Thread(watcher_service);
                watcher_thread.setName("WatchService");
                watcher_thread.start();
            } catch (IOException e) {
                logger.severe("Watch service malfunction, failed to start the service, no changes will be recorded.");
            }
        }
    }

    /**
     * Unwatch a directory from the watch service.
     * @param path  Path of directory to be unwatch.
     */
    private void unWatchDir(Path path){
        watcher_service.deRegisterAll(path);
    }

    /**
     * Use this to check if data directory list already has the path or not.
     * @param dataDir   ArrayList of data directory.
     * @param path      Path to be checked.
     * @return          True, if path already in the ArrayList, otherwise false.
     */
    private boolean contains(ArrayList<File> dataDir, Path path){
        //File path_dir = new File(path.toString());
        File path_dir = path.toFile();
        for(File dir : dataDir){
            try {
                if(Files.isSameFile(path_dir.toPath(), dir.toPath())) return true;
            } catch (IOException e) {
                logger.warning("Cannot check isSameFile with " + path_dir.toPath() + ", " +
                dir.toPath());
            }
        }
        return false;
    }

    public static boolean hasPath(Path path){
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        return directoryHandler.contains(directoryHandler.getDataDirectories(), path);
    }
    public void stopWatchService(){
        if(watcher_service != null) watcher_service.kill();
    }
}
