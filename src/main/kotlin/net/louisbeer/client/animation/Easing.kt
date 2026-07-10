package net.louisbeer.client.animation

object Easing {
	fun cubicEaseOut(t: Float): Float {
		val inverted = 1.0f - t
		return 1.0f - inverted * inverted * inverted
	}

	fun cubicEaseIn(t: Float): Float = t * t * t

	fun backEaseOut(t: Float): Float {
		val c1 = 1.70158f
		val c3 = c1 + 1.0f
		val shifted = t - 1.0f
		return 1.0f + c3 * shifted * shifted * shifted + c1 * shifted * shifted
	}
}
