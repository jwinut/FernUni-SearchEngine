package com.fernuni.searchengine;

import com.fernuni.searchengine.FileWatcher.WatchService;
import com.fernuni.searchengine.SearchEngine.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.URIUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.*;

@SpringBootApplication
@RestController
@CrossOrigin
/**
 * This is a REST Controller, by Spring.
 * for more information, visit spring.io or just Google it.
 */
public class RESTController {
    private static SimpleDateFormat sdf_file_log = new SimpleDateFormat("yyyy-MM-dd");
    private static String logfile_str = "log" + File.separatorChar + "info_log_" + sdf_file_log.format(Calendar.getInstance().getTime()) + ".log";
    private static Logger logger = Logger.getLogger("com.fernuni.searchengine.RESTController");
    public static FileHandler fh;
    public static final String PROPERTIES = "setting.properties";
    public static final String INDEX = "index_dir";
    public static final String DATA = "data_dirs";
    public static final String AUTO = "auto_index";
    public static final String PRECONTENT = "pre_content";
    public static final String CONTENT = "content";
    private static boolean isFirstTime = true;

    static {
        try {
            Path path = Paths.get("./log/files_changes");
            Files.createDirectories(path);
            fh = new FileHandler(logfile_str, true);
        } catch (IOException e) {
            logger.severe("Cannot log to file: " + logfile_str);
        }
        fh.setFormatter(new Formatter() {
            public String format(LogRecord rec) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                StringBuilder buf = new StringBuilder(1000);
                buf.append(sdf.format(Calendar.getInstance().getTime()).toString());
                buf.append(' ');
                buf.append(rec.getLevel());
                buf.append(' ');
                buf.append(rec.getLoggerName());
                buf.append(" : ");
                buf.append(formatMessage(rec));
                buf.append('\n');
                return buf.toString();
            }
        });
    }

    public static void main(String[] args) {
        //Starting log.
        logger.addHandler(fh);
        logger.setLevel(Level.ALL);
        logger.finest("TheSearchEngine is starting...");
        try {
            //Read config.properties.
            InputStream inputStream = new FileInputStream(PROPERTIES);
            Properties properties = new Properties();
            properties.load(inputStream);
            if (properties.getProperty("first_time").equals("No")) isFirstTime = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SpringApplication.run(RESTController.class, args);
    }

    /**
     * This method is called when CORs happens
     *
     * @param request
     * @return
     */
    @RequestMapping()
    @SuppressWarnings("unchecked")
    public ResponseEntity handle(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        //headers.add("Access-Control-Allow-Origin", "*");
        headers.setAccessControlAllowOrigin("/**");
        logger.info("Sent ResponseEntity with CORS header from " +
                "\tIP: " + request.getRemoteHost());
        return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
    }

    /**
     * Ready check for front-end.
     *
     * @return    "Welcome, to The Search Engine."
     */
    @RequestMapping(value = "/")
    public
    @ResponseBody
    String isRunning() {
        return "Welcome, to The Search Engine.";
    }

    /**
     * Index files in data directories.
     *
     * @return Messages, total number of indexed file.
     */
    @RequestMapping(value = "/index")
    public
    @ResponseBody
    String index() {
        logger.info("[/index] was called.");
        Indexer indexer = Indexer.getIndexer();
        if (indexer == null) {
            logger.severe("Indexer is NULL, it is not possible to index.");
            return "Indexer is NULL, it is not possible to index.";
        }
        Thread t_index = new Thread(indexer);
        t_index.start();
        try {
            t_index.join();
        } catch (InterruptedException e) {
            logger.severe("Thread error: " + e.toString());
        }
        return "Total files indexed: " + indexer.getNumOfFilesIndexed();
    }

    /**
     * Delete index.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.DELETE, value = "/deleteOldIndex")
    public
    @ResponseBody
    boolean deleteOldIndex() {
        logger.info("[/deleteOldIndex] was called.");
        IndexManager manager = new IndexManager();
        return manager.deleteIndex();
    }

    /**
     * Delete a data directory path.
     *
     * @param path Path to be removed, or '*' to remove all path.
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/deleteDataDir", params = "path")
    public
    @ResponseBody
    boolean deleteDataDir(@RequestParam String path) {
        logger.info("[/deleteDataDir] was called with parameters\t[path:" +
                path + "]");
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        boolean temp = directoryHandler.clearDataDir(path);
        updateProperties();
        return temp;
    }

    /**
     * Set index directory path.
     *
     * @param path Index directory path.
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/setIndexDir", params = "path")
    public
    @ResponseBody
    boolean setIndexDir(@RequestParam String path) {
        logger.info("[/setIndexDir] was called with parameters\t[path:" +
                path + "]");
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        boolean temp = directoryHandler.setIndexDirectory(path);
        updateProperties();
        return temp;
    }

    /**
     * Add a data directory path.
     *
     * @param path Data directory path.
     * @return Messages report.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/addDataDir", params = "path")
    public
    @ResponseBody
    String addDataDir(@RequestParam String path) {
        logger.info("[/addDataDir] was called with parameters\t[path:" +
                path + "]");
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        String temp = directoryHandler.addDataDir(path);
        updateProperties();
        return temp;
    }

    /**
     * Status report.
     *
     * @return Messages contain status of TheSearchEngine.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/status")
    public
    @ResponseBody
    String statusReport() {
        logger.info("[/status] was called.");
        return IndexManager.statusReport();
    }

    /**
     * Status of data directories.
     *
     * @return Data directory paths.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/status/dataDir")
    public
    @ResponseBody
    String statusDataDir() {
        logger.info("[/status/dataDir] was called.");
        String dataDir_str = DirectoryHandler.getDirectoryHandler().getDataDirectoriesString();
        if (dataDir_str == null || dataDir_str.length() == 0 || dataDir_str.equals(""))
            return "No data directory has been entered.";
        else return dataDir_str;
    }

    /**
     * Status of index directory.
     *
     * @return Index directory path.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/status/indexDir")
    public
    @ResponseBody
    String statusIndexDir() {
        logger.info("[/status/indexDir] was called.");
        String indexDir_str = DirectoryHandler.getDirectoryHandler().getIndexDirectoryString();
        if (indexDir_str == null || indexDir_str.length() == 0 || indexDir_str.equals(""))
            return "No index directory has been entered.";
        else return indexDir_str;
    }

    /**
     * Search query from index.
     *
     * @param searchString Query String.
     * @return ArrayList of Results.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/search", params = "searchString")
    public
    @ResponseBody
    ArrayList<Result> searchString(@RequestParam String searchString) {
        logger.info("[/search] was called with parameters\t[searchString:" +
                searchString + "]");
        Searcher searcher = new Searcher();
        return searcher.run(searchString);
    }

    /**
     * Set TheSearchEngine to store contents of files in index.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/useContent")
    public
    @ResponseBody
    boolean setContentStoreTrue() {
        logger.info("[/useContent] was called.");
        IndexManager.setContentStoreTrue();
        updateProperties();
        return IndexManager.isContent();
    }

    /**
     * Set TheSearchEngine to not store contents of files in index.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/notUseContent")
    public
    @ResponseBody
    boolean setContentStoreFalse() {
        logger.info("[/notUseContent] was called.");
        IndexManager.setContentStoreFalse();
        updateProperties();
        return !IndexManager.isContent();
    }

    /**
     * Set TheSearchEngine to store pre-contents of files in index.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/usePreContent")
    public
    @ResponseBody
    boolean setPreContentStoreTrue() {
        logger.info("[/usePreContent] was called.");
        IndexManager.setPreContentStoreTrue();
        updateProperties();
        return IndexManager.isPreContent();
    }

    /**
     * Set TheSearchEngine to not store pre-contents of files in index.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/notUsePreContent")
    public
    @ResponseBody
    boolean setPreContentStoreFalse() {
        logger.info("[/notUsePreContent] was called.");
        IndexManager.setPreContentStoreFalse();
        updateProperties();
        return !IndexManager.isPreContent();
    }

    /**
     * Set auto indexing on.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/autoIndex")
    public
    @ResponseBody
    boolean setAutoIndexOn() {
        logger.info("[/autoIndex] was called.");
        WatchService.setAuto(true);
        updateProperties();
        return WatchService.isAuto();
    }

    /**
     * Set auto indexing off.
     *
     * @return true, if success.
     */
    @RequestMapping(method = RequestMethod.PUT, value = "/noAutoIndex")
    public
    @ResponseBody
    boolean setAutoIndexOff() {
        logger.info("[/noAutoIndex] was called.");
        WatchService.setAuto(false);
        updateProperties();
        return !WatchService.isAuto();
    }

    /**
     * Stop the server and services.
     */
    @RequestMapping(value = "/stop")
    public
    @ResponseBody
    void exit() {
        updateProperties();
        logger.info("Closing the program...");
        fh.close();
        DirectoryHandler.getDirectoryHandler().stopWatchService();
        System.exit(0);
    }

    /**
     * Is index stores pre-contents of files ?
     *
     * @return    "PreContent is on." or "PreContent is off."
     */
    @RequestMapping(value = "/isPrecontent")
    public
    @ResponseBody
    String isPrecontent() {
        logger.info("[/isPrecontent] was called.");
        return IndexManager.isPreContent() ? "PreContent is on." : "PreContent is off.";
    }

    /**
     * Is index stores contents of files ?
     *
     * @return    "Content is on." or "Content is off."
     */
    @RequestMapping(value = "/isContent")
    public
    @ResponseBody
    String isContent() {
        logger.info("[/isContent] was called.");
        return IndexManager.isContent() ? "Content is on." : "Content is off.";
    }

    /**
     * Is TheSearchEngine auto indexing new files ?
     *
     * @return    "Auto index is on." or "Auto index is off."
     */
    @RequestMapping(value = "/isAutoIndex")
    public
    @ResponseBody
    String isAutoIndex() {
        logger.info("[/isAutoIndex] was called.");
        return WatchService.isAuto() ? "Auto index is on." : "Auto index is off.";
    }

    /**
     * Search with Bing.
     *
     * @param searchString String query.
     * @param skip         number of results to be skipped.
     * @return Search results in String with JSON format.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/searchBing", produces = "application/json")
    public
    @ResponseBody
    String searchBing(@RequestParam String searchString, @RequestParam int skip) {
        logger.info("[/searchBing] was called with parameters \"" + searchString + "\", skip = " + skip);
        try {
            String webPage = "https://api.datamarket.azure.com/Bing/SearchWeb/v1/Web?Query='" + searchString + "'&$format=json&$skip=" + skip;
            webPage = URIUtil.encodeQuery(webPage);
            //Basic Authentication with Bing API
            String name = "";
            //Key belongs to Mr.Winut Jiraruekmongkol, KMITL, Thailand
            String password = "2CSGEb+d8yB6RuWsCM5yoO/hCd8BPWtMWno0MqXYfCk=";

            String authString = name + ":" + password;
            //System.out.println("auth string: " + authString);
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = new String(authEncBytes);
            //System.out.println("Base64 encoded auth string: " + authStringEnc);

            URL url = new URL(webPage);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuffer sb = new StringBuffer();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            String result = sb.toString();
            return result;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This @PostConstruct runs after initialization, it reads config.properties and setup TheSearchEngine to last config.
     */
    public
    @PostConstruct
    void setting() {
        try {
            InputStream inputStream = new FileInputStream(PROPERTIES);
            Properties properties = new Properties();
            properties.load(inputStream);
            //If it is not a first time use, set flag.
            if (properties.getProperty("first_time").equals("No")) {
                isFirstTime = false;
            }
            //Read and add configurations.
            String index_path = properties.getProperty("index_dir");
            String[] data_dir_paths = properties.getProperty("data_dirs").split(",");
            setIndexDir(index_path);
            for (String dir : data_dir_paths) {
                addDataDir(dir);
            }
            //Setup features.
            if (properties.getProperty(AUTO).equals("No")) setAutoIndexOff();
            if (properties.getProperty(PRECONTENT).equals("No")) setPreContentStoreFalse();
            if (properties.getProperty(CONTENT).equals("Yes")) setContentStoreTrue();
            inputStream.close();
        } catch (FileNotFoundException e) {
            logger.warning("file setting.properties is not present, assuming 1st time run.");
        } catch (IOException e) {
            logger.warning("file setting.properties is unusable, assuming 1st time run.");
        } catch (NullPointerException e) {
            logger.warning("setting.properties is not complete, force to setup again.");
        }
    }

    /**
     * This method is used to update the config.properties file according to present configuration.
     */
    private static void updateProperties() {
        DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
        OutputStream outputStream;
        Properties properties = new Properties();
        try {
            outputStream = new FileOutputStream(PROPERTIES);
            properties.setProperty("first_time", "No");
            properties.setProperty(INDEX, directoryHandler.getIndexDirectoryString());
            properties.setProperty(DATA, directoryHandler.getDataDirectoriesString().replace('\n', ','));
            properties.setProperty(AUTO, WatchService.isAuto() ? "Yes" : " No");
            properties.setProperty(PRECONTENT, IndexManager.isPreContent() ? "Yes" : " No");
            properties.setProperty(CONTENT, IndexManager.isContent() ? "Yes" : "No");
            properties.store(outputStream, Calendar.getInstance().getTime().toString());
            outputStream.close();
        } catch (FileNotFoundException e1) {
            logger.warning("Couldn't create a new file name: " + PROPERTIES + ", changes will not be saved.\n" + e1.toString());
        } catch (IOException e1) {
            logger.warning("Couldn't save changes to file " + PROPERTIES + ".\n" + e1.toString());
        }
    }

    /**
     * Launch a web browser with start pages.
     *
     * @throws IOException
     */
    private static void start() throws IOException {
        File html;
        if (isFirstTime) {
            html = new File("intern/wait_index.html");
        } else {
            html = new File("intern/wait_search.html");
        }
        Desktop.getDesktop().browse(html.toURI());
    }
}
