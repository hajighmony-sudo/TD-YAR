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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
                Text("مرکز اعلان‌ها", style = MaterialTheme.typography.headlineSmall)
                Text("${notifications.count { !it.read }} اعلان خوانده‌نشده", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("فعلاً اعلانی وجود ندارد", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium)
                        Text("اعلان‌های ژورنال و یادآوری‌ها اینجا نمایش داده می‌شوند.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.read) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.category.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text(SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()).format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!item.read) Spacer(Modifier.size(7.dp))
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(item.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClick) { Text("باز کردن") }
                if (!item.done && item.category == NotifyCategory.REMINDER) TextButton(onClick = onDone) { Text("انجام شد") }
                if (item.done) Text("تکمیل‌شده", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
