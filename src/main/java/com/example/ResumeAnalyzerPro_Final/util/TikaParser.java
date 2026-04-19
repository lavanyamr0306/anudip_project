package com.example.ResumeAnalyzerPro_Final.util;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class TikaParser {

    public String parse(InputStream stream) {
        if (stream == null) {
            return "";
        }

        try {
            Tika tika = new Tika();
            String content = tika.parseToString(stream);
            return content != null ? content.trim() : "";
        } catch (Exception e) {
            // Log the error for debugging
            System.err.println("Tika parsing error: " + e.getMessage());
            return "";
        }
    }

}
