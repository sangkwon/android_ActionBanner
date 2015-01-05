package com.daybe.egg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.egloos.realmove.actionbanner.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class EggView extends GLSurfaceView {
	EggRenderer mRenderer;

	int[] PROFILES = {
			R.drawable.img8bit, R.drawable.img8bit, R.drawable.img8bit
			, R.drawable.img8bit, R.drawable.img8bit, R.drawable.img8bit
	};
	int OBJECT_COUNT = PROFILES.length;

	private static final float LOOK_ANGLE_V_MAX = (float) (Math.PI / 2 * 0.99);

	public EggView(Context context) {
		super(context);

		init();
	}

	public EggView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	private void init() {
		if (!isInEditMode()) {
			mRenderer = new EggRenderer();
			setRenderer(mRenderer);
		}
	}

	private float downX = 0;
	private float downY = 0;

	private float gapX = 0;
	private float gapY = 0;

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				downX = event.getX();
				downY = event.getY();
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				gapX = 0;
				gapY = 0;

				downX = 0;
				downY = 0;
				break;

			case MotionEvent.ACTION_MOVE:
				if (event.getPointerCount() == 1) {
					if (downX > 0 && downY > 0) {
						mRenderer.lookAngleH = mRenderer.lookAngleH + (event.getX() - downX) / (float) Math.PI / 1000
								* (float) Math.sqrt(mRenderer.lookRadius);
						mRenderer.lookAngleV = mRenderer.lookAngleV + (event.getY() - downY) / (float) Math.PI / 1000
								* (float) Math.sqrt(mRenderer.lookRadius);

						if (mRenderer.lookAngleV > LOOK_ANGLE_V_MAX) {
							mRenderer.lookAngleV = LOOK_ANGLE_V_MAX;
						} else if (mRenderer.lookAngleV < 0 - LOOK_ANGLE_V_MAX) {
							mRenderer.lookAngleV = 0 - LOOK_ANGLE_V_MAX;
						}
					}
				} else if (event.getPointerCount() == 2) {
					float dx = event.getX(1) - event.getX(0);
					float dy = event.getY(1) - event.getY(0);
					if (gapX != 0 || gapY != 0) {
						mRenderer.lookRadius = mRenderer.lookRadius - mRenderer.lookRadius * (distSq(dx, dy) - distSq(gapX, gapY)) / 400000;
						if (mRenderer.lookRadius < .5f)
							mRenderer.lookRadius = .5f;
						else if (mRenderer.lookRadius > 100)
							mRenderer.lookRadius = 100;
					}
					gapX = dx;
					gapY = dy;
				}

				downX = event.getX();
				downY = event.getY();
				break;
		}

		return true;
	}

	float distSq(float x, float y) {
		return x * x + y * y;
	}

	public class EggRenderer extends GLES10 implements GLSurfaceView.Renderer {

		private int mWidth;
		private int mHeight;
		private int[] textures = new int[OBJECT_COUNT];

		private FloatBuffer fbVtxAxis, fbColAxis, fbVtxGrid, fbColGrid;
		private FloatBuffer fbVtxPlane, fbTexPlane;

		public EggRenderer() {
			init();

			initAxisGridVtx();
			initPlane();
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			resetLookAt(gl);

			// Set the background frame color
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glMatrixMode(GL_MODELVIEW);

			drawGrid();
//			drawAxis();

			drawPlanes();
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			mWidth = width;
			mHeight = height;

			resetLookAt(gl);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			// Redraw background color
			glClearColor(1, 1, 1, 1);

			glEnable(GL_DEPTH_TEST);
			glDepthFunc(GL_LEQUAL);

			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);

			glGenTextures(textures.length, textures, 0);
			for (int i = 0; i < OBJECT_COUNT; i++) {
				loadTexture(textures[i], PROFILES[i]);
			}
		}

		// ---------------------------------------------------------------
		// Logics
		// ---------------------------------------------------------------

		float[] mAngles = new float[OBJECT_COUNT];
		float[] mX = new float[OBJECT_COUNT];
		float[] mY = new float[OBJECT_COUNT];
		float[] mZ = new float[OBJECT_COUNT];
		float[] mAngularVelocity = new float[OBJECT_COUNT];
		float[] mScale = new float[OBJECT_COUNT];

		private void init() {
			Random rand = new Random();
			float c = 7.0f;
			for (int i = 0; i < OBJECT_COUNT; i++) {
				mX[i] = rand.nextFloat() * c - c / 2;
				mY[i] = 1f; // rand.nextInt(OBJECT_COUNT) - c/2;
				mZ[i] = rand.nextFloat() * c - c / 2;

				mScale[i] = rand.nextFloat() + 1.0f;

				mAngularVelocity[i] = 0f; //rand.nextFloat() * .5f;
			}
		}

		private void initAxisGridVtx() {
			float faVtxAxis[] = {
					0, 0, 0, 10, 0, 0,
					0, 0, 0, 0, 10, 0,
					0, 0, 0, 0, 0, 10
			};
			float faColAxis[] = {
					1, 0, 0, 1, 1, 0, 0, 1,
					0, 1, 0, 1, 0, 1, 0, 1,
					0, 0, 1, 1, 0, 0, 1, 1
			};

			fbVtxAxis = loadBuffer(faVtxAxis);
			fbColAxis = loadBuffer(faColAxis);

			float faVtxGrid[] = {
					-10, 0, 0, 10, 0, 0,
					0, 0, -10, 0, 0, 10
			};
			float faColGrid[] = {
					.5f, .5f, .5f, 1, .5f, .5f, .5f, 1,
					.5f, .5f, .5f, 1, .5f, .5f, .5f, 1
			};
			fbVtxGrid = loadBuffer(faVtxGrid);
			fbColGrid = loadBuffer(faColGrid);
		}

		private void initPlane() {
			float faVtxPlane[] = {
					-1, 1, 0, -1, -1, 0, 1, -1, 0,
					-1, 1, 0, 1, -1, 0, 1, 1, 0
			};

			fbVtxPlane = loadBuffer(faVtxPlane);

			float faTexPlane[] = {
					0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0
			};
			fbTexPlane = loadBuffer(faTexPlane);
		}

		private void drawAxis() {
			glLineWidth(5);

			glLoadIdentity();
			glEnableClientState(GL_COLOR_ARRAY);
			glVertexPointer(3, GL_FLOAT, 0, fbVtxAxis);
			glColorPointer(4, GL_FLOAT, 0, fbColAxis);
			glDrawArrays(GL10.GL_LINES, 0, 6);
			glDisableClientState(GL_COLOR_ARRAY);
		}

		private void drawGrid() {
			glEnableClientState(GL_COLOR_ARRAY);
			glLoadIdentity();
			glLineWidth(1);
			glVertexPointer(3, GL_FLOAT, 0, fbVtxGrid);
			glColorPointer(4, GL_FLOAT, 0, fbColGrid);
			for (float f = -10.0f; f <= 10.0f; f++) {
				glLoadIdentity();
				glTranslatef(0, 0, f);
				glDrawArrays(GL_LINES, 0, 2);

				glLoadIdentity();
				glTranslatef(f, 0, 0);
				glDrawArrays(GL_LINES, 2, 2);
			}
			glDisableClientState(GL_COLOR_ARRAY);
		}

		long mTimeStart = 0;
		long mTime = 0;

		private long getTimeElapsed() {
			mTime = System.currentTimeMillis();

			if (mTimeStart == 0) {
				mTimeStart = mTime;
			}

			return mTime - mTimeStart;
		}

		private void drawPlanes() {
			glEnable(GL_ALPHA_TEST);
			glAlphaFunc(GL_GREATER, 0);

			long time = getTimeElapsed();

			float timeJ = time % 500;
			float height = 0;
			if (timeJ < 250f) {
				height = timeJ / 250f;
			} else {
				height = (500f - timeJ) / 250f;
			}
			height = (float) Math.sin(height / 2 * Math.PI);

			float angle = 0;
			long timeA = ((time - 250) / 1000) % 4;
			if (timeA == 0) {
				angle = 20;
			} else if (timeA == 2) {
				angle = -20;
			}

			for (int i = 0; i < OBJECT_COUNT; i++) {
				mAngles[i] += mAngularVelocity[i];

				float y = mY[i] + height;

				glLoadIdentity();
				glScalef(mScale[i], mScale[i], 1);
				glTranslatef(mX[i], y, mZ[i]);
				glRotatef(mAngles[i], 0, 1, 0);
				glRotatef(angle, 0, 0, 1);
				drawPlane(textures[i], fbTexPlane, fbVtxPlane);
			}

			glDisable(GL_ALPHA_TEST);
		}

		private void drawPlane(int textureId, FloatBuffer fbTexPlane, FloatBuffer fbVtxPlane) {
			glEnable(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, textureId);
			glTexCoordPointer(2, GL_FLOAT, 0, fbTexPlane);

			glVertexPointer(3, GL_FLOAT, 0, fbVtxPlane);
			glDrawArrays(GL_TRIANGLES, 0, 6);
			glDisable(GL_TEXTURE_2D);
		}

		public float lookAngleH = (float) Math.PI / 4;
		public float lookAngleV = (float) Math.PI / 8;
		public float lookRadius = 20;

		private float oldLookAngleH = -1, oldLookAngleV = -1, oldLookRadius = -1;

		private void resetLookAt(GL10 gl) {
			if (oldLookAngleH != lookAngleH || oldLookAngleV != lookAngleV || oldLookRadius != lookRadius) {
				glViewport(0, 0, mWidth, mHeight);

				glMatrixMode(GL10.GL_PROJECTION);
				glLoadIdentity();

				//FIXME 2D 로 만들려면 이 부분 수정해야 하는 것 같은데...
//				glOrthof(0,mWidth,0,mHeight,1,-1);
//				glLoadIdentity();

				GLU.gluPerspective(gl, 45, (float) mWidth / mHeight, 1, 100);
				float lookX = (float) (Math.cos(lookAngleH) * Math.cos(lookAngleV)) * lookRadius;
				float lookY = (float) (Math.sin(lookAngleV)) * lookRadius;
				float lookZ = (float) (Math.sin(lookAngleH) * Math.cos(lookAngleV)) * lookRadius;

				GLU.gluLookAt(gl, lookX, lookY, lookZ, 0, 0, 0, 0, 1, 0);
				// GLU.gluLookAt(gl, 0, 4, 6, 0, 0, -4, 0, 1, 0);

				oldLookAngleH = lookAngleH;
				oldLookAngleV = lookAngleV;
				oldLookRadius = lookRadius;
			}
		}

		// -------------------------------------------------------------
		// U T I L S
		// -------------------------------------------------------------

		int floatToFixed(float fx) {
			int xx = (int) (fx * (float) (1 << 16));
			return xx;
		}

		float fixedToFloat(int xx) {
			float fx = xx * (1.0f / (float) (1 << 16));
			return fx;
		}

		private void loadTexture(int texid, int resid) {
			glBindTexture(GL_TEXTURE_2D, texid);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			Bitmap bmp = BitmapFactory.decodeResource(getContext().getResources(), resid);
			GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
			bmp.recycle();
		}

		FloatBuffer loadBuffer(float[] fa) {
			FloatBuffer fb = ByteBuffer.allocateDirect(fa.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			fb.put(fa).position(0);
			return fb;
		}

		FloatBuffer[] loadPolygon(int num, float radius) {
			FloatBuffer[] result = new FloatBuffer[2];
			float[] faVtx = new float[(num + 2) * 3];
			float[] faCol = new float[(num + 2) * 4];

			Random random = new Random();

			float angle = 2 * (float) Math.PI / num;
			for (int i = 0; i <= num + 1; i++) {
				if (i == 0) {
					faVtx[i * 3] = 0;
					faVtx[i * 3 + 1] = 0;

					faCol[i * 4] = 0;
					faCol[i * 4 + 1] = 0;
					faCol[i * 4 + 2] = 0;
				} else {
					faVtx[i * 3] = radius * (float) Math.cos(angle * i);
					faVtx[i * 3 + 1] = radius * (float) Math.sin(angle * i);

					faCol[i * 4] = random.nextFloat();
					faCol[i * 4 + 1] = random.nextFloat();
					faCol[i * 4 + 2] = random.nextFloat();
				}

				faVtx[i * 3 + 2] = 0;
				faCol[i * 4 + 3] = 1;
			}

			result[0] = loadBuffer(faVtx);
			result[1] = loadBuffer(faCol);

			return result;
		}

	}    // end of Render class

}