package microtrafficsim.core.vis.context;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import microtrafficsim.core.vis.Renderer;
import microtrafficsim.core.vis.context.state.*;
import microtrafficsim.core.vis.context.tasks.FutureRenderTask;
import microtrafficsim.core.vis.context.tasks.RenderTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import static microtrafficsim.build.BuildSetup.DEBUG_VISUALIZATION;


public class RenderContext implements GLEventListener {
    private static Logger logger = LoggerFactory.getLogger(RenderContext.class);
    private static long RTASK_EXECUTION_BUDGED_NS = 32_000_000;
	
	public interface UncaughtExceptionHandler {
		void uncaughtException(RenderContext context, Throwable cause);
	}

	
	// -- state ---------------------------------------------------------------
	
	public final ClearColor ClearColor;
	public final ClearDepth ClearDepth;
	public final DepthTest DepthTest;
	public final BlendMode BlendMode;
	public final PrimitiveRestart PrimitiveRestart;
	public final Points Points;
    public final ShaderState ShaderState;

	{
		ClearColor = new ClearColor();
		ClearDepth = new ClearDepth();
		DepthTest = new DepthTest();
		BlendMode = new BlendMode();
		PrimitiveRestart = new PrimitiveRestart();
		Points = new Points();
        ShaderState = new ShaderState();
	}
	
	
	// -- context -------------------------------------------------------------
	
	private Renderer renderer;
	private Queue<FutureRenderTask<?>> tasks;
	
	private GLAnimatorControl animator;
	private GLAutoDrawable drawable;
	private UncaughtExceptionHandler exhdlr;
	
	private ShaderManager shaders;
	private UniformManager uniforms;
	private VertexAttributeManager attributes;


	public RenderContext() {
		this.tasks = new ConcurrentLinkedQueue<>();
		
		this.renderer = null;
		this.animator = null;
		this.drawable = null;
		this.exhdlr = null;
		
		this.shaders = new ShaderManager();
		this.uniforms = new UniformManager();
		this.attributes = new VertexAttributeManager();
	}
	
	
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}
	
	public Renderer getRenderer() {
		return renderer;
	}
	
	
	public ShaderManager getShaderManager() {
		return shaders;
	}
	
 	public UniformManager getUniformManager() {
		return uniforms;
	}
	
	public VertexAttributeManager getVertexAttribManager() {
		return attributes;
	}
	
	
	public Queue<? extends Future<?>> getTaskQueue() {
		return tasks;
	}
	
	public <V> Future<V> addTask(RenderTask<V> task) {
        FutureRenderTask<V> future = new FutureRenderTask<>(task);

        // if the context is current on this thread, run the task instantly
		GLAutoDrawable drawable = this.drawable;
        if (drawable != null && drawable.getContext().isCurrent())
            future.run(this);
        else
		    tasks.add(future);

		return future;
	}
	
	public boolean hasTasks() {
		return tasks.isEmpty();
	}
	
	
	public GLAnimatorControl getAnimator() {
		return animator;
	}
	
	public void setAnimator(GLAnimatorControl animator) {
		this.animator = animator;
	}
	
	
	public UncaughtExceptionHandler getUncaughtExceptionHandler() {
		return exhdlr;
	}
	
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
		this.exhdlr = handler;
	}
	
	
	public GLAutoDrawable getDrawable() {
		return drawable;
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		this.drawable = drawable;
		
		try {
			renderer.init(this);
		} catch (Throwable exception) {
			if(!handleException(exception))
				throw exception;
		} finally {
			this.drawable = null;
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		this.drawable = drawable;
		
		try {
			renderer.dispose(this);
		} catch (Throwable exception) {
			if(!handleException(exception))
				throw exception;
		} finally {
			this.drawable = null;
		}
		
		shaders.dispose(drawable.getGL().getGL2ES2(), true);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		this.drawable = drawable;

        // execute tasks on work queue
        // TODO: print task execution time
        long t = System.nanoTime();
        while (!tasks.isEmpty()) {
			tasks.poll().run(this);
			Thread.interrupted();		// interrupts are task-local, clear if necessary

            // make sure we do not block the main thread
            long dt = System.nanoTime() - t;
            if (dt > RTASK_EXECUTION_BUDGED_NS) {
                logger.warn("time for combined task execution exceeded threshold: " + (dt / 1_000_000) + "ms");
                break;
            }
		}

        // display renderer
		try {
			renderer.display(this);
        } catch (Throwable exception) {
            if(!handleException(exception))
				throw exception;
		} finally {
			this.drawable = null;
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		this.drawable = drawable;
		
		try {
			renderer.reshape(this, x, y, width, height);
		} catch (Throwable exception) {
			if(!handleException(exception))
				throw exception;
		} finally {
			this.drawable = null;
		}
	}
	
	
	private boolean handleException(Throwable exception) {
		if (animator != null) animator.stop();
		
		if (exhdlr != null) {
			try {
				exhdlr.uncaughtException(this, exception);
			} catch (Throwable t) {
				/* 
				 * Any Exceptions thrown by the exception-handler are ignored,
				 * we are already handling an exception.
				 */
				
				if (DEBUG_VISUALIZATION) {
					System.err.println("RenderEventController -- Ignoring Exception from ExceptionHandler:");
					t.printStackTrace();
				}
			}
			
			return true;
		}
		
		return false;
	}
}
