import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class PaintApp extends JFrame {
    private final List<DrawAction> actions = new ArrayList<>();
    private Color currentColor = Color.RED;
    private String paintingName = "Untitled";
    private Point lastPoint = null;

    public PaintApp() {
        setTitle("Standalone Paint App");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        DrawingArea drawingArea = new DrawingArea(actions);
        add(drawingArea, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton redButton = new JButton("Red");
        JButton blueButton = new JButton("Blue");
        JButton eraseButton = new JButton("Erase");
        JButton renameButton = new JButton("Rename");

        redButton.addActionListener(e -> currentColor = Color.RED);
        blueButton.addActionListener(e -> currentColor = Color.BLUE);

        eraseButton.addActionListener(e -> {
            actions.clear();
            drawingArea.repaint();
        });

        Timer timer = new Timer(1000, e -> savePaint(drawingArea));
        timer.start();

        renameButton.addActionListener(e -> renamePainting(drawingArea));

        controls.add(redButton);
        controls.add(blueButton);
        controls.add(eraseButton);
        controls.add(renameButton);

        add(controls, BorderLayout.SOUTH);

        drawingArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastPoint = null;
            }
        });

        drawingArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastPoint != null) {
                    actions.add(new DrawAction(lastPoint, e.getPoint(), currentColor));
                    lastPoint = e.getPoint();
                    drawingArea.repaint();
                }
            }
        });

        setVisible(true);
    }

    private void savePaint(DrawingArea drawingArea) {
        BufferedImage image = new BufferedImage(drawingArea.getWidth(), drawingArea.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        drawingArea.paint(g2d);
        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] imageBytes = baos.toByteArray();

            String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
            main.client.sendMessage("IMAGE:" + base64Image);
            System.out.println("Painting sent to the server.");
        } catch (IOException e) {
            System.err.println("Error saving or sending painting: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renamePainting(DrawingArea drawingArea) {
        JDialog dialog = new JDialog(this, "Rename Painting", true);
        dialog.setSize(500, 400);
        dialog.setLayout(new BorderLayout());

        JTextField textField = new JTextField(paintingName, 20);
        textField.setFont(new Font("Arial", Font.PLAIN, 18));
        JPanel keyboardPanel = new JPanel();
        keyboardPanel.setLayout(new GridLayout(3, 9, 5, 5));

        boolean[] isUppercase = {false};

        String[] keys = {
                "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
                "A", "S", "D", "F", "G", "H", "J", "K", "L",
                "Z", "X", "C", "V", "B", "N", "M"
        };

        for (String key : keys) {
            JButton button = new JButton(key);
            button.setFont(new Font("Arial", Font.PLAIN, 16));
            button.addActionListener(e -> {
                String text = isUppercase[0] ? button.getText() : button.getText().toLowerCase();
                textField.setText(textField.getText() + text);
            });
            keyboardPanel.add(button);
        }

        JPanel specialKeysPanel = new JPanel();
        specialKeysPanel.setLayout(new GridLayout(1, 4, 10, 10));

        JButton spaceButton = new JButton("Space");
        spaceButton.setFont(new Font("Arial", Font.BOLD, 15));
        spaceButton.addActionListener(e -> textField.setText(textField.getText() + " "));
        specialKeysPanel.add(spaceButton);

        JButton backspaceButton = new JButton("Backspace");
        backspaceButton.setFont(new Font("Arial", Font.BOLD, 13));
        backspaceButton.addActionListener(e -> {
            String currentText = textField.getText();
            if (!currentText.isEmpty()) {
                textField.setText(currentText.substring(0, currentText.length() - 1));
            }
        });
        specialKeysPanel.add(backspaceButton);

        JButton toggleCaseButton = new JButton("Uppercase");
        toggleCaseButton.setFont(new Font("Arial", Font.BOLD, 15));
        toggleCaseButton.addActionListener(e -> {
            isUppercase[0] = !isUppercase[0];
            toggleCaseButton.setText(isUppercase[0] ? "Lowercase" : "Uppercase");
        });
        specialKeysPanel.add(toggleCaseButton);

        JButton doneButton = new JButton("Done");
        doneButton.setFont(new Font("Arial", Font.BOLD, 15));
        doneButton.addActionListener(e -> {
            paintingName = textField.getText().trim();
            drawingArea.repaint();
            dialog.dispose();
        });
        specialKeysPanel.add(doneButton);

        dialog.add(textField, BorderLayout.NORTH);
        dialog.add(keyboardPanel, BorderLayout.CENTER);
        dialog.add(specialKeysPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private class DrawingArea extends JPanel {
        private final List<DrawAction> actions;

        public DrawingArea(List<DrawAction> actions) {
            this.actions = actions;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (DrawAction action : actions) {
                g.setColor(action.getColor());
                if (action.getEndPoint() != null) {
                    g.drawLine(
                            action.getStartPoint().x, action.getStartPoint().y,
                            action.getEndPoint().x, action.getEndPoint().y
                    );
                }
            }
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString(paintingName, 10, 20);
        }
    }

    private static class DrawAction {
        private final Point startPoint;
        private final Point endPoint;
        private final Color color;

        public DrawAction(Point startPoint, Point endPoint, Color color) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.color = color;
        }

        public Point getStartPoint() {
            return startPoint;
        }

        public Point getEndPoint() {
            return endPoint;
        }

        public Color getColor() {
            return color;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PaintApp::new);
    }
}
