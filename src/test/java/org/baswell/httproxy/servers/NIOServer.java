package org.baswell.httproxy.servers;

import org.baswell.httproxy.*;

import javax.crypto.Cipher;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NIOServer
{
  public static void main(String[] args) throws Exception
  {
    final SSLContext sslContext = SSLContext.getInstance("TLS");

    TrustManager trustAll = new X509TrustManager()
    {
      @Override
      public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
      {
//        System.out.println(x509Certificates[0]);
      }

      @Override
      public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException
      {
//        System.out.println(x509Certificates);
      }

      @Override
      public X509Certificate[] getAcceptedIssuers()
      {
        return new X509Certificate[0];
      }
    };

    sslContext.init(null, new TrustManager[]{trustAll}, null);

    final ThreadPoolExecutor sslThreadPool = new ThreadPoolExecutor(250, 2000, 25, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    SSLEngine sslEngine = sslContext.createSSLEngine("localhost", 44301);

    SSLContext sslContextServer = SSLContext.getInstance("TLS");
    TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager()
    {
      public X509Certificate[] getAcceptedIssuers()
      {
        return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType)
      {}

      public void checkServerTrusted(X509Certificate[] certs, String authType)
      {}
    }};
    sslContextServer.init(null, trustManagers, new SecureRandom());



    SimpleNIOProxyDirector proxyDirector = new SimpleNIOProxyDirector("localhost", 44303, sslContext, sslThreadPool);
    proxyDirector.logLevel = SimpleProxyLogger.DEBUG_LEVEL;

    ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(proxyDirector);
//    ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(new SimpleNIOProxyDirector("localhost", 48001));


    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream("test.keystore"), "changeit".toCharArray());

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "changeit".toCharArray());

    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    SSLContext serverContext = SSLContext.getInstance("TLS");
    serverContext.init(keyManagers, new TrustManager[]{trustAll}, null);

//    serverContext = SSLContext.getDefault();

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(9090));

    SSLServerSocketChannel sslServerSocketChannel = new SSLServerSocketChannel(serverSocketChannel, serverContext, proxyDirector);

    acceptLoop.start(sslServerSocketChannel);
  }
}
