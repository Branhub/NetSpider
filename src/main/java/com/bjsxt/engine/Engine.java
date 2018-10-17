package com.bjsxt.engine;

import com.bjsxt.connection.PooledHttpClient;
import com.bjsxt.downloader.PooledDownloader;
import com.bjsxt.parser.IPaserBean;
import com.bjsxt.parser.PaserBean;
import com.bjsxt.parser.PooledParserByClass;
import com.bjsxt.persistence.DefaultPersistence;
import com.bjsxt.persistence.IPersistence;
import com.bjsxt.utils.BoundedBuffer;
import com.bjsxt.utils.LeveledPage;
import com.bjsxt.utils.LeveledURL;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class Engine
{
    private static final BoundedBuffer<LeveledPage> downloadResults = new BoundedBuffer();
    private static final BlockingQueue<Future<LeveledURL>> targetURLs = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Future<Map<String,String>>> dataParsed = new LinkedBlockingQueue<>();
    //TODO 从文件中读取类名
    private static final String IPaserBeanClassName = "com.bjsxt.parser.PaserBean";
    private final IPaserBean iPaserBean;

    private final PooledDownloader pooledDownloader;
    private final PooledParserByClass pooledParserByClass;
    private final PooledHttpClient pooledHttpClient;
    private final IPersistence iPersistence;

    private final Thread downloaderThread;
    private final Thread parserThread;
    private final Thread persistenceThread;

    private boolean isFinished = false;

    private Engine() throws Exception
    {
        try
        {
            iPaserBean = (IPaserBean) Class.forName(IPaserBeanClassName).newInstance();
        }
        catch (ClassNotFoundException e)
        {
            System.out.println(IPaserBeanClassName + " not found");
            throw e;
        }
        catch (IllegalAccessException e)
        {
            System.out.println(IPaserBeanClassName + " or its nullary constructor is not accessible");
            throw e;
        }
        catch (Exception e)
        {
            throw e;
        }

        pooledHttpClient = new PooledHttpClient();
        pooledDownloader = new PooledDownloader(downloadResults,targetURLs,iPaserBean,pooledHttpClient);
        pooledParserByClass = new PooledParserByClass(downloadResults,targetURLs,dataParsed,iPaserBean);
        iPersistence = new DefaultPersistence(dataParsed,iPaserBean);

        downloaderThread = pooledDownloader.start();
        parserThread = pooledParserByClass.start();
        persistenceThread = iPersistence.start();
    }

    private static class EngineHolder
    {
        private static final Engine INSTANCE;
        static
        {
            try
            {
                INSTANCE = new Engine();
            }
            catch (Exception e)
            {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static Engine getInstance()
    {
        return EngineHolder.INSTANCE;
    }

    public void stop()
    {
        isFinished = true;
        downloaderThread.interrupt();
        parserThread.interrupt();
        persistenceThread.interrupt();
        pooledHttpClient.close();
    }

    public boolean isFinished()
    {
        return isFinished;
    }

    public static void main(String[] args)
    {
        Engine.getInstance();
    }
}
