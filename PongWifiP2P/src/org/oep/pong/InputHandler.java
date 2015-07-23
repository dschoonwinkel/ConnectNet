package org.oep.pong;

import android.view.MotionEvent;

public abstract class InputHandler {

	public static InputHandler getInstance() {
		return MultiInput.Holder.sInstance;
	}

	public abstract int getTouchCount(MotionEvent e);

	public abstract float getX(MotionEvent e, int i);

	public abstract float getY(MotionEvent e, int i);

	private static class MultiInput extends InputHandler {
		private static class Holder {
			private static final MultiInput sInstance = new MultiInput();
		}

		@Override
		public int getTouchCount(MotionEvent e) {
			return e.getPointerCount();
		}

		@Override
		public float getX(MotionEvent e, int i) {
			return e.getX(i);
		}

		@Override
		public float getY(MotionEvent e, int i) {
			return e.getY(i);
		}
	}
}
