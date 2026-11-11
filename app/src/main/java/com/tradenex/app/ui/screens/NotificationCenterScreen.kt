package com.tradenex.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import com.tradenex.app.notifications.InboxNotification
import com.tradenex.app.notifications.NotificationInbox
import com.tradenex.app.notifications.NotifyCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(onOpen: (String) -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(NotificationInbox.all(context)) }
    var selected by remember { mutableStateOf("all") }
    fun refresh() { notifications = NotificationInbox.all(context) }
    val filtered = notifications.filter { selected == "all" || it.category.key == selected }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("مرکز اعلان‌ها", color = Tx.Text, fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text("${notifications.count { !it.read }} اعلان خوانده‌نشده  •  NEX NOTIFY", fontSize = 10.sp, color = Tx.Gold)
            }
            androidx.compose.material3.IconButton(onClick = { NotificationInbox.markAllRead(context); refresh() }) {
                Icon(Icons.Outlined.DoneAll, contentDescription = "خواندن همه")
            }
            androidx.compose.material3.IconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "تنظیمات اعلان‌ها")
            }
            androidx.compose.material3.IconButton(onClick = { NotificationInbox.clear(context); refresh() }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "پاک‌کردن تاریخچه")
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item { FilterChip(selected == "all", { selected = "all" }, label = { Text("همه") }) }
                    items(NotifyCategory.entries, key = { it.key }) { category ->
                        FilterChip(selected == category.key, { selected = category.key }, label = { Text(category.label) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(42.dp), tint = Tx.Muted)
                        Text("فعلاً اعلانی وجود ندارد", modifier = Modifier.padding(top = 12.dp), color = Tx.Text, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("اعلان‌های ژورنال و یادآوری‌ها اینجا نمایش داده می‌شوند.", color = Tx.Muted, fontSize = 11.sp)
                    }
                }
            }
            items(filtered, key = { it.id }) { item ->
                AnimatedVisibility(visible = true, enter = fadeIn()) {
                    NotificationCard(item, onClick = {
                        NotificationInbox.markRead(context, item.id)
                        refresh()
                        onOpen(item.route)
                    }, onDone = {
                        NotificationInbox.markDone(context, item.id)
                        refresh()
                    })
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(item: InboxNotification, onClick: () -> Unit, onDone: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), border = if (!item.read) Tx.Gold.copy(alpha = 0.48f) else Color.Transparent) {
        Column(Modifier.clickable(onClick = onClick).padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.label.uppercase(), fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, color = Tx.Gold, modifier = Modifier.weight(1f))
                Text(SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()).format(Date(item.createdAt)), fontSize = 10.sp, color = Tx.Muted)
                if (!item.read) Box(Modifier.padding(start = 7.dp).size(7.dp).clip(RoundedCornerShape(50)).background(Tx.Green))
            }
            Text(item.title, color = Tx.Text, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text(item.body, color = Tx.Muted, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TxChip("باز کردن", false, onClick)
                if (!item.done && item.category == NotifyCategory.REMINDER) TxChip("انجام شد", true, onDone)
                if (item.done) Text("تکمیل‌شده", color = Tx.Green, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
    }
}
