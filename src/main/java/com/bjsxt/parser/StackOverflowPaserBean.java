package com.bjsxt.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackOverflowPaserBean implements IPaserBean
{
    public String getStartURL()
    {
        return "https://stackoverflow.com/";
    }
    //获取停止数据条数，即数据达到这些条后就停止爬取
    public int getStopDataCount()
    {
        return 11;
    }
    public String nextPage(Document html)
    {
        return null;
    }
    //是否有二级URL，如果返回true，就会调用SecondaryUrl来获取二级Url，并从二级Url下载的页面上运行getData
    public boolean hasSecondaryUrl()
    {
        return false;
    }
    public String[] SecondaryUrls(Document html)
    {
        return null;
    }
    public List<Map<String, String>> getData(Document html)
    {
        List<Map<String,String>> dataList = new ArrayList<>();
        Elements elements = html.select(".question-summary>.summary>h3 a");
        for (Element element:elements)
        {
            Map<String,String> data = new HashMap<>();
            data.put("title",element.text());
            data.put("link",element.absUrl("href"));
            dataList.add(data);
        }
        return dataList;
    }

    //返回数据的名字，必须与getData返回map的key对应
    public String[] getDataNames()
    {
        return new String[] {"title","link"};
    }

    public String getOutputFileName()
    {
        return "./src/main/resources/ParseResult/StackOverflowIntersetingQuestions.csv";
    }
}
