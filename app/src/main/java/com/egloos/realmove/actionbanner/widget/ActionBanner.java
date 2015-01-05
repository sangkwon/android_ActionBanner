package com.egloos.realmove.actionbanner.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;

import com.egloos.realmove.actionbanner.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by sangkwon on 14. 12. 26..
 */
public class ActionBanner extends GLSurfaceView {

	private ActionBannerRenderer mRenderer;

	public ActionBanner(Context context) {
		super(context);

		init();
	}

	public ActionBanner(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	public void init() {
		if (!isInEditMode()) {
			mRenderer = new ActionBannerRenderer();
			setRenderer(mRenderer);
		}
	}

	class ActionBannerRenderer extends GLES10 implements GLSurfaceView.Renderer {

		private static final int OBJECT_COUNT = 1;
		private final int[] PROFILES = {R.drawable.img8bit};

		private int mWidth, mHeight;
		private FloatBuffer fbVtxAxis, fbColAxis, fbVtxGrid, fbColGrid;
		private FloatBuffer fbVtxPlane, fbTexPlane;
		private int[] textures = new int[OBJECT_COUNT];

		public ActionBannerRenderer() {
			init();
		}

		/**
		 * 관련 변수들을 초기화한다.
		 */
		private void init() {
			initPlane();
			initAxisGridVtx();
		}

		private void initPlane() {
			float faVtxPlane[] = {
					-1, 1, 0, -1, -1, 0, 1, -1, 0,
					-1, 1, 0, 1, -1, 0, 1, 1, 0
			};
//			{0.0f, 0.0f, 0.0f,
//					240.0f, 800.0f, 0.0f,
//					480.0f, 0.0f, 0.0f};

			fbVtxPlane = loadBuffer(faVtxPlane);

			float faTexPlane[] = {
					0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0
			};
			fbTexPlane = loadBuffer(faTexPlane);
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


		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			glShadeModel(GL_SMOOTH);            // Smooth Shading이 가능하도록 설정
			glClearColor(1.0f, 1.0f, 1.0f, 1.0f);    // 하얀 바탕 그리기
			glClearDepthf(1.0f);                     // Depth Buffer 세팅
			glEnable(GL_DEPTH_TEST);            // Depth Test 가능하도록 설정
			glDepthFunc(GL_LEQUAL);             // The Type Of Depth Testing
			glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);// glHint 설정

			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);

			glGenTextures(textures.length, textures, 0);
			for (int i = 0; i < OBJECT_COUNT; i++) {
				loadTexture(textures[i], PROFILES[i]);
			}
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			if (height == 0) {
				height = 1;                             // 0으로 나누는 것을 방지하기 위해서
			}

			mWidth = width;
			mHeight = height;

			gl.glViewport(0, 0, width, height);         // ViewPort 리셋
			gl.glMatrixMode(GL10.GL_PROJECTION);        // MatrixMode를 Project Mode로
			gl.glLoadIdentity();                        // Matrix 리셋

			// 윈도우의 Aspect Ratio 설정
			//gl.glOrthof(0, mWidth, mHeight, 0, 1, -1);

			gl.glMatrixMode(GL10.GL_MODELVIEW);         // Matrix를 ModelView Mode로 변환
			gl.glLoadIdentity();
		}

		@Override
		public void onDrawFrame(GL10 gl) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glMatrixMode(GL_MODELVIEW);

			drawGrid();
//			drawAxis();

			drawPlanes();
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
			time++;

			for (int i = 0; i < textures.length; i++) {
				float y = 0;

				glLoadIdentity();
				glScalef(0.5f,0.5f,0.5f);
				drawPlane(textures[i], fbTexPlane, fbVtxPlane);
			}

			glDisable(GL_ALPHA_TEST);
		}

		private void drawPlane(int textureId, FloatBuffer fbTexPlane, FloatBuffer fbVtxPlane) {
			glEnableClientState(GL_COLOR_ARRAY);

			glEnable(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, textureId);
			glTexCoordPointer(2, GL_FLOAT, 0, fbTexPlane);

			glVertexPointer(3, GL_FLOAT, 0, fbVtxPlane);
			glDrawArrays(GL_TRIANGLES, 0, 6);
			glDisable(GL_TEXTURE_2D);

			glDisableClientState(GL_COLOR_ARRAY);
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
//			glLineWidth(1);
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

	}

}
