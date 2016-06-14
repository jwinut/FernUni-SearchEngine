package com.fernuni.searchengine.FileWatcher;

import com.fernuni.searchengine.RESTController;
import com.fernuni.searchengine.SearchEngine.DirectoryHandler;
import com.fernuni.searchengine.SearchEngine.IndexManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/10/2016 AD.
 * Use this class to create an instance to create a watcher thread.
 */
public class WatchDirFactory implements Runnable{
    //Log related instances.
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.FileWatcher.WatchDirFactory");
    private static FileHandler fh = RESTController.fh;
    static {
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
    }

    private boolean recursive;
    private Path path;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private ArrayList<Thread> watchers;
    private ArrayList<WatchDir> runnables;

    /**
     * General constructor, create this instance to create a new watcher, this class reduce response time when create a new watcher service.
     * @param path  Path to be watched.
     * @param recursive Also watch sub-directory when true, otherwise set to false.
     */
    public WatchDirFactory(Path path, boolean recursive) {
        this.path = path;
        this.recursive = recursive;
        watchers = DirectoryHandler.getDirectoryHandler().getWatchers();
        runnables = DirectoryHandler.getDirectoryHandler().getRunnables();
    }

    /**
     * run this to create a new watcher thread, No guarantee of new watch service.
     */
    public @Override void run() {
        WatchDir watchDir = null;
        Path dir = path;
        Thread watch_dir = null;
        try {
            watchDir = new WatchDir(dir, recursive);
            watch_dir = new Thread(watchDir);
        } catch (IOException e) {
            logger.severe("Watcher thread error: " + e.toString());
        }
        if(watchDir != null && watch_dir != null) {
            watch_dir.setName(path.toString()); //Set a thread name as same as watched directory.
            watch_dir.start();
            watchers.add(watch_dir);    //Add a new watcher to the list, so it is possible to stop thread in the future.
            runnables.add(watchDir);    //Add a new runnable to the list, so it is possible to kill a running thread in the future.
        }
        else logger.info("No watcher has been assign to " + path);
    }
}
