package de.theiling.neatlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min

// I want a pointed end, so neither BUTT nor ROUND nor SQUARE are right.
fun makeClockHand(sz: Float, len: Float): Path =
    Path().also { with (it) { // why does .also insist on {}?
        val hz = sz / 2
        moveTo(hz, 0F)
        rLineTo(0F, len)
        rLineTo(-hz, hz)
        rLineTo(-hz, -hz)
        rLineTo(0F, -len)
        close()
    }}

class NeatAnalogClock(
    c: Context,
    attrs: AttributeSet):
    View(c, attrs)
{
    override fun onMeasure(specW: Int, specH: Int) =
        setMeasuredDimension(
            resolveSizeAndState(
                paddingLeft + paddingRight + suggestedMinimumWidth,
                specW, 1),
            resolveSizeAndState(
                paddingTop + paddingBottom + suggestedMinimumHeight,
                specH, 1))

    private fun paintOfColor(color: Int): Paint {
        val p = Paint()
        p.style = Paint.Style.FILL
        p.isAntiAlias = true
        p.color = color
        return p
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cal = Calendar.getInstance()

        val hi = cal[Calendar.HOUR]
        val mi = cal[Calendar.MINUTE]
        val mf = mi / 60F
        val hf = (hi + mf) / 12F

        val ox = paddingLeft
        val oy = paddingTop
        val cx = (width  - ox - paddingRight)  / 2F
        val cy = (height - oy - paddingBottom) / 2F
        val r = min(cx, cy)

        val ref = r / 100
        val tsz = ref * 0.75F
        val msz = 2 * ref
        val hsz = 3.5F * ref
        val dot = 2 * ref
        val dut = dot * 0.75F
        val pm = dut * 1.75F

        val pf = paintOfColor(context.mainForeground)
        val pa = paintOfColor(context.accentColor)
        pf.strokeWidth = tsz

        with (canvas) {
            save()
            translate(cx + ox, cy + oy)
            rotate(180F)

            // dial
            for (i in 0..59) {
                val qu = (i % 15) == 0
                val ho = (i % 5) == 0
                val ro = if (qu) r - (dot * 4) else r
                val ri = if (ho) r * 0.85F else r * 0.95F
                save()
                rotate(i * 6F)
                // tick
                drawLine(0F, ro, 0F, ri, pf)

                // dots
                when {
                    i == 0 -> {
                        drawCircle(+pm, r - dut, dut, pa)
                        drawCircle(-pm, r - dut, dut, pa)
                    }
                    qu -> drawCircle(0F, r - dot, dot, pa)
                }
                restore()
            }

            // hour
            save()
            rotate(360F * hf)
            drawPath(makeClockHand(hsz, r * 0.6F), pf)
            restore()

            // minute
            save()
            rotate(360F * mf)
            drawPath(makeClockHand(msz, r * 0.9F), pf)
            restore()

            // center dot
            drawCircle(0F, 0F, hsz / 2, pf)

            restore()
        }
    }

    fun updateTime()
    {
        requestLayout()
        invalidate()
    }
}
