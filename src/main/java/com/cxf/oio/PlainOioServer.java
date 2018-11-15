package com.cxf.oio;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by cxf on 2018/11/15.
 */
public class PlainOioServer {
    public void serve(int port) throws IOException {
        //将服务器绑定到指定端口
        final ServerSocket socket = new ServerSocket(port);
        try {
            for (;;) {
                //接受连接
                final Socket clientSocket = socket.accept();
                System.out.println("Accepted connection from " + clientSocket);
                new Thread(new Runnable() {
                    public void run() {
                        OutputStream out;
                        try {
                            out = clientSocket.getOutputStream();
                            byte[] b = {1,2};
                            //将消息写给已连接的客户端
                            out.write(b);
                            out.flush();
                            clientSocket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                //关闭连接
                                clientSocket.close();
                            } catch (IOException ex) {
                                // ignore on close
                            }
                        }
                    }
                }).start();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
