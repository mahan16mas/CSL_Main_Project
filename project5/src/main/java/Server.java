import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server extends Thread {
    private Socket newSoc;
    public static int tag = 100;

    private Socket webSoc;
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    @Override
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(34800)) {
            System.out.println("Server is listening on port 34800");
            webSoc = serverSocket.accept();
            System.out.println("web:" + webSoc.toString());
            while (true) {
                newSoc = serverSocket.accept();
                UserThread userThread = new UserThread(newSoc, webSoc,tag);
                userThread.start();
                tag++;
                System.out.println(newSoc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
