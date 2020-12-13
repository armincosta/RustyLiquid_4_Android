package it.seesolutions.f2f.graphics;


import android.graphics.Color;

import it.seesolutions.f2f.DrawView;


/**
 * Author: Armin Costa
 * e-mail: armincost@gmail.com
 *
 * Here is the main algorithm involved in wave generation (not the texture generation)
 * a 4x4 height filtermap is used
 *
 * This code is distributed under the GNU GENERAL PUBLIC LICENSE Version 3. See LICENSE file
 *-------------------------------------------------------------------------------------------

 for(y = 1; y < h-1; y++){
 tmp2 += w;
 for(x = 1; x < w-1; x++){
 tmp = tmp2 + x;
 int newh= ((  w_old[tmp + w]
 + w_old[tmp - w]
 + w_old[tmp + 1]
 + w_old[tmp - 1]
 + w_old[tmp - w - 1]
 + w_old[tmp - w + 1]
 + w_old[tmp + w - 1]
 + w_old[tmp + w + 1]
 ) >> 2 )
 - w_new[tmp];

 w_new[tmp] = newh - (newh >> 3);
 }

 }
 tmp_ = w_old;
 w_old = w_new;

 w_new = tmp_;
 */


public class RustyLiquid implements Runnable {


    private int speed = 0;

    private String pic;
    private String by;

    public int bw, bh, bs;
    public int w = 512;
    public int h = 512;
    public static int buffer[] = null, black_buffer[] = null,
            curr_pix[] = null;


    private boolean buffers_constructed = false;

    private int[] w_old; // old walues
    private int[] w_new; // new generated values

    private int[] tmp_; // just used to swap buffers

    public int[] pix_; // buffer for picture update


    // VARS
    public int d_f = 4; //30; // deep factor

    public int w_depth1 = 4700; // drop

    public int w_depth2 = 1200; //2959; // blob
    public int max_radius = 60;//23; // 22 82

    public int lum = 3; // 2 luminance factor -- TEXTURING

    boolean running = true;

    // setup initial color
    private final int paintColor = Color.BLACK;
    private final int paintColorDiscovery = 0x3F51B5;



    private Thread t; // thread

    DrawView target_view = null;

    boolean run_status = false;

    int EFFECT_TIMEOUT = 16000; // timeout for the effect to run-off
    long time_elapsed = 0;
    long startTime = 0;


    public RustyLiquid(DrawView target_view) {
        this.target_view = target_view;
        setupPaint();
    }


    private void setupPaint() {
        // not needed here. This is done by the bitmap drawing class DrawView
    }

    /**
     * initialization
     */
    public boolean init(int[] pixels, int width, int height) {
        by = "coded by Armin Costa";

        if ((by != null)) { // by != null) && (by.equals("www.artcosta.com")
            // pic = this.getParameter("picture");
            pic = "rusty.jpg";
            if (pic == null) {
                pic = "rusty.jpg";
            }

            w = width;
            h = height;
            bw = width;
            bh = height;
            bs = bw * bh;

            this.setCurrentPix(pixels);

            initArr();

            constructBuffers(); // allocates all buffers

            loadTexture();

            t = new Thread(this);
            t.setPriority(t.NORM_PRIORITY);

            this.start();

            return running;
        } else {
            return false;
        }
    }



    /**
     * Memory allocation and arry initializations
     */
    private void initArr() {
        bh = h;
        bw = w;
        bs = bw * bh;

        w_old = new int[bs];
        w_new = new int[bs];
        pix_ = new int[bs];

        tmp_ = new int[bs];

        for (int y = 0; y < bs; y++) {
            w_old[y] = 0; // values for untouched water == 0
            w_new[y] = 0;
        }
    }

    /**
     * Loads the texture into an array pix[]
     */
    private void loadTexture() {

    }

    public void setCurrentPix(int[] source_pix){
        curr_pix = source_pix;
    }

    /**
     * constructs all needed buffers
     */
    private void constructBuffers() {
        buffer = new int[bs]; // pixel buffer used to update the image
        black_buffer = new int[bs];
        for (int i = 0; i < bs; i++)
            black_buffer[i] = 0xFF000000;

        System.arraycopy(black_buffer, 0, buffer, 0, buffer.length);


        this.buffers_constructed = true;
    }

    /**
     * clear the buffer to 0xFF000000
     */
    private void clearBuffer() {
        System.arraycopy(black_buffer, 0, buffer, 0, buffer.length);
    }

    /**
     *  return the buffer
     */
    public int[] getBuffer(){
        synchronized (buffer) {
            return buffer;
        }
    }


    /**
     * paints the image
     */
    public void paintImg() {
        target_view.setBuffer(buffer.clone());
        target_view.postInvalidate();
    }


    void repaint() {

        filterMap();

        genTexture();

        paintImg(); // paint all stuff

        clearBuffer(); // clear the buffer --- to avoid unwanted pix on screen

    }


    /**
     * this is the actual water effect calculation based on a technique called
     * 8-Pix filtermap
     */
    private void filterMap() {
        int y;
        int x;

        int tmp2 = 0;
        int tmp = 0;
        for (y = 1; y < h - 1; y++) {
            tmp2 += w;
            for (x = 1; x < w - 1; x++) {
                tmp = tmp2 + x;
                int newh = ((w_old[tmp + w] + w_old[tmp - w] + w_old[tmp + 1]
                        + w_old[tmp - 1] + w_old[tmp - w - 1] // 4 pixel-map
                        // would be
                        // sufficient
                        + w_old[tmp + w - 1] + w_old[tmp - w + 1] + w_old[tmp
                        + w + 1]) >> 2)
                        - w_new[tmp];

                w_new[tmp] = newh - (newh >> 3);
            }

        }

        tmp_ = w_old;
        w_old = w_new;

        w_new = tmp_;
    }

    /**
     * generates the texture according to current buffer-colors and wave_map
     */
    private void genTexture() {
            int tmp = 0;
            int t_ver = 0;

            int tm = 0;
            int tmp3 = 0;

            int y;
            int x;
            for (y = 0; y < (h - 1); y++) {
                for (x = 0; x < (w - 1); x++) {
                    // calculate the 'slope' for the current pixel
                    int dx = w_old[tmp] - w_old[tmp + 1];
                    int dy = w_old[tmp] - w_old[tmp + w];

                    int ox = (dx >> 3) + x;
                    int oy = (dy >> 3) + y;

                    int shading = (dx + dy) >> lum; // calc reflection

                    // keep offset inside the array
                    ox = ox >= w ? (w - 1) : (ox < 0 ? 0 : ox);
                    oy = oy >= h ? (h - 1) : (oy < 0 ? 0 : oy);

                    int color = curr_pix[ox + oy * w];

                    int r = (color & 0xFF0000) >> 16;
                    int g = (color & 0xFF00) >> 8;
                    int b = (color & 0xFF);

                    r = controlCol(r + shading);
                    g = controlCol(g + shading);
                    b = controlCol(b + shading);

                    int tmpX;
                    int tmpY;
                    int tmp2;


                    tmp2 = tmp;


                    if ((tmp2 >= bs) || (tmp2 <= 0)) {
                        // prevent array index out of bounds
                    } else {

                                buffer[tmp2] = 0xFF000000 | (r << 16)
                                        | (g << 8) | b;// paint pixel at offset
                                // tmp2

                    }
                    // increment one pixel
                    tmp++;
                    t_ver++;
                }
                tmp++;
                t_ver++;
            }
    }



    /**
     * adds drops in rasterized form
     */
    public void addSome(int depth) {
        for (int y = 0; y < h - 1; y++) {
            for (int x = 0; x < w - 1; x++) {
                if (((x % 2) == 0) && ((y % 2) == 0)) {
                    w_old[(x % w) + (y % h) * w] = depth;
                }
            }
        }

    }

    /**
     * dropping water
     */
    public void addDrop(int x, int y, int depth) {
        // int t_depth;
        w_old[(x % w) + (y % h) * w] = depth * d_f;


    }

    /**
     * adds a nice looking blob to the hmap (uses sqrt => slow)
     */
    public void addBlob(int x, int y, int rad, int depth) {
        // check/adjust position of blob
        x = x < 1 ? 1 : (x >= w ? w - 1 : x); // was w - 1
        y = y < 1 ? 1 : (y >= h ? h - 1 : y); // was w - 1

        // check/adjust radius
        rad = x + (rad << 1) >= w ? (w - x) >> 1 : rad;
        rad = y + (rad << 1) >= h ? (h - y) >> 1 : rad;

        // precalcs/-casts
        double drad = (double) rad;
        double cx = x + drad;
        double cy = y + drad;

        for (int i = x; i < x + (rad * 2); i++) {
            for (int j = y; j < y + (rad * 2); j++) {
                // offsets from center
                double dx = Math.abs(cx - ((double) i));
                double dy = Math.abs(cy - ((double) j));
                // vector distance from center
                double l = Math.sqrt(dx * dx + dy * dy);
                // get inverse distance
                double mul = Math.max(drad - l, 0.0);
                // normalize it by radius
                mul /= drad;
                // multiply the depth with it to get a smooth blob
                // and add this value to the hmap at the current position
                // the modulos shouldn't be necessary, but.....
                w_old[(i % w) + (j % h) * w] += (int) (mul * ((double) depth * d_f));
            }

        }

    }

    /**
     * copies src[] into two-dimensional arrays -- dest
     */
    public int[][] getArr(int[] src) {
        int[][] dest = new int[h][w];
        int y;
        int x;
        int tmp = 0;
        for (y = 0; y < h - 1; y++) {
            for (x = 0; x < w - 1; x++) {
                dest[y][x] = src[tmp];
                tmp++;
            }
            tmp++;
        }
        return dest;

    }

    /**
     * helper function that avoids color over/underflow
     */
    private int controlCol(int c) {
        return (c > 255 ? 255 : (c < 0 ? 0 : c));
    }

    /**
     * called when the applet starts
     */
    public void start() {
        if (t != null) {
            running = true;
            t.start();
        }
    }

    /**
     * called when the applet should stop
     */
    public void stop() {

        if (t != null) {
            running = false;
            t.stop();
            t = null;
        }

    }

    /**
     * Thread runner
     */
    public void run() {
        while (running) {
            if(run_status){
                long runout_time = System.currentTimeMillis() - startTime;
                if(runout_time > EFFECT_TIMEOUT) {
                    speed = 300;
                }else{
                    speed = 0;
                }
            }else{
                speed = 0;
            }
            if (speed != 0) {
                if(speed == 300){
                    try {
                        t.sleep((long) speed);
                    } catch (InterruptedException e) {
                        System.out.println("error in applying speed-factor : "
                                + e.getMessage());
                    }
                }else {
                    try {
                        t.sleep((long) speed);
                        repaint();

                    } catch (InterruptedException e) {
                        System.out.println("error in applying speed-factor : "
                                + e.getMessage());
                    }
                }

            } else {
                repaint();
            }
        }
    }

    public void setRunning(boolean run_status){
        startTime = System.currentTimeMillis();
        this.run_status = run_status;
    }
}

