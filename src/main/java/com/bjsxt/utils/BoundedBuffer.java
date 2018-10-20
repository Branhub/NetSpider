package com.bjsxt.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//TODO 写成泛型
public class BoundedBuffer<T>
{
    //private List<Future<String>> downloadResults = new LinkedList<Future<String>>();
    private List<Future<T>> downloadResults = new LinkedList<>();
    //TODO 从配置文件读取
    private final int MAX_CAPACITY;
    private ReentrantLock lock;
    private Condition full;
    private Condition empty;

    public BoundedBuffer()
    {
        lock = new ReentrantLock();
        full = lock.newCondition();
        empty = lock.newCondition();
        MAX_CAPACITY = 100;
    }
    public BoundedBuffer(int maxCapacity)
    {
        lock = new ReentrantLock();
        full = lock.newCondition();
        empty = lock.newCondition();
        MAX_CAPACITY = maxCapacity;
    }
    public boolean isEmpty()
    {
        return downloadResults.isEmpty();
    }
    public boolean isFull()
    {
        return downloadResults.size() >= MAX_CAPACITY;
    }

    public boolean add(Future<T> downloadResult)
    {
        try
        {
            lock.lock();
            while (isFull())
            {
                full.await();
            }
            downloadResults.add(downloadResult);
            empty.signal();
            return true;
        }
        catch (InterruptedException e)
        {
            //TODO 记录日志
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
        finally
        {
            lock.unlock();
        }
    }

    public Future<T> pop() throws InterruptedException
    {
        try
        {
            lock.lock();
            while (isEmpty())
            {
                empty.await();
            }

            int finishedIndex = -1;
            Future<T> result = null;
            for (int i = 0;i < downloadResults.size();i++)
            {
                if (downloadResults.get(i).isDone())
                {
                    finishedIndex = i;
                    break;
                }
            }
            if (finishedIndex > -1)
            {
                //返回已完成的任务结果
                result = downloadResults.remove(finishedIndex);
            }
            else
            {
                //如果没有已完成的任务，返回队首
                result = downloadResults.remove(0);
            }
            full.signal();
            return result;
        }
        catch (InterruptedException e)
        {
            //TODO 记录日志
            //e.printStackTrace();
            System.out.println("BoundedBuffer Interrupted");
            Thread.currentThread().interrupt();
            throw e;
            //return null;
        }
        finally
        {
            lock.unlock();
        }
    }
}
