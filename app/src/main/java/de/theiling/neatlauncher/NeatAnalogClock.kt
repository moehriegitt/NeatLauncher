package de.theiling.neatlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

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
    var weatherData: WeatherData? = null

    override fun onMeasure(specW: Int, specH: Int) =
        setMeasuredDimension(
            resolveSizeAndState(
                paddingLeft + paddingRight + suggestedMinimumWidth,
                specW, 1),
            resolveSizeAndState(
                paddingTop + paddingBottom + suggestedMinimumHeight,
                specH, 1))

    private fun paintOfColor(color: Int, style: Paint.Style = Paint.Style.FILL): Paint {
        val p = Paint()
        p.style = style
        p.isAntiAlias = true
        p.color = color
        return p
    }

    private fun drawRain(canvas: Canvas, shower: Boolean, rm: Float, a: Float, l: Float, p: Paint) {
        val r = RectF(-rm, -rm, +rm, +rm)
        if (shower) {
            // show it in 7.5min steps, on and off
            val deg = 3.75F
            val cnt = (l / deg).roundToInt()
            for (i in 0..<cnt) {
                val aa = a + ((l / cnt) * i)
                canvas.drawArc(r, aa + (deg/4), (deg/2), false, p)
            }
            return
        }
        canvas.drawArc(r, a, l, false, p)
    }

    private fun drawWeatherData(canvas: Canvas, cal: Calendar, zero: Float, ri: Float, ro: Float) {
        val wd = weatherData ?: return
        val now = cal.time.time
        val start = now - 15 * 60_000L
        val end = now + (9 * 60 * 60_000L)
        var lastEnd = 0L
        val p = paintOfColor(context.accentColor, Paint.Style.STROKE)
        for (d in wd.step) {
            val thisStart = if (d.start.time >= lastEnd) d.start.time else lastEnd
            if ((start >= d.end.time) || (thisStart >= end)) continue
            if (thisStart >= d.end.time) continue
            lastEnd = d.end.time

            val prop = WeatherProp.prop[d.code.wmo] ?: continue
            val intense = prop.level
            if (intense == 0) continue
            val shower = prop.shower

            val a = zero + ((d.start.time - now) / 43200_000F)
            val b = zero + ((d.end.time   - now) / 43200_000F)

            val ru = ri + (((ro - ri) / 4F) * (intense + 1))
            val rm = (ru + ri) / 2
            p.strokeWidth = ru - ri

            drawRain(canvas, shower, rm, (0.25F + a) * 360F, (b - a) * 360F, p)
        }
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
        val al = min(cx, cy)

        val r0 = al * 0.97F
        val r1 = al * 0.95F
        val rr = r1
        val ref = rr / 100
        val tsz = ref * 0.75F
        val msz = 2 * ref
        val hsz = 3.5F * ref
        val dot = 2 * ref
        val dut = dot * 0.75F
        val pm = dut * 1.75F

        val pf = paintOfColor(context.mainForeground)
        val pd = paintOfColor(context.dimBackground)
        val pa = paintOfColor(context.accentColor)
        pf.strokeWidth = tsz

        with (canvas) {
            save()
            translate(cx + ox, cy + oy)
            rotate(180F)

            // face
            drawCircle(0F, 0F, r0, pd)

            // weather first, to be behind anything else (if there's overlap)
            drawWeatherData(canvas, cal, hf, r1, al)

            // dial
            for (i in 0..59) {
                val qu = (i % 15) == 0
                val ho = (i % 5) == 0
                val ro = if (qu) r1 - ((if (i == 0) dut else dot) * 4) else rr
                val ri = if (ho) rr * 0.85F else rr * 0.95F
                save()
                rotate(i * 6F)
                // tick
                drawLine(0F, ro, 0F, ri, pf)

                // dots
                when {
                    i == 0 -> {
                        drawCircle(+pm, r1 - dut, dut, pa)
                        drawCircle(-pm, r1 - dut, dut, pa)
                    }
                    qu -> drawCircle(0F, r1 - dot, dot, pa)
                }
                restore()
            }

            // hour
            save()
            rotate(360F * hf)
            drawPath(makeClockHand(hsz, rr * 0.6F), pf)
            restore()

            // minute
            save()
            rotate(360F * mf)
            drawPath(makeClockHand(msz, rr * 0.9F), pf)
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
