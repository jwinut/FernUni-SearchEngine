package com.fernuni.searchengine;

import com.fernuni.searchengine.FileWatcher.WatchService;
import com.fernuni.searchengine.SearchEngine.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
		logger.addHandler(fh);
		logger.setLevel(Level.ALL);
		logger.finest("TheSearchEngine is starting...");
		SpringApplication.run(RESTController.class, args);
	}

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

	@RequestMapping(value = "/")
	public @ResponseBody String isRunning() {
		return "Welcome, to The Search Engine.";
	}

	@RequestMapping(value = "/index")
	public @ResponseBody String index(){
		logger.info("[/index] was called.");
		Indexer indexer = Indexer.getIndexer();
		if(indexer == null){
			logger.severe("Indexer is NULL, it is not possible to index.");
			return "Indexer is NULL, it is not possible to index.";
		}
		Thread t_index = new Thread(indexer);
		t_index.start();
		try {
			t_index.join();
		}
		catch (InterruptedException e){
			logger.severe("Thread error: " + e.toString());
		}
		return "Total files indexed: " + indexer.getNumOfFilesIndexed();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/deleteOldIndex")
	public @ResponseBody boolean deleteOldIndex(){
		logger.info("[/deleteOldIndex] was called.");
		IndexManager manager = new IndexManager();
		return manager.deleteIndex();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/deleteDataDir", params = "path")
	public @ResponseBody boolean deleteDataDir(@RequestParam String path){
		logger.info("[/deleteDataDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.clearDataDir(path);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/setIndexDir", params = "path")
	public @ResponseBody boolean setIndexDir(@RequestParam String path){
		logger.info("[/setIndexDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.setIndexDirectory(path);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/addDataDir", params = "path")
	public @ResponseBody String addDataDir(@RequestParam String path){
		logger.info("[/addDataDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.addDataDir(path);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status")
	public @ResponseBody String statusReport() {
		logger.info("[/status] was called.");
		return IndexManager.statusReport();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status/dataDir")
	public @ResponseBody String statusDataDir(){
		logger.info("[/status/dataDir] was called.");
		String dataDir_str = DirectoryHandler.getDirectoryHandler().getDataDirectoriesString();
		if(dataDir_str == null|| dataDir_str.length() == 0 || dataDir_str.equals("") ) return "No data directory has been entered.";
		else return dataDir_str;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status/indexDir")
	public @ResponseBody String statusIndexDir(){
		logger.info("[/status/indexDir] was called.");
		String indexDir_str = DirectoryHandler.getDirectoryHandler().getIndexDirectoryString();
		if(indexDir_str == null|| indexDir_str.length() == 0 || indexDir_str.equals("") ) return "No index directory has been entered.";
		else return indexDir_str;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search", params = "searchString")
	public @ResponseBody ArrayList<Result> searchString(@RequestParam String searchString){
		logger.info("[/search] was called with parameters\t[searchString:" +
				searchString + "]");
		Searcher searcher = new Searcher();
		return searcher.run(searchString);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/useContent")
	public @ResponseBody boolean setContentStoreTrue(){
		logger.info("[/useContent] was called.");
		return IndexManager.setContentStoreTrue();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/notUseContent")
	public @ResponseBody boolean setContentStoreFalse(){
		logger.info("[/notUseContent] was called.");
		return IndexManager.setContentStoreFalse();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/usePreContent")
	public @ResponseBody boolean setPreContentStoreTrue(){
		logger.info("[/usePreContent] was called.");
		return IndexManager.setPreContentStoreTrue();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/notUsePreContent")
	public @ResponseBody boolean setPreContentStoreFalse(){
		logger.info("[/notUsePreContent] was called.");
		return IndexManager.setPreContentStoreFalse();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/autoIndex")
	public @ResponseBody boolean setAutoIndexOn(){
		logger.info("[/autoIndex] was called.");
		WatchService.setAuto(true);
		if(WatchService.isAuto()) return true;
		else return false;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/noAutoIndex")
	public @ResponseBody boolean setAutoIndexOff(){
		logger.info("[/noAutoIndex] was called.");
		WatchService.setAuto(false);
		if(WatchService.isAuto()) return true;
		else return false;
	}

	@RequestMapping(value = "/stop")
	public @ResponseBody void exit(){
		logger.info("Closing the program...");
		fh.close();
		DirectoryHandler.getDirectoryHandler().stopWatchService();
		System.exit(0);
	}

}
