import java.util.*;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;


class RobotArm {

    
    private Canny canny;
    private PrintWriter out;
    private MotorControl controller;

    // where the pen currently is
    private double currentX;
    private double currentY;
    
    // motor signals for raising and lowering the pen
    private final double PEN_UP = 1300;
    private final double PEN_DOWN = 1500;

    // 2d array of pixels to draw. Pixel is on or off
    private boolean[][] image = new boolean[100][100];

    // holds (x, y) in order of drawing
    private ArrayList<double[]> drawOrder = new ArrayList<>();

    // limits
    private static final double LEFT_LIMIT = -100;
    private static final double RIGHT_LIMIT = 110;
    private static final double TOP_LIMIT = 360;
    private static final double BOTTOM_LIMIT = 360;

    double width = RIGHT_LIMIT - LEFT_LIMIT; // 260
    double height = TOP_LIMIT - BOTTOM_LIMIT; // 170


    
    public RobotArm() {
        try {
            out = new PrintWriter(new File("commands.txt"));
        } catch (FileNotFoundException e) {System.out.println("File not found "+ e);}
        controller = new MotorControl();
        canny = new Canny();
    }
    
    
    //// DRAWING METHODS ////

    /**
     * Draws a straight line from (x1, y1) to (x2, y2)
     */
    private void drawLine(double x1, double y1, double x2, double y2) {
        if (currentX != x1 || currentY != y1) {
            moveTo(x1, y1, "UP");
        }
        System.out.printf("Line from (%.1f, %.1f) to (%.1f, %.1f)\n", currentX, currentY, x2, y2);
        moveTo(x1, y1, "DOWN");
        moveTo(x2, y2, "DOWN");
        moveTo(x2, y2, "UP");
        // draw line between (x1, y1) and (x2, y2)

        currentX = x2;
        currentY = y2;
        out.flush(); //REMOVE THIS
    }

    /**
     * Draws a circle centred at centre, with radius radius
     */
    private void drawCircle(double centre, double radius) {}

    /**
     * Lifts the pen and moves in a straight line from (x1, y1) to (x2, y2)
     */
    private void moveTo(double x2, double y2, String penState) {
        System.out.printf("Move from (%.1f, %.1f) to (%.1f, %.1f)\n", currentX, currentY, x2, y2);
        // pen up
        
        // move from (currentX, currentY) to (x2, y2)
        double m1 = controller.getMotor1Signal(x2, y2);
        double m2 = controller.getMotor2Signal(x2, y2);
        if (penState.equals("UP")) {
            out.printf("%.0f, %.0f, %.0f\n", m1, m2, PEN_UP);
        } else if (penState.equals("DOWN")) {
            out.printf("%.0f, %.0f, %.0f\n", m1, m2, PEN_DOWN);
        } 
        // pen down
        
        currentX = x2;
        currentY = y2;
    }
    /**
     * Adds pixel co-ords from 2d bool array to a list in order of drawing
     */
    private void createOrderList() {
        for (int row = 0; row < image.length; row++) {
            for (int col = 0; col < image[0].length; col++) {
                if (image[row][col]) {
                    followEdge(row, col);
                }
            }
        }
    }

    private void followEdge(int row, int col) {
        double[] xy = new double[2];
        xy[0] = col + LEFT_LIMIT;
        xy[1] = row + TOP_LIMIT;
        drawOrder.add(xy);
        image[row][col] = false;

        if (col > 0 && row > 0 && image[row-1][col-1]) {
            followEdge(row-1, col-1);
        }

        else if (col > 0 && image[row][col-1]) {
            followEdge(row, col-1);
        }

        else if (col > 0 && row < image.length-1 && image[row+1][col-1]) {
            followEdge(row+1, col-1);
        }

        else if (row < image.length-1 && image[row+1][col]) {
            followEdge(row+1, col);
        }

        else if (row < image.length-1 && col < image[0].length-1 && image[row+1][col+1]) {
            followEdge(row+1, col+1);
        }

        else if (col < image[0].length-1 && image[row][col+1]) {
            followEdge(row, col+1);
        }

        else if (row > 0 && col < image[0].length-1 && image[row-1][col+1]) {
            followEdge(row-1, col+1);
        }

        else if (row > 0 && image[row-1][col]) {
            followEdge(row-1, col);
        }
    }

    /**
     * Checks if a pixel is next to another pixel
     */
    private boolean inRange(double x1, double y1, double x2, double y2) {
        if (x2 <= x1+1 && x2 >= x1-1 && y2 <= y1+1 && y2 >= y1-1) {
            return true;
        }
        return false;
    }

    /**
     * Takes the draw order list and draws lines between adjacent pixels
     */
    private void drawImage() {
        double x2 = drawOrder.get(0)[0]; // first point to draw
        double y2 = drawOrder.get(0)[1];
        moveTo(x2, y2, "UP"); // moves to first point in picture
        for (int i = 1; i < drawOrder.size(); i++) {
            x2 = drawOrder.get(i)[0];
            y2 = drawOrder.get(i)[1];
            if (inRange(currentX, currentY, x2, y2)) { // draws line if adjacent
                drawLine(currentX, currentY, x2, y2);
            }
            else { // otherwise moves pen to next location
                moveTo(x2, y2, "UP");
            }
        }
        out.flush();
    }

    private void loadTestImage() {
        boolean[] row0 = {false, false, false, true, false, false, false, false, false, true};
        boolean[] row1 = {false, false, true, false, true, false, false, false, true, false};
        boolean[] row2 = {false, true, false, false, true, true, true, true, false, false};
        boolean[] row3 = {true, false, false, false, true, false, false, false, false, false};
        boolean[] row4 = {false, true, false, false, false, true, false, false, false, false};
        boolean[] row5 = {false, true, false, false, false, false, true, true, true, true};
        boolean[] row6 = {false, true, false, false, false, false, false, false, false, false};
        boolean[] row7 = {false, true, false, false, true, false, false, false, false, false};
        boolean[] row8 = {false, true, true, true, false, false, false, false, false, false};
        boolean[] row9 = {false, false, false, false, false, false, false, false, false, false};

        image[0] = row0;
        image[1] = row1;
        image[2] = row2;
        image[3] = row3;
        image[4] = row4;
        image[5] = row5;
        image[6] = row6;
        image[7] = row7;
        image[8] = row8;
        image[9] = row9;
    }

    private void printOrder() {
        for (double[] xy : drawOrder) {
            System.out.println(xy[0] + " " + xy[1]);
        }
    }

    private void loadImage() {
        boolean[][] img = canny.getImage();
        for (int r = 0; r < 100; r++) {
            for (int c = 0; c < 100; c++) {
                image[r][c] = img[r][c];
            }
        }
    }




    public static void main(String[] args) {
        System.out.println("RobotArm started!");
        RobotArm drawer = new RobotArm();
//        drawer.loadImage();
//        drawer.createOrderList();
//        drawer.printOrder();
//        drawer.drawImage();
        drawer.drawLine(-50, 330, 50, 330);
//        for (int i = -50; i <= 50; i++) {
//            drawer.drawLine(i-1, 400, i, 400);
//        }
//        for(int i = 290; i<470; i++){
//            drawer.drawLine(50, i-1, 50, i);
//        }

//        drawer.drawLine(RIGHT_LIMIT, BOTTOM_LIMIT, RIGHT_LIMIT, TOP_LIMIT);
//        drawer.drawLine(RIGHT_LIMIT, TOP_LIMIT, LEFT_LIMIT, TOP_LIMIT);
//        drawer.drawLine(LEFT_LIMIT, TOP_LIMIT, LEFT_LIMIT, BOTTOM_LIMIT);
//        drawer.drawLine(10, 010, 200, 200);
//        MotorControl con1 = new MotorControl();
//        //// 1945, 1155
//        System.out.println("MOTOR SIGNALS");
//        System.out.println(con1.getMotor1Signal(0, 200));
//        System.out.println(con1.getMotor2Signal(0, 200));
//
//        drawer.loadTestImage();
//        drawer.createOrderList();
//        drawer.printOrder();
//        drawer.drawImage();
    }
}
