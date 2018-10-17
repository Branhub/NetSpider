package com.bjsxt.connection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PooledHttpClient
{
    private CloseableHttpClient client;
    //private Thread[] postThreads;
    //private ExecutorService postThreadPool;
    private Thread connectionMonitor;

    //TODO 从配置文件中读取
    //private int maxPostThreadCount = 5;

    public PooledHttpClient()
    {
        client = HttpClients.custom()
                            .setConnectionManager(ConnectionPoolSingleton.getInstance())
                            .build();

        connectionMonitor = new Thread(new Runnable()
        {
            //TODO 从配置文件中读取
            private int tickTime = 5000;
            public void run()
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    try
                    {
                        synchronized (this)
                        {
                            wait(tickTime);
                            ConnectionPoolSingleton.getInstance().closeExpiredConnections();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        System.out.println("Connection Monitor is Interrupted");
                        Thread.currentThread().interrupt();
                    }
                }
            }
        },"connectionMonitor");
        connectionMonitor.start();
    }

    public CloseableHttpResponse executePost(HttpPost post, HttpContext context) throws ClientProtocolException,IOException
    {
        return client.execute(post,context);
    }
    public void close()
    {
        try
        {
            client.close();
            connectionMonitor.interrupt();
        }
        catch (IOException e)
        {
            //TODO 记录日志
            e.printStackTrace();
        }
    }
}
