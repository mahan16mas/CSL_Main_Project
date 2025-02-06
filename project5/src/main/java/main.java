import javax.swing.*;

public class main {
    static public Client client;

    public static void main(String[] args) {
        Client client = new Client();
        main.client = client;
        client.start();
        SwingUtilities.invokeLater(PaintApp::new);
    }
}