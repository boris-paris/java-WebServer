package paris.boris.java.http.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 *
 * @author boris paris
 */
public class CacheUtils {
    
    private static final Logger LOG = Logger.getLogger(CacheUtils.class);
    
    /**
     * Reads the last modified date from the given file, and returns a String
     * object containing the date in RFC 1123 format
     *
     * @param file
     * @return a String containing the last modified in RFC 1123 format
     */
    public static String generateLastModified(File file) {
        return ZonedDateTime.ofInstant(new Date(file.lastModified()).toInstant(),
                ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * Generates an md5 hash that can be used as an ETag. The hash generation
     * only takes the file conten into account. While this slows down the
     * generation of the hash compared to using the Inode, it enables the ETag
     * to work in distributed environments.
     *
     * @param file the file to generate an ETag hash for
     * @return a String containing an ETag hash
     */
    public static String generateETag(File file) {
        try (InputStream in = new FileInputStream(file)){
            
            // Read the content of the file into a StringBuilder
            StringBuilder buffer = new StringBuilder();
            int c;
            while ((c = in.read()) > -1) {
                buffer.append((char)c);
            }
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(buffer.toString().getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return "\"" + number.toString(16) + "\""; // adding quotes for opaque-tag
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error finding algorithm for MessageDigest", e);
        } catch (IOException e) {
            LOG.error("Error generating ETag for file: " + file.getAbsolutePath(), e);
        }

        return null;
    }    
}
