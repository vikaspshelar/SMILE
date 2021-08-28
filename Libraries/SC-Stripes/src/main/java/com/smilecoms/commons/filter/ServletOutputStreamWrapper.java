package com.smilecoms.commons.filter;

import com.smilecoms.commons.base.cache.CacheHelper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper to provide a concrete implementation of the servlet output stream,
 * so we can wrap other streams. Such as in a filter wrapping a servlet
 * response.
 *
 * @author thein
 *
 */
public class ServletOutputStreamWrapper extends ServletOutputStream {

    OutputStream _out;
    boolean closed = false;
    private static Logger log = LoggerFactory.getLogger(ServletOutputStreamWrapper.class);
    private HttpServletResponse origResponse = null;
    private String ssoid = null;

    public ServletOutputStreamWrapper(OutputStream realStream, HttpServletResponse origResponse, String ssoid) {
        log.debug("In ServletOutputStreamWrapper constructor");
        this._out = realStream;
        this.origResponse = origResponse;
        this.ssoid = ssoid;
    }

    @Override
    public void close() throws IOException {
        log.debug("In close");
        if (closed) {
            log.debug("Already closed");
            return;
        }
        flush();
        _out.close();
        closed = true;
    }

    @Override
    public void flush() throws IOException {
        log.debug("In flush");
        if (closed) {
            log.debug("Closed so wont flush");
            return;
        }
        _out.flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            log.warn("1: Cannot write to a closed output stream");
            throw new IOException("Cannot write to a closed output stream");
        }
        if (log.isDebugEnabled()) {
            log.debug("In write of int [{}] ", b);
        }
        _out.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("In write of [{}] bytes", b.length);
        }
        write(b, 0, b.length);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            log.warn("2: Cannot write to a closed output stream");
            throw new IOException("Cannot write to a closed output stream");
        }
        if (log.isDebugEnabled()) {
            log.debug("In write of [{}] bytes", b.length);
        }
        _out.write(b, off, len);
    }

    @Override
    public void print(String s) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("In print of [{}]", s);
        }
        _out.write(s.getBytes());
    }

    @Override
    public void print(char c) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("In print of [{}]", c);
        }
        _out.write(c);
    }

    @Override
    public void print(boolean b) throws IOException {
        super.print(b); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void print(int i) throws IOException {
        super.print(i); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void print(long l) throws IOException {
        super.print(l); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void print(float f) throws IOException {
        super.print(f); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void print(double d) throws IOException {
        super.print(d); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println() throws IOException {
        super.println(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(String s) throws IOException {
        super.println(s); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(boolean b) throws IOException {
        super.println(b); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(char c) throws IOException {
        super.println(c); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(int i) throws IOException {
        super.println(i); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(long l) throws IOException {
        super.println(l); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(float f) throws IOException {
        super.println(f); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void println(double d) throws IOException {
        super.println(d); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isReady() {
        log.debug("In isReady");
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        log.debug("setWriteListener");
    }

    void writeOutput() {
        try {
            close();
            String content = (((ByteArrayOutputStream) _out).toString("UTF-8"));
            //log.debug("Writing result to origional response [{}]", content);
            origResponse.getWriter().write(content);
            if (log.isDebugEnabled()) {
                log.debug("Writing response of length [{}] to cached for user [{}]", content.length(), ssoid);
            }
            CacheHelper.putInRemoteCache("Track_" + ssoid, content, 600);
        } catch (Exception e) {
            log.warn("Unable to put response in cache: " + e.toString());
        }
    }
    
    
}