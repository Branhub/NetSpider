package com.bjsxt.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaserBean implements IPaserBean
{
    @Override
    public String getStartURL()
    {
        return "https://thepiratebay.rocks/browse/201";
    }

    @Override
    public int getStopDataCount()
    {
        return 10;
    }

    public String nextPage(Document html)
    {
        return html.select("[alt=Next]").parents().get(0).absUrl("href");
    }

    @Override
    public boolean hasSecondaryUrl()
    {
        return true;
    }

    public String[] SecondaryUrls(Document html)
    {
        Elements elements = html.select("a.detLink");
        String[] result = new String[elements.size()];
        for (int i = 0;i < elements.size();i++)
        {
            result[i] = elements.get(i).absUrl("href");
        }
        return result;
    }
    public Map<String, String> getData(Document html)
    {
        Map<String,String> data = new HashMap<>();
        //data.put("title",html.select("#title>a:first-child").html());
        data.put("title",html.select("#title").html());
        //System.out.println(data.get("title"));
        data.put("magnet",html.select("div.download>a:first-child").attr("href"));
        return data;
    }

    @Override
    public String[] getDataNames()
    {
        return new String[] {"title","magnet"};
    }

    @Override
    public String getOutputFileName()
    {
        return "./src/main/resources/ParseResult/pirateBay.csv";
    }
}
