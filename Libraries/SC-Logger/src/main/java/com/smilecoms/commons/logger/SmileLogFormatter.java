package com.smilecoms.commons.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class SmileLogFormatter extends Formatter {

    // Create a DateFormat to format the logger timestamp.
    private static final DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder builder = new StringBuilder(1000);
        builder.append(df.format(new Date(record.getMillis()))).append(" ");
        switch (record.getLevel().intValue()) {
            case Integer.MIN_VALUE:
                builder.append("ALL    ");
                break;
            case 300:
                builder.append("FINEST ");
                break;
            case 400:
                builder.append("FINER  ");
                break;
            case 500:
                builder.append("FINE   ");
                break;
            case 700:
                builder.append("CONFIG ");
                break;
            case 800:
                builder.append("INFO   ");
                break;
            case 900:
                builder.append("WARNING");
                break;
            case 1000:
                builder.append("SEVERE ");
                break;
            case Integer.MAX_VALUE:
                builder.append("OFF    ");
                break;
            default:
                builder.append("NA     ");
                break;
        }
        builder.append(" [").append(Thread.currentThread().getName()).append("][");
        builder.append(record.getThreadID()).append("][");
        builder.append(record.getSourceClassName()).append(".");
        builder.append(record.getSourceMethodName()).append("] |");
        builder.append(formatMessage(record));
        if (record.getThrown() != null) {
            builder.append("\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            builder.append(sw.toString());
        }
        builder.append("\n");
        return builder.toString();
    }

}
