/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.opengl.test.junit.newt;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.*;

import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestDisplayLifecycle02NEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static final int width = 500, height = 500, xy_pos=100;
    static long durationPerTest = 100; // ms

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        caps = new GLCapabilities(glp);
    }

    static GLWindow createWindow(final GLCapabilities caps, final int width, final int height)
        throws InterruptedException
    {
        Assert.assertNotNull(caps);

        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLWindow glWindow = GLWindow.create(caps);
        glWindow.setUpdateFPSFrames(1, null);

        final GLEventListener demo = new GearsES2();
        setDemoFields(demo, glWindow);
        glWindow.addGLEventListener(demo);
        glWindow.addWindowListener(new TraceWindowAdapter());
        glWindow.setSize(width, height);
        return glWindow;
    }

    private void testDisplayCreate01Impl() throws InterruptedException {
        // start-state == end-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Window, pending lazy native creation
        final GLWindow window = createWindow(caps, width, height);
        final Screen screen = window.getScreen();
        final Display display = screen.getDisplay();
        window.setPosition(xy_pos, xy_pos);

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,Screen.getActiveScreenNumber());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());
        Assert.assertTrue(NewtTestUtil.hasPositionMax2xInsetsOr64Pix(window, xy_pos, xy_pos));

        // lazy native creation sequence: Display, Screen and Window
        Assert.assertEquals(0, window.getTotalFPSFrames());
        window.setVisible(true);

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,Screen.getActiveScreenNumber());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        Assert.assertTrue(NewtTestUtil.hasPositionMax2xInsetsOr64Pix(window, xy_pos, xy_pos));
        System.err.println("Frames for setVisible(true) 1: "+window.getTotalFPSFrames());
        Assert.assertTrue(0 < window.getTotalFPSFrames());

        final RectangleImmutable screenBoundsInWinU = screen.getViewportInWindowUnits();
        System.err.println("screenBoundsInWinU: "+screenBoundsInWinU);

        while(window.getTotalFPSDuration()<1*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getTotalFPSDuration());

        // just make the Window invisible
        window.setVisible(false);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        // just make the Window visible again
        window.resetFPSCounter();
        Assert.assertEquals(0, window.getTotalFPSFrames());
        window.setVisible(true);
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        Assert.assertTrue(NewtTestUtil.hasPositionMax2xInsetsOr64Pix(window, xy_pos, xy_pos));
        System.err.println("Frames for setVisible(true) 1: "+window.getTotalFPSFrames());
        Assert.assertTrue(0 < window.getTotalFPSFrames());

        while(window.getTotalFPSDuration()<2*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getTotalFPSDuration());

        // destruction.. ref count down, but keep all
        window.destroy();
        Assert.assertTrue(NewtTestUtil.waitForRealized(window, false, null));
        Assert.assertTrue(NewtTestUtil.waitForRealized(screen, false, null));

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,Screen.getActiveScreenNumber());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertNotNull(window.getScreen());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        window.resetFPSCounter();
        Assert.assertEquals(0, window.getTotalFPSFrames());

        // a display call shall not change a thing
        window.display();
        Assert.assertEquals(0, window.getTotalFPSFrames());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        // recover Window
        window.setVisible(true);

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,Screen.getActiveScreenNumber());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window.isNativeValid());
        Assert.assertEquals(true,window.isVisible());
        System.err.println("Frames for setVisible(true) 2: "+window.getTotalFPSFrames());
        Assert.assertTrue(0 < window.getTotalFPSFrames());

        while(window.getTotalFPSDuration()<1*durationPerTest) {
            window.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window.getTotalFPSDuration());

        window.destroy();
        Assert.assertTrue(NewtTestUtil.waitForRealized(window, false, null));
        Assert.assertTrue(NewtTestUtil.waitForRealized(screen, false, null));

        Assert.assertEquals(screen,window.getScreen());
        Assert.assertEquals(false,window.isNativeValid());
        Assert.assertEquals(false,window.isVisible());

        Display.dumpDisplayList("Post destroy(true)");

        // end-state == start-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,Screen.getActiveScreenNumber());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
    }

    @Test
    public void testDisplayCreate01() throws InterruptedException {
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Display/Screen, pending lazy native creation
        System.err.println("Pass - 1");
        testDisplayCreate01Impl();
        System.err.println("Pass - 2");
        testDisplayCreate01Impl();

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    private void testDisplayCreate02Impl() throws InterruptedException {
        // start-state == end-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Window, pending lazy native creation
        final GLWindow window1 = createWindow(caps, width, height);
        final Screen screen = window1.getScreen();
        final Display display = screen.getDisplay();
        window1.setPosition(xy_pos, xy_pos);

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,Screen.getActiveScreenNumber());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(false,window1.isNativeValid());
        Assert.assertEquals(false,window1.isVisible());
        Assert.assertEquals(xy_pos,window1.getX());
        Assert.assertEquals(xy_pos,window1.getY());

        // lazy native creation sequence: Display, Screen and Window
        Assert.assertEquals(0, window1.getTotalFPSFrames());
        window1.setVisible(true);

        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,Screen.getActiveScreenNumber());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        Assert.assertEquals(xy_pos,window1.getX());
        Assert.assertEquals(xy_pos,window1.getY());
        System.err.println("Frames for setVisible(true) 1: "+window1.getTotalFPSFrames());
        Assert.assertTrue(0 < window1.getTotalFPSFrames());

        final RectangleImmutable screenBoundsInWinU = screen.getViewportInWindowUnits();
        System.err.println("screenBoundsInWinU: "+screenBoundsInWinU);
        final int x_pos2 = screenBoundsInWinU.getWidth()-xy_pos-width;

        final GLWindow window2 = createWindow(caps, width, height);
        Assert.assertSame(screen, window2.getScreen());
        Assert.assertSame(display, window2.getScreen().getDisplay());
        window2.setPosition(x_pos2, xy_pos);

        Assert.assertEquals(false,window2.isNativeValid());
        Assert.assertEquals(false,window2.isVisible());
        Assert.assertEquals(x_pos2,window2.getX());
        Assert.assertEquals(xy_pos,window2.getY());
        Assert.assertEquals(0, window2.getTotalFPSFrames());
        window2.setVisible(true);

        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,Screen.getActiveScreenNumber());
        Assert.assertEquals(2,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(true,window2.isNativeValid());
        Assert.assertEquals(true,window2.isVisible());
        Assert.assertEquals(x_pos2,window2.getX());
        Assert.assertEquals(xy_pos,window2.getY());
        System.err.println("Frames for setVisible(true) 2: "+window2.getTotalFPSFrames());
        Assert.assertTrue(0 < window2.getTotalFPSFrames());

        while(window1.getTotalFPSDuration()<1*durationPerTest) {
            window1.display();
            Thread.sleep(100);
        }
        System.err.println("duration: "+window1.getTotalFPSDuration());

        // just make the Window invisible
        window1.setVisible(false);
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(false,window1.isVisible());
        Assert.assertEquals(xy_pos,window1.getX());
        Assert.assertEquals(xy_pos,window1.getY());

        // destruction ...
        window1.destroy();
        Assert.assertTrue(NewtTestUtil.waitForRealized(window1, false, null));

        Assert.assertNotNull(window1.getScreen());
        Assert.assertEquals(false,window1.isNativeValid());
        Assert.assertEquals(false,window1.isVisible());

        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,Screen.getActiveScreenNumber());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());

        // destruction
        window2.destroy();
        Assert.assertTrue(NewtTestUtil.waitForRealized(window2, false, null));
        Assert.assertTrue(NewtTestUtil.waitForRealized(screen, false, null));

        Assert.assertNotNull(window2.getScreen());
        Assert.assertEquals(false,window2.isNativeValid());
        Assert.assertEquals(false,window2.isVisible());

        // end-state == start-state
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,Screen.getActiveScreenNumber());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());

        // invalidate (again) ..
        window1.destroy();
        Assert.assertEquals(false,window1.isNativeValid());
        Assert.assertEquals(false,window1.isVisible());

        // invalidate (again) ..
        window2.destroy();
        Assert.assertEquals(false,window2.isNativeValid());
        Assert.assertEquals(false,window2.isVisible());

    }

    // @Test
    public void testDisplayCreate02() throws InterruptedException {
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // Create Display/Screen, pending lazy native creation
        System.err.println("Pass - 1");
        testDisplayCreate02Impl();
        System.err.println("Pass - 2");
        testDisplayCreate02Impl();

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.err.println("durationPerTest: "+durationPerTest);
        final String tstname = TestDisplayLifecycle02NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
