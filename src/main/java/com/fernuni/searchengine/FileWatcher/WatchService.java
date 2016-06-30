/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.fernuni.searchengine.FileWatcher;

import com.fernuni.searchengine.RESTController;
import com.fernuni.searchengine.SearchEngine.DirectoryHandler;
import com.fernuni.searchengine.SearchEngine.IndexManager;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Example to watch a directory (or tree) for changes to files.
 * Modified from the original to use with TheSearchEngine.
 */
public class WatchService implements Runnable{

    //Log related instances.
    private static SimpleDateFormat sdf_file_log = new SimpleDateFormat("yyyy-MM-dd");
    private static String logfile_str = "log" + File.separatorChar + "files_changes" + File.separatorChar + "file_changes_log_" + sdf_file_log.format(Calendar.getInstance().getTime()) + ".log";
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.FileWatcher.WatchDir");
    private static FileHandler fh;
    static {
        try{
            fh = new FileHandler(logfile_str, true);
        } catch (IOException e){
            logger.severe("Cannot log to file: " + logfile_str);
        }
        logger.addHandler(fh);
        logger.addHandler(RESTController.fh);
        logger.setLevel(Level.ALL);
    }

    private final java.nio.file.WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private volatile boolean isStop = false;
    private IndexManager indexManager;
    private static boolean auto = true;

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    public void register(Path dir) {
        if(dir.equals(DirectoryHandler.getDirectoryHandler().getIndexDirectory().toPath()))
            return;
        try {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                    logger.info("register: " + dir);
                } else {
                    if (!dir.equals(prev)) {
                        logger.info("update: " + prev + " -> " + dir);
                    }
                }
            }
            keys.put(key, dir);
        }
        catch (IOException e){
            logger.severe("Error register: " + e.toString());
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    public void registerAll(final Path start) {
        try {
            // register directory and sub-directories
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e){
            logger.severe("Error register all: " + e.toString());
        }
    }

    /**
     * De-Register a given path from a watch service, stop watching a directory with a given path.
     * @param path  Path to a directory.
     * @return  De-registered path.
     * @throws IOException
     */
    public Path deRegister(Path path) throws IOException {
        WatchKey key = path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        Path dir = keys.get(key);
        if(dir.equals(path)){
            logger.info("de-register: " + dir.toString());
            key.cancel();
            return dir;
        }
        else{
            logger.warning("de-register failed: " + dir.toString());
            return null;
        }
    }

    /**
     * De-Register a given path and sub-directory inside a directory according to this path.
     * @param start A path to be de-registered.
     */
    public void deRegisterAll(Path start){
        try {
            // register directory and sub-directories
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    if(!DirectoryHandler.hasPath(dir))  //Don't de-register path on the list.
                        deRegister(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e){
            logger.severe("Error de-register all: " + e.toString());
        }
        catch (Exception e){
            logger.severe("Error de-register all:" + e.toString());
        }
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public WatchService(Path dir, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<>();
        this.recursive = recursive;

        if (recursive) {
            logger.info("Scanning " + dir + " ...");
            registerAll(dir);
            logger.info("Done.");
        } else {
            register(dir);
        }

        // enable trace after initial registration
        this.trace = true;

        indexManager = new IndexManager();
    }

    /**
     * Process all events for keys queued to the watcher
     * If it is possible to watch only visible files, it will be more practical for use.
     */
    public void run() {
        while(!isStop) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            } catch (ClosedWatchServiceException e){
                logger.info("WatchService has been close, Stop servicing...");
                break;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                logger.warning("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                //Log into a file.
                logger.info("" + event.kind().name() + ": " + child);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    if(isAuto()) {
                        try {
                            indexManager.addFileToIndex(child.toFile());
                        } catch (IOException e) {
                            logger.severe("Failed to add file: " + child.toString() + " to index, Please re-index to update index.");
                        }
                    }
                    /*try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }*/
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        registerAll(child);
                    }
                }

                if (kind == ENTRY_DELETE && isAuto()) {
                    indexManager.deleteDocumentFromIndexUsingPath(child);
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }

    }

    /**
     *  Stop the watcher when called.
     */
    public void kill(){
        System.out.println("kill");
        try {
            logger.info("Stopping WatchService...");
            watcher.close();
            fh.close();
        } catch (IOException e) {
            logger.warning("Cannot kill thread watch service. WatchService hasn't been initialize.");
            logger.warning(e.toString());
        }
        isStop = true;
    }

    /**
     * Indicator of auto indexing.
     * @return  True, if auto indexing is ON.
     */
    public static boolean isAuto() {
        return auto;
    }

    /**
     * Set auto indexing ON or OFF
     * @param auto  True if you want auto indexing to be on.
     */
    public static void setAuto(boolean auto) {
        WatchService.auto = auto;
    }
}
