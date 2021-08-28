package com.smilecoms.commons.util;

import com.smilecoms.commons.base.cache.CacheHelper;
import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.base.lifecycle.Async;
import com.smilecoms.commons.base.lifecycle.SmileBaseRunnable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.*;

/**
 * Utility class of all sorts of common stuff from logging to date conversion
 *
 * @author PCB
 */
public class Utils {

    private static final String CLASS = Utils.class.getName();
    private static final Logger myLogger = LoggerFactory.getLogger(CLASS);
    public static final String AT = "@";
    public static final String SIP_PREFIX = "sip:";
    private static DatatypeFactory df = null;
    private static final Random random = new Random(System.currentTimeMillis());
    private static final DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
    private static final DecimalFormatSymbols commaFormatSymbol = new DecimalFormatSymbols();
    private static final String mbmib = BaseUtils.getProperty("env.portals.data.volume.display.suffix", "MB");
    private static final DecimalFormat dataWithDecimalPlace = new DecimalFormat("#,##0.0 " + mbmib, formatSymbols);
    private static final DecimalFormat dataWithoutDecimalPlace = new DecimalFormat("#,##0 " + mbmib, formatSymbols);
    private static final DecimalFormat commaDataWithDecimalPlace = new DecimalFormat("#,##0.0" + mbmib, commaFormatSymbol);
    private static final DecimalFormat commaDataWithoutDecimalPlace = new DecimalFormat("#,##0" + mbmib, commaFormatSymbol);
    private static final DecimalFormat commaDataWithoutDecimalPlaceGB = new DecimalFormat("#,##0", commaFormatSymbol);
    private static final DecimalFormat commaDataWithDecimalPlaceGB = new DecimalFormat("#,##0.0", formatSymbols);

    static {
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
        }
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        commaFormatSymbol.setDecimalSeparator('.');
        commaFormatSymbol.setGroupingSeparator(',');
    }

    /**
     * Convert a XMLGregorianCalendar to a java.util.Date
     *
     * @param d XML date
     * @return XMLGregorianCalendar date
     */
    public static Date getJavaDate(XMLGregorianCalendar d) {
        if (d == null) {
            return null;
        }
        return d.toGregorianCalendar().getTime();
    }

    public static String removeNonASCIIChars(String str) {
        return str.replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Current date and time in java.util.Date format As its commonly used for
     * inserting into MYSQL data fields, the milliseconds field is 0
     *
     * @return Current date
     */
    public static Date getCurrentJavaDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static String format(String msg, Object... arguments) {
        int index = 0;
        for (Object s : arguments) {
            msg = msg.replaceAll("\\{" + index + "\\}", s.toString());
            index++;
        }
        return msg;
    }

    public static int getDaysBetweenDates(Date start, Date end) {
        return (int) round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24), 0);
    }

    public static boolean isDateADayBeforeAPublicHoliday(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.add(Calendar.DATE, 1);
        return isDateAPublicHoliday(c.getTime());
    }

    public static int getNumWorkingDaysBetweenDates(Date start, Date end) {

        if (start.after(end)) {
            Date tmp = new Date(start.getTime());
            start.setTime(end.getTime());
            end.setTime(tmp.getTime());
        }

        int numWorkingDays = 0;
        // Date curDate = new Date(start.getTime());
        Calendar calCurDate = Calendar.getInstance();
        calCurDate.setTime(start);
        calCurDate.add(Calendar.DATE, 1); // Start from next day after sale.      

        while (calCurDate.getTime().before(end)) {
            if (!(isDateOverAWeekend(calCurDate.getTime()) || isDateAPublicHoliday(calCurDate.getTime()))) {
                numWorkingDays++;
            }
            calCurDate.add(Calendar.DATE, 1);
        }
        return numWorkingDays;
    }

    public static int getDateDayOfWeek(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    public static int getDaysAgo(Date dt) {
        return (int) round(((new Date()).getTime() - dt.getTime()) / (1000 * 60 * 60 * 24), 0);
    }

    public static long getFirstNumericPartOfString(String s) {
        return Long.parseLong(getFirstNumericPartOfStringAsString(s));
    }

    public static String getFirstNumericPartOfStringAsString(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        boolean canBreak = false;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
                canBreak = true;
            } else if (canBreak) {
                break;
            }
        }
        return sb.toString();
    }

    public static long getLastNumericPartOfString(String s) {
        return Long.parseLong(getLastNumericPartOfStringAsString(s));
    }

    public static String getLastNumericPartOfStringAsString(String s) {
        final StringBuilder sb = new StringBuilder(s.length());
        boolean canBreak = false;
        for (int i = s.length() - 1; i >= 0; i--) {
            final char c = s.charAt(i);
            if (c > 47 && c < 58) {
                sb.append(c);
                canBreak = true;
            } else if (canBreak) {
                break;
            }
        }
        return sb.reverse().toString();
    }

    /**
     * Get the phone number part from a SIP URI id including country code etc
     *
     * @param uri
     * @return phone number
     */
    public static String getPhoneNumberFromSIPURI(String uri) {
        int atLoc = uri.indexOf(AT);
        if (atLoc != -1) {
            uri = uri.substring(0, atLoc);
        }

        int sipLoc = uri.indexOf(SIP_PREFIX);
        if (sipLoc != -1) {
            uri = uri.substring(sipLoc + 4);
        }
        return uri;
    }

    /**
     * Get the phone number in format + COUNTRY CODE NUMBER - e.g.
     * +2347020000007
     *
     * @param number
     * @return e614 number
     */
    public static String getNumberInInternationalE164Format(String number) {
        String e164Number = "";
        if (number.startsWith("+")) {
            e164Number = number;
        } else if (number.startsWith("0")) {
            e164Number = "+" + BaseUtils.getProperty("env.e164.country.code", "234") + number.substring(1);
        } else if (number.startsWith("00")) {
            e164Number = "+" + BaseUtils.getProperty("env.e164.country.code", "234") + number.substring(2);
        } else {
            e164Number = "+" + number;
        }
        return e164Number;
    }

    public static String getPhoneNumberFromSIPURIWithoutPlus(String uri) {
        return getPhoneNumberFromSIPURI(uri).replace("+", "");
    }

    public static String getPublicIdentityForPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        String cleanedUp = SIP_PREFIX + "+" + getCleanDestination(phoneNumber);
        if (!cleanedUp.contains("@")) {
            return cleanedUp + AT + BaseUtils.getProperty("env.sip.domain");
        } else {
            return cleanedUp;
        }
    }

    /**
     * Returns a globally unique GUID
     *
     * @return UUID
     */
    public static String getUUID() {
        return java.util.UUID.randomUUID().toString();
    }

    public static Date getFutureDate(int unit, int value) {
        Calendar c = Calendar.getInstance();
        c.add(unit, value);
        return c.getTime();
    }

    public static Date getPastDate(int unit, int value) {
        Calendar c = Calendar.getInstance();
        c.add(unit, value * -1);
        return c.getTime();
    }

    public static Date getPastDateFromDate(int unit, int value, Date inDate) {
        Calendar c = Calendar.getInstance();
        c.setTime(inDate);
        c.add(unit, value * -1);
        return c.getTime();
    }

    public static long getSecondsBetweenDates(Date start, Date end) {
        return (end.getTime() - start.getTime()) / 1000;
    }

    public static String getDateOfLastDayOfMonthAsString(Date date, String dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return getDateAsString(cal.getTime(), dateFormat);
    }

    public static int getMaxDaysInMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return days;
    }

    /**
     * milliseconds since echo for current timezone
     *
     * @param timeZone
     * @param timeInMilliseconds
     * @return millisecinds since epoch
     */
    public static long getTimeInTimeZone(TimeZone timeZone, long timeInMilliseconds) {
        long zoneOffset = timeZone.getOffset(timeInMilliseconds);
        return timeInMilliseconds + zoneOffset;
    }

    /**
     * Converts an object to an array of bytes.
     *
     * @param object The object to convert.
     * @return the associated byte array.
     */
    public static byte[] toBytes(Object object) {
        if (object == null) {
            return null;
        }
        java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
        try {
            java.io.ObjectOutputStream objectOutputStream = new java.io.ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(object);
        } catch (java.io.IOException ex) {
            myLogger.error("Error", ex);
        } finally {
            try {
                byteArrayOutputStream.close();
            } catch (IOException ex) {
                myLogger.error("Error", ex);
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts an array of bytes back to its constituent object. The input
     * array is assumed to have been created from the original object.
     *
     * @param bytes The byte array to convert.
     * @return the associated object.
     */
    public static Object toObject(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        Object object = null;
        try {
            object = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes)).readObject();
        } catch (java.io.IOException ex) {
            myLogger.error("Error", ex);
        } catch (java.lang.ClassNotFoundException ex) {
            myLogger.error("Error", ex);
        }
        return object;
    }

    public static String getObjectAsString(Object object, String defaultValue) {
        String value = defaultValue;

        if (object == null) {
            return value;
        }
        if (!(object instanceof java.lang.String)) {
            return value;
        }
        value = (String) object;

        return value;
    }

    public static double getObjectAsDouble(Object object, double defaultValue) {
        double value;

        if (object == null) {
            return defaultValue;
        }
        if (!(object instanceof java.lang.Double)) {
            if ((object instanceof java.math.BigDecimal)) {
                value = ((BigDecimal) (object)).doubleValue();
                return value;
            }
            return defaultValue;
        }
        value = ((Double) (object));

        return value;
    }

    public static int getObjectAsInteger(Object object, int defaultValue) {
        int value;

        if (object == null) {
            return defaultValue;
        }
        if (!(object instanceof java.lang.Integer)) {
            return defaultValue;
        }
        value = ((Integer) (object));
        return value;
    }

    public static long getObjectAsLong(Object object, long defaultValue) {
        long value;

        if (object == null) {
            return defaultValue;
        }
        if (!(object instanceof java.lang.Long)) {
            return defaultValue;
        }
        value = ((Long) (object));
        return value;
    }

    public static Date getStringAsDate(String string, String dateFormat) {
        return getStringAsDate(string, dateFormat, null);
    }

    public static Date getStringAsDate(String string, String dateFormat, Date defaultValue) {
        if (string == null) {
            return defaultValue;
        }
        Date value = defaultValue;
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
        ParsePosition pos = new ParsePosition(0);
        try {
            value = formatter.parse(string, pos);
        } catch (Exception ex) {
            myLogger.warn("Error: ", ex);
        }
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    public static String getDateAsString(Date date, String dateFormat) {
        return getDateAsString(date, dateFormat, null);
    }

    public static String getDateAsString(Date date, String dateFormat, String defaultValue) {
        String value = defaultValue;
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        try {
            value = formatter.format(date);
        } catch (Exception ex) {
        }

        return value;
    }

    public static Date getDateFromString(String date, String dateFormat) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
            Date oDate = formatter.parse(date);
            return oDate;
        } catch (Exception ex) {
            return null;
        }
    }

    public static int getStringAsInt(String string, int defaultValue) {
        int value = defaultValue;

        try {
            value = Integer.parseInt(string);
        } catch (Exception ex) {
        }
        return value;
    }

    public static double getStringAsDouble(String string, double defaultValue) {
        double value = defaultValue;

        try {
            value = Double.parseDouble(string);
        } catch (Exception ex) {
        }
        return value;
    }

    public static BigDecimal getStringAsBigDecimal(String string, double defaultValue) {
        try {
            return new BigDecimal(string);
        } catch (Exception ex) {
            return new BigDecimal(defaultValue);
        }
    }

    public static BigDecimal getStringAsBigDecimal(String string, BigDecimal defaultValue) {
        try {
            return new BigDecimal(string);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    public static long getStringAsLong(String string, long defaultValue) {
        long value = defaultValue;

        try {
            value = Long.parseLong(string);
        } catch (Exception ex) {
        }
        return value;
    }

    /**
     * Returns a java date for the XMLGregorianCalendar
     *
     * @param xgc
     * @param defaultValuetr
     * @return Java Date object
     */
    public static Date getXMLGregorianCalendarAsDate(XMLGregorianCalendar xgc, Date defaultValue) {
        Date value = defaultValue;

        if (xgc == null) {
            return value;
        }
        GregorianCalendar gc = xgc.toGregorianCalendar();
        value = gc.getTime();
        return value;
    }

    /**
     * Returns a date String for the XMLGregorianCalendar in the required format
     *
     * @param xgc
     * @param dateFormat
     * @param defaultDate
     * @param defaultValue
     * @return String
     */
    public static String getXMLGregorianCalendarAsString(XMLGregorianCalendar xgc, String dateFormat, Date defaultDate, String defaultValue) {
        String value = defaultValue;

        if (xgc == null) {
            return value;
        }

        Date date = Utils.getXMLGregorianCalendarAsDate(xgc, defaultDate);
        value = getDateAsString(date, dateFormat, value);

        return value;
    }

    /**
     * Converts java date into XMLGregorianCalendar date
     *
     * @param d
     * @return XMLGregorianCalendar Date
     */
    public static XMLGregorianCalendar getDateAsXMLGregorianCalendar(Date d) {
        if (d == null) {
            return null;
        }
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(d);
        return df.newXMLGregorianCalendar(calendar);
    }

    /**
     * Returns the default string if the main one is null
     *
     * @param string
     * @param defaultValue
     * @return String
     */
    public static String getStringAsString(String string, String defaultValue) {
        if (string == null) {
            return defaultValue;
        } else {
            return string;
        }
    }

    /**
     * Convert a stream into a string
     *
     * @param is InputStream
     * @return String
     * @throws java.lang.Exception
     */
    public static String parseStreamToString(java.io.InputStream is, String encoding) throws Exception {
        try {
            return IOUtils.toString(is, encoding);
        } finally {
            is.close();
        }
    }

    public static byte[] parseStreamToByteArray(java.io.InputStream is) throws Exception {
        try {
            return IOUtils.toByteArray(is);
        } finally {
            is.close();
        }
    }

    public static String oneWayHash(String str) {
        String string = "d9(&*UYT&*GIouhIOU*Y(&)(Y&*GVBI" + str + (new Boolean(true)).getClass().getName();
        String encrypted = HashUtils.sha(string);
        return encrypted;
    }

    public static String oneWayHashImproved(String str) {
        String string = "17(_*UYT*GIKj&9sjY(&)(7&*GV12I" + str + (new Boolean(false)).getClass().getName();
        byte[] encrypted = HashUtils.sha256(string);
        try {
            return Utils.encodeBase64(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static String oneWayHashImprovedHex(String str) {
        String string = "17(_*UYT*GIKj&9sjY(&)(7&*GV12I" + str + (new Boolean(false)).getClass().getName();
        byte[] encrypted = HashUtils.sha256(string);
        try {
            return Codec.binToHexString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    public static String hashPasswordWithComplexityCheck(String pass) throws Exception {
        String regex = BaseUtils.getProperty("env.complex.passwords.pattern", "");
        // ^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=\\!\\*\\(\\)\\-\\,\\.\\?\\_])(?=\\S+$).{8,}$
        if (!regex.isEmpty() && !Utils.matchesWithPatternCache(pass, regex)) {
            throw new Exception("Password is too simple");
        }
        if (pass.equalsIgnoreCase("smile123")) {
            throw new Exception("Password is too simple");
        }
        return oneWayHash(pass);
    }

    /**
     * returns base64 encoded string of the zipped result
     *
     * @param uncompressed
     * @return
     */
    public static String zip(String uncompressed) {
        if (uncompressed == null || uncompressed.isEmpty()) {
            return uncompressed;
        }
        GZIPOutputStream gzip = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(out);
            gzip.write(uncompressed.getBytes());
            gzip.close();
            return new String(Base64.encodeBase64(out.toByteArray()), "ISO-8859-1");
        } catch (Exception e) {
            myLogger.warn("Error zipping: ", e);
            return "";
        } finally {
            if (gzip != null) {
                try {
                    gzip.close();
                } catch (IOException ex) {
                    myLogger.warn("Error zipping: ", ex);
                }
            }
        }
    }

    /*
    Unzip base64 encoded string
     */
    public static String unzip(String compressed) {
        if (compressed == null || compressed.isEmpty()) {
            return compressed;
        }
        try {
            byte[] b = Base64.decodeBase64(compressed.getBytes("ISO-8859-1"));
            ByteArrayInputStream in = new ByteArrayInputStream(b);
            GZIPInputStream gzip = new GZIPInputStream(in);
            return parseStreamToString(gzip, "ISO-8859-1");
        } catch (Exception e) {
            myLogger.warn("Error unzipping: ", e);
            return "";
        }

    }

    /**
     * Capitalise a string
     *
     * @param string
     * @return Capitalised string
     */
    public static String capitalize(String string) {
        return WordUtils.capitalizeFully(string);
    }

    /**
     * Gets the stream and unzips it into the destination folder. Will create
     * the folder if it doesnt exist
     *
     * @param input
     * @param destinationFolder
     * @return List of files extracted
     * @throws java.io.IOException
     */
    public static List<String> extractFilesFromZippedStream(InputStream input, String destinationFolder) throws IOException {
        BufferedOutputStream bos = null;
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));
        ZipEntry entry;
        final int BUFFER = 2048;
        File destFolder = new File(destinationFolder);
        if (!destFolder.exists()) {
            destFolder.mkdir();
        }
        List<String> filesInZip = new ArrayList<>();
        try {
            while ((entry = zis.getNextEntry()) != null) {
                //loop through each file in the archive
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                String pathFinal = destinationFolder + File.separatorChar + entry.getName();
                File finalFile = new File(pathFinal);
                if (finalFile.exists()) {
                    throw new IOException("File " + pathFinal + " already exists. Cannot extract from archive to filesystem");
                }
                try {
                    bos = new BufferedOutputStream(new FileOutputStream(finalFile), BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        bos.write(data, 0, count);
                    }
                } catch (IOException e) {
                    throw e;
                } finally {
                    bos.flush();
                    bos.close();
                    filesInZip.add(entry.getName());
                }
            }
        } catch (IOException e3) {
            throw e3;
        } finally {
            zis.close();
        }
        return filesInZip;
    }

    /**
     * Move a file from one location to another
     *
     * @param source
     * @param dest
     * @throws java.io.IOException
     */
    public static void moveFile(File source, File dest) throws IOException {
        source.renameTo(dest);
    }

    public static void moveFile(String source, String dest) throws IOException {
        File s = new File(source);
        File d = new File(dest);
        moveFile(s, d);
    }

    /**
     * Moves all files from one directory to another
     *
     * @param sourceDir
     * @param destDir
     * @throws java.io.IOException
     */
    public static void moveFiles(File sourceDir, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File[] children = sourceDir.listFiles();
        for (File sourceChild : children) {
            String name = sourceChild.getName();
            File destChild = new File(destDir, name);
            if (sourceChild.isDirectory()) {
                moveFiles(sourceChild, destChild);
            } else {
                moveFile(sourceChild, destChild);
            }
        }
    }

    /**
     * Delete a directory and all its contents.. just like rm -rf
     *
     * @param directory
     * @throws java.io.IOException
     */
    public static void deleteDirRecursive(File directory) throws IOException {
        if (directory.isDirectory()) {
            File[] childFiles = directory.listFiles();
            for (File child : childFiles) {
                deleteDirRecursive(child);
            }
        }
        directory.delete();
    }

    /**
     * Writes a stream into a given folder and file name. Will create the folder
     * if the doesn't exist. Will throw an exception if the file already exists.
     *
     * @param filename
     * @param input
     * @param destinationFolder
     * @throws java.io.IOException
     */
    public static void writeStreamToDisk(String filename, InputStream input, String destinationFolder) throws IOException {
        BufferedOutputStream bos = null;
        String path = destinationFolder + File.separatorChar + filename;
        File file = new File(path);
        if (file.exists()) {
            throw new IOException("File " + path + " already exists. Cannot write stream to filesystem");
        }
        File destFolder = new File(destinationFolder);
        if (!destFolder.exists()) {
            destFolder.mkdir();
        }
        BufferedInputStream bis = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bis = new BufferedInputStream(input);
            int aByte;
            while ((aByte = bis.read()) != -1) {
                bos.write(aByte);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception e) {
                myLogger.warn("Error cleaning up ", e);
            }
        }
    }

    public static void appendStringToFile(String filename, String input) throws IOException {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
            out.print(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static final Map<String, Pattern> patternMap = new ConcurrentHashMap<>();

    public static boolean matchesWithPatternCache(String field, String regex) {
        if (field == null) {
            return false;
        }
        Pattern pattern = patternMap.get(regex);
        if (pattern == null) {
            pattern = Pattern.compile(regex);
            patternMap.put(regex, pattern);
        }
        return pattern.matcher(field).find();
    }

    public static boolean matches(String field, String regex) {
        if (field == null) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(field).find();
    }

    private static final Pattern p = Pattern.compile("([0-9]*)");

    public static boolean isNumeric(String string) {
        Matcher m = p.matcher(string);
        return m.matches();
    }
    private static final Pattern hexp = Pattern.compile("[0-9a-fA-F]*");

    public static boolean isHexadecimal(String string) {
        Matcher m = hexp.matcher(string);
        return m.matches();
    }

    public static String makePrivateIdentityFromIMSI(String imsi) {
        return imsi + "@private." + BaseUtils.getProperty("env.sip.domain");
    }

    public static String makeNAIIdentityFromUsername(String username) {
        int atLoc = username.indexOf(AT);
        int naiPart = username.indexOf("nai");
        if (atLoc != -1 && naiPart != -1) {
            // There is an @ in the username
            return username;
        }
        return username + "@nai." + BaseUtils.getProperty("env.sip.domain");
    }

    public static String getIMSIfromPrivateIdentity(String privateIdentity) {
        String[] privateIdentityDetails = privateIdentity.split("@");
        return privateIdentityDetails[0];
    }

    public static Date getMaxDate(Date d1, Date d2) {
        if (d1.after(d2)) {
            return d1;
        } else {
            return d2;
        }
    }

    public static void setIPLocation(String ip, String location) {
        CacheHelper.putInRemoteCache("IPLocation_" + ip, location, 7200);
    }

    public static String getIPLocation(String ip) {
        return (String) CacheHelper.getFromRemoteCache("IPLocation_" + ip);
    }

    // For performance, only get these once as this function is used a LOT!
    private static final ReentrantLock lock = new ReentrantLock();
    private static String mobilePrefix = null;
    private static String internationalAccessCode = null;
    private static String nationalDirectDial = null;
    private static String countryCode = null;
    private static boolean e164PropsInit = false;

    public static void initE164Props() {
        if (!e164PropsInit) {
            // Only get props once for performance reasons
            try {
                if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    myLogger.debug("Another thread is already initialising E164 peoperties");
                    return;
                }
            } catch (Exception e) {
            }
            try {
                if (!e164PropsInit) {
                    mobilePrefix = BaseUtils.getProperty("env.e164.international.prefix.mobile");
                    internationalAccessCode = BaseUtils.getProperty("env.e164.international.access.code");
                    nationalDirectDial = BaseUtils.getProperty("env.e164.direct.dial.code");
                    countryCode = BaseUtils.getProperty("env.e164.country.code");
                    e164PropsInit = true;
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static String getBetween(String s, String start, String end) {
        if (s == null || start == null || end == null) {
            return null;
        }
        int startL = start.length();
        int startBegin = s.indexOf(start);
        if (startBegin == -1) {
            return null;
        }
        String afterStart = s.substring(startBegin + startL);
        int endBegin = afterStart.indexOf(end);
        if (endBegin == -1) {
            return null;
        }
        return afterStart.substring(0, endBegin);
    }

    public static String getLastBetween(String s, String start, String end) {
        if (s == null || start == null || end == null) {
            return null;
        }
        int startL = start.length();
        int startBegin = s.lastIndexOf(start);
        if (startBegin == -1) {
            return null;
        }
        String afterStart = s.substring(startBegin + startL);
        int endBegin = afterStart.indexOf(end);
        if (endBegin == -1) {
            return null;
        }
        return afterStart.substring(0, endBegin);
    }

    /**
     * Return the destination as E164 number along with the domain part
     * afterwards e.g. 27834427179@smilecoms.com
     *
     * @param destination
     * @return
     */
    public static String getCleanDestination(String destination) {
        initE164Props();
        if (destination == null || destination.isEmpty()) {
            return "";
        }
        String orig = destination;
        String partBeforeDomain;
        String domainPart;

        if (destination.startsWith(SIP_PREFIX)) {
            destination = destination.substring(4);
        }

        int atLoc = destination.indexOf(AT);
        if (atLoc != -1) {
            // There is an @ in the destination
            partBeforeDomain = destination.substring(0, atLoc);
            domainPart = destination.substring(atLoc);
        } else {
            // This is not a uri, but more like a phone number
            partBeforeDomain = destination;
            domainPart = "";
        }

        int semiPart = domainPart.indexOf(";");
        if (semiPart != -1) {
            // Remove part after ; in domain
            domainPart = domainPart.substring(0, semiPart);
        }

        //Get rid of spaces
        partBeforeDomain = partBeforeDomain.replaceAll("\\(0\\)", ""); // Remove something like (0)
        partBeforeDomain = partBeforeDomain.replaceAll("[ \\+\\(\\)-]", ""); // Remove + ( ) -
        domainPart = domainPart.trim();

        if (!Utils.isNumeric(partBeforeDomain)) {
            // The part before the domain is not of the format of a phone number so return it unmodified
            return partBeforeDomain + domainPart;
        }
        // From here on, we know the part before the domain is a phone number

        try {
            if (partBeforeDomain.startsWith(internationalAccessCode)) {
                //Number in the format 00 27 83 5791766 must be changed to 27 83 5791766
                partBeforeDomain = partBeforeDomain.substring(internationalAccessCode.length());
            } else if (partBeforeDomain.startsWith(mobilePrefix)) {
                //Number in the format +27 83 5791766 must be changed to 27 83 5791766
                partBeforeDomain = partBeforeDomain.substring(mobilePrefix.length());
            } else if (partBeforeDomain.startsWith(nationalDirectDial)) {
                //Number in the format 0 83 5791766 must be changed to 27 83 5791766
                partBeforeDomain = countryCode + partBeforeDomain.substring(nationalDirectDial.length());
            }
        } catch (Exception e) {
            myLogger.warn("Error getting e.164 format for a phone number. Number passed in was [" + orig + "] will return original number. Error:" + e.toString());
            return orig;
        }
        //Other messages do not start with the international access code, the
        //mobilePrefix or the national direct dial and must hence be in the correct format
        if (myLogger.isDebugEnabled()) {
            myLogger.debug("Returning e164 version of [" + orig + "] as [" + partBeforeDomain + domainPart + "]");
        }
        return partBeforeDomain + domainPart;

    }

    public static String getFriendlyPhoneNumber(String destination) {
        // Make it e164 and then strip off the int dial code
        destination = getCleanDestination(destination);
        if (destination.startsWith(countryCode)) {
            destination = destination.replaceFirst(countryCode, nationalDirectDial);
        }
        //Get rid of domain if there is one in the destination
        int atLoc = destination.indexOf(Utils.AT);
        if (atLoc != -1) {
            destination = destination.substring(0, atLoc);
        }
        return destination;
    }

    public static String getFriendlyNAI(String identity) {
        //Get rid of domain if there is one in the identity
        int atLoc = identity.indexOf(Utils.AT);
        if (atLoc != -1) {
            identity = identity.substring(0, atLoc);
        }
        return identity;
    }

    /**
     * Make any number look like a national direct dial number basically return
     * number concatenated with nationalDirectDial e.g. 834427179 = 0834427179,
     * 27834427179 = 0834427179
     *
     * @param number
     * @return
     */
    public static String makeNationalDirectDial(String num) {

        if (num == null || num.isEmpty()) {
            return "";
        }

        if (!num.startsWith(nationalDirectDial)) {
            if (num.startsWith(countryCode)) { // 27834427179 - replace 27 with nationalDirectDial
                return nationalDirectDial + num.substring(countryCode.length());
            } else {
                return nationalDirectDial + num;
            }
        }
        return num;
    }

    public static String getFriendlyPhoneNumberKeepingCountryCode(String destination) {
        // Make it e164 and then strip off the domain part
        destination = getCleanDestination(destination);
        //Get rid of domain if there is one in the destination
        int atLoc = destination.indexOf(Utils.AT);
        if (atLoc != -1) {
            destination = destination.substring(0, atLoc);
        }
        return destination;
    }

    public static String replace(
            final String aInput,
            final String aOldPattern,
            final String aNewPattern) {
        if (aOldPattern.equals("")) {
            throw new IllegalArgumentException("Old pattern must have content.");
        }

        final StringBuffer result = new StringBuffer();
        //startIdx and idxOld delimit various chunks of aInput; these
        //chunks always end where aOldPattern begins
        int startIdx = 0;
        int idxOld;
        while ((idxOld = aInput.indexOf(aOldPattern, startIdx)) >= 0) {
            //grab a part of aInput which does not include aOldPattern
            result.append(aInput.substring(startIdx, idxOld));
            //add aNewPattern to take place of aOldPattern
            result.append(aNewPattern);

            //reset the startIdx to just after the current match, to see
            //if there are any further matches
            startIdx = idxOld + aOldPattern.length();
        }
        //the final chunk will go to the end of aInput
        result.append(aInput.substring(startIdx));
        return result.toString();
    }

    public static double[] convertDMSToDegree(String gps) {
        // in format //26 02 39.5 S 28 01 23.0 E
        //             0123456789012345678901234
        int isSouth = 1;
        int isWest = 1;
        if (gps.substring(11, 12).equals("S")) {
            isSouth = -1;
        }
        if (gps.substring(24).equals("W")) {
            isWest = -1;
        }
        int deg1;
        int min1;
        float sec1;
        int deg2;
        int min2;
        float sec2;
        deg1 = Integer.parseInt(gps.substring(0, 2));
        min1 = Integer.parseInt(gps.substring(3, 5));
        sec1 = Float.parseFloat(gps.substring(6, 10));
        deg2 = Integer.parseInt(gps.substring(13, 15));
        min2 = Integer.parseInt(gps.substring(16, 18));
        sec2 = Float.parseFloat(gps.substring(19, 23));
        return new double[]{isSouth * convertDMSToDegree(deg1, min1, sec1), isWest * convertDMSToDegree(deg2, min2, sec2)};
    }

    private static double convertDMSToDegree(int deg, int min, float sec) {
        double dec_min = (min * 1.0 + (sec / 60.0));
        return deg * 1.0 + (dec_min / 60.0);
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            myLogger.warn("Error while sleeping: " + e.toString());
        }
    }

    public static boolean booleanValue(String x) {
        if (x == null) {
            return false;
        }

        if (x.equalsIgnoreCase("Y")) {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the given date is greater or equal than the from-date,
     * and less than the to-date
     *
     * @param theDate
     * @param from
     * @param to
     * @return
     */
    public static boolean isBetween(Date theDate, Date from, Date to) {
        if ((theDate.after(from) || theDate.getTime() == from.getTime()) && theDate.before(to)) {
            return true;
        }
        return false;
    }

    public static boolean isBetweenInclusive(Date theDate, Date from, Date to) {
        if ((theDate.after(from) || theDate.getTime() == from.getTime()) && (theDate.before(to) || theDate.getTime() == to.getTime())) {
            return true;
        }
        return false;
    }

    public static boolean isNowInTimeframe(Date from, Date to) {
        return isBetween(new Date(), from, to);
    }

    public static boolean isNowInTimeframe(XMLGregorianCalendar from, XMLGregorianCalendar to) {
        return isBetween(new Date(), from, to);
    }

    public static boolean areDatesInSameMonth(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));
    }

    public static boolean areDatesOnSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    public static boolean isDateBeforeYesterday(Date d) {
        return (d.getTime() < getBeginningOfYesterday().getTime());
    }

    public static boolean isDateToday(Date d) {
        return areDatesOnSameDay(d, new Date());
    }

    public static boolean isDateInTimeframe(Date theDate, int timeBack, int calendarTimeUnit) {
        Calendar cal = Calendar.getInstance();
        cal.add(calendarTimeUnit, timeBack * -1);
        Date daysBackDate = cal.getTime();
        return isBetween(theDate, daysBackDate, new Date());
    }

    public static boolean isDateInTimeframeInclusive(Date theDate, int timeBack, int calendarTimeUnit) {
        Calendar cal = Calendar.getInstance();
        cal.add(calendarTimeUnit, timeBack * -1);
        Date daysBackDate = cal.getTime();
        return isBetweenInclusive(theDate, daysBackDate, new Date());
    }

    public static boolean isBetween(Date theDate, XMLGregorianCalendar fromxml, XMLGregorianCalendar toxml) {
        return isBetween(theDate, getJavaDate(fromxml), getJavaDate(toxml));
    }

    public static boolean isInTheFuture(XMLGregorianCalendar xmldt) {
        Date javaDate = getJavaDate(xmldt);
        return new Date().before(javaDate);
    }

    public static boolean isInTheFuture(Date dt) {
        return new Date().before(dt);
    }

    private static final ConcurrentHashMap<Class, Marshaller> soapmarshallerMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class, Marshaller> xmlmarshallerMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class, Unmarshaller> unmarshallerMap = new ConcurrentHashMap<>();

    public static Unmarshaller getJAXBUnmarshaller(Class clazz) throws JAXBException {

        Unmarshaller unmarshaller = unmarshallerMap.get(clazz);
        if (unmarshaller == null) {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz.getPackage().getName());
            unmarshaller = jaxbContext.createUnmarshaller();
            unmarshallerMap.put(clazz, unmarshaller);
        }
        return unmarshaller;
    }

    public static Marshaller getJAXBMarshallerForSoap(Class clazz) throws JAXBException {
        Marshaller marshaller = soapmarshallerMap.get(clazz);
        if (marshaller == null) {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz.getPackage().getName());
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
            soapmarshallerMap.put(clazz, marshaller);
        }
        return marshaller;
    }

    public static Marshaller getJAXBMarshallerForXML(Class clazz) throws JAXBException {
        Marshaller marshaller = xmlmarshallerMap.get(clazz);
        if (marshaller == null) {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            xmlmarshallerMap.put(clazz, marshaller);
        }
        return marshaller;
    }

    public static String marshallSoapObjectToString(Object inputObject) {

        if (myLogger.isDebugEnabled()) {
            myLogger.debug("Marshalling object to xml...");
        }
        if (inputObject == null) {
            if (myLogger.isDebugEnabled()) {
                myLogger.debug("Cannot marshall a null object - will return null");
            }
            return null;
        }

        String xml = null;
        try {
            Class inputClass = inputObject.getClass();
            String objectFactoryClassName = inputClass.getPackage().getName() + ".ObjectFactory";
            myLogger.debug("Object factory class name is [{}] for input class [{}]", objectFactoryClassName, inputClass.getName());
            Object objectFactory = inputClass.getClassLoader().loadClass(objectFactoryClassName).newInstance();
            JAXBElement element;
            Method mCreate = null;
            Method[] allMethods = objectFactory.getClass().getDeclaredMethods();

            for (Method m : allMethods) {
                Class<?>[] pType = m.getParameterTypes();
                if (pType.length == 1 && pType[0].equals(inputClass)) {
                    mCreate = m;
                    break;
                }
            }

            if (mCreate != null) {
                element = (JAXBElement) mCreate.invoke(objectFactory, new Object[]{inputObject});
                if (element != null) {
                    StringWriter writer = new StringWriter();
                    Marshaller marshaller = getJAXBMarshallerForSoap(inputClass);
                    synchronized (marshaller) {
                        long start = 0;
                        if (myLogger.isDebugEnabled()) {
                            start = System.currentTimeMillis();
                        }
                        marshaller.marshal(element, writer);
                        if (myLogger.isDebugEnabled()) {
                            myLogger.debug("Marshalling took [{}]ms", System.currentTimeMillis() - start);
                        }
                    }
                    xml = writer.toString();
                }
            }
        } catch (Exception e) {
            myLogger.debug("Error marshalling request object to a string. Will return <Error Parsing>. Error: [{}]", e.toString());
            myLogger.warn("Error: ", e);
            xml = "<Error Parsing>";
        }
        if (myLogger.isDebugEnabled()) {
            myLogger.debug("Finished marshalling object to xml. Result is [{}]", xml);
        }
        return xml;
    }

    public static String marshallBeanToXML(Object inputObject, String namespace, String rootName) throws Exception {
        Marshaller marshaller = getJAXBMarshallerForXML(inputObject.getClass());
        StringWriter writer = new StringWriter();
        QName qName = new QName(namespace, rootName);
        JAXBElement<Object> root = new JAXBElement<>(qName, Object.class, inputObject);

        synchronized (marshaller) {
            marshaller.marshal(root, writer);
        }
        return writer.toString();
    }

    public static ByteArrayInputStream stringToStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.getBytes("ISO-8859-1"));
    }

    public static String streamtoString(ByteArrayOutputStream s) throws UnsupportedEncodingException {
        return s.toString("ISO-8859-1");
    }

    public static String doXSLTransform(String xml, String xslt, ClassLoader cl) throws Exception {
        try {
            // Create transformer factory
            TransformerFactory factory;
            if (xslt.contains(" version=\"2.0\"")) {
                myLogger.debug("Using SAXON as this is xslt 2.0");
                try {
                    factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", cl);
                } catch (Throwable e) {
                    myLogger.warn("Error: [{}]", e.toString());
                    factory = TransformerFactory.newInstance();
                }
            } else {
                factory = TransformerFactory.newInstance();
            }
            // Use the factory to create a template containing the xsl stream
            Templates template = factory.newTemplates(new StreamSource(stringToStream(xslt)));
            // Use the template to create a transformer
            Transformer xformer = template.newTransformer();
            // Prepare the input and output streams
            Source source = new StreamSource(stringToStream(xml));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Result result = new StreamResult(baos);
            // Apply the xsl file to the source file and write the result to the output stream
            xformer.transform(source, result);
            return streamtoString(baos);
        } catch (Exception e) {
            // Report a config issue
            myLogger.warn("Error doing XSL transform of [{}] with [{}]", xml, xslt, e);
            new ExceptionManager(myLogger).reportError(e);
            throw e;
        }
    }

    public static double round(double d, int decimalPlace) {
        return roundHalfEven(d, decimalPlace);
    }

    private static double roundHalfEven(double d, int decimalPlace) {
        // see the Javadoc about why we use a String in the constructor
        // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_EVEN);
        return bd.doubleValue();
    }

    public static String getDateWithoutSlashes(String date) {
        if (date == null) {
            return "";
        }
        return date.replace("/", "");
    }

    public static String addSlashesToDate(String date) {
        if (date == null || date.trim().length() < 8) {
            return "";
        }

        if (date.equalsIgnoreCase("For-Life")) { // To Handle UG case for permanent VisaExpiryDates
            return date;
        }

        return new StringBuffer(date).insert(4, "/").insert(7, "/").toString();
    }

    private static long rolesLastRefreshed = 0;
    private static Map<String, String> roleMapKeyedOnUnfriendly = new HashMap<>();
    private static Map<String, String> roleMapKeyedOnFriendly = new HashMap<>();

    private static void checkCache() {
        long now = System.currentTimeMillis();
        if (now - rolesLastRefreshed > 60000) {
            rolesLastRefreshed = now;
            Map<String, String> roleMapKeyedOnUnfriendlyTmp = new HashMap<>();
            Map<String, String> roleMapKeyedOnFriendlyTmp = new HashMap<>();
            List<String> mappings = BaseUtils.getPropertyAsList("global.customer.securitygroups");
            for (String mapping : mappings) {
                String[] bits = mapping.split("-");
                // e.g. Role01-Administrator
                roleMapKeyedOnUnfriendlyTmp.put(bits[0], bits[1]);
                roleMapKeyedOnFriendlyTmp.put(bits[1], bits[0]);
            }
            roleMapKeyedOnUnfriendly = roleMapKeyedOnUnfriendlyTmp;
            roleMapKeyedOnFriendly = roleMapKeyedOnFriendlyTmp;
        }
    }

    private static Map<String, String> getCachedRolesKeyedOnUnfriendly() {
        checkCache();
        return roleMapKeyedOnUnfriendly;
    }

    private static Map<String, String> getCachedRolesKeyedOnFriendly() {
        checkCache();
        return roleMapKeyedOnFriendly;
    }

    /**
     * In web.xml we define a list of placeholder roles (e.g. Role01) and then
     * map them to friendly role names so that we can change/add roles without
     * redeploy or changing web.xml
     *
     * @param friendlyName
     * @return
     */
    public static String getUnfriendlyRoleName(String friendlyName) {
        return getCachedRolesKeyedOnFriendly().get(friendlyName);
    }

    public static String getFriendlyRoleName(String unfriendlyName) {
        return getCachedRolesKeyedOnUnfriendly().get(unfriendlyName);
    }

    public static List<String> getListFromCRDelimitedString(String s) {
        List<String> ret = new ArrayList<>();
        if (s != null) {
            StringTokenizer stValues = new StringTokenizer(s, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (!val.isEmpty()) {
                    ret.add(val.trim());
                }
            }
        }
        return ret;
    }

    public static Set<String> getSetFromCRDelimitedString(String s) {
        Set<String> ret = new HashSet<>();
        if (s != null) {
            StringTokenizer stValues = new StringTokenizer(s, "\r\n");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (!val.isEmpty()) {
                    ret.add(val.trim());
                }
            }
        }
        return ret;
    }

    public static List<String> getListFromCommaDelimitedString(String s) {
        List<String> ret = new ArrayList<>();
        if (s != null) {
            StringTokenizer stValues = new StringTokenizer(s, ",");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (!val.isEmpty()) {
                    ret.add(val.trim());
                }
            }
        }
        return ret;
    }

    public static Set<String> getSetFromCommaDelimitedString(String s) {
        Set<String> ret = new HashSet<>();
        if (s != null) {
            StringTokenizer stValues = new StringTokenizer(s, ",");
            while (stValues.hasMoreTokens()) {
                String val = stValues.nextToken();
                if (!val.isEmpty()) {
                    ret.add(val.trim());
                }
            }
        }
        return ret;
    }

    public static boolean collectionsMatch(Collection<String> c1, Collection<String> c2) {

        if (c1.size() != c2.size()) {
            return false;
        }
        for (String s1 : c1) {
            if (!c2.contains(s1)) {
                return false;
            }
        }
        return true;
    }

    public static String makeCommaDelimitedString(Collection<String> list) {
        if (list == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s.trim()).append(",");
        }
        if (sb.length() != 0) {
            // Remove trailing ,
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static boolean getBooleanValueFromCRDelimitedAVPString(String crDelimitedList, String key) {
        String res = getValueFromCRDelimitedAVPString(crDelimitedList, key);
        if (res == null || !res.equals("true")) {
            return false;
        } else {
            return true;
        }
    }

    public static String getValueFromCRDelimitedAVPString(String crDelimitedList, String key) {
        if (crDelimitedList == null) {
            return null;
        }
        StringTokenizer stValues = new StringTokenizer(crDelimitedList, "\r\n");
        while (stValues.hasMoreTokens()) {
            String val = stValues.nextToken();
            if (!val.isEmpty()) {
                String[] propVal = val.split("=", 2);
                if (propVal[0].trim().equalsIgnoreCase(key)) {
                    if (propVal.length == 1) {
                        return "";
                    } else {
                        return propVal[1].trim();
                    }
                }
            }
        }
        return null;
    }

    public static double getDoubleValueFromCRDelimitedAVPString(String crDelimitedList, String key) {
        String res = getValueFromCRDelimitedAVPString(crDelimitedList, key);
        if (res == null) {
            return -1;
        } else {
            return Double.valueOf(res);
        }
    }

    public static String getValueFromCommaDelimitedAVPString(String commaDelimitedList, String key) {
        StringTokenizer stValues = new StringTokenizer(commaDelimitedList, ",");
        while (stValues.hasMoreTokens()) {
            String val = stValues.nextToken();
            if (!val.isEmpty()) {
                String[] propVal = val.split("=", 2);
                if (propVal[0].trim().equalsIgnoreCase(key)) {
                    if (propVal.length == 1) {
                        return "";
                    } else {
                        return propVal[1].trim();
                    }
                }
            }
        }
        return null;
    }

    public static String getPropertyValueFromList(String propKey, String valueKey) {

        List<String> lstProperty = BaseUtils.getPropertyAsList(propKey);
        StringTokenizer st;

        for (String prop : lstProperty) {
            st = new StringTokenizer(prop, "=");
            if (st.nextToken().equals(valueKey)) {
                return st.nextToken();
            }
        }
        return null;
    }

    public static boolean isDateAPublicHoliday(Date d) {
        Set publicHols;
        publicHols = BaseUtils.getPropertyAsSet("env.bm.nightandweekend.public.holidays");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        String dt = fmt.format(d);
        if (publicHols.contains(dt)) {
            myLogger.debug("This is a public holiday [{}]", dt);
            return true;
        }
        return false;
    }

    public static boolean isDateOverAWeekend(Date d) {

        Calendar et = Calendar.getInstance();
        et.setTime(d);

        int dayOfWeek = et.get(Calendar.DAY_OF_WEEK);
        int hour = et.get(Calendar.HOUR_OF_DAY); // 0..23
        myLogger.debug("Day of week is [{}] and hour is [{}]", dayOfWeek, hour);

        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            myLogger.debug("This is a weekend");
            return true;
        }

        return false; //Not a weekend.
    }

    public static String setValueInCRDelimitedAVPString(String fullString, String attribute, String value) {
        List<String> avpList = getListFromCRDelimitedString(fullString);
        StringBuilder newInfo = new StringBuilder();
        String lookup = attribute + "=";
        boolean added = false;
        for (String avp : avpList) {
            if (avp.replace(" ", "").startsWith(lookup)) {
                if (value != null) {
                    newInfo.append(lookup);
                    newInfo.append(value);
                    newInfo.append("\r\n");
                }
                added = true;
            } else {
                newInfo.append(avp);
                newInfo.append("\r\n");
            }
        }
        if (!added && value != null) {
            newInfo.append(lookup);
            newInfo.append(value);
        }
        return newInfo.toString();
    }

    public static String makeCRDelimitedStringFromList(Collection<String> sList) {
        if (sList.isEmpty()) {
            return "";
        }

        StringBuilder ret = new StringBuilder();
        for (String s : sList) {
            ret.append(s);
            ret.append("\r\n");
        }
        return ret.substring(0, ret.length() - 2); // dont include trailing CRLF
    }

    public static boolean listsIntersect(Collection<String> l1, Collection<String> l2) {
        for (String s1 : l1) {
            for (String s2 : l2) {
                if (s2.equals(s1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean listContains(List<String> l, String s) {
        for (String s1 : l) {
            if (s.equals(s1)) {
                return true;
            }
        }
        return false;
    }

    public static byte[] decodeBase64(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            return Base64.decodeBase64(encoded.getBytes("ISO-8859-1"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encodeBase64(byte[] binary) throws UnsupportedEncodingException {
        if (binary == null) {
            return null;
        }
        return new String(Base64.encodeBase64(binary), "ISO-8859-1");
    }

    public static String readFileAsBase64String(String filePath) throws Exception {

        String base64File = "";
        File file = new File(filePath);
        try (FileInputStream imageInFile = new FileInputStream(file)) {
            // Reading a file from file system
            byte fileData[] = new byte[(int) file.length()];
            imageInFile.read(fileData);
            base64File = new String(Base64.encodeBase64(fileData), "ISO-8859-1");
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
            throw e;
        } catch (IOException ioe) {
            System.out.println("Exception while reading the file " + ioe);
            throw ioe;
        }
        return base64File;
    }

    public static byte[] getDataFromTempFile(String guid) throws Exception {
        byte[] data = null;
        try {
            InputStream fileInputStream = getTempFileStream(guid);
            data = parseStreamToByteArray(fileInputStream);
        } catch (Exception ex) {
            myLogger.warn("Error in getDataFromTempFile", ex);
            throw ex;
        }
        return data;
    }
    
    public static byte[] getDataFromFile(String fileName) throws Exception {
        byte[] data = null;
        try {
            File file = new File(fileName);        
            InputStream fileInputStream = new FileInputStream(file);
            data = parseStreamToByteArray(fileInputStream);
        } catch (Exception ex) {
            myLogger.warn("Error in getDataFromTempFile", ex);
            throw ex;
        }
        return data;
    }

    private static long lastCleanedOutTmp = 0;

    private static void deleteOldTempFiles() {

        if (System.currentTimeMillis() - lastCleanedOutTmp < 60000) {
            myLogger.debug("Not doing deleteOldTempFiles yet");
            return;
        }
        try {
            File directory = new File(System.getProperty("java.io.tmpdir") + File.separator + "/hobit_tmp");
            if (directory.exists()) {
                long purgeTime = System.currentTimeMillis() - (2 * 3600 * 1000); // delete older than 2 hours
                File[] files = directory.listFiles();
                for (File file : files) {
                    myLogger.debug("Looking at file [{}] modified at [{}]", file.getAbsolutePath(), new Date(file.lastModified()));
                    if (file.lastModified() < purgeTime) {
                        myLogger.debug("Deleting [{}]", file.getAbsolutePath());
                        try {
                            file.delete();
                        } catch (Exception e) {
                            myLogger.warn("Error deleting file", e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            myLogger.warn("Error in deleteOldTempFiles", e);
        }
        lastCleanedOutTmp = System.currentTimeMillis();
    }

    public static InputStream getTempFileStream(String guid) throws FileNotFoundException {
        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "/hobit_tmp" + File.separator + guid);
        return new FileInputStream(file);
    }

    public static void deleteTempFile(String guid) {
        try {
            File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "/hobit_tmp" + File.separator + guid);
            file.delete();
        } catch (Exception e) {
            myLogger.debug("Error deleting temp file", e);
        }
    }

    public static File createTempFile(String fileName, byte[] data) {
        deleteOldTempFiles();
        File tmpFile = null;
        FileOutputStream fOs = null;
        DataOutputStream dos = null;
        try {
            String tempDir = System.getProperty("java.io.tmpdir") + "/hobit_tmp";
            tmpFile = new File(tempDir, fileName);
            tmpFile.mkdirs();
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            fOs = new FileOutputStream(tmpFile);
            dos = new DataOutputStream(fOs);
            dos.write(data);
            tmpFile.deleteOnExit();
        } catch (Exception e) {
            myLogger.warn("Error in createTempFile", e);
        } finally {
            if (fOs != null) {
                try {
                    fOs.close();
                } catch (Exception ex) {
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (Exception e2) {
                }
            }
        }
        return tmpFile;
    }
    private static final String FORWARDED_FOR = "X-FORWARDED-FOR";

    public static String getRemoteIPAddress(javax.servlet.http.HttpServletRequest sr) {
        if (sr == null) {
            return "UNKNOWN";
        }
        String ipAddress = sr.getHeader(FORWARDED_FOR);
        if (ipAddress == null) {
            ipAddress = sr.getRemoteAddr();
        }
        return ipAddress;
    }

    public static int getRandomNumber(int startInclusive, int endExclusive) {
        return random.nextInt(endExclusive - startInclusive) + startInclusive;
    }
    
    public static int getRandomNumberWithFixedLength(int digitsLength) {
        return digitsLength < 1 ? 0 : new Random()
                .nextInt((9 * (int) Math.pow(10, digitsLength - 1)) - 1)
                + (int) Math.pow(10, digitsLength - 1);
    }
    

    /**
     *
     * @param value - string representation of the number to BCD encode
     * @return - byte array of BCD encoded number
     */
    public static byte[] numberToBcd(String value) {
        int charpos = 0;
        int bufpos = 0;

        if (myLogger.isDebugEnabled()) {
            myLogger.debug("about to convert [{}] to BCD", value);
        }
        //strip off leading plus
        if (value.startsWith("+")) {
            value = value.substring(1);
            myLogger.debug(("Removing leading + before converting number to BCD"));
        }

        int byteArrLength = (value.length() / 2) + (value.length() % 2);
        byte[] buf = new byte[byteArrLength];

        while (charpos < value.length()) {
            if ((charpos + 1) == value.length()) {
                buf[bufpos] = (byte) (((value.charAt(charpos) - 48))
                        | (0xf0));
                break;
            }

            buf[bufpos] = (byte) (((value.charAt(charpos) - 48))
                    | (value.charAt(charpos + 1) - 48) << 4);
            charpos += 2;
            bufpos++;
        }
        return buf;
    }

    public static boolean isAnyOfUserRolesIncludedInRolesPropertyList(List<String> securityGroups, String key) {
        List<String> allowedRoles = BaseUtils.getPropertyAsList(key);

        for (String role : allowedRoles) {
            role = role.trim();
            if (securityGroups.contains(role)) {
                myLogger.debug("Role [{}] is allowed.", role);
                return true;
            }
        }
        myLogger.debug("Roles [{}] are not allowed by [{}].", securityGroups, allowedRoles);
        return false;
    }

    public static Set<String> getRoleBasedSubsetOfPropertyAsSet(String key, List<String> securityGroups) {
        Set<String> ret = new TreeSet<>();
        for (String row : BaseUtils.getPropertyAsList(key)) {
            if (!row.contains("=")) {
                ret.add(row.trim()); // Default to allowing everyone
                continue;
            }
            String[] bits = row.split("=");
            String val = bits[0].trim();
            String roles = bits[1];
            String[] rolesList = roles.split("\\,");
            myLogger.debug("Value [{}] is accessible by [{}]", val, rolesList);
            for (String role : rolesList) {
                role = role.trim();
                if (securityGroups.contains(role)) {
                    ret.add(val);
                    break;
                }
            }
        }
        myLogger.debug("Returning values [{}]", ret);
        return ret;
    }

    public static String getFullThreadDump() {
        try {
            final StringBuilder dump = new StringBuilder();
            final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
            for (ThreadInfo threadInfo : threadInfos) {

                printThreadInfo(threadInfo, dump);
                LockInfo[] syncs = threadInfo.getLockedSynchronizers();
                printLockInfo(syncs, dump);
                dump.append("\n\n");
            }
            return dump.toString();
        } catch (Exception e) {
            myLogger.warn("Error: ", e);
            return "Error in getFullThreadDump: " + e.toString();
        }
    }

    private static final String INDENT = "    ";

    private static void printThreadInfo(ThreadInfo ti, StringBuilder dump) {
        // print thread information
        printThread(ti, dump);
        dump.append("\n");
        // print stack trace with locks
        StackTraceElement[] stacktrace = ti.getStackTrace();
        MonitorInfo[] monitors = ti.getLockedMonitors();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            dump.append(INDENT + "at ").append(ste.toString()).append("\n");
            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == i) {
                    dump.append(INDENT + "  - locked ").append(mi).append("\n");
                }
            }
        }
        dump.append("\n");
    }

    private static void printThread(ThreadInfo ti, StringBuilder dump) {
        StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState());
        if (ti.getLockName() != null) {
            sb.append(" on lock=").append(ti.getLockName());
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)");
        }
        if (ti.isInNative()) {
            sb.append(" (running in native)");
        }
        dump.append(sb.toString());
        if (ti.getLockOwnerName() != null) {
            dump.append(INDENT + " owned by ").append(ti.getLockOwnerName()).append(" Id=").append(ti.getLockOwnerId());
        }
    }

    private static void printLockInfo(LockInfo[] locks, StringBuilder dump) {
        dump.append(INDENT + "Locked synchronizers: count = ").append(locks.length);
        for (LockInfo li : locks) {
            dump.append(INDENT + "  - ").append(li);
        }
        dump.append("\n");
    }

    private static Map<String, String> tacData = null;

    public static String getDeviceMakeAndModel(String imei) {

        if (tacData == null) {
            if (lock.tryLock() && tacData == null) {
                try {
                    tacData = new ConcurrentHashMap<>();
                    Async.makeHappenThenScheduleWithFixedDelay(new SmileBaseRunnable("Util.TACCache") {
                        @Override
                        public void run() {
                            myLogger.warn("Refreshing device tac/make cache");
                            List<String[]> data = BaseUtils.getPropertyFromSQL("global.imei.make.data");
                            for (String[] row : data) {
                                tacData.put(row[0], row[1]);
                            }
                            myLogger.warn("Loaded device tac/make cache. Cache has [{}] rows", tacData.size());
                        }

                    }, 3000, getRandomNumber(3600000 * 6, 3600000 * 8));
                } finally {
                    lock.unlock();
                }
            }
        }

        if (imei == null || imei.isEmpty() || imei.length() < 8) {
            myLogger.debug("Invalid IMEI [{}]. Returning blank manufacturer", imei);
            return null;
        }
        if (tacData.isEmpty()) {
            myLogger.warn("Cache load not done so pretending device exists");
            return "Cache load in progress";
        }
        String TAC = imei.substring(0, 8);
        myLogger.debug("TAC is [{}]", TAC);
        return tacData.get(TAC);
    }

    private static DecimalFormat CurrencyShort = null;
    private static DecimalFormat CurrencyLong = null;

    // PCB 202 - Round down when displaying to users so there is no confusion when they cant buy something cause their balance is rounded up when displayed
    public static String convertCentsToCurrencyShort(double minorUnit) {
        if (CurrencyShort == null) {
            CurrencyShort = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.shortformat"), formatSymbols);
        }
        double majorUnit = (double) (Math.floor(minorUnit) / 100.0);
        return CurrencyShort.format(majorUnit);
    }

    public static String convertCentsToCurrencyLong(double minorUnit) {
        if (CurrencyLong == null) {
            CurrencyLong = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat"), formatSymbols);
        }
        double majorUnit = (double) (Math.floor(minorUnit) / 100.0);
        return CurrencyLong.format(majorUnit);
    }

    public static String convertCentsToCurrencyLongRoundHalfEven(double minorUnit) {
        if (CurrencyLong == null) {
            CurrencyLong = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat"), formatSymbols);
        }
        return CurrencyLong.format(Utils.round(minorUnit / 100.0, 2));
    }

    public static String convertCentsToSpecifiedCurrencyLongRoundHalfEven(String currency, double minorUnit) {
        if (currency.equals(BaseUtils.getProperty("env.locale.currency.majorunit"))) {
            return convertCentsToCurrencyLongRoundHalfEven(minorUnit);
        }
        DecimalFormat fmt = new DecimalFormat(BaseUtils.getProperty("env.locale.currency.longformat" + "." + currency), formatSymbols);
        return fmt.format(Utils.round(minorUnit / 100.0, 2));
    }

    public static String displayVolumeAsString(double units, String unitType) {
        if (unitType.equalsIgnoreCase("byte")) {
            // convert to Mb or MiB
            units = units / BaseUtils.getDoubleProperty("env.portals.data.volume.display.denominator", 1000000d); // to show MB as MiB, change this property to 1048576
            if (units > -1 && units < 1) {
                //value is between -1 and 1 so return 1 decimal place
                return dataWithDecimalPlace.format(units);
            } else {
                return dataWithoutDecimalPlace.format(units);
            }
        } else if (unitType.equalsIgnoreCase("sec") || unitType.equalsIgnoreCase("second")) {
            int minutes = (int) units / 60;
            int seconds = (int) Utils.round(units % 60.0d, 0);
            return String.format("%dm%02ds", minutes, seconds);
        } else if (unitType.equalsIgnoreCase("cent")) {
            return convertCentsToCurrencyLong(units);
        } else {
            return formatBigNumber(units);
        }

    }

    public static String displayVolumeAsStringWithCommaGroupingSeparator(double units, String unitType) {
        if (unitType.equalsIgnoreCase("byte")) {
            // convert to Mb or MiB
            units = units / BaseUtils.getDoubleProperty("env.portals.data.volume.display.denominator", 1000000d); // to show MB as MiB, change this property to 1048576
            if (units > -1 && units < 1) {
                //value is between -1 and 1 so return 1 decimal place
                return commaDataWithDecimalPlace.format(units);
            } else {
                return commaDataWithoutDecimalPlace.format(units);
            }
        } else if (unitType.equalsIgnoreCase("sec") || unitType.equalsIgnoreCase("second")) {
            int minutes = (int) units / (60);
            int seconds = (int) units % 60;
            return String.format("%dm%02ds", minutes, seconds);
        } else {
            return formatBigNumber(units);
        }

    }

    public static String displayVolumeAsStringInGB(double units, String unitType) {
        if (unitType.equalsIgnoreCase("byte")) {
            // convert to GB
            if (units < (1024 * 1024 * 1024)) {
                units = units / (1024 * 1024d);
            } else {
                units = units / (1024 * 1024 * 1024d);
            }

            if (units > -1 && units < 1) {
                //value is between -1 and 1 so return 1 decimal place
                return commaDataWithDecimalPlaceGB.format(units);
            } else {
                if (units % 1 == 0) {
                    return commaDataWithoutDecimalPlaceGB.format(units);
                } else {
                    return commaDataWithDecimalPlaceGB.format(units);
                }
            }
        } else if (unitType.equalsIgnoreCase("sec") || unitType.equalsIgnoreCase("second")) {
            int minutes = (int) units / (60);
            int seconds = (int) units % 60;
            return String.format("%dm%02ds", minutes, seconds);
        } else {
            return formatBigNumber(units);
        }

    }

    public static InputStream getStreamFromString(String s) {
        try {
            return new ByteArrayInputStream(s.getBytes("UTF-8"));
        } catch (Exception e) {
            myLogger.warn("Error converting string to stream", e);
            return null;
        }
    }

    public static XMLGregorianCalendar getBeginningOfNextDay(XMLGregorianCalendar date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(Utils.getJavaDate(date));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return getDateAsXMLGregorianCalendar(cal.getTime());
    }

    public static Date getBeginningOfNextDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
        return cal.getTime();
    }

    public static Date getBeginningOfYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    public static Date getBeginningOfPreviousDay(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    public static Date getBeginningOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static XMLGregorianCalendar getEndOfDay(XMLGregorianCalendar date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(Utils.getJavaDate(date));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return getDateAsXMLGregorianCalendar(cal.getTime());
    }

    public static Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static final SimpleDateFormat sdfLong = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    private static final SimpleDateFormat sdfShort = new SimpleDateFormat("yyyy/MM/dd");

    public static String formatDateLong(Date d) {
        if (d == null) {
            return "";
        }
        String ret = sdfLong.format(d);
        return ret;
    }

    public static String formatDateShort(Date d) {
        if (d == null) {
            return "";
        }
        String ret = sdfShort.format(d);
        return ret;
    }

    public static Throwable getDeepestCause(Throwable e) {
        if (e == null) {
            return null;
        }
        Throwable underlying;
        if (e instanceof InvocationTargetException) {
            underlying = ((InvocationTargetException) e).getTargetException();
        } else {
            underlying = e.getCause();
        }
        while (underlying != null && underlying.getCause() != null) {
            underlying = underlying.getCause();
        }
        if (underlying == null) {
            //There is no deeper level error, use e
            underlying = e;
        }
        return e;
    }

    private static String getRandomAscii(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int character = 65 + random.nextInt(26); // A, B , C ... to Z
            if (random.nextBoolean()) {
                character += 32; // make lowercase randomly
            }
            sb.append((char) character);
        }
        return sb.toString();
    }

    public static String shortenURLWithSmile(String longUrl) {
        return shortenURLWithSmile(longUrl, (3600 * 24 * 14));
    }

    public static String shortenURLWithSmile(String longUrl, int timeInSec) {
        boolean foundUnused;
        String randomString = null;
        do {
            randomString = getRandomAscii(5);
            foundUnused = (CacheHelper.getFromRemoteCache("TinyURL_" + randomString) == null);
        } while (!foundUnused);
        CacheHelper.putInRemoteCache("TinyURL_" + randomString, longUrl, timeInSec);
        return "https://" + BaseUtils.getProperty("env.portal.url") + "/t/" + randomString;
    }

    public static String lengthenURL(String tinyBit) {
        return (String) CacheHelper.getFromRemoteCache("TinyURL_" + tinyBit);
    }

    public static String shortenURL(String longUrl) {
        String shortUrl = "";
        String googUrl = "https://www.googleapis.com/urlshortener/v1/url?shortUrl=http://goo.gl/fbsS&key=" + BaseUtils.getProperty("env.google.api.key", "AIzaSyAlgM9nvHuHvNYZHjGiZkPVXLWlgcZCKJE");
        OutputStreamWriter wr = null;
        BufferedReader rd = null;
        try {
            URLConnection conn;
            String proxyHost = BaseUtils.getProperty("env.http.proxy.host", "");
            int proxyPort = BaseUtils.getIntProperty("env.http.proxy.port", 0);
            if (!proxyHost.isEmpty() && proxyPort > 0) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                conn = new URL(googUrl).openConnection(proxy);
            } else {
                conn = new URL(googUrl).openConnection();
            }
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json");
            wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write("{\"longUrl\":\"" + longUrl + "\"}");
            wr.flush();

            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.contains("id")) {
                    // I'm sure there's a more elegant way of parsing
                    // the JSON response, but this is quick/dirty...
                    shortUrl = line.substring(8, line.length() - 2);
                    break;
                }
            }

        } catch (MalformedURLException ex) {
            myLogger.warn("Error shortening URL with goo.gl", ex);
        } catch (IOException ex) {
            myLogger.warn("Error shortening URL with goo.gl", ex);
        } finally {
            if (wr != null) {
                try {
                    wr.close();
                } catch (Exception e) {
                }
            }
            if (rd != null) {
                try {
                    rd.close();
                } catch (Exception e) {
                }
            }
        }
        return shortUrl;
    }

    public static int getLuhnCheckDigit(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {

            // Get the digit at the current position.
            int digit = Integer.parseInt(number.substring(i, (i + 1)));

            if ((i % 2) == 0) {
                digit = digit * 2;
                if (digit > 9) {
                    digit = (digit / 10) + (digit % 10);
                }
            }
            sum += digit;
        }

        // The check digit is the number required to make the sum a multiple of
        // 10.
        int mod = sum % 10;
        return ((mod == 0) ? 0 : 10 - mod);
    }

    public static String getStackTrace(Throwable t) {
        if (t == null) {
            return "Throwable is null";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private static ConcurrentHashMap<String, String> sectorTownMap = null;
    private static long mapLastRefreshed = 0;

    public static String getSectorsTown(String location) {

        if (location == null) {
            return "unknown";
        }

        location = location.toLowerCase();

        if (sectorTownMap == null || System.currentTimeMillis() - mapLastRefreshed > 650000) {
            mapLastRefreshed = System.currentTimeMillis();
            List<String[]> sectorTowns = BaseUtils.getPropertyFromSQL("global.sector.towns");
            ConcurrentHashMap<String, String> sectorTownMapTmp = new ConcurrentHashMap<>();
            for (String[] row : sectorTowns) {
                sectorTownMapTmp.put(row[0], row[1]);
            }
            sectorTownMap = sectorTownMapTmp;
        }

        String town = sectorTownMap.get(location);
        if (town == null) {
            // Look if the location has an ip address in it (e.g. for voice)
            int ipStart = location.indexOf("app-ip-address=");
            if (ipStart != -1) {
                String ip = location.substring(ipStart + 15).split(";")[0];
                myLogger.debug("IP is [{}]", ip);
                return getSectorsTown((String) CacheHelper.getFromRemoteCache("IPLocation_" + ip));
            }

            town = "unknown";
            if (!location.contains(";") && !location.isEmpty()) {
                BaseUtils.sendTrapToOpsManagement(BaseUtils.MAJOR, "No town data for sector with TAI:ECGI: " + location + ". Please add it to Network.sector_location table", "BM");
            }
        }
        return town;
    }

    public static void getGlobalLock(EntityManager em, String lockName) {
        Date requested = new Date();
        myLogger.debug("Getting lock");
        Query q = em.createNativeQuery("update global_lock set REQUESTED_DATE_TIME=?, HELD_BY=? WHERE LOCK_NAME=?");
        q.setParameter(1, requested);
        q.setParameter(2, BaseUtils.getHostNameFromKernel());
        q.setParameter(3, lockName);
        int updates = q.executeUpdate();
        if (updates == 0) {
            myLogger.debug("Lock row does not exist so adding");
            q = em.createNativeQuery("insert ignore into global_lock values (?,?,?,?)");
            q.setParameter(1, lockName);
            q.setParameter(2, BaseUtils.getHostNameFromKernel());
            q.setParameter(3, requested);
            q.setParameter(4, requested);
            q.executeUpdate();
        }
        myLogger.debug("Got lock");
        q = em.createNativeQuery("update global_lock set OBTAINED_DATE_TIME=? WHERE LOCK_NAME=?");
        q.setParameter(1, new Date());
        q.setParameter(2, lockName);
        q.executeUpdate();
        // Lock will be removed when transaction on em is committed or rolled back
    }

    public static long getDaysBetween2Dates(Date d1, Date d2) {
        long diff = d2.getTime() - d1.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    public static String getMillisHumanReadable(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        }
        if (ms >= 1000 && ms < 60000) {
            return ms / 1000 + "s";
        }
        if (ms >= 60000 && ms < 3600000) {
            return ms / 60000 + "m";
        }
        if (ms >= 3600000 && ms < 86400000) {
            return ms / 3600000 + "h";
        }
        if (ms >= 86400000) {
            return ms / 86400000 + "d";
        }
        return "";
    }

    private static final DecimalFormat BigNumber = new DecimalFormat("#,##0", formatSymbols);

    private static String formatBigNumber(double d) {
        return BigNumber.format(d);
    }

    public static long getDayCoveredByDates(Date start, Date end) {
        return getDaysBetween2Dates(start, end) + 1;
    }

    public static byte[] readFileBytes(String filename) throws IOException {
        Path path = Paths.get(filename);
        return Files.readAllBytes(path);
    }

    public static boolean checkifImeiIsEquivalentToImeisv(String imei, String imeisv) {
        // If either imei or imeisv is  not specified,  assmume they are equivalent - to handle the case when the network did not supply imeisv
        if (imei == null || imeisv == null || imei.isEmpty() || imeisv.isEmpty()) {
            return true;
        }

        if (imeisv.startsWith("IMEISV=")) {
            imeisv = imeisv.substring(7);
            System.out.println(imeisv);
        }

        // IMEI must be length 15 and imeisv must be 16 in length. 
        if (imei.length() != 15) {
            // "Invalid imei length"
            return false;
        }

        if (imeisv.length() != 16) {
            // "Invalid imeisv length"
            return false;
        }

        return (imei.substring(0, 13).equals(imeisv.substring(0, 13)));
    }

    public static Date stringToDate(String dateInString) {
        Date date = null;
        if (dateInString != null && !dateInString.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                date = sdf.parse(dateInString);
            } catch (Exception e) {
                myLogger.error("Error:", e);
            }
        }

        return date;
    }
}
