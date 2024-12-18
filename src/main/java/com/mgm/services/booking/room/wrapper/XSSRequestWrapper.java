package com.mgm.services.booking.room.wrapper;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.owasp.esapi.ESAPI;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Log4j2
public class XSSRequestWrapper extends HttpServletRequestWrapper {

    private byte[] rawData;
    private HttpServletRequest request;
    private ResettableServletInputStream servletStream;

    public XSSRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
        this.servletStream = new ResettableServletInputStream();
    }

    public void resetInputStream(byte[] newRawData) {
        rawData = newRawData;
        servletStream.stream = new ByteArrayInputStream(newRawData);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getReader(), Charsets.UTF_8.name());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }
        return servletStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getReader(), Charsets.UTF_8.name());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }
        return new BufferedReader(new InputStreamReader(servletStream));
    }

    private class ResettableServletInputStream extends ServletInputStream {

        private InputStream stream;

        @Override
        public int read() throws IOException {
            return stream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // no handle
        }
    }

    @Override
    public String[] getParameterValues(String parameter) {
        String[] values = super.getParameterValues(parameter);
        if (values == null) {
            return null;
        }
        int count = values.length;
        String[] encodedValues = new String[count];
        for (int i = 0; i < count; i++) {
            encodedValues[i] = stripXSS(values[i]);
        }
        return encodedValues;
    }

    @Override
    public String getParameter(String parameter) {
        String value = super.getParameter(parameter);
        return stripXSS(value);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> result = new ArrayList<>();
        Enumeration<String> headers = super.getHeaders(name);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (StringUtils.isNotBlank(header)) {
                String[] tokens = header.split(",");
                for (String token : tokens) {
                    token = dropHeaderOnXss(token);
                    if (StringUtils.isEmpty(token)) {
                        log.warn("Value of header {} dropped due to XSS", name);
                    }
                    result.add(token);
                }
            }
        }
        return Collections.enumeration(result);
    }

    public static String stripXSS(String value) {
        if (value == null) {
            return null;
        }
        value = value.replaceAll("\0", "");
        return Jsoup.clean(value, Safelist.none());
    }

    public static String dropHeaderOnXss(String value) {
        if (value == null) {
            return null;
        }
        try {
            value = ESAPI.encoder().canonicalize(value).replaceAll("\0", "");
        } catch (Exception e) {
            return null;
        }
        return Jsoup.clean(value, Safelist.none());
    }
}
