import javax.sound.sampled.*;
import java.util.*;
import java.awt.Font;
import java.awt.Color;

public class Application {
   public static int WIDTH = 400;
   public static int HEIGHT = WIDTH+WIDTH/2;
   public static int SAMPLES_PER_SECOND = 50;
   public static int SAMPLES_PER_AVERAGE = 10;
   public static int AVERAGES_PER_PENALTY = 3;
   public static int MAX_HISTOGRAM_ELEMENTS = 100;
   public static int SENSITIVITY = 50; 
   public static int MINIMUM_DECIBLE = 30;
   public static int COUNTER_START = 30;
   private static float range = 90 - MINIMUM_DECIBLE;
   
   
   // R G B
   public static Color WARNING_LINE_COLOR = new Color(255, 0, 0);
   public static Color PEAK_MARKER_COLOR = new Color(100, 100, 100);
   public static Color HISTOGRAM_COLOR = new Color(70, 250, 70);
   
   public static Font defaultFont = new Font("Courier New", Font.PLAIN, 14);
   public static Font counterFont = new Font("Courier New", Font.PLAIN, 40);

   public static boolean DEBUG_PRINTS = false;
   private static boolean isExtended = false;
   private static LinkedList<Double> histogram;
   public static ArrayList<TargetDataLine> micLines;
   public static ArrayList<String> micNames;
   public static ArrayList<Button> buttons;
   
   
   
   public static void main(String[] args) {
      buttons = new ArrayList<Button>();
   // 55 90 105 60
      buttons.add(buttons.size(), new Button(54, 90, 50, 30)); // Top 1
      buttons.add(buttons.size(), new Button(114, 90, 50, 30)); // Top 2
      buttons.add(buttons.size(), new Button(174, 90, 50, 30)); // Top 3
      buttons.add(buttons.size(), new Button(234, 90, 50, 30)); // Top 4
      buttons.add(buttons.size(), new Button(294, 90, 50, 30)); // Top 5
      buttons.add(buttons.size(), new Button(54, 54, 50, 30)); // Bot Left
      buttons.add(buttons.size(), new Button(114, 54, 50, 30)); // Bot Left
      buttons.add(buttons.size(), new Button(174, 54, 50, 30)); // Bot Left
      buttons.add(buttons.size(), new Button(234, 54, 50, 30)); // Bot Left
      buttons.add(buttons.size(), new Button(294, 54, 50, 30)); // Bot Left
      
      buttons.get(0).text = "-";
      buttons.get(1).text = "+";
      buttons.get(2).text = "=";
      buttons.get(2).angle = 270;
      buttons.get(3).text = "r";
      buttons.get(4).text = "h";
      
      init();
      histogram = new LinkedList<Double>();
      
      CustomStdDraw.setFont(defaultFont);
      
      
      int counter = COUNTER_START;
      int averagesAboveSensitivity = 0;
      boolean paused = false;
   
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
            
            histogram.add(targetdB);
            if (histogram.size() > MAX_HISTOGRAM_ELEMENTS) {
               histogram.removeFirst();
            }
            
            if (targetdB > SENSITIVITY && !paused) {
               averagesAboveSensitivity++;
             
               if (averagesAboveSensitivity == AVERAGES_PER_PENALTY) {
                  counter--;
                  averagesAboveSensitivity = 0;
               }
            }
            else {
               averagesAboveSensitivity = 0;
            }
         }
      
         double SMOOTHING = 0.2;
         displayeddB += (targetdB - displayeddB) * SMOOTHING;
         if (displayeddB > relativeMax) {
            relativeMax = displayeddB;
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
         if (DEBUG_PRINTS) System.out.println(cd);
      
         CustomStdDraw.picture(WIDTH / 2, HEIGHT / 2, "BasicUI.png", WIDTH, HEIGHT);
         
         int rectHeight = getHeight(displayeddB-MINIMUM_DECIBLE);
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
         
         
         
         
      
         
         CustomStdDraw.setPenColor(CustomStdDraw.BLACK);
         Color warning2 = new Color(
            Math.max(1, WARNING_LINE_COLOR.getRed() - 75),
            Math.max(1, WARNING_LINE_COLOR.getGreen() - 75),
            Math.max(1, WARNING_LINE_COLOR.getBlue() - 75)
            );
         if (!paused) CustomStdDraw.setPenColor(warning2);
         CustomStdDraw.filledRectangle(WIDTH/2 + 2, -2 + HEIGHT * 0.2 + HEIGHT * 0.7 * ((SENSITIVITY-MINIMUM_DECIBLE) / range), WIDTH*0.3, 2);
         
         if (!paused) CustomStdDraw.setPenColor(WARNING_LINE_COLOR);
         
         CustomStdDraw.filledRectangle(WIDTH/2, HEIGHT * 0.2 + HEIGHT * 0.7 * ((SENSITIVITY-MINIMUM_DECIBLE) / range), WIDTH*0.3, 2);
         //CustomStdDraw.setPenColor(WARNING_LINE_COLOR);
         //CustomStdDraw.line(WIDTH * 0.2, HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0), WIDTH * 0.8, HEIGHT * 0.2 + HEIGHT * 0.7 * (SENSITIVITY / 90.0));
         
         
         
         Color peak2 = new Color(
            Math.max(1, PEAK_MARKER_COLOR.getRed() - 50),
            Math.max(1, PEAK_MARKER_COLOR.getGreen() - 50),
            Math.max(1, PEAK_MARKER_COLOR.getBlue() - 50)
            );
         CustomStdDraw.setPenColor(peak2);
         CustomStdDraw.filledRectangle(WIDTH/2 + 2, -2 + HEIGHT * 0.2 + Math.max(0, HEIGHT * 0.7 * ((absoluteMax-MINIMUM_DECIBLE)/ range)), WIDTH/4, 2);
         
         CustomStdDraw.setPenColor(PEAK_MARKER_COLOR);
         CustomStdDraw.filledRectangle(WIDTH/2, HEIGHT * 0.2 + Math.max(0, HEIGHT * 0.7 * ((absoluteMax-MINIMUM_DECIBLE)/ range)), WIDTH/4, 2);
         //CustomStdDraw.line(WIDTH * 0.25, HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0), WIDTH * 0.75, HEIGHT * 0.2 + HEIGHT * 0.7 * (absoluteMax / 90.0));
         
         
         if (isExtended) {
            CustomStdDraw.setPenColor(HISTOGRAM_COLOR);
            for (int i = 0; i < histogram.size(); i++) {
               Double d = histogram.get(i);
            
               double width = (Math.max(2, (d - MINIMUM_DECIBLE))/ range) * 100;
               
               CustomStdDraw.filledRectangle(WIDTH + 100 - width / 2, HEIGHT / MAX_HISTOGRAM_ELEMENTS * i, width / 2, HEIGHT / (MAX_HISTOGRAM_ELEMENTS*2));
            }
            
            CustomStdDraw.setPenColor(WARNING_LINE_COLOR);
            CustomStdDraw.filledRectangle(WIDTH + 100 - Math.max(2, (SENSITIVITY - MINIMUM_DECIBLE))/range * 100, HEIGHT / 2, 1, HEIGHT/2);
         
         }
                     
         CustomStdDraw.setPenColor(CustomStdDraw.BLACK);
         CustomStdDraw.setFont(defaultFont);
         CustomStdDraw.text( WIDTH * 0.90, HEIGHT * 0.2 + Math.max(0, HEIGHT * 0.7 * ((absoluteMax-MINIMUM_DECIBLE)/ range)), (int)absoluteMax+"dB");
         CustomStdDraw.text( WIDTH * 0.80, HEIGHT * 0.2 + rectHeight, (int)displayeddB+"dB");
         CustomStdDraw.text( WIDTH * 0.5, HEIGHT * 0.25 + + HEIGHT * 0.7 * ((SENSITIVITY-MINIMUM_DECIBLE) / range), (int)SENSITIVITY+"dB");
         
         CustomStdDraw.setFont(counterFont);
         CustomStdDraw.text(WIDTH * 0.50, HEIGHT * 0.95, ""+counter);
         //1575
         for (int i = 0; i < buttons.size(); i++) {
            Button b = buttons.get(i);
            b.update();
            b.draw();
            
            if (b.isPressed) {
               //System.out.println("Is pressed!!" + i);
               if (i == 0) {
                  counter--;
               }
               else if (i == 1) {
                  counter++;
               }
               else if (i == 2) {
                  paused = !paused;
                  if (paused == true) 
                     buttons.get(2).text = "Δ";
                  else
                     buttons.get(2).text = "=";
               }
               else if (i == 3) {
                  counter = 30;
               }
               else if (i == 4) {
                  if (!isExtended) {
                     isExtended = true;
                     CustomStdDraw.setCanvasSize(WIDTH + 100,HEIGHT);
                     CustomStdDraw.setXscale(0, WIDTH+100);
                     CustomStdDraw.setYscale(0, HEIGHT);
                  }
                  else {
                     isExtended = false;
                     CustomStdDraw.setCanvasSize(WIDTH,HEIGHT);
                     CustomStdDraw.setXscale(0, WIDTH);
                     CustomStdDraw.setYscale(0, HEIGHT);
                  }
               
               }
            }
         }
         
         if (DEBUG_PRINTS) {
            System.out.println("X:"+CustomStdDraw.mouseX() + "    Y:"+CustomStdDraw.mouseY());
         }
         
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
      double num = (dB / range) * maxHeight;
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
   
   
   static class Button {
      public double x;
      public double y;
      public double width;
      public double height;
      public String text;
   
      private boolean wasPressed;
      public boolean isPressed;
      public int angle;
   
      public Button(double x, double y, double width, double height) {
         this.x = x;
         this.y = y;
         this.width = width;
         this.height = height;
         text = "?";
         angle = 0;
      
         wasPressed = false;
         isPressed = false;
      }
   
      public void update() {
         double currentMouseX = CustomStdDraw.mouseX();
         double currentMouseY = CustomStdDraw.mouseY();
      
         if (CustomStdDraw.isMousePressed() && !wasPressed) {
            if (currentMouseX > x && currentMouseX < x + width
            && currentMouseY > y - height && currentMouseY < y) { // In Bounds
            //CustomStdDraw.rectangle(x + width / 2, y + height / 2 - height, width / 2, height / 2);
            //System.out.println("Pressed!");
               isPressed = true;
            }
            wasPressed = true;
         
         
         }
         else if (!CustomStdDraw.isMousePressed()) {
            wasPressed = false;
            isPressed = false;
         }
         else {
            isPressed = false;
         }
      }
   
      public void draw() {
      //CustomStdDraw.setPenColor(CustomStdDraw.RED);
      //CustomStdDraw.rectangle(x + width / 2, y - width / 3, width/2, height/2);
         CustomStdDraw.setPenColor(CustomStdDraw.BLACK);
         CustomStdDraw.setFont(Application.counterFont);
         CustomStdDraw.text(x + width / 2, y - width / 3, text, angle);
      }
   
   }
}
