package com.fernuni.searchengine;

import com.fernuni.searchengine.SearchEngine.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

@SpringBootApplication
@RestController
@CrossOrigin
/**
 * This is a REST Controller, by Spring.
 * for more information, visit spring.io or just Google it.
 */
public class RESTController {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static void main(String[] args) {
		SpringApplication.run(RESTController.class, args);
	}

	@RequestMapping()
	@SuppressWarnings("unchecked")
	public ResponseEntity handle(@RequestHeader HttpHeaders httpHeaders, HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		//headers.add("Access-Control-Allow-Origin", "*");
		headers.setAccessControlAllowOrigin("/**");
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"Sent ResponseEntity with CORS header from " +
				"\tIP: " + request.getRemoteHost());
		return new ResponseEntity(headers, HttpStatus.NO_CONTENT);
	}

	@RequestMapping(value = "/")
	public @ResponseBody String isRunning() {
		return "Welcome, to The Search Engine.";
	}

	@RequestMapping(value = "/index")
	public @ResponseBody String index(){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/index] was called.");
		Indexer indexer = Indexer.getIndexer();
		if(indexer == null){
			System.out.println(sdf.format(Calendar.getInstance().getTime()) + "\t " +
					"Indexer is NULL, it is not possible to index.");
			return "Indexer is NULL, it is not possible to index.";
		}
		Thread t_index = new Thread(indexer);
		t_index.start();
		try {
			t_index.join();
		}
		catch (InterruptedException e){
			System.out.print(sdf.format(Calendar.getInstance().getTime()) +
					"\t " + "Thread error: " + e.toString());
		}
		return "Total files indexed: " + indexer.getNumOfFilesIndexed();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/deleteOldIndex")
	public @ResponseBody boolean deleteOldIndex(){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/deleteOldIndex] was called.");
		IndexManager manager = new IndexManager();
		return manager.deleteIndex();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/deleteDataDir", params = "path")
	public @ResponseBody boolean deleteDataDir(@RequestParam String path){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/deleteDataDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.clearDataDir(path);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/setIndexDir", params = "path")
	public @ResponseBody boolean setIndexDir(@RequestParam String path){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/setIndexDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.setIndexDirectory(path);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/addDataDir", params = "path")
	public @ResponseBody String addDataDir(@RequestParam String path){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/addDataDir] was called with parameters\t[path:" +
				path + "]");
		DirectoryHandler directoryHandler = DirectoryHandler.getDirectoryHandler();
		return directoryHandler.addDataDir(path);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status")
	public @ResponseBody String statusReport() {
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/status] was called.");
		return Indexer.statusReport();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status/dataDir")
	public @ResponseBody String statusDataDir(){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/status/dataDir] was called.");
			String dataDir_str = DirectoryHandler.getDirectoryHandler().getDataDirectoriesString();
			if(dataDir_str == null|| dataDir_str.length() == 0 || dataDir_str.equals("") ) return "No data directory has been entered.";
			else return dataDir_str;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/status/indexDir")
	public @ResponseBody String statusIndexDir(){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/status/indexDir] was called.");
		String indexDir_str = DirectoryHandler.getDirectoryHandler().getIndexDirectoryString();
		if(indexDir_str == null|| indexDir_str.length() == 0 || indexDir_str.equals("") ) return "No index directory has been entered.";
		else return indexDir_str;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/search", params = "searchString")
	public @ResponseBody ArrayList<Result> searchString(@RequestParam String searchString){
		System.out.println(sdf.format(Calendar.getInstance().getTime()) +
				"\t " +
				"[/search] was called with parameters\t[searchString:" +
				searchString + "]");
		Searcher searcher = new Searcher();
		return searcher.run(searchString);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/useContent")
	public @ResponseBody boolean setContentStoreTrue(){
		Indexer indexer = Indexer.getIndexer();
		return indexer.setContentStoreTrue();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/notUseContent")
	public @ResponseBody boolean setContentStoreFalse(){
		Indexer indexer = Indexer.getIndexer();
		return indexer.setContentStoreFalse();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/usePreContent")
	public @ResponseBody boolean setPreContentStoreTrue(){
		Indexer indexer = Indexer.getIndexer();
		return indexer.setPreContentStoreTrue();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/notUsePreContent")
	public @ResponseBody boolean setPreContentStoreFalse(){
		Indexer indexer = Indexer.getIndexer();
		return indexer.setPreContentStoreFalse();
	}

}
