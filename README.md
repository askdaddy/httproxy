# HTTProxy
HTTProxy is a low overhead (in terms of memory and processing time) Java based HTTP proxy. It provides simple APIs you can use to sift HTTP request headers and determine
the server connection to be proxied.

## Getting Started

### Direct Download
You can download <a href="https://github.com/baswerc/httproxy/releases/download/v1.0/httproxy-1.0.jar">httproxy-1.0.jar</a> directly and place in your project.

### Using Maven
Add the following dependency into your Maven project:

````xml
<dependency>
    <groupId>org.baswell</groupId>
    <artifactId>httproxy</artifactId>
    <version>1.0</version>
</dependency>
````

### Dependencies
HTTProxy had one external dependency on <a href="http://trove.starlight-systems.com/">Trove4j</a>.

````xml
<dependency>
  <groupId>net.sf.trove4j</groupId>
  <artifactId>trove4j</artifactId>
  <version>[3,)</version>
  <scope>provided</scope>
</dependency>
````

## Using The Proxy

HTTProxy has two implementations. One that uses standard blocking IO and one that uses non-blocking IO. Both implementations rely on the base interface
<a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/ProxyDirector.html">ProxyDirector</a> you must implement that performs the following:

* Provides proxy configuration.
* Provides the server connection to proxy requests to.
* Receives proxy event notifications.
* Tell HTTProxy to modify or add proxied HTTP headers.

### Blocking IO
For blocking IO use <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/ServerSocketAcceptLoop.html">ServerSocketAcceptLoop</a>. You must implement
the interface <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/IOProxyDirector.html">IOProxyDirector</a> and pass it and an `ExecutorService` to `ServerSocketAcceptLoop` when constructed.
Once you are ready to start listening for incoming HTTP requests to proxy call the `start()` method which blocks the current thread for the lifetime of the server accept loop.

```Java
IOProxyDirector proxyDirector = new MyIOProxyDirector();
ThreadPoolExecutor threadPool = new ThreadPoolExecutor(250, 2000, 25,
     TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
ServerSocket serverSocket = new ServerSocket(8080);
ServerSocketAcceptLoop acceptLoop = new ServerSocketAcceptLoop(proxyDirector, threadPool);

// blocks until acceptLoop.stop() is called from another thread or
// the server socket throws an IOException
acceptLoop.start(serverSocket);
```

### Non-Blocking IO
For non-blocking IO use <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/ServerSocketChannelAcceptLoop.html">ServerSocketChannelAcceptLoop</a>. You must implement
the interface <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/NIOProxyDirector.html">NIOProxyDirector</a> and pass it to `ServerSocketChannelAcceptLoop` when constructed.
Once you are ready to start listening for incoming HTTP requests to proxy call the `start()` method which blocks the current thread for the lifetime of the server accept loop.

```Java
NIOProxyDirector proxyDirector = new MyNIOProxyDirector();
ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(proxyDirector);
ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
serverSocketChannel.socket().bind(new InetSocketAddress(8080));

// blocks until acceptLoop.stop() is called from another thread or
// the server socket channel throws an IOException
acceptLoop.start(serverSocketChannel);
```

#### SSL With Non-Blocking IO
Unlike blocking IO, the JVM does not provide standard SSLSocketChannel and SSLServerSocketChannel classes that extend the base one. Instead the SSL exchanges must be manually orchestrated using a <a href="http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLEngine.html">SSLEngine</a>. 
HttProxy provides two helper classes that implement this orchestration so you don't have to. Use the <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/SSLServerSocketChannel.html">SSLServerSocketChannel</a> to accept incoming SSL connections with non-blocking IO.

```Java
NIOProxyDirector proxyDirector = new MyNIOProxyDirector();
ServerSocketChannelAcceptLoop acceptLoop = new ServerSocketChannelAcceptLoop(proxyDirector);

ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
serverSocketChannel.socket().bind(new InetSocketAddress(8080));

SSLContext sslContext = SSLContext.getInstance("TLS");
ThreadPoolExecutor sslThreadPool = new ThreadPoolExecutor(250, 2000, 25, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

SSLServerSocketChannel sslServerSocketChannel = new SSLServerSocketChannel(serverSocketChannel, serverContext, sslThreadPool, proxyDirector.getLogger());
acceptLoop.start(sslServerSocketChannel);
```

Use can also use <a href="http://baswerc.github.io/httproxy/javadoc/org/baswell/httproxy/SSLSocketChannel.html">SSLSocketChannel</a> to create non-blocking, SSL connections to a proxied server.
```Java
public class MyNIOProxyDirector implements NIOProxyDirector
{
...

  public SocketChannel connectToProxiedHost(ProxiedRequest request) throws IOException
  {
    InetSocketAddress address = new InetSocketAddress("localhost", 443);
    SocketChannel socketChannel = SocketChannel.open(address);
    socketChannel.configureBlocking(false);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    SSLEngine sslEngine = sslContext.createSSLEngine(address.getHostName(), address.getPort());
    sslEngine.setUseClientMode(true);

    return new SSLSocketChannel(socketChannel, sslEngine, sslThreadPool, getLogger());
  }
}
```


# Additional Documentation

* <a href="http://baswerc.github.io/httproxy/javadoc/">Javadoc</a>

# Developed By

Corey Baswell - <a href="mailto:corey.baswell@gmail.com">corey.baswell@gmail.com</a>

# License
````
Copyright 2015 Corey Baswell

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````
