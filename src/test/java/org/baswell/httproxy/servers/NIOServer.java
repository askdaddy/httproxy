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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NIOServer
{
  public static void main(String[] args) throws Exception
  {
    int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
    System.out.println(maxKeyLen);

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
    final ProxyLogger logger = new SimpleProxyLogger(SimpleProxyLogger.INFO_LEVEL);

    SSLEngine sslEngine = sslContext.createSSLEngine("localhost", 44301);

    ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(new SimpleNIOProxyDirector("localhost", 8080)
    {
      /*
      public SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException
      {
        InetSocketAddress address = new InetSocketAddress("localhost", 44301);
        SocketChannel socketChannel = SocketChannel.open(address);
        socketChannel.configureBlocking(false);
        SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostName(), address.getPort());
        sslEngine.setUseClientMode(true);

        return new SSLSocketChannel(socketChannel, sslEngine, sslThreadPool, logger);
      }
      */
    }, 1);
//    ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(new SimpleNIOProxyDirector("localhost", 48001));


    KeyStore keyStore = KeyStore.getInstance("JKS");
    keyStore.load(new FileInputStream("test.keystore"), "changeit".toCharArray());

    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, "changeit".toCharArray());

    KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
    SSLContext serverContext = SSLContext.getInstance("TLS");
    serverContext.init(keyManagers, new TrustManager[]{trustAll}, null);

    //serverContext = SSLContext.getDefault();

    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(9090));

    SSLServerSocketChannel sslServerSocketChannel = new SSLServerSocketChannel(serverSocketChannel, serverContext, sslThreadPool, logger);
    acceptLoop.start(sslServerSocketChannel);
  }
}
