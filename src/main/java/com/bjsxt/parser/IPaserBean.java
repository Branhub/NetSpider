package com.bjsxt.parser;

import org.jsoup.nodes.Document;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface IPaserBean
{
    public String getStartURL();
    //获取停止数据条数，即数据达到这些条后就停止爬取
    public int getStopDataCount();
    public String nextPage(Document html);
    //是否有二级URL，如果返回true，就会调用SecondaryUrl来获取二级Url，并从二级Url下载的页面上运行getData
    public boolean hasSecondaryUrl();
    public String[] SecondaryUrls(Document html);
    public Map<String, String> getData(Document html);
    //返回数据的名字，必须与getData返回map的key对应
    public String[] getDataNames();

    public String getOutputFileName();

}
