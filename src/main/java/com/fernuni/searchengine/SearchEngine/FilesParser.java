package com.fernuni.searchengine.SearchEngine;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Winut Jiraruekmongkol, KMITL, Thailand on 6/4/2016 AD.
 * FilesParser's instance can parse supported file types into a String.
 */
class FilesParser {

    /**
     * Parse a supported file contents into a String.
     *
     * @param file Supported file to be parsed.
     * @return Contents of the file in a String format.
     * @throws IOException   File not found?
     * @throws SAXException
     * @throws TikaException Unsupported file type? cannot parse the file.
     */
    String parseToString(File file) throws IOException, SAXException, TikaException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        try (InputStream stream = new FileInputStream(file)) {
            parser.parse(stream, handler, metadata);
            return handler.toString();
        }
    }
}
