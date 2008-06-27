package codeswarm.jogl;

/*
	 Copyright 2008 Michael Ogawa
	 Additional work by:
	   Arjen Wiersma

	 This file is part of code_swarm.

	 code_swarm is free software: you can redistribute it and/or modify
	 it under the terms of the GNU General Public License as published by
	 the Free Software Foundation, either version 3 of the License, or
	 (at your option) any later version.

	 code_swarm is distributed in the hope that it will be useful,
	 but WITHOUT ANY WARRANTY; without even the implied warranty of
	 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	 GNU General Public License for more details.

	 You should have received a copy of the GNU General Public License
	 along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GL;
import javax.media.opengl.GLJPanel;
// import javax.media.opengl.DebugGL;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * This is the Java OpenGL version of the original processing code_swarm application
 * <p/>
 * It is modelled after the original prototype.
 *
 * @author <a href="mailto:arjenw@gmail.com">Arjen Wiersma</a>
 */
public class CodeSwarm implements GLEventListener {
    public static final Logger log = Logger.getLogger(CodeSwarm.class.getName());

    public CodeSwarm() {

    }

    public void init(GLAutoDrawable glAutoDrawable) {
        // Use debug pipeline
        //glAutoDrawable.setGL(new DebugGL(glAutoDrawable.getGL()));

        GL gl = glAutoDrawable.getGL();
        gl.setSwapInterval(1);
    }

    public void display(GLAutoDrawable glAutoDrawable) {
        GL gl = glAutoDrawable.getGL();

        if ((glAutoDrawable instanceof GLJPanel) && !((GLJPanel) glAutoDrawable).isOpaque() && ((GLJPanel) glAutoDrawable).shouldPreserveColorBufferIfTranslucent()) {
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
        }
        else {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }
    }

    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        GL gl = glAutoDrawable.getGL();

        // ratio...
        // float h = (float) height / (float) width;

        gl.glMatrixMode(GL.GL_PROJECTION);

    }

    public void displayChanged(GLAutoDrawable glAutoDrawable, boolean modeChanged, boolean deviceChanged) { }

    public static void main(String[] args) {
        log.info("Running CodeSwarm with parameters: ");
        for (String arg : args) {
            log.info("Argument: " + arg);
        }

        Frame frame = new Frame("CodeSwarm JOGL");
        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(new CodeSwarm());
        frame.add(canvas);
        // set the size
        frame.setSize(640, 480);
        // also add something to stop the animation thread
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log.info("Closing the window");
                new Thread(new Runnable() {
                    public void run() {
                        // stop the animator here...
                        System.exit(0);
                    }
                }).start();
            }
        });
        frame.setVisible(true);
    }
}
