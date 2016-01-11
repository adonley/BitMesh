package network.bitmesh.cloudserver.Utils;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersioningTest extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(VersioningTest.class.getName());

    public void testLatestReleaseName() throws Exception
    {
        log.info(Versioning.latestReleaseName());
    }

    public void testLatestReleaseInfo() throws Exception
    {
        log.info(Versioning.latestReleaseInfo().getPackageName());
    }
}