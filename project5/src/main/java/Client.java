import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class Client extends Thread {

    Socket socket;
    DataOutputStream sendBuffer;
    DataInputStream receiveBuffer;

    @Override
    public void run(){
        try {
            socket = new Socket("172.20.10.6", 34800);
            sendBuffer = new DataOutputStream(socket.getOutputStream());
            receiveBuffer = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("unable to initialize socket");
            throw new RuntimeException(e);
        }
        System.out.println("new Client " + this.toString() + "socket be " + socket.toString());
        getMessageFromOtherClient();
    }


    public void sendMessage(String command) {
        try {
            sendBuffer.writeUTF(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void getMessageFromOtherClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                while (true) {
                    try {
                        message = receiveBuffer.readUTF();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println(message);
                    serverResponseToAction(message);
                }
            }
        }).start();
    }

    private void serverResponseToAction(String response) {
    }
}


