package com.liushao.redislockframework;


import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPool;

public class SecKillTest {
	private static Long commidityId1 = 10000001L;
	private static Long commidityId2 = 10000002L;
	private 
	RedisClient client;
	public static String HOST = "127.0.0.1";
	private JedisPool jedisPool;
	@Before
	public synchronized void  beforeTest() throws IOException{
		
		
		jedisPool = new JedisPool("127.0.0.1");
		
	}
	
	@Test
	public void testSecKill(){
		int threadCount = 1000;
		int splitPoint = 500;
		final CountDownLatch endCount = new CountDownLatch(threadCount);//一般用于某个线程A等待若干个其他线程执行完任务之后，它才执行
		//final CountDownLatch beginCount = new CountDownLatch(1);//此用法存在部分线程还未调度另一部分线程已经开始执行秒杀任务了，初始化线程进行到一半就开发发号施令，存在不公平现象
		final CyclicBarrier beginBarrier=new CyclicBarrier(threadCount);//一般用于一组线程互相等待至某个状态，然后这一组线程再同时执行；当所有线程都初始化完成后才发号施令，进行资源抢夺
		final SecKillImpl testClass = new SecKillImpl();
		
		Thread[] threads = new Thread[threadCount];
		//起500个线程，秒杀第一个商品
		for(int i= 0;i < splitPoint;i++){
			threads[i] = new Thread(new  Runnable() {
				public void run() {
					try {
						//等待在一个信号量上，挂起
						System.out.println("操作"+commidityId1+",线程"+Thread.currentThread().getName());
						//beginCount.await();
						beginBarrier.await();
						//用动态代理的方式调用secKill方法
						SeckillInterface proxy = (SeckillInterface) Proxy.newProxyInstance(SeckillInterface.class.getClassLoader(), 
							new Class[]{SeckillInterface.class}, new CacheLockInterceptor(testClass));
						proxy.secKill("test", commidityId1);
						System.out.println("操作结束"+commidityId1+",线程"+Thread.currentThread().getName());
						endCount.countDown();
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}
			});
			threads[i].start();

		}
		
		for(int i= splitPoint;i < threadCount;i++){
			threads[i] = new Thread(new  Runnable() {
				public void run() {
					try {
						//等待在一个信号量上，挂起
						System.out.println("操作"+commidityId2+",线程"+Thread.currentThread().getName());
						beginBarrier.await();
						//用动态代理的方式调用secKill方法
						SeckillInterface proxy = (SeckillInterface) Proxy.newProxyInstance(SeckillInterface.class.getClassLoader(),
							new Class[]{SeckillInterface.class}, new CacheLockInterceptor(testClass));
						proxy.secKill("test", commidityId2);
						System.out.println("操作结束"+commidityId2+",线程"+Thread.currentThread().getName());
						endCount.countDown();
					}catch (InterruptedException e) {
						e.printStackTrace();
					} catch (BrokenBarrierException e) {
						e.printStackTrace();
					}
				}
			});
			threads[i].start();

		}
		
		
		long startTime = System.currentTimeMillis();
		//主线程释放开始信号量，并等待结束信号量
		System.out.println("=============================================================");
		//beginCount.countDown();
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		try {
			//主线程等待结束信号量
			endCount.await();
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			//观察秒杀结果是否正确
			System.out.println(SecKillImpl.inventory.get(commidityId1));
			System.out.println(SecKillImpl.inventory.get(commidityId2));
			System.out.println("error count" + CacheLockInterceptor.ERROR_COUNT);
			System.out.println("total cost " + (System.currentTimeMillis() - startTime));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
