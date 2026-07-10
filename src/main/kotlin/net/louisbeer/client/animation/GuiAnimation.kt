package net.louisbeer.client.animation

class GuiAnimation private constructor(
	private var progress: Float,
	private var opening: Boolean,
) {
	fun tick(deltaSeconds: Float) {
		progress = if (opening) {
			minOf(1.0f, progress + deltaSeconds / DURATION_SECONDS)
		} else {
			maxOf(0.0f, progress - deltaSeconds / DURATION_SECONDS)
		}
	}

	fun startOpening() {
		opening = true
		progress = 0.0f
	}

	fun startClosing() {
		if (!opening && progress <= 0.0f) {
			return
		}
		opening = false
		if (progress <= 0.0f) {
			progress = 1.0f
		}
	}

	val isOpening: Boolean
		get() = opening && progress < 1.0f

	/** True from the moment close starts until the screen is torn down. */
	val isClosing: Boolean
		get() = !opening

	/** Close animation has reached the end and the screen should be removed. */
	val hasFinishedClosing: Boolean
		get() = !opening && progress <= 0.0f

	val isFinished: Boolean
		get() = if (opening) progress >= 1.0f else progress <= 0.0f

	val opacity: Float
		get() {
			val eased = if (opening) {
				Easing.cubicEaseOut(progress)
			} else {
				Easing.cubicEaseIn(progress)
			}
			return eased.coerceIn(0.0f, 1.0f)
		}

	val scale: Float
		get() {
			val eased = if (opening) {
				Easing.backEaseOut(progress)
			} else {
				Easing.cubicEaseIn(progress)
			}
			return MIN_SCALE + (1.0f - MIN_SCALE) * eased
		}

	val verticalOffset: Float
		get() {
			val eased = if (opening) Easing.cubicEaseOut(progress) else Easing.cubicEaseIn(progress)
			return MAX_VERTICAL_OFFSET * (1.0f - eased)
		}

	companion object {
		const val DURATION_SECONDS = 0.28f
		const val MIN_SCALE = 0.96f
		const val MAX_VERTICAL_OFFSET = 8.0f

		fun opening(): GuiAnimation = GuiAnimation(0.0f, opening = true)
	}
}
