import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebThread extends Thread {

    private Socket socket;
    private DataOutputStream sendBuffer;
    private DataInputStream receiveBuffer;

    // Store up to 4 images, keyed by user tag
    private final Map<String, BufferedImage> userImages = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        WebThread server = new WebThread();
        server.start();
    }

    @Override
    public void run() {
        try {
            socket = new Socket("172.20.10.6", 34800);
            sendBuffer = new DataOutputStream(socket.getOutputStream());
            receiveBuffer = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Unable to initialize socket");
            throw new RuntimeException(e);
        }

        System.out.println("WebThread initialized: " + this.toString() + ", Socket: " + socket.toString());

        // Start HTTP server
        new Thread(this::startHttpServer).start();

        // Listen for messages from the other client
        getMessageFromOtherClient();
    }

    private void startHttpServer() {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("HTTP server started on port 8080");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleHttpRequest(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleHttpRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
             OutputStream rawOut = clientSocket.getOutputStream()) {

            // Read HTTP request headers
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
            }

            // Generate HTML for up to 4 images
            StringBuilder imageHtml = new StringBuilder();
            for (Map.Entry<String, BufferedImage> entry : userImages.entrySet()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(entry.getValue(), "png", baos);
                String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());
                imageHtml.append("<div style='margin: 10px;'>")
                        .append("<h3>User: ").append(entry.getKey()).append("</h3>")
                        .append("<img src='data:image/png;base64,").append(base64Image).append("' alt='User Image' style='max-width:300px;height:auto;'>")
                        .append("</div>");
            }

            String response = """
                    HTTP/1.1 200 OK
                    Content-Type: text/html

                    <html>
                    <head><title>Painting Viewer</title></head>
                    <body>
                    <h1>Images from Users</h1>
                    %s
                    </body>
                    </html>
                    """.formatted(imageHtml.toString());

            // Send response
            out.print(response);
            out.flush();

        } catch (IOException e) {
            System.err.println("Error handling HTTP request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(String command) {
        try {
            sendBuffer.writeUTF(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void getMessageFromOtherClient() {
        new Thread(() -> {
            while (true) {
                try {
                    String message = receiveBuffer.readUTF();
                    System.out.println(message);
                    serverResponseToAction(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void serverResponseToAction(String response) {
        if (response.startsWith("TAG:")) {
            // Extract user tag and base64 image
            String userTag = response.substring(4, 7); // Extract tag
            String base64Image = response.substring(14);

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Convert byte array to BufferedImage
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                BufferedImage image = ImageIO.read(bais);

                // Store image in the map (max 4 images)
                synchronized (userImages) {
                    if (userImages.size() >= 4) {
                        String oldestKey = userImages.keySet().iterator().next();
                        userImages.remove(oldestKey); // Remove the oldest entry
                    }
                    userImages.put(userTag, image);
                }

                System.out.println("Image updated successfully for user: " + userTag);

            } catch (IOException e) {
                System.err.println("Error reading image data: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
