package common.page;

import common.Config;
import common.Util;
import common.page.Page;
import db.DbManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageInserter
{
    private static Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})_(\\d{2}-\\d{2}-\\d{2})_(\\d+)_page.json");
    private File pageJsonFile;
    private String crawlDate;
    private String crawlTime;
    private String pageId;
    private String dbCrawlDateTime;

    public PageInserter(File pageJsonFile)
    {
        this.pageJsonFile = pageJsonFile;
        Matcher matcher = pattern.matcher(pageJsonFile.getName());
        if(matcher.matches())
        {
            crawlDate = matcher.group(1);
            crawlTime = matcher.group(2);
            pageId = matcher.group(3);
            dbCrawlDateTime = crawlDate + " " + crawlTime.replaceAll("-", ":");
        }
    }

    public void processPage()
    {
        JSONObject pageJson = null;
        InputStream is = null;
        try
        {
            is = new FileInputStream(pageJsonFile);
            JSONParser parser = new JSONParser();
            pageJson = (JSONObject) parser.parse(new InputStreamReader(is, Charset.forName("UTF-8")));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try { if(null != is) is.close(); } catch (Exception e) { e.printStackTrace(); }
        }

        Page page = new Page(pageJson, dbCrawlDateTime);

        page.updateDb();

        Util.sleepMillis(100);

        Integer tempLikes = DbManager.getInt("SELECT likes FROM `Page` WHERE id='" + page.getId() + "'");
        boolean success = null != tempLikes && page.getLikes() == tempLikes;

        if(success)
        {
            String dir = Util.buildPath("archive", page.getUsername(), "page");
            String path = dir + "/" + pageJsonFile.getName();
            success = pageJsonFile.renameTo(new File(path));
            if(!success)
            {
                System.err.println(Util.getDbDateTimeEst() + " failed to move " + pageJsonFile.getAbsolutePath() + " to " + path);
                System.exit(0);
            }

            if(Config.scrapeHistory)
            {
                page.insertPageCrawl();
            }
        }
    }
}
