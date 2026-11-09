package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx

@Composable
fun RoundStep(plus: Boolean, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Tx.Gold.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (plus) Icons.Rounded.Add else Icons.Rounded.Remove, desc, tint = Tx.Gold)
    }
}

/** − value + */
@Composable
fun Stepper(valueText: String, onMinus: () -> Unit, onPlus: () -> Unit, valueColor: Color = Tx.GoldPale, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RoundStep(true, "افزایش", onPlus)
        Text(valueText, color = valueColor, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        RoundStep(false, "کاهش", onMinus)
    }
}

@Composable
fun SwitchRow(label: String, desc: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Tx.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (desc != null) Text(desc, color = Tx.Muted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Tx.Ink,
                checkedTrackColor = Tx.Gold,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A42),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun ChipsRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { i, o -> TxChip(o, i == selected, { onSelect(i) }) }
    }
}

@Composable
fun SettingGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Tx.Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 4.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) { content() }
        }
    }
}
