package com.smilecoms.commons.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.*;

public class InputStreamHandler extends Thread {

    /**
     * Stream being read
     */
    private InputStream m_stream;
    /**
     * The StringBuffer holding the captured output
     */
    private StringBuffer m_captureBuffer;
    private static final Logger log = LoggerFactory.getLogger(InputStreamHandler.class.getName());
    
    public InputStreamHandler(StringBuffer captureBuffer, InputStream stream) {
        m_stream = stream;
        m_captureBuffer = captureBuffer;
        start();
    }

    /**
     * Stream the data.
     */
    @Override
    public void run() {
        try {
            int nextChar;
            while ((nextChar = m_stream.read()) != -1) {
                m_captureBuffer.append((char) nextChar);
            }
        } catch (IOException ioe) {
        }
        if (log.isDebugEnabled()) {
            log.debug("Finished getting stream:" + m_captureBuffer.toString());
        }
    }
}
