package com.zhuri.daybyday.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhuri.daybyday.model.Task
import com.zhuri.daybyday.ui.theme.Zhuri

/**
 * 任务行。规格见 docs/03 6.1：18dp 圆角、surface 底 + 1dp 描边、内边距 16/14。
 */
@Composable
fun TaskRow(
    task: Task,
    done: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Zhuri.colors
    val shape = RoundedCornerShape(18.dp)

    val background by animateColorAsState(
        targetValue = if (done) colors.surfaceAlt else colors.surface,
        animationSpec = tween(140),
        label = "rowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (done) Color.Transparent else colors.line,
        animationSpec = tween(140),
        label = "rowBorder"
    )
    val titleColor by animateColorAsState(
        targetValue = if (done) colors.ink3 else colors.ink,
        animationSpec = tween(140),
        label = "titleColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        CheckCircle(
            checked = done,
            onToggle = onToggle,
            modifier = Modifier.padding(top = 6.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 14.dp, bottom = 14.dp)
        ) {
            Text(
                text = task.title,
                style = Zhuri.type.body,
                color = titleColor,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (task.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.note,
                    style = Zhuri.type.bodySm,
                    color = if (done) colors.ink3 else colors.ink2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = task.metaLabel()
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = meta,
                    style = Zhuri.type.caption,
                    color = colors.ink3
                )
            }
        }
    }
}
