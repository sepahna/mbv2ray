package com.v2ray.ang.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingIdle
import com.v2ray.ang.ui.compose.colorPingSlow
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay

private val HubAccent = Color(0xFF6D5CFF)
private val HubAccentSoft = Color(0xFF9B8CFF)
private val HubCard = Color(0xEB161A26)
private val HubLine = Color(0x14FFFFFF)

@Composable
fun MainHomeHub(
    groups: List<GroupMapItem>,
    selectedGroupIndex: Int,
    isRunning: Boolean,
    statusText: String,
    selectedServerName: String,
    selectedServerMeta: String,
    selectedPingMillis: Long,
    onSelectGroup: (Int) -> Unit,
    onToggleService: () -> Unit,
    onOpenServers: () -> Unit,
    onTestCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            elapsedSec = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(1000)
            elapsedSec += 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (groups.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                itemsIndexed(groups, key = { _, g -> g.id }) { index, group ->
                    val selected = index == selectedGroupIndex
                    Text(
                        text = group.remarks.ifBlank { "Group" },
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (selected) HubAccent.copy(alpha = 0.22f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (selected) HubAccent.copy(alpha = 0.55f) else HubLine,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onSelectGroup(index) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        color = if (selected) Color.White else Color(0xFF8B93A7),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = stringResource(R.string.hub_download),
                value = if (isRunning) stringResource(R.string.hub_speed_live) else "—",
                accent = colorPing,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = stringResource(R.string.hub_upload),
                value = if (isRunning) stringResource(R.string.hub_speed_live) else "—",
                accent = HubAccentSoft,
                modifier = Modifier.weight(1f)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PowerOrbit(
                isRunning = isRunning,
                onToggle = onToggleService,
            )
        }

        Text(
            text = formatElapsed(elapsedSec),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            color = Color.White,
        )
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRunning) colorPing else Color(0xFF8B93A7),
            modifier = Modifier
                .padding(top = 4.dp, bottom = 16.dp)
                .clickable(onClick = onTestCurrent)
                .semantics { contentDescription = statusText }
        )

        SelectedServerCard(
            name = selectedServerName,
            meta = selectedServerMeta,
            pingMillis = selectedPingMillis,
            onClick = onOpenServers,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(HubCard)
            .border(1.dp, HubLine, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = label, color = Color(0xFF8B93A7), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = value,
            color = accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PowerOrbit(
    isRunning: Boolean,
    onToggle: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radii = listOf(size.minDimension * 0.48f, size.minDimension * 0.38f, size.minDimension * 0.28f)
            radii.forEachIndexed { index, radius ->
                drawCircle(
                    color = HubAccent.copy(alpha = 0.16f + index * 0.08f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            if (isRunning) {
                drawCircle(
                    color = HubAccent.copy(alpha = (1f - pulse) * 0.35f),
                    radius = size.minDimension * 0.48f + pulse * 36.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        if (isRunning) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = spin }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawArc(
                    color = HubAccentSoft.copy(alpha = 0.7f),
                    startAngle = -20f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - size.minDimension * 0.28f, center.y - size.minDimension * 0.28f),
                    size = androidx.compose.ui.geometry.Size(size.minDimension * 0.56f, size.minDimension * 0.56f),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        val powerDesc = stringResource(if (isRunning) R.string.acc_stop else R.string.acc_start)
        Box(
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) {
                        Brush.radialGradient(listOf(HubAccentSoft, HubAccent, Color(0xFF4A3FD4)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF2A3142), Color(0xFF171B26)))
                    }
                )
                .clickable(onClick = onToggle)
                .semantics { contentDescription = powerDesc },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_power_24dp),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    text = stringResource(if (isRunning) R.string.vpn_fab_on else R.string.vpn_fab_off),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }
    }
}

@Composable
private fun SelectedServerCard(
    name: String,
    meta: String,
    pingMillis: Long,
    onClick: () -> Unit,
) {
    val pingText = when {
        pingMillis == 0L -> "—"
        pingMillis < 0L -> "—"
        else -> stringResource(R.string.server_test_delay_value, pingMillis)
    }
    val pingColor = when {
        pingMillis <= 0L -> colorPingIdle
        pingMillis >= 350L -> colorPingSlow
        else -> HubAccentSoft
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = HubCard,
        border = BorderStroke(1.dp, HubLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1F2E))
                    .border(1.dp, HubLine, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_subscriptions_24dp),
                    contentDescription = null,
                    tint = HubAccentSoft,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = meta,
                    color = Color(0xFF8B93A7),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = pingText,
                color = pingColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(pingColor.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

private fun formatElapsed(sec: Int): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
