package orbisoftware;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Simple Swing image-frame stream viewer.
 *
 * Frames are loaded from classpath resources under:
 *   /image_frames/
 *
 * Required resource file:
 *   /image_frames/index.txt
 *
 * Example index.txt contents:
 *   00000.jpg
 *   00001.jpg
 *   00002.jpg
 *
 * This works both:
 * - from an IDE/classes directory
 * - from a runnable JAR containing image_frames inside the JAR
 */
public class SwingImageFramePlayer extends JFrame {

    private static final String FRAME_DIR = "/image_frames/";
    private static final String FRAME_INDEX = FRAME_DIR + "index.txt";

    private final List<String> frameResourceNames;
    private final ImagePanel imagePanel;
    private final JLabel statusLabel;

    private Timer playbackTimer;
    private int currentFrameIndex = 0;
    private float fps = 0.1f;
    private boolean playing = true;
    private boolean randomSeqMode = false; // Random / Sequential mode
    
    private final float epsilon = 0.00001f;

    public SwingImageFramePlayer() {
        super("Simple Frame Stream Viewer");

        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.ddforcevram", "true"); 

        this.frameResourceNames = loadFrameIndex();
        if (frameResourceNames.isEmpty()) {
            throw new IllegalArgumentException("No frame entries found in resource: " + FRAME_INDEX);
        }

        this.imagePanel = new ImagePanel();
        this.statusLabel = new JLabel();

        buildUi();
        loadAndShowFrame(0);
        updateTimerDelay();
        setVisible(true);
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(imagePanel, BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);

        setSize(900, 650);
        setLocationRelativeTo(null);
    }

    private JPanel buildControls() {
        JPanel root = new JPanel(new BorderLayout());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton playPauseButton = new JButton("Pause");
        JButton restartButton = new JButton("Restart");
        JButton randSeqButton = new JButton("Random");
        JButton prevButton = new JButton("Prev");
        JButton nextButton = new JButton("Next");
        JButton slowerButton = new JButton("- FPS");
        JButton fasterButton = new JButton("+ FPS");

        playPauseButton.addActionListener(e -> {
            playing = !playing;
            if (playing) {
            	updateTimerDelay();
                playPauseButton.setText("Pause");
            } else {
            	playbackTimer.cancel();
                playPauseButton.setText("Play");
            }
            updateStatus();
        });

        restartButton.addActionListener(e -> loadAndShowFrame(0));

        randSeqButton.addActionListener(e -> {
        	randomSeqMode = !randomSeqMode;
        	
        	if (randomSeqMode)
        		randSeqButton.setText("Sequential");
        	else
        		randSeqButton.setText("Random");
        });
        
        prevButton.addActionListener(e -> {
            int prev = currentFrameIndex - 1;
            if (prev < 0) {
                prev = frameResourceNames.size() - 1;
            }
            loadAndShowFrame(prev);
        });

        nextButton.addActionListener(e -> {
            int next = currentFrameIndex + 1;
            if (next >= frameResourceNames.size()) {
                next = 0;
            }
            loadAndShowFrame(next);
        });

        slowerButton.addActionListener(e -> {
        	
        	if (fps < 1.01f)
        		fps -= 0.1;
        	else
        		fps = Math.max(1, (int) fps - 5.0f);
        	
        	if (fps < 0.1f)
        		fps = 0.1f;
        	
            updateTimerDelay();
            updateStatus();
        });

        fasterButton.addActionListener(e -> {
        	
        	if (fps < 1.01f)
        		fps += 0.1;
        	else
        		fps = Math.min(30, (int) fps + 5.0f);
        	
        	if (Math.abs(fps - 1.1) < epsilon)
        		fps = 5.0f;
        	
            updateTimerDelay();
            updateStatus();
        });

        buttons.add(playPauseButton);
        buttons.add(restartButton);
        buttons.add(randSeqButton);
        buttons.add(prevButton);
        buttons.add(nextButton);
        buttons.add(slowerButton);
        buttons.add(fasterButton);

        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        updateStatus();

        root.add(buttons, BorderLayout.WEST);
        root.add(statusLabel, BorderLayout.CENTER);

        return root;
    }

    private void updateTimerDelay() {
    	
    	if (playbackTimer != null)
    		playbackTimer.cancel();
    	
    	playbackTimer = new Timer();
    	
    	playbackTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	int next;
            	
            	if (randomSeqMode) {
            		Random rand = new Random();
            		int randomNum = rand.nextInt(frameResourceNames.size());
            		next = randomNum;
            	}
            	else {
            		next = currentFrameIndex + 1;
	                if (next >= frameResourceNames.size())
	                    next = 0;
            	}
            	
                loadAndShowFrame(next);
            }
        }, 0, (long) (1.0f/fps * 1000));
    }

    private void loadAndShowFrame(int index) {
        if (index < 0 || index >= frameResourceNames.size()) {
            return;
        }

        String resourceName = frameResourceNames.get(index);
        String resourcePath = FRAME_DIR + resourceName;

        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }

            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unsupported or unreadable image resource: " + resourcePath);
            }

            currentFrameIndex = index;
            imagePanel.setImage(image);
            updateStatus();
        } catch (IOException ex) {
            imagePanel.setError("Failed to load frame: " + resourceName);
            updateStatus();
        }
    }

    private void updateStatus() {
        statusLabel.setText(
                "FPS: " + String.format("%.1f", fps)  +
                "   Frame: " + currentFrameIndex + "/" + (frameResourceNames.size() - 1) +
                "   State: " + (playing ? "Playing" : "Paused") +
                "   File: " + frameResourceNames.get(currentFrameIndex)
        );
    }

    private List<String> loadFrameIndex() {
        List<String> frames = new ArrayList<>();

        try (InputStream in = getClass().getResourceAsStream(FRAME_INDEX)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing required resource: " + FRAME_INDEX);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (trimmed.startsWith("#")) {
                        continue;
                    }
                    frames.add(trimmed);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read frame index: " + FRAME_INDEX, e);
        }

        return frames;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new SwingImageFramePlayer();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit(1);
            }
        });
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage sourceImage;
        private String errorMessage;

        private BufferedImage backBuffer;
        private int backBufferW = -1;
        private int backBufferH = -1;

        ImagePanel() {
            setBackground(Color.BLACK);
            setDoubleBuffered(false);
        }

        void setImage(BufferedImage image) {
            this.sourceImage = image;
            this.errorMessage = null;
            renderBackBuffer();
            repaint();
        }

        void setError(String errorMessage) {
            this.sourceImage = null;
            this.errorMessage = errorMessage;
            renderBackBuffer();
            repaint();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            backBuffer = null;
            backBufferW = -1;
            backBufferH = -1;
        }

        @Override
        public void doLayout() {
            super.doLayout();
            if (getWidth() != backBufferW || getHeight() != backBufferH) {
                renderBackBuffer();
            }
        }

        private void renderBackBuffer() {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());

            if (backBuffer == null || w != backBufferW || h != backBufferH) {
                backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                backBufferW = w;
                backBufferH = h;
            }

            Graphics2D g2 = backBuffer.createGraphics();
            try {
            	
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, w, h);

                if (errorMessage != null) {
                    g2.setColor(Color.RED);
                    g2.drawString(errorMessage, 20, 30);
                    return;
                }

                if (sourceImage == null) {
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.drawString("No frame loaded", 20, 30);
                    return;
                }

                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE); 
                
                int imageW = sourceImage.getWidth();
                int imageH = sourceImage.getHeight();

                double scale = Math.min(w / (double) imageW, h / (double) imageH);
                int drawW = (int) Math.round(imageW * scale);
                int drawH = (int) Math.round(imageH * scale);

                int x = (w - drawW) / 2;
                int y = (h - drawH) / 2;

                g2.drawImage(sourceImage, x, y, drawW, drawH, null);
            } finally {
                g2.dispose();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (backBuffer == null) {
                renderBackBuffer();
            }
            g.drawImage(backBuffer, 0, 0, null);
        }

        @Override
        public void update(Graphics g) {
            paint(g);
        }

        @Override
        public void paint(Graphics g) {
            paintComponent(g);
            paintBorder(g);
            paintChildren(g);
        }
    }
}