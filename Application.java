import javax.sound.sampled.*;
import java.util.*;
import java.awt.Font;
import java.awt.Color;

public class Application {
    public static int WIDTH = 400;
    public static int HEIGHT = WIDTH+WIDTH/2;
    public static int SAMPLES_PER_SECOND = 50;
    public static int SAMPLES_PER_AVERAGE = 10;
    public static int SENSITIVITY = 70; 

    // R G B
    public static Color WARNING_LINE_COLOR = new Color(255, 0, 0);
    public static Color PEAK_MARKER_COLOR = new Color(100, 100, 100);

    public static boolean DEBUG_PRINTS = true;
    private static boolean isExtended = false;
    public static ArrayList<TargetDataLine> micLines;
    public static ArrayList<String> micNames;
    public static void main(String[] args) {

        init();
        Font defaultFont = new Font("Courier New", Font.PLAIN, 14);
        Font counterFont = new Font("Courier New", Font.PLAIN, 40);
        CustomStdDraw.setFont(defaultFont);

        // Get mic stuff
        micLines = new ArrayList<>();
        micNames = new ArrayList<>();
        getMics(micLines, micNames);

        double averager = 0;
        double printeddB = 0;
        long timer = 0;

        // Draw stuff
        double targetdB = 0;
        double displayeddB = 0;

        double relativeMax = 0;
        double absoluteMax = 0;
        int secondsBetweenChange = 3 * SAMPLES_PER_SECOND / 2;
        int cd = secondsBetweenChange; 

        while (true) {
            CustomStdDraw.clear();

            double decibel = getdB();
            decibel = Math.max(0, (int)(decibel * 100) / 100.0);
            averager += decibel;

            if (timer % SAMPLES_PER_AVERAGE == 0 && timer != 0) {
                targetdB = averager / SAMPLES_PER_AVERAGE;
                averager = 0;
            }

            double SMOOTHING = 0.2;
            displayeddB += (targetdB - displayeddB) * SMOOTHING;
            if (displayeddB > relativeMax) {
                relativeMax = displayeddB;
            }

            if (CustomStdDraw.hasNextKeyTyped()) {
                char typed = CustomStdDraw.nextKeyTyped();
                if (typed == 'a') {
                    CustomStdDraw.enableDoubleBuffering();
                    CustomStdDraw.setCanvasSize((int)(WIDTH*1.5),HEIGHT);
                    CustomStdDraw.setXscale(0, (int)(WIDTH*1.5));
                    CustomStdDraw.setYscale(0, HEIGHT);
                    isExtended = true;
                }
                else if (typed == 's') {
                    CustomStdDraw.enableDoubleBuffering();
                    CustomStdDraw.setCanvasSize(WIDTH,HEIGHT);
                    CustomStdDraw.setXscale(0, WIDTH);
                    CustomStdDraw.setYscale(0, HEIGHT);
                    isExtended = false;
                }
            }
            
            

            cd--;
            if (cd <= 0) {
                absoluteMax *= 0.99;
            }
            if (absoluteMax <= relativeMax) {
                absoluteMax = relativeMax;
                relativeMax = 0;
                cd = secondsBetweenChange;
            }
            System.out.println(cd);

            CustomStdDraw.picture(WIDTH / 2, HEIGHT / 2, "BasicUI.png", WIDTH, HEIGHT);
            
            int rectHeight = getHeight(displayeddB);
            Color bar = getColor(displayeddB);
            Color bar2 = new Color(
                Math.max(1, bar.getRed() - 50),
                Math.max(1, bar.getGreen() - 50),
                Math.max(1, bar.getBlue() - 50)
            );
            
            CustomStdDraw.setPenColor(bar2);
            CustomStdDraw.filledRectangle(
                WIDTH * 0.5 + 3,
                HEIGHT * 0.2 + rectHeight - 3,
                WIDTH * 0.25,
                rectHeight
            );
            
            CustomStdDraw.setPenColor(bar);
            CustomStdDraw.filledRectangle(
                WIDTH * 0.5,
                HEIGHT * 0.2 + rectHeight,
                WIDTH * 0.25,
                rectHeight
            );
            
            
            
            

            
            
            Color warning2 = new Color(
                Math.max(1, WARNING_LINE_COLOR.getRed() - 75),
                Math.max(1, WARNING_LINE_COLOR.getGreen() - 75),
                Math.max(1, WARNING_LINE_COLOR.getBlue() - 75)
            );
            CustomStdDraw.setPenColor(warning2);
            CustomStdDraw.filledRectangle(WIDTH/2 + 2, -2 + HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0), WIDTH*0.3, 2);
            
            CustomStdDraw.setPenColor(WARNING_LINE_COLOR);
            CustomStdDraw.filledRectangle(WIDTH/2, HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0), WIDTH*0.3, 2);
            //CustomStdDraw.setPenColor(WARNING_LINE_COLOR);
            //CustomStdDraw.line(WIDTH * 0.2, HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0), WIDTH * 0.8, HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0));
            
            
            
            Color peak2 = new Color(
                Math.max(1, PEAK_MARKER_COLOR.getRed() - 50),
                Math.max(1, PEAK_MARKER_COLOR.getGreen() - 50),
                Math.max(1, PEAK_MARKER_COLOR.getBlue() - 50)
            );
            CustomStdDraw.setPenColor(peak2);
            CustomStdDraw.filledRectangle(WIDTH/2 + 2, -2 + HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0), WIDTH/4, 2);
            
            CustomStdDraw.setPenColor(PEAK_MARKER_COLOR);
            CustomStdDraw.filledRectangle(WIDTH/2, HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0), WIDTH/4, 2);
            //CustomStdDraw.line(WIDTH * 0.25, HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0), WIDTH * 0.75, HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0));
            
            //1575
            CustomStdDraw.show();
            

            timer++;
            wait((int) (1.0 / SAMPLES_PER_SECOND));
        }

    }

    private static void init() {
        CustomStdDraw.enableDoubleBuffering();
        CustomStdDraw.setCanvasSize(WIDTH,HEIGHT);
        CustomStdDraw.setXscale(0, WIDTH);
        CustomStdDraw.setYscale(0, HEIGHT);
        CustomStdDraw.setPenRadius(.002);
    }

    private static void getMics(List<TargetDataLine> micLines, List<String> micNames) {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLines = mixer.getTargetLineInfo();

            for (Line.Info lineInfo : targetLines) {
                if (TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                    try {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                        AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, true);

                        if (DEBUG_PRINTS) System.out.println("Found microphone: " + mixerInfo.getName());
                        line.open(format);
                        line.start();

                        micLines.add(line);
                        micNames.add(mixerInfo.getName());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    public static int getHeight(double dB) {
        double maxHeight = HEIGHT * 0.7;
        double num = (dB / 90) * maxHeight;
        return (int) Math.min(maxHeight, Math.max(HEIGHT*0.005, num)) / 2;
    }

    private static double getdB() {
        byte[] buffer = new byte[256];
        double decibel = 0;
        for (int i = 0; i < micLines.size(); i++) {
            TargetDataLine line = micLines.get(i);
            String name = micNames.get(i);

            int bytesRead = line.read(buffer, 0, buffer.length/2); // buffer, buffer.length/2, buffer.length/2
            //System.out.println(Arrays.toString(buffer));
            if (bytesRead > 0) {
                long sum = 0;
                for (int j = 0; j < bytesRead; j += 2) {
                    int sample = (buffer[j] << 8) | (buffer[j + 1] & 0xff);
                    sum += sample * sample;
                }

                double rms = Math.sqrt(sum / (bytesRead / 2.0));
                double dB = 20 * Math.log10(rms);

                if (Double.isInfinite(dB)) {
                    dB = -90.0; 
                }
                if (dB > decibel) decibel = dB;
            }
        }
        return decibel;
    }

    private static Color getColor(double dB) {
        int redThres = 10;
        double x = (dB - redThres) / 90;
        int red = (int) (-Math.cos(x * 2.5 + 2.2 * Math.PI) * 255); //Math.min(150, Math.max(1, (150 * distanceFromThres2)));
        int green =  (int) (Math.cos((x) * 2)*255); //Math.min(255, Math.max(1, (255 - 255 * Math.max(distanceFromThres2, 0.2))));
        int blue = 1;

        red = Math.min(255, Math.max(1, red));
        green = Math.min(255, Math.max(1, green));
        blue = Math.min(255, Math.max(1, blue));
        return new Color(red, green, blue);
    }

    private static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
