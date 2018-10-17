package com.bjsxt.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Properties;

public class Configs
{
    private static final String configFilePath = "./src/main/resources/Configs.properties";
    private int downloadThreadCount;
    private final int downloadThreadCountDefault = 10;
    private String downloadDir;
    private final String downloadDirDefault = "D:\\JavaTemp\\";

    private static class Holder
    {
        private static final Configs INSTANCE = new Configs();
    }
    public static Configs getInstance()
    {
        return Holder.INSTANCE;
    }

    private Configs()
    {
        //在这里读取文件中的配置
        Properties properties = null;
        try
        {
            properties = new Properties();
            FileInputStream fileInputStream = new FileInputStream(configFilePath);
            properties.load(fileInputStream);
        }
        catch (FileNotFoundException fileNotFound)
        {
            System.out.println(configFilePath + " not Found");
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }

        String downloadThreadCountStr = properties.getProperty("downloadThreadCount");
        if (null == downloadThreadCountStr)
        {
            downloadThreadCount = downloadThreadCountDefault;
            //这里应该记录日志
            System.out.println("Cannot find downloadThreadCount in Configs.properties");
        }
        else
        {
            downloadThreadCount = Integer.parseInt(downloadThreadCountStr);
        }

        downloadDir = properties.getProperty("downloadDir");
        if (null == downloadDir)
        {
            downloadDir = downloadDirDefault;
            System.out.println("Cannot find downloadThreadCount in Configs.properties");
        }

        //通过反射读取各个字段，还不完善
        /*
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field f : fields)
        {
            if (!Modifier.isFinal(f.getModifiers()))  //作为配置项的字段都不是final的，所以这里只处理不是final的字段
            {
                String fieldName = f.getName();
                String fieldValueString = properties.getProperty(fieldName);
                try
                {
                    f.set(this, Integer.valueOf(fieldValueString));
                }
                catch (IllegalAccessException e)
                {
                    e.fillInStackTrace();
                }
            }
        }
        */
    }

    public int getDownloadThreadCount()
    {
        return downloadThreadCount;
    }

    public String getDownloadDir()
    {
        return downloadDir;
    }

    /*
    public static void main(String[] args)
    {
        Configs configs = Configs.getInstance();
        System.out.println(configs.getDownloadThreadCount());
    }
    */

}
