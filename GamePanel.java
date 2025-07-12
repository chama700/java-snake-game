package snakeGame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {
    private static final int UNIT_SIZE = 30;
    private static final int COLS = 40; 
    private static final int ROWS = 25; 
    private static final int SCREEN_WIDTH = COLS * UNIT_SIZE; 
    private static final int SCREEN_HEIGHT = ROWS * UNIT_SIZE;
    private static final int GAME_UNITS = COLS * ROWS;
    private static final int DELAY = 150;

    private final int[] x = new int[GAME_UNITS];
    private final int[] y = new int[GAME_UNITS];
    private int bodyParts = 6; //length
    private int applesEaten; // score
    private int appleX, appleY; // Apple position
    private char direction = 'R';
    private boolean running = false;
    private boolean paused = false;
    private Timer timer;
    private final Random random = new Random(); // For random positions
    private final ArrayList<Point> obstacles = new ArrayList<>(); // Store obstacles
    private Image appleImage;
    private Image snakeHeadImage;
    private Image obstacleImage;


    // Theme management
    private enum Theme {
        DAY(
        	new Color(255, 240, 245),  new Color(255, 228, 241), // Background (LightPink to Lavender)
            new Color(218, 112, 214), new Color(238, 130, 238), // Snake head (Orchid, Violet)
            new Color(186, 85, 211), new Color(148, 0, 211), // Snake body (MediumOrchid, DarkViolet)
            new Color(255, 105, 180), new Color(255, 182, 193), // Apple (HotPink, LightPink)
            new Color(221, 160, 221),  new Color(238, 130, 238), // Obstacle (MediumTurquoise, PaleTurquoise)
            new Color(255, 192, 203), new Color(255, 215, 0) // UI (Pink, Gold)
        ),
        NIGHT(
            new Color(15, 25, 35), new Color(25, 35, 45), // Background
            new Color(34, 139, 34), new Color(50, 205, 50), // Snake head
            new Color(46, 125, 50), new Color(27, 94, 32), // Snake body
            new Color(220, 53, 69), new Color(255, 99, 132), // Apple
            new Color(63, 81, 181), new Color(92, 107, 192), // Obstacle
            new Color(0, 255, 127), new Color(255, 215, 0) // UI
        );

        final Color backgroundPrimary;
        final Color snakeBodyBase;
        final Color snakeBodyDark;
        final Color neonGreen;
        final Color gold;

        Theme(Color bgPrimary, Color bgSecondary, Color headBase, Color headHighlight,
              Color bodyBase, Color bodyDark, Color appleRed, Color appleHighlight,
              Color obsBase, Color obsHighlight, Color neonGreen, Color gold) {
            this.backgroundPrimary = bgPrimary;
            this.snakeBodyBase = bodyBase;
            this.snakeBodyDark = bodyDark;
            this.neonGreen = neonGreen;
            this.gold = gold;
        }
    }

    private Theme currentTheme = Theme.NIGHT;

    // Animation
    private float pulseAnimation = 0;
    private final Timer animationTimer;

 // Constructor
    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(currentTheme.backgroundPrimary);
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());
        
     // Load images
        appleImage = new ImageIcon(getClass().getResource("apple.png")).getImage();
        snakeHeadImage = new ImageIcon(getClass().getResource("snake_head.png")).getImage();
        obstacleImage = new ImageIcon(getClass().getResource("cactus.png")).getImage();
        
        animationTimer = new Timer(50, e -> {
            pulseAnimation += 0.15f;
            if (pulseAnimation > Math.PI * 2) pulseAnimation = 0;
            repaint();
        });
        animationTimer.start();

        startGame();
    }

    private void startGame() {
        newApple();
        generateObstacles(12);
        running = true;
        timer = new Timer(DELAY, this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBackground(g2d);
        draw(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        if (currentTheme == Theme.DAY) {
            g2d.setColor(new Color(0xB2868E));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.setStroke(new BasicStroke(1));

            for (int i = 0; i <= COLS; i++) {
                g2d.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, SCREEN_HEIGHT);
            }
            for (int i = 0; i <= ROWS; i++) {
                g2d.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
            }
        } else {
            g2d.setColor(new Color(0x18392B));
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.setStroke(new BasicStroke(1));

            for (int i = 0; i <= COLS; i++) {
                g2d.drawLine(i * UNIT_SIZE, 0, i * UNIT_SIZE, SCREEN_HEIGHT);
            }
            for (int i = 0; i <= ROWS; i++) {
                g2d.drawLine(0, i * UNIT_SIZE, SCREEN_WIDTH, i * UNIT_SIZE);
            }
        }
    }

    private void draw(Graphics2D g2d) {
        if (running && !paused) {
            drawApple(g2d);
            drawObstacles(g2d);
            drawSnake(g2d);
            drawUI(g2d);
        } else if (paused) {
            drawApple(g2d);
            drawObstacles(g2d);
            drawSnake(g2d);
            drawUI(g2d);
            drawPaused(g2d);
        } else {
            gameOver(g2d);
        }
    }

    private void drawPaused(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        String pauseText = "PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(pauseText, (SCREEN_WIDTH - fm.stringWidth(pauseText)) / 2, SCREEN_HEIGHT / 2);

        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        String instruction = "Press 'P' to resume, 'T' to toggle theme";
        fm = g2d.getFontMetrics();
        g2d.drawString(instruction, (SCREEN_WIDTH - fm.stringWidth(instruction)) / 2, SCREEN_HEIGHT / 2 + 40);
    }

    private void drawApple(Graphics2D g2d) {
        g2d.drawImage(appleImage, appleX, appleY, UNIT_SIZE, UNIT_SIZE, this);
    }

    private void drawObstacles(Graphics2D g2d) {
        int size = (int)(UNIT_SIZE * 1.5);
        int offset = (size - UNIT_SIZE) / 2;
        for (Point p : obstacles) {
            g2d.drawImage(obstacleImage, p.x - offset, p.y - offset, size, size, this);
        }
    }

    private void drawSnake(Graphics2D g2d) {
        for (int i = 0; i < bodyParts; i++) {
            if (i == 0) {
                drawSnakeHead(g2d, x[i], y[i]);
            } else {
                drawSnakeBody(g2d, x[i], y[i], i);
            }
        }
    }

    private void drawSnakeHead(Graphics2D g2d, int headX, int headY) {
        g2d.drawImage(snakeHeadImage, headX, headY, UNIT_SIZE, UNIT_SIZE, this);
    }

    private void drawSnakeBody(Graphics2D g2d, int bodyX, int bodyY, int segment) {
        float sizeMultiplier = 1.0f - (segment * 0.02f);
        if (sizeMultiplier < 0.7f) sizeMultiplier = 0.7f;
        int size = (int) (UNIT_SIZE * 0.85f * sizeMultiplier);
        int offset = (UNIT_SIZE - size) / 2;

        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillOval(bodyX + offset + 1, bodyY + offset + 1, size, size);

        float alpha = 1.0f - (segment * 0.05f);
        if (alpha < 0.3f) alpha = 0.3f;

        Color bodyColor1, bodyColor2;

        if (currentTheme == Theme.DAY) {
            Color pink = new Color(0xfb93c4);
            Color darkPink = new Color(0xD95A70);
            bodyColor1 = new Color(pink.getRed(), pink.getGreen(), pink.getBlue(), (int)(255 * alpha));
            bodyColor2 = new Color(darkPink.getRed(), darkPink.getGreen(), darkPink.getBlue(), (int)(255 * alpha));
        } else {
            bodyColor1 = new Color(currentTheme.snakeBodyBase.getRed(), currentTheme.snakeBodyBase.getGreen(), currentTheme.snakeBodyBase.getBlue(), (int)(255 * alpha));
            bodyColor2 = new Color(currentTheme.snakeBodyDark.getRed(), currentTheme.snakeBodyDark.getGreen(), currentTheme.snakeBodyDark.getBlue(), (int)(255 * alpha));
        }

        g2d.setPaint(new RadialGradientPaint(bodyX + UNIT_SIZE * 0.35f, bodyY + UNIT_SIZE * 0.35f, size * 0.6f,
                new float[]{0f, 0.6f, 1f}, new Color[]{bodyColor1, bodyColor1, bodyColor2}));
        g2d.fillOval(bodyX + offset, bodyY + offset, size, size);

        drawSnakeScales(g2d, bodyX + offset, bodyY + offset, size, false);

        g2d.setColor(new Color(255, 255, 255, (int)(40 * alpha)));
        g2d.fillOval(bodyX + offset + size / 3, bodyY + offset + size / 4, size / 4, size / 6);
    }

    private void drawSnakeScales(Graphics2D g2d, int x, int y, int size, boolean isHead) {
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(isHead ? new Color(255, 255, 255, 60) : new Color(255, 255, 255, 30));

        int scaleSize = size / 8;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int scaleX = x + (size/4) + (j * size/4);
                int scaleY = y + (size/4) + (i * size/4);
                if (isHead || (i + j) % 2 == 0) {
                    g2d.drawOval(scaleX - scaleSize/2, scaleY - scaleSize/2, scaleSize, scaleSize);
                }
            }
        }
    }

    private void drawUI(Graphics2D g2d) {
        int panelHeight = 50;
        g2d.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), 0, panelHeight, new Color(0, 0, 0, 100)));
        g2d.fillRect(0, 0, SCREEN_WIDTH, panelHeight);

        g2d.setColor(currentTheme.neonGreen);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        String scoreText = "SCORE: " + applesEaten;
        g2d.drawString(scoreText, 20, 32);

        g2d.setColor(currentTheme.gold);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        String titleText = "CLASSIC SNAKE GAME";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(titleText, (SCREEN_WIDTH - fm.stringWidth(titleText)) / 2, 28);

        g2d.setColor(currentTheme.neonGreen);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(0, panelHeight - 2, SCREEN_WIDTH, panelHeight - 2);
    }

    private void newApple() {
        boolean valid;
        do {
            valid = true;
            appleX = random.nextInt(COLS) * UNIT_SIZE;
            appleY = random.nextInt(ROWS) * UNIT_SIZE;
            for (int i = 0; i < bodyParts; i++) {
                if (x[i] == appleX && y[i] == appleY) valid = false;
            }
            for (Point p : obstacles) {
                if (p.x == appleX && p.y == appleY) valid = false;
            }
        } while (!valid);
    }

    private void generateObstacles(int count) {
        obstacles.clear();
        while (obstacles.size() < count) {
            int ox = random.nextInt(COLS) * UNIT_SIZE;
            int oy = random.nextInt(ROWS) * UNIT_SIZE;
            boolean overlap = false;
            for (int i = 0; i < bodyParts; i++) {
                if (x[i] == ox && y[i] == oy) overlap = true;
            }
            if (ox == appleX && oy == appleY) overlap = true;
            for (Point p : obstacles) {
                if (p.x == ox && p.y == oy) overlap = true;
            }
            if (!overlap) obstacles.add(new Point(ox, oy));
        }
    }

    private void move() {
        for (int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }
        switch (direction) {
            case 'U' -> y[0] -= UNIT_SIZE;
            case 'D' -> y[0] += UNIT_SIZE;
            case 'L' -> x[0] -= UNIT_SIZE;
            case 'R' -> x[0] += UNIT_SIZE;
        }
    }

    private void checkApple() {
        if (x[0] == appleX && y[0] == appleY) {
            bodyParts++;
            applesEaten++;
            newApple();
        }
    }

    private void checkCollisions() {
        for (int i = bodyParts; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) running = false;
        }
        for (Point p : obstacles) {
            if (x[0] == p.x && y[0] == p.y) running = false;
        }
        if (x[0] < 0 || x[0] >= SCREEN_WIDTH || y[0] < 0 || y[0] >= SCREEN_HEIGHT) running = false;

        if (!running) timer.stop();
    }

    private void gameOver(Graphics2D g2d) {
        g2d.setPaint(new RadialGradientPaint(SCREEN_WIDTH/2f, SCREEN_HEIGHT/2f, SCREEN_WIDTH/2f,
                new float[]{0f, 1f}, new Color[]{new Color(0, 0, 0, 200), new Color(0, 0, 0, 100)}));
        g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int panelWidth = 500, panelHeight = 300;
        int panelX = (SCREEN_WIDTH - panelWidth) / 2, panelY = (SCREEN_HEIGHT - panelHeight) / 2;

        g2d.setPaint(new RadialGradientPaint(panelX + panelWidth/2f, panelY + panelHeight/2f, panelWidth/2f,
                new float[]{0f, 1f}, new Color[]{new Color(currentTheme.neonGreen.getRed(), currentTheme.neonGreen.getGreen(), currentTheme.neonGreen.getBlue(), 50), new Color(currentTheme.neonGreen.getRed(), currentTheme.neonGreen.getGreen(), currentTheme.neonGreen.getBlue(), 0)}));
        g2d.fillRoundRect(panelX - 20, panelY - 20, panelWidth + 40, panelHeight + 40, 40, 40);

        g2d.setPaint(new GradientPaint(panelX, panelY, new Color(255, 255, 255, 30), panelX, panelY + panelHeight, new Color(255, 255, 255, 10)));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 25, 25);

        g2d.setColor(new Color(currentTheme.neonGreen.getRed(), currentTheme.neonGreen.getGreen(), currentTheme.neonGreen.getBlue(), 150));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 25, 25);

        g2d.setColor(new Color(255, 50, 50, 100));
        g2d.setFont(new Font("Arial", Font.BOLD, 52));
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        for (int i = 0; i < 3; i++) {
            g2d.drawString(gameOverText, panelX + (panelWidth - fm.stringWidth(gameOverText)) / 2 + i, panelY + 80 + i);
        }
        g2d.setColor(new Color(255, 100, 100));
        g2d.drawString(gameOverText, panelX + (panelWidth - fm.stringWidth(gameOverText)) / 2, panelY + 80);

        g2d.setColor(currentTheme.gold);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        String scoreText = "FINAL SCORE: " + applesEaten;
        fm = g2d.getFontMetrics();
        g2d.drawString(scoreText, panelX + (panelWidth - fm.stringWidth(scoreText)) / 2, panelY + 140);

        String rating = applesEaten < 5 ? "NOVICE" : applesEaten < 15 ? "SKILLED" : applesEaten < 25 ? "EXPERT" : "MASTER";
        g2d.setColor(currentTheme.neonGreen);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        fm = g2d.getFontMetrics();
        g2d.drawString("RANK: " + rating, panelX + (panelWidth - fm.stringWidth("RANK: " + rating)) / 2, panelY + 170);

        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        String restartText = "Press ENTER to Play Again, 'T' to toggle theme";
        fm = g2d.getFontMetrics();
        g2d.drawString(restartText, panelX + (panelWidth - fm.stringWidth(restartText)) / 2, panelY + 220);

        g2d.setColor(new Color(currentTheme.neonGreen.getRed(), currentTheme.neonGreen.getGreen(), currentTheme.neonGreen.getBlue(), 80));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawLine(panelX + 50, panelY + 190, panelX + panelWidth - 50, panelY + 190);
    }

    private void restartGame() {
        bodyParts = 6;
        applesEaten = 0;
        direction = 'R';
        Arrays.fill(x, 0);
        Arrays.fill(y, 0);
        obstacles.clear();
        startGame();
    }

    private void toggleTheme() {
        currentTheme = (currentTheme == Theme.DAY) ? Theme.NIGHT : Theme.DAY;
        setBackground(currentTheme.backgroundPrimary);

        if (currentTheme == Theme.DAY) {
            appleImage = new ImageIcon(getClass().getResource("apple_pink.png")).getImage();
            snakeHeadImage = new ImageIcon(getClass().getResource("head.png")).getImage();
            obstacleImage = new ImageIcon(getClass().getResource("cactus_day.png")).getImage();
        } else {
            appleImage = new ImageIcon(getClass().getResource("apple.png")).getImage();
            snakeHeadImage = new ImageIcon(getClass().getResource("snake_head.png")).getImage();
            obstacleImage = new ImageIcon(getClass().getResource("cactus.png")).getImage();
        }

        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        repaint();
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> { if (direction != 'R') direction = 'L'; }
                case KeyEvent.VK_RIGHT -> { if (direction != 'L') direction = 'R'; }
                case KeyEvent.VK_UP -> { if (direction != 'D') direction = 'U'; }
                case KeyEvent.VK_DOWN -> { if (direction != 'U') direction = 'D'; }
                case KeyEvent.VK_ENTER -> { if (!running) restartGame(); }
                case KeyEvent.VK_P -> {
                    if (running && !paused) {
                        paused = true;
                        timer.stop();
                    } else if (paused) {
                        paused = false;
                        timer.start();
                    }
                }
                case KeyEvent.VK_T -> toggleTheme();
            }
        }
    }
}