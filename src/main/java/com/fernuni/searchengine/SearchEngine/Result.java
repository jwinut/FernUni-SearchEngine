package com.fernuni.searchengine.SearchEngine;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/2/2016 AD.
 * This class is a template of results to be returned to front-end.
 * It is meant to parse into a JSON format.
 */
public class Result {
    public String file_name;
    public String file_path;
    public String file_pre_contents;
    public String file_type;

    public String getFile_type() {
        return file_type;
    }

    public void setFile_type(String file_type) {
        this.file_type = file_type;
    }

    public String getFile_pre_contents() {
        return file_pre_contents;
    }

    public void setFile_pre_contents(String file_pre_contents) {
        this.file_pre_contents = file_pre_contents;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public Result(String file_name, String file_path, String file_pre_contents, String file_type) {
        this.file_name = file_name;
        this.file_path = file_path;
        this.file_pre_contents = file_pre_contents;
        this.file_type = file_type;
    }

    public Result() {
    }
}
