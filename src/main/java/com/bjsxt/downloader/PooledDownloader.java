package com.bjsxt.downloader;

import com.bjsxt.connection.PooledHttpClient;
import com.bjsxt.constant.Constant;
import com.bjsxt.engine.Engine;
import com.bjsxt.exceptions.ContentTypeException;
import com.bjsxt.parser.IPaserBean;
import com.bjsxt.utils.BoundedBuffer;
import com.bjsxt.utils.LeveledPage;
import com.bjsxt.utils.LeveledURL;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.*;

public class PooledDownloader implements Runnable
{
    //TODO 从配置文件读取
    private int maxDownloadThread = 5;
    private ExecutorService downloaderPool = Executors.newFixedThreadPool(maxDownloadThread, new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable r)
        {
            return new Thread(r,"PooledDownloader Pool Thread");
        }
    });
    private BoundedBuffer<LeveledPage> downloadResults;
    private BlockingQueue<Future<LeveledURL>> targetURLs;
    private String startURL;
    //是否已经下载完所有网页
    private boolean isFinished = false;

    private PooledHttpClient client;
    ConcurrentSkipListSet<String> downloadedURLs = new ConcurrentSkipListSet<String>();

    public PooledDownloader(BoundedBuffer<LeveledPage> downloadResults,
                            BlockingQueue<Future<LeveledURL>> targetURLs,
                            IPaserBean iPaserBean,
                            PooledHttpClient pooledHttpClient)
    {
        this.downloadResults = downloadResults;
        this.targetURLs = targetURLs;
        this.client = pooledHttpClient;
        startURL = iPaserBean.getStartURL();
    }

    @Override
    public void run()
    {
        startDownload(startURL);
    }

    public Thread start()
    {
        Thread newThread = new Thread(this,"PooledDownloader Self Thread");
        newThread.start();
        return newThread;
    }

    public void startDownload(String starterURL)
    {
        //TODO 需要把循环放到这里来，因为如果在downloadFromURL被打断的话，线程池可能不会正确关闭
        downloadFromURL(starterURL,1);
        downloadFromTargetURLs();
    }

    public void downloadFromTargetURLs()
    {
        try
        {
            while (!isFinished && !Thread.currentThread().isInterrupted())
            {
                final LeveledURL finalUrl;
                try
                {
                    finalUrl = targetURLs.remove().get();
                }
                catch (Exception e)
                {
                    //get会抛出异常，这时继续循环
                    continue;
                }

                Future<LeveledPage> result = downloaderPool.submit(new Callable<LeveledPage>()
                {
                    @Override
                    public LeveledPage call() throws ContentTypeException, ClientProtocolException, IOException, IllegalArgumentException, URISyntaxException
                    {
                        return download(finalUrl.getUrl(), finalUrl.getLevel());
                    }
                });
                downloadedURLs.add(finalUrl.getUrl());
                downloadResults.add(result);
            }
        }
        finally
        {
            downloaderPool.shutdown();
        }
    }

    /**
     * 下载页面，以字符串的形式返回页面内容。
     * Content-Type必须是text/html，否则会抛出异常
     * @param url
     * @return
     */
    public Future<LeveledPage> downloadFromURL(String url,int level)
    {
        final String finalUrl = url;
        if (!downloadedURLs.contains(finalUrl))
        {
            Future<LeveledPage> result = downloaderPool.submit(new Callable<LeveledPage>()
            {
                public LeveledPage call() throws ContentTypeException, ClientProtocolException, IOException, IllegalArgumentException,URISyntaxException
                {
                    return download(finalUrl,level);
                }
            });
            downloadedURLs.add(finalUrl);
            downloadResults.add(result);
            return result;
        }
        else
        {
            return null;
        }

    }

    private LeveledPage download(String urlString,int level) throws ContentTypeException, ClientProtocolException, IOException, IllegalArgumentException,URISyntaxException
    {
        HttpClientContext context = new HttpClientContext();

        //URL里可能带有特殊字符，例如中括号，会引起异常，必须在这里转换成URI对象
        URI uri;
        try
        {
            URL urlTemp = new URL(urlString);
            uri = new URI(urlTemp.getProtocol(), urlTemp.getHost(), urlTemp.getPath(), urlTemp.getQuery(), null);
        }
        catch (MalformedURLException e)
        {
            throw new IOException(urlString,e);
        }


        HttpPost post = new HttpPost(uri);
        CloseableHttpResponse response = null;
        try
        {
            response = client.executePost(post, context);
        }
        catch (IOException e)
        {
            System.out.println(post.getURI());
            if (!Engine.getInstance().isFinished())
            {
                e.printStackTrace();
            }
            throw new IOException(post.getURI().toString(),e);
            //throw e;
        }
        HttpEntity entity = response.getEntity();
        String contentType = ContentType.getOrDefault(entity).getMimeType();
        String downloadedHtml = null;
        try
        {
            if (!contentType.equals(Constant.PAGE_CONTENT_TYPE))
            {
                throw new ContentTypeException("Content type exception:" + Constant.PAGE_CONTENT_TYPE);
            }
            else
            {
                //TODO 需要处理非UTF8编码的网页
                downloadedHtml = EntityUtils.toString(entity);
            }
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Entity is null or too long");
            throw e;
        }
        catch (ContentTypeException e)
        {
            throw e;
        }
        finally
        {
            EntityUtils.consume(entity);
        }
        LeveledPage result = new LeveledPage();
        result.setHtml(downloadedHtml);
        result.setLevel(level);
        result.setUri(uri);
        return result;
    }
}
