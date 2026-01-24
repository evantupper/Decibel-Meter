import javax.sound.sampled.*;
import java.util.*;
import java.awt.Font;
import java.awt.Color;

public class Main {
    public static Queue messageQueue; 

    public static double decible = 0;
    public static int counter = 15;
    public static int samplesPerAverage = 10;
    public static int redThres = 10;
    public static int maxHeight = 300;
    public static int rangeLimiter = 40; // Default 30
    public static final int MAX_DECIMAL_OF_CARE = 80;
    public static final int PENALTY_DECIBLE = 50;
    
    private Main() {}

    public static void main(String[] args) {
        messageQueue = new LinkedList<>();

        Mixer.Info[] mixers = null;
        List<TargetDataLine> micLines  = null;
        List<String> micNames = null;

        StdDraw.enableDoubleBuffering();
        long timer = 0;
        Queue<Double> decQueue = new LinkedList<>();

        StdDraw.setCanvasSize(1200,400);
        StdDraw.setXscale(0, 1200);
        StdDraw.setYscale(0, 400);
        StdDraw.setPenRadius(.02);
        StdDraw.setPenColor(StdDraw.GREEN);
        String strungDecible ="null";

        Font defaultFont = new Font("Courier New", Font.PLAIN, 14);
        Font counterFont = new Font("Courier New", Font.PLAIN, 40);
        StdDraw.setFont(defaultFont);
        
        double longAverage = 0;
        double bufferedLA = 0;
        int countLA = 1;

        for (int i = 0; i < 1; i++) {
            StdDraw.clear();
            clear();
            StdDraw.setPenColor(StdDraw.BLACK);

            print("Setting up objects... ");
            mixers = AudioSystem.getMixerInfo();
            micLines = new ArrayList<>();
            micNames = new ArrayList<>();
            
            print("Setting up microphones... ");
            for (Mixer.Info mixerInfo : mixers) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] targetLines = mixer.getTargetLineInfo();

                for (Line.Info lineInfo : targetLines) {
                    if (TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                        try {
                            TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                            AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, true);

                            System.out.println("Found microphone: " + mixerInfo.getName());
                            line.open(format);
                            line.start();

                            micLines.add(line);
                            micNames.add(mixerInfo.getName());
                            //System.out.println("Enabled microphone: " + mixerInfo.getName());
                            print("Enabled microphone: " + mixerInfo.getName());
                        } catch (Exception e) {
                            print("Could not access: " + mixerInfo.getName());
                        }
                    }
                }

            }
            print("Working microphones: " + micLines.size());
            wait(100);
        }

        byte[] buffer = new byte[256];
        double averager = 0;

        wait(10);

        while (true) {
            StdDraw.clear();
            decible = 0;
            clear();
            print("Updating "+ micLines.size() + " microphones... " );

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
                    if (dB > decible) decible = dB;

                    print("["+name+"] Level: "+dB);
                }
            }

            if (timer % 10 == 0)
                strungDecible = "" + Math.max(0,  (int) ((decible)*100)/100.0);

            StdDraw.setPenColor(getColor(decible));
            int rectHeight = getHeight(decible);
            StdDraw.filledRectangle(40, 50 + rectHeight, 30, rectHeight);

            StdDraw.setPenColor(StdDraw.BLACK);
            StdDraw.textLeft (10, 30, strungDecible);
            StdDraw.textLeft (60, 30, "dB");
            print("Status: " + ((decible <= 0)? "too quiet or no available mic @ "+decible + "dB" : "current volume at " + decible + "dB"));

            if (timer % samplesPerAverage == 0) {
                averager += decible;
                decQueue.offer(averager/samplesPerAverage);
                averager = 0;
                if (decQueue.size() > 110) decQueue.poll();
            }
            else {
                averager += decible;
            }
            
            if (timer % samplesPerAverage * 20 == 0) {
                longAverage = bufferedLA / 20;
            }
            else {
                bufferedLA += decible;
                countLA++;
            }

            // Drawing the timeline
            int i = 0;
            double high = 0;
            double low = 90;
            for (Double doub : decQueue) {
                if (doub > high) high = doub;
                if (doub < low) low = doub;
                
                StdDraw.setPenColor(getColor(doub));

                int rectHeight2 = getHeight(doub);

                StdDraw.filledRectangle(100+i*6, 50 + rectHeight2, 3, rectHeight2);
                i++;
            }

            StdDraw.setPenColor(StdDraw.BLACK);
            int h = 0;
            for (Object obj : messageQueue) {
                StdDraw.textLeft(760, 30 + h*30, obj.toString());
                h++;
            }

            // Drawing the Counter.
            StdDraw.setFont(counterFont);
            StdDraw.textLeft(760, 300, "" + counter);
            StdDraw.setFont(defaultFont);

            StdDraw.textLeft(200, 30, "Low: " + (int)(low*100)/100.0);
            StdDraw.textLeft(325, 30, "Long Average: " + (int)(longAverage*100/countLA)/100.0);
            StdDraw.textLeft(550, 30, "High: " + (int)(high*100)/100.0);
            timer++;
            StdDraw.show();

            //Pause time (20x per second)
            wait(50);

        }
    }

    private static int getHeight(double dB) {
        return (int) Math.min(maxHeight, Math.max(5, ((dB-rangeLimiter)/(MAX_DECIMAL_OF_CARE-rangeLimiter)) * maxHeight)) / 2;
        //(int) Math.max(10, Math.min(maxHeight, dB * scale)); 9/30/2025 14:26
    }
    
    private static Color getColor(double dB) {
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

    private static <T> void print(T obj) {
        messageQueue.offer(obj);
        if (messageQueue.size() > 10) {
            messageQueue.poll();
        }
    }

    private static void clear() {
        messageQueue.clear();
    }
}
