package com.bjsxt.connection;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class ConnectionPoolSingleton extends PoolingHttpClientConnectionManager
{
    //private PoolingHttpClientConnectionManager poolManager;
    //TODO 从配置文件中读取
    private int maxTotalConnectionConfig = 50;
    private int maxDefaultPerRouteConfig = 5;

    private ConnectionPoolSingleton()
    {
        //poolManager = new PoolingHttpClientConnectionManager();
        super();
        super.setMaxTotal(maxTotalConnectionConfig);
        super.setDefaultMaxPerRoute(maxDefaultPerRouteConfig);
    }
    private static class InstanceHolder
    {
        private static final ConnectionPoolSingleton INSTANCE = new ConnectionPoolSingleton();
    }
    public static ConnectionPoolSingleton getInstance()
    {
        return InstanceHolder.INSTANCE;
    }
}
