package com.fernuni.searchengine.SearchEngine;

import com.fernuni.searchengine.FileWatcher.WatchDir;
import com.fernuni.searchengine.FileWatcher.WatchDirFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 5/30/2016 AD.
 * This class has all the information about where to get files and where to store other files including the index location.
 */
public class DirectoryHandler {
    private File indexDirectory;
    private ArrayList<File> dataDirectories;
    private static DirectoryHandler directoryHandler;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private ArrayList<Thread> watchers = new ArrayList<>();
    private ArrayList<WatchDir> runnables = new ArrayList<>();

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
        System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                "\t clearDataDir is called, and processing.");
        System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                "\t Status report before remove data directory.");
        Indexer.statusReport();
        ArrayList<File> dataDirs = getDataDirectories();
        if (path.equals("*")) {
            int size = dataDirs.size();
            for(int i = size - 1; i >= 0; i--){
                unWatchDir(i);
                this.dataDirectories.remove(i);
            }
            if(dataDirs.size() == 0) return true;
            else {
                System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                "Remove all data directory error. Cannot clear all directory.");
                return false;
            }
        } else {
            Iterator<File> fileIterator = dataDirs.iterator();
            Path dir_path;
            Path file_path = Paths.get(path);
            /**
             * Iterate through a list of paths, If matched, remove.
             */
            while(fileIterator.hasNext()){
                dir_path = Paths.get(fileIterator.next().getAbsolutePath());
                if(dir_path.equals(file_path)){  //If it is matched.
                    fileIterator.remove();  //remove the path that is matched.
                    //Report to a terminal.
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                            "\t Following path has been removed: " + dir_path);
                    Indexer.statusReport(); //Log
                    unWatchDir(file_path);
                    return true;
                }
            }
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t No directory match.");
            Indexer.statusReport();
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
    public File getIndexDirectory() {
        return indexDirectory;
    }

    /**
     * Set a index directory path.
     * @param path  Path to the index directory.
     * @return
     */
    public boolean setIndexDirectory(String path) {
        if(path != null && path.length() != 0) {
            File indexDir = new File(path);
            this.indexDirectory = indexDir;
            return true;
        }
        else {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " + "No new path has been set for index directory.");
            return false;
        }
    }

    /**
     * Getter of dataDirectories.
     * @return  dataDirectories.
     */
    public ArrayList<File> getDataDirectories() {
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
                    //dataDir.add(new File(dir));
                    dataDir.add(dir_path.toFile()); //Add a path into a list of directory to be indexed.
                    watchDir(dir_path);
                    tmp += dir_path.toString() + "\n";
                }
                else {
                    tmp += dir + " is already added.\n";
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                            "\t " + dir + " is a duplicated path.");
                }
            }
            else {
                tmp += dir + " is an incorrect path, Please enter an exist path.\n";
                System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " + dir +
                        " is an incorrect path, Please enter an exist path.");
            }
        }
        return tmp; //Report back what path had been added.
    }

    /**
     * Assign a new thread to watch a data directory.
     * @param path  Path to be watched.
     */
    private void watchDir(Path path){
        ArrayList<Thread> watchers = this.watchers;
        int index = 0;

        String path_str = path.toString();  //Get path as String.
        //If there is a watcher already running inside a new directory, remove it and assign a new one.
        if(!isWatching(path)){   //If the path is already in the system, just end this method.

            Iterator<Thread> watchersIterator = watchers.listIterator();   //Create an iterator through a data directory list.

            //Find a possible sub-directory inside a directory.
            while (watchersIterator.hasNext()){

                Thread watcher = watchersIterator.next();
                String watcher_dir = watcher.getName(); //Get the path of the registered directory, this directory is being watched.
                //If path (newly added directory) is a sub-directory inside a registered directory,
                //Then no need to add new Watcher service.
                if(path_str.contains(watcher_dir)){
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                    path + " is a sub-directory of " + watcher_dir + ", No watcher service has been added.");
                    return;
                }
                //If registered path is a sub-directory of a newly added path, then unwatch the old sub-directory,
                //and watch the directory instead.
                else if(watcher_dir.contains(path_str)){
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                            watcher_dir + " is a sub-directory of " + path + ", Unwatch thread " +
                            watcher.getName());
                    unWatchDir(watcher, runnables.remove(index));
                    watchersIterator.remove();
                }
                index++;
            }
        }
        else{
            System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                    path + " is already registered and watching.");
            return;
        }
        //Create a new factory thread, the new thread will create a new watcher to watch a directory.
        new Thread(new WatchDirFactory(path, true)).start();
    }

    /**
     * Unwatch a directory, automatically kill the thread and remove objects from Thread list and Runnable list.
     * @param index Index of the thread to be unwatched, newly added thread will be the last.
     */
    private void unWatchDir(int index){
        if(index >= 0 && index < watchers.size() && index < runnables.size()) { //Check input
            Thread watch_dir = watchers.remove(index);  //remove target from watcher's list.
            WatchDir runnable = runnables.remove(index);    //remove runnable target from the list.
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t " + "Stopping thread: " + watch_dir.getName());
            runnable.kill();    //Stop watching, cause runnable to stop.
            try {
                watch_dir.join();   //Stop thread.
            } catch (InterruptedException e) {
                System.out.println("Stop thread " + watch_dir.getName() +
                        "error: " + e.toString());
            }
        }
        else System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
        "Cannot unWatchDir at index [" + index + "]");
    }

    /**
     * Unwatch a directory, automatically kill the thread and remove objects from Thread list and Runnable list.
     * @param path  Path of directory to be unwatch.
     */
    private void unWatchDir(Path path){
        int index = 0;
        //Find the watcher which is watching a directory.
        for(Thread thread : watchers){
            if(thread.getName().equals(path.toString())) {  //Use the assigned name to find the correct thread.
                unWatchDir(index);  //unwatch a watcher and stop this method.
                return;
            }
            else index++;
        }

        //If program reach this line, it didn't unwatch anything.
        System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
        "No watcher is watching " + path.toString());
    }

    /**
     * Unwatch a directory, kill the given runnable and stop the given thread.
     * @param watcher
     * @param runnable
     */
    private void unWatchDir(Thread watcher, WatchDir runnable){
        System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                "\t " + "Stopping thread: " + watcher.getName());
        runnable.kill();
        try {
            watcher.join();
        } catch (InterruptedException e) {
            System.out.println("Stop thread " + watcher.getName() +
                    "error: " + e.toString());
        }
    }

    /**
     * Check a directory, If it is being watched then return true, otherwise false.
     * @param path  Path to a directory that will be check.
     * @return  True when the given path is being watched, otherwise false.
     */
    private boolean isWatching(Path path){
        ArrayList<Thread> watchers = this.watchers;
        for(Thread watcher : watchers){
            String watcher_name = watcher.getName();
            if(path.toString().equals(watcher_name)) return true;
        }
        return false;
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
                System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                        "Cannot check isSameFile with " + path_dir.toPath() + ", " +
                dir.toPath());
            }
        }
        return false;
    }

    /**
     * Getter of watchers list.
     * @return  ArrayList of watchers.
     */
    public ArrayList<Thread> getWatchers() {
        return watchers;
    }

    /**
     * Getter of runnable list.
     * @return  ArrayList of runnable objects.
     */
    public ArrayList<WatchDir> getRunnables() {
        return runnables;
    }

}
