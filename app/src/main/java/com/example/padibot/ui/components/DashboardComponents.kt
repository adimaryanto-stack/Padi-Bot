package com.example.padibot.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.padibot.theme.*

@Composable
fun ActionDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun TelemetryMetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Gray600)
        }
    }
}

@Composable
fun StepIndicatorHeader(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepDot(step = 1, title = "Parameter", isDone = currentStep >= 1, isActive = currentStep == 1)
        HorizontalDivider(modifier = Modifier.width(40.dp), color = if (currentStep >= 2) Green700 else Gray300, thickness = 2.dp)
        StepDot(step = 2, title = "Preview", isDone = currentStep >= 2, isActive = currentStep == 2)
        HorizontalDivider(modifier = Modifier.width(40.dp), color = if (currentStep >= 3) Green700 else Gray300, thickness = 2.dp)
        StepDot(step = 3, title = "Eksekusi", isDone = currentStep >= 3, isActive = currentStep == 3)
    }
}

@Composable
fun StepDot(step: Int, title: String, isDone: Boolean, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDone) Green700 else Gray200,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$step",
                    color = if (isDone) Color.White else Gray700,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Green900 else Gray600
        )
    }
}
