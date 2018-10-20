package com.bjsxt.parser;

import com.bjsxt.engine.Engine;
import com.bjsxt.utils.BoundedBuffer;
import com.bjsxt.utils.LeveledPage;
import com.bjsxt.utils.LeveledURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class PooledParserByClass implements Runnable
{
    private int maxParseThread = 5;
    private ExecutorService parserPool = Executors.newFixedThreadPool(maxParseThread, new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r,"PooledParserByClass Pool Thread");
        }
    });

    //TODO 从文件中读取类名
    //private String IPaserBeanClassName = "PiratebayPaserBean";
    private IPaserBean paserBean;

    private BoundedBuffer<LeveledPage> downloadResults;
    private BlockingQueue<Future<LeveledURL>> targetURLs;
    private BlockingQueue<Future<List<Map<String, String>>>> dataParsed;

    private boolean isFinished = false;

    public PooledParserByClass(BoundedBuffer<LeveledPage> downloadResults,
                               BlockingQueue<Future<LeveledURL>> targetURLs,
                               BlockingQueue<Future<List<Map<String, String>>>> dataParsed,
                               IPaserBean iPaserBean) throws Exception
    {
        this.downloadResults = downloadResults;
        this.targetURLs = targetURLs;
        this.dataParsed = dataParsed;
        this.paserBean = iPaserBean;
    }

    @Override
    public void run()
    {
        parse();
    }

    public Thread start()
    {
        Thread newThread = new Thread(this,"PooledParserByClass Self Thread");
        newThread.start();
        return newThread;
    }

    public void parse()
    {
        try
        {
            while (!isFinished && !Thread.currentThread().isInterrupted())
            {
                try
                {
                    final LeveledPage leveledPage = downloadResults.pop().get();

                    if (paserBean.hasSecondaryUrl() && leveledPage.getLevel() == 2)
                    {
                        //从二级URL中获取数据
                        Future<List<Map<String, String>>> data = parserPool.submit(new Callable<List<Map<String, String>>>()
                        {
                            @Override
                            public List<Map<String, String>> call() throws Exception
                            {
                                Document document = Jsoup.parse(leveledPage.getHtml(), leveledPage.getUri().toString());
                                return paserBean.getData(document);
                            }
                        });
                        dataParsed.add(data);
                    }
                    else if (paserBean.hasSecondaryUrl() && leveledPage.getLevel() == 1)
                    {
                        //从一级URL中获取二级URL
                        final Document document = Jsoup.parse(leveledPage.getHtml(), leveledPage.getUri().toString());
                        Future<String[]> secondaryURLs = parserPool.submit(new Callable<String[]>()
                        {
                            @Override
                            public String[] call() throws Exception
                            {
                                //Document document = Jsoup.parse(leveledPage.getHtml());
                                String[] urlStrings = paserBean.SecondaryUrls(document);
                                for (String url : urlStrings)
                                {
                                    Future<LeveledURL> leveledURLFuture = parserPool.submit(new Callable<LeveledURL>()
                                    {
                                        @Override
                                        public LeveledURL call() throws Exception
                                        {
                                            LeveledURL result = new LeveledURL();
                                            result.setUrl(url);
                                            result.setLevel(2);
                                            return result;
                                        }
                                    });
                                    targetURLs.add(leveledURLFuture);
                                }
                                return urlStrings;
                            }
                        });

                        //从一级URL中获取下一页
                        Future<LeveledURL> nextPage = parserPool.submit(new Callable<LeveledURL>()
                        {
                            @Override
                            public LeveledURL call() throws Exception
                            {
                                String nextPageUrl = paserBean.nextPage(document);
                                LeveledURL result = new LeveledURL();
                                result.setUrl(nextPageUrl);
                                result.setLevel(1);
                                return result;
                            }
                        });
                        targetURLs.add(nextPage);
                    }
                    else if (!paserBean.hasSecondaryUrl())
                    {
                        Future<List<Map<String, String>>> data = parserPool.submit(new Callable<List<Map<String, String>>>()
                        {
                            @Override
                            public List<Map<String, String>> call() throws Exception
                            {
                                Document document = Jsoup.parse(leveledPage.getHtml(), leveledPage.getUri().toString());
                                return paserBean.getData(document);
                            }
                        });
                        dataParsed.add(data);
                    }
                }
                catch (ExecutionException e)
                {
                    System.out.println("download result Execution Exception");
                    if (!Engine.getInstance().isFinished())
                    {
                        e.printStackTrace();
                        System.out.println(e.getCause().getMessage());
                    }
                    continue;
                }
                catch (InterruptedException e)
                {
                    System.out.println("parsing download result interrupted");
                    if (!Engine.getInstance().isFinished())
                    {
                        e.printStackTrace();
                    }
                    Thread.currentThread().interrupt();
                    continue;
                }
            }
        }
        finally
        {
            parserPool.shutdown();
        }
    }
}
