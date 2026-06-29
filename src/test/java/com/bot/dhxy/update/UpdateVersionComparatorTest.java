package com.bot.dhxy.update;

public class UpdateVersionComparatorTest {

    public static void main(String[] args) {
        assertCompare("patch version is newer", 1, UpdateVersionComparator.compare("1.0.1", "1.0.0"));
        assertCompare("same version is equal", 0, UpdateVersionComparator.compare("1.0.0", "1.0.0"));
        assertCompare("v prefix is ignored", 1, UpdateVersionComparator.compare("v1.2.0", "1.1.9"));
        assertCompare("older major version is older", -1, UpdateVersionComparator.compare("1.9.9", "2.0.0"));
        assertCompare("snapshot suffix does not beat release with same base", 0,
                UpdateVersionComparator.compare("1.0.0-SNAPSHOT", "1.0.0"));
    }

    private static void assertCompare(String caseName, int expectedSign, int actual) {
        int actualSign = Integer.compare(actual, 0);
        if (actualSign != expectedSign) {
            throw new AssertionError(caseName + ": expectedSign=" + expectedSign + " actual=" + actual);
        }
    }
}
