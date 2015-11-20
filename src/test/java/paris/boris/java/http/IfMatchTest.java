package paris.boris.java.http;

import com.dasanjos.java.http.HttpRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import junit.framework.Assert;
import org.junit.Test;

public class IfMatchTest {

    @Test
    public void IfMatchContainsTagMultipleTags() throws IOException {
        // arrange
        HttpRequest req = new HttpRequest(new ByteArrayInputStream("GET /somefile.txt HTTP/1.1\nIf-Match: \"1234\", \"4567\", \"8910\"\n\n".getBytes()));
        String currentTag = "\"1234\"";

        // act
        boolean result = req.ifMatch(currentTag);

        // assert
        Assert.assertTrue(result);
    }

    @Test
    public void IfMatchContainsTagSingleTag() throws IOException {
        // arrange
        HttpRequest req = new HttpRequest(new ByteArrayInputStream("GET /somefile.txt HTTP/1.1\nIf-Match: \"1234\"\n\n".getBytes()));
        String currentTag = "\"1234\"";

        // act
        boolean result = req.ifMatch(currentTag);

        // assert
        Assert.assertTrue(result);
    }

    @Test
    public void IfMatchNotContainsTagMultipleTags() throws IOException {
        // arrange
        HttpRequest req = new HttpRequest(new ByteArrayInputStream("GET /somefile.txt HTTP/1.1\nIf-Match: \"1234\", \"4567\", \"8910\"\n\n".getBytes()));
        String currentTag = "\"asdf\"";

        // act
        boolean result = req.ifMatch(currentTag);

        // assert
        Assert.assertFalse(result);
    }

    @Test
    public void IfMatchNotContainsTagSingleTag() throws IOException {
        // arrange
        HttpRequest req = new HttpRequest(new ByteArrayInputStream("GET /somefile.txt HTTP/1.1\nIf-Match: \"8910\"\n\n".getBytes()));
        String currentTag = "\"asdf\"";

        // act
        boolean result = req.ifMatch(currentTag);

        // assert
        Assert.assertFalse(result);
    }

    @Test
    public void IfMatchWildcard() throws IOException {
        // arrange
        HttpRequest req = new HttpRequest(new ByteArrayInputStream("GET /somefile.txt HTTP/1.1\nIf-Match: *\n\n".getBytes()));
        String currentTag = "*";

        // act
        boolean result = req.ifMatch(currentTag);

        // assert
        Assert.assertTrue(result);
    }
}
