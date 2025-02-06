import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

public class UserThread extends Thread {
    public Socket socket;
    public Socket webSocket;
    public int tag;
    public JFrame frame = new JFrame("Received Painting");
    private DataInputStream dataInputStream1;
    private DataOutputStream dataOutputStream1;

    public UserThread(Socket socket1, Socket webSocket, int tag) {
        this.socket =socket1;
        this.webSocket = webSocket;
        this.tag = tag;
    }


    @Override
    public void run(){
        try {
            dataInputStream1 = new DataInputStream(socket.getInputStream());
            dataOutputStream1 = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (true) {

            String command1 = null; // Read the command from the client
            try {
                command1 = dataInputStream1.readUTF();
                if (command1.startsWith("IMAGE:")) {
                    String base64Image = command1.substring(6);
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

                    // Convert byte array to BufferedImage
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                        BufferedImage image = ImageIO.read(bais);
                        frame.getContentPane().removeAll();
                        frame.setSize(800, 600);
                        JLabel label = new JLabel(new ImageIcon(image));
                        frame.add(label);
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        frame.setVisible(true);
                        frame.toFront();
                        try {
                            DataOutputStream targetUser = new DataOutputStream(webSocket.getOutputStream());
                            targetUser.writeUTF("TAG:"+ tag + ":" + command1);
                            targetUser.flush();
                            System.out.println("sent to web");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("Painting displayed on server.");
                    } catch (IOException e) {
                        System.err.println("Error reading image data: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                else
                    System.out.println("what was that mate?");
            } catch (IOException e) {
                return;
            }
        }
    }
}
