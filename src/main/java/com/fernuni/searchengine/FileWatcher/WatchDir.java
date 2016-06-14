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

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 * Modified from the original to use with TheSearchEngine.
 */

public class WatchDir implements Runnable{

    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private volatile boolean isStop = false;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) {
        try {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (trace) {
                Path prev = keys.get(key);
                if (prev == null) {
                    System.out.format("register: %s\n", dir);
                } else {
                    if (!dir.equals(prev)) {
                        System.out.format("update: %s -> %s\n", prev, dir);
                    }
                }
            }
            keys.put(key, dir);
        }
        catch (IOException e){
            System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
            "Error register: " + e.toString());
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) {
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
            System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
            "Error register all: " + e.toString());
        }
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    public WatchDir(Path dir, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;

        if (recursive) {
            System.out.format(sdf.format(Calendar.getInstance().getTime()) +
                    "\t " + "Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t " + "Done.");
        } else {
            register(dir);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     * If it is possible to watch only visible files, it will be more practical for use.
     */
    public void run() {
        SimpleDateFormat sdf_file_log = new SimpleDateFormat("yyyy-MM-dd");
        String logfile_str = "file_changes_log_" + sdf_file_log.format(Calendar.getInstance().getTime());
        FileWriter writer;
        while(!isStop) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            } catch (ClosedWatchServiceException e){
                System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                        "\t " + "WatchService has been close, Stop servicing...");
                break;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                writer = getFileLogWriter(logfile_str, 1);
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // print out event
                System.out.format(sdf.format(Calendar.getInstance().getTime()) +
                        "\t " + "%s: %s\n", event.kind().name(), child);
                //Log into a file.
                try {
                    writer.write(sdf.format(Calendar.getInstance().getTime()) + "\t " +
                    event.kind().name() + ": " + child + '\n');
                } catch (IOException e) {
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t Cannot write log.");
                }
                try {
                    writer.close();
                } catch (IOException e) {
                    System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t Cannot close log's file writer.");
                }

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
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
        try {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t " + "Stopping WatchService...");
            watcher.close();
        } catch (IOException e) {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) +
                    "\t " + "Cannot kill thread watch service. WatchService hasn't been initialize. ");
            e.printStackTrace();
        }
        isStop = true;
    }

    /**
     * Create a FileWriter to write a log file.
     * @param filename  Log file's name.
     * @param count     Log file's number.
     * @return  FileWriter instance.
     */
    private FileWriter getFileLogWriter(String filename, int count){
        File logfile = new File(filename);
        try {
            FileWriter writer = new FileWriter(logfile, true);
            return writer;
        } catch (IOException e) {
            System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " + "Cannot create: " +
                    filename);
            filename += "(" + count +")";
            return getFileLogWriter(filename, ++count);
        }
    }
}
