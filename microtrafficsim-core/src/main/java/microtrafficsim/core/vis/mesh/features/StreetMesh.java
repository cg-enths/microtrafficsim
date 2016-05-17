package microtrafficsim.core.vis.mesh.features;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;

import microtrafficsim.core.vis.context.RenderContext;
import microtrafficsim.core.vis.mesh.Mesh;
import microtrafficsim.core.vis.mesh.MeshBucket;
import microtrafficsim.core.vis.opengl.BufferStorage;
import microtrafficsim.core.vis.opengl.DataTypes;
import microtrafficsim.core.vis.opengl.shader.ShaderProgram;
import microtrafficsim.core.vis.opengl.shader.attributes.VertexArrayObject;
import microtrafficsim.core.vis.opengl.shader.attributes.VertexAttributePointer;
import microtrafficsim.core.vis.opengl.shader.attributes.VertexAttributes;
import microtrafficsim.core.vis.opengl.utils.LifeTimeObserver;
import microtrafficsim.math.Vec3f;


public class StreetMesh implements Mesh {
	private State state;

	private int usage;
	private int mode;
	
	private FloatBuffer vertices;
	private IntBuffer indices;
	
	private BufferStorage vbo;
	private BufferStorage ibo;
	private VertexArrayObject vao;		// from OpenGL 3.1 upwards a VAO is always required for drawing
	
	private VertexAttributePointer ptrPosition;

	private List<Bucket> buckets;
	
	private HashSet<LifeTimeObserver<Mesh>> ltObservers;

	
	public StreetMesh(int usage, int mode, FloatBuffer vertices, IntBuffer indices) {
		this.state = State.UNINITIALIZED;
		
		this.usage = usage;
		this.mode = mode;
		
		this.vertices = vertices;
		this.indices = indices;
		
		this.vbo = null;
		this.ibo = null;

		this.ltObservers = new HashSet<>();
	}

	public void setBuckets(List<Bucket> buckets) {
		this.buckets = buckets;
	}


	@Override
	public State getState() {
		return state;
	}


	@Override
	public boolean initialize(RenderContext context, boolean force) {
		if (state == State.DISPOSED) return false;

		GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();
		if (state == State.INITIALIZED || state == State.LOADED) {
			if (!force) return false;

			// dispose old
			vbo.dispose(gl);
			ibo.dispose(gl);
		}

		vbo = BufferStorage.create(gl, GL.GL_ARRAY_BUFFER);
		ibo = BufferStorage.create(gl, GL.GL_ELEMENT_ARRAY_BUFFER);

		ptrPosition = VertexAttributePointer.create(VertexAttributes.POSITION3, DataTypes.FLOAT_3, vbo);

		state = State.INITIALIZED;
		return true;
	}

	@Override
	public boolean dispose(RenderContext context) {
		boolean disposable = state != State.DISPOSED && state != State.UNINITIALIZED;

        if (disposable) {
            GL gl = context.getDrawable().getGL();

            vbo.dispose(gl);
            ibo.dispose(gl);

            state = State.DISPOSED;
        }

		for (LifeTimeObserver<Mesh> lto : ltObservers)
			lto.disposed(this);

		return disposable;
	}

	@Override
	public boolean load(RenderContext context, boolean force) {
		if (state == State.DISPOSED || state == State.UNINITIALIZED) return false;

		GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();
		if (state == State.LOADED) {
			if (!force) return false;

			vao.dispose(gl);
		}

		// vertices
		gl.glBindBuffer(vbo.target, vbo.handle);
		gl.glBufferData(vbo.target, vertices.capacity() * 4L, vertices, usage);
		gl.glBindBuffer(vbo.target, 0);
		
		// indices
		gl.glBindBuffer(ibo.target, ibo.handle);
		gl.glBufferData(ibo.target, indices.capacity() * 4L, indices, usage);
		gl.glBindBuffer(ibo.target, 0);
		
		// generate dummy vao
		vao = VertexArrayObject.create(gl);

		state = State.LOADED;
		return true;
	}

	@Override
	public void display(RenderContext context, ShaderProgram shader) {
		GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();
		
		vao.bind(gl);
		vbo.bind(gl);
		ibo.bind(gl);
		
		ptrPosition.set(gl);
		ptrPosition.enable(gl);
		
		gl.glDrawElements(mode, indices.capacity(), GL2GL3.GL_UNSIGNED_INT, 0);
		
		vao.unbind(gl);
	}
	
	@Override
	public void display(RenderContext context, VertexArrayObject vao) {
		GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();
		
		vao.bind(gl);
		gl.glDrawElements(mode, indices.capacity(), GL2GL3.GL_UNSIGNED_INT, 0);
		vao.unbind(gl);
	}

	@Override
	public VertexArrayObject createVAO(RenderContext context, ShaderProgram program) {
		GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();

		VertexArrayObject vao = VertexArrayObject.create(gl);

		vao.bind(gl);
		vbo.bind(gl);
		ibo.bind(gl);

		ptrPosition.set(gl);
		ptrPosition.enable(gl);

		vao.unbind(gl);

		return vao;
	}


	@Override
	public List<MeshBucket> getBuckets() {
		return Collections.unmodifiableList(buckets);
	}


	@Override
	public void addLifeTimeObserver(LifeTimeObserver<Mesh> lto) {
		ltObservers.add(lto);
	}

	@Override
	public void removeLifeTimeObserver(LifeTimeObserver<Mesh> lto) {
		ltObservers.remove(lto);
	}

	@Override
	public Set<LifeTimeObserver<Mesh>> getLifeTimeObservers() {
		return Collections.unmodifiableSet(ltObservers);
	}



	public class Bucket implements MeshBucket {

		private final Vec3f position;
		private final int offset;
		private final int count;

		public Bucket(Vec3f position, int offset, int count) {
			this.position = position;
			this.offset = offset;
			this.count = count;
		}

		@Override
		public Vec3f getPosition() {
			return position;
		}

		@Override
		public void display(RenderContext context, ShaderProgram shader) {
			GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();

			vao.bind(gl);
			vbo.bind(gl);
			ibo.bind(gl);

			ptrPosition.set(gl);
			ptrPosition.enable(gl);

			gl.glDrawElements(mode, count, GL2GL3.GL_UNSIGNED_INT, offset * 4);

			vao.unbind(gl);
		}

		@Override
		public void display(RenderContext context, VertexArrayObject vao) {
			GL2GL3 gl = context.getDrawable().getGL().getGL2GL3();

			vao.bind(gl);
			gl.glDrawElements(mode, count, GL2GL3.GL_UNSIGNED_INT, offset * 4);
			vao.unbind(gl);
		}
	}
}