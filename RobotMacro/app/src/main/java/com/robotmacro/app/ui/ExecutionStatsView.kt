package com.robotmacro.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.robotmacro.app.data.entity.ExecutionLogEntity
import com.robotmacro.app.data.entity.ExecutionStatus

class ExecutionStatsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var logs: List<ExecutionLogEntity> = emptyList()

    fun setLogs(value: List<ExecutionLogEntity>) { logs = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val completed = logs.count { it.status == ExecutionStatus.COMPLETED }.coerceAtLeast(0)
        val failed = logs.count { it.status == ExecutionStatus.ERROR }
        val total = (completed + failed).coerceAtLeast(1)
        val barWidth = width / 2f - 24f
        val maxHeight = height - 32f
        drawBar(canvas, 16f, maxHeight * completed / total, Color.rgb(76, 175, 80), "OK $completed")
        drawBar(canvas, 32f + barWidth, maxHeight * failed / total, Color.rgb(244, 67, 54), "ERR $failed")
    }

    private fun drawBar(canvas: Canvas, left: Float, barHeight: Float, color: Int, label: String) {
        val top = height - barHeight - 20f
        val right = left + width / 2f - 32f
        paint.color = color
        canvas.drawRoundRect(left, top, right, height - 20f, 12f, 12f, paint)
        paint.color = Color.WHITE
        paint.textSize = 26f
        canvas.drawText(label, left, height - 4f, paint)
    }
}
