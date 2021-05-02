package eu.bunburya.apogee.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.regex.Pattern

internal class PathStringUtilsKtTest {

    private val expectedSplits = mapOf(
        "test.gmi" to Pair("test", "gmi"),
        "test2.html" to Pair("test2", "html"),
        "test" to Pair("test", null),
        "test.txt.zip" to Pair("test.txt", "zip"),
        ".test" to Pair(".test", null),
        ".test.zip" to Pair(".test", "zip")
    )

    private val expectedInResults = listOf(
        Triple("/home/test/test.txt", "/home", true),
        Triple("/home/test/test.txt", "/home/", true),
        Triple("/home/test/test.txt", "/", true),
        Triple("/home/test/test.txt", "/home/test2", false),
        Triple("/home/test/..", "/home/test", false)
    )

    private val expectedRegexResults = listOf(
        Triple("^/test", "/test", true),
        Triple("^/test", "/test/", true),
        Triple("^/test", "test", false),
        Triple("^/test", "/atest", false),
        Triple("^/some_test$", "/some_test", true),
        Triple("^/some_test$", "/some_test/", false),
        Triple("^/docs/\\d+/file.txt$", "/docs/44/file.txt", true),
        Triple("^/docs/\\d+/file.txt$", "/docs/9/file.txt", true),
        Triple("^/docs/\\d+/file.txt$", "/docs/87655676/file.txt", true),
        Triple("^/docs/\\d+/file.txt$", "/docs//file.txt", false),
        Triple("^/docs/\\d+/file.txt$", "/docs/44/files.txt", false)
    )

    @Test
    fun testSplitExt() {
        for ((toTest, expected) in expectedSplits) {
            assertEquals(expected, splitExt(toTest))
        }
    }

    @Test
    fun testIsInDirectory() {
        for ((file, dir, result) in expectedInResults) {
            assertEquals(result, fileIsInDirectory(file, dir))
        }
    }

    @Test
    fun testRegexMatching() {
        var pattern: Pattern
        for ((patternStr, str, expected) in expectedRegexResults) {
            //println("Checking pattern $patternStr against $str, expecting $expected")
            pattern = Pattern.compile(patternStr)
            assertEquals(expected, pattern.matcher(str).find())
        }
    }

}