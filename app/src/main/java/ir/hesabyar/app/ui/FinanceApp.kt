package ir.hesabyar.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hesabyar.app.data.EntrySource
import ir.hesabyar.app.data.EntryType
import ir.hesabyar.app.data.FinanceEntry
import ir.hesabyar.app.data.FinanceSummary
import ir.hesabyar.app.data.Installment
import ir.hesabyar.app.data.SourceSummary
import ir.hesabyar.app.util.formatJalali
import ir.hesabyar.app.util.formatMoney
import ir.hesabyar.app.util.toAmountOrNull
import java.time.Instant
import java.time.ZoneId

private enum class Destination(val title: String, val icon: ImageVector) {
    DASHBOARD("خلاصه", Icons.Rounded.Dashboard),
    TRANSACTIONS("تراکنش‌ها", Icons.Rounded.ListAlt),
    INSTALLMENTS("اقساط", Icons.Rounded.CreditCard),
    SMS("پیامک بانک", Icons.Rounded.Sms)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(viewModel: MainViewModel) {
    val appContext = LocalContext.current
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val pendingSms by viewModel.pendingSms.collectAsStateWithLifecycle()
    val installments by viewModel.installments.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var destination by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    var showAddEntry by rememberSaveable { mutableStateOf(false) }
    var showAddInstallment by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(notice) {
        val text = notice ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.clearNotice()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.scanSms(appContext, silent = true)
        }
    }

    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("حساب‌یار", fontWeight = FontWeight.Bold)
                            Text(
                                "کاملاً آفلاین",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = "محافظت‌شده",
                            modifier = Modifier.padding(start = 16.dp).size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.title, fontSize = 10.sp) }
                        )
                    }
                }
            },
            floatingActionButton = {
                when (destination) {
                    Destination.DASHBOARD, Destination.TRANSACTIONS ->
                        ExtendedFloatingActionButton(
                            onClick = { showAddEntry = true },
                            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                            text = { Text("ثبت دستی") }
                        )
                    Destination.INSTALLMENTS -> FloatingActionButton(
                        onClick = { showAddInstallment = true }
                    ) { Icon(Icons.Rounded.Add, contentDescription = "قسط جدید") }
                    Destination.SMS -> Unit
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    Destination.DASHBOARD -> DashboardScreen(summary, entries)
                    Destination.TRANSACTIONS -> TransactionsScreen(entries, viewModel::deleteEntry)
                    Destination.INSTALLMENTS -> InstallmentsScreen(
                        installments,
                        viewModel::markInstallmentPaid
                    )
                    Destination.SMS -> SmsScreen(pendingSms, viewModel)
                }
            }
        }
    }

    if (showAddEntry) {
        AddEntryDialog(
            onDismiss = { showAddEntry = false },
            onSave = { type, amount, note, account, category ->
                viewModel.addEntry(type, amount, note, account, category)
                showAddEntry = false
            }
        )
    }
    if (showAddInstallment) {
        AddInstallmentDialog(
            onDismiss = { showAddInstallment = false },
            onSave = { title, lender, account, total, each, count, dueDay ->
                viewModel.addInstallment(title, lender, account, total, each, count, dueDay)
                showAddInstallment = false
            }
        )
    }
}

@Composable
private fun DashboardScreen(summary: FinanceSummary, entries: List<FinanceEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("حساب‌ها با هم مخلوط نمی‌شوند", style = MaterialTheme.typography.titleMedium)
            Text(
                "هر بخش جمع درآمد، خرج و مانده مستقل خودش را دارد.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            SourceSummaryCard(
                title = "ثبت‌های دستی",
                subtitle = "مواردی که خودت وارد کرده‌ای",
                summary = summary.manual,
                accent = MaterialTheme.colorScheme.primary
            )
        }
        item {
            SourceSummaryCard(
                title = "پیامک‌های بانکیِ تأییدشده",
                subtitle = if (summary.pendingSms == 0) "موردی در انتظار بررسی نیست"
                else "${summary.pendingSms} مورد در انتظار بررسی",
                summary = summary.sms,
                accent = MaterialTheme.colorScheme.secondary
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("اقساط فعال", summary.activeInstallments.toString(), Modifier.weight(1f))
                MiniStat("بررسی پیامک", summary.pendingSms.toString(), Modifier.weight(1f))
            }
        }
        val latestManual = entries.filter { it.source == EntrySource.MANUAL }.take(3)
        val latestSms = entries.filter { it.source == EntrySource.SMS }.take(3)
        item { Text("آخرین ثبت‌های دستی", style = MaterialTheme.typography.titleMedium) }
        if (latestManual.isEmpty()) {
            item { EmptyCard("هنوز ثبت دستی وجود ندارد.") }
        } else {
            items(latestManual, key = FinanceEntry::id) { EntryRow(it) }
        }
        item { Text("آخرین پیامک‌های تأییدشده", style = MaterialTheme.typography.titleMedium) }
        if (latestSms.isEmpty()) {
            item { EmptyCard("هنوز پیامک بانکی تأیید نشده است.") }
        } else {
            items(latestSms, key = FinanceEntry::id) { EntryRow(it) }
        }
    }
}

@Composable
private fun SourceSummaryCard(
    title: String,
    subtitle: String,
    summary: SourceSummary,
    accent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(color = accent.copy(alpha = 0.25f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MoneyStat("درآمد", summary.income, Color(0xFF0B7A4B))
                MoneyStat("خرج", summary.expense, Color(0xFFB42318))
            }
            Text(
                "مانده: ${formatMoney(summary.balance)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (summary.balance >= 0) Color(0xFF0B7A4B) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MoneyStat(label: String, amount: Long, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Text(formatMoney(amount), color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionsScreen(entries: List<FinanceEntry>, onDelete: (Long) -> Unit) {
    var filter by rememberSaveable { mutableStateOf(EntrySource.MANUAL) }
    val filtered = entries.filter { it.source == filter }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filter == EntrySource.MANUAL,
                onClick = { filter = EntrySource.MANUAL },
                label = { Text("ثبت دستی") }
            )
            FilterChip(
                selected = filter == EntrySource.SMS,
                onClick = { filter = EntrySource.SMS },
                label = { Text("پیامک تأییدشده") }
            )
        }
        if (filtered.isEmpty()) {
            EmptyCard("در این بخش تراکنشی وجود ندارد.")
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = FinanceEntry::id) { entry ->
                    EntryRow(entry, onDelete = { onDelete(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: FinanceEntry, onDelete: (() -> Unit)? = null) {
    val isIncome = entry.type == EntryType.INCOME
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (isIncome) "+ ${formatMoney(entry.amount)}" else "− ${formatMoney(entry.amount)}",
                    color = if (isIncome) Color(0xFF0B7A4B) else Color(0xFFB42318),
                    fontWeight = FontWeight.Bold
                )
                Text(entry.note.ifBlank { entry.category })
                Text(
                    listOf(entry.account, entry.bank, formatJalali(entry.occurredAt))
                        .filterNotNull().filter(String::isNotBlank).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun InstallmentsScreen(
    installments: List<Installment>,
    onPaid: (Installment) -> Unit
) {
    if (installments.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            EmptyCard("قسطی ثبت نشده؛ با دکمه + اولین قسط را اضافه کن.")
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("مدیریت اقساط", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("پرداخت قسط در جمع خرج دستی یا پیامکی وارد نمی‌شود.")
        }
        items(installments, key = Installment::id) { item ->
            InstallmentCard(item, onPaid)
        }
    }
}

@Composable
private fun InstallmentCard(item: Installment, onPaid: (Installment) -> Unit) {
    val progress = if (item.totalCount == 0) 0f else item.paidCount.toFloat() / item.totalCount
    val nextDue = Instant.ofEpochMilli(item.firstDueAt)
        .atZone(ZoneId.systemDefault()).toLocalDate()
        .plusMonths(item.paidCount.toLong())
        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text(if (item.active) "فعال" else "تسویه‌شده") })
            }
            Text("مبلغ هر قسط: ${formatMoney(item.eachAmount)}")
            Text("پرداخت‌شده: ${item.paidCount} از ${item.totalCount}")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            if (item.active) {
                Text("سررسید بعدی: ${formatJalali(nextDue)}")
                Button(onClick = { onPaid(item) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Payments, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت یک قسط پرداخت‌شده")
                }
            }
        }
    }
}

@Composable
private fun SmsScreen(pending: List<FinanceEntry>, viewModel: MainViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.READ_SMS] == true) viewModel.scanSms(context)
    }
    val hasReadPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ورود خودکار از پیامک بانک", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "همه تحلیل‌ها روی گوشی انجام می‌شود. متن کامل پیامک ذخیره یا ارسال نمی‌شود؛ فقط مبلغ، نوع و بانک پس از رمزگذاری نگهداری می‌شود."
                    )
                    Button(
                        onClick = {
                            if (hasReadPermission) viewModel.scanSms(context)
                            else launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Sms, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasReadPermission) "بررسی پیامک‌های ۹۰ روز اخیر" else "دادن دسترسی پیامک")
                    }
                }
            }
        }
        item {
            Text("در انتظار تأیید (${pending.size})", style = MaterialTheme.typography.titleMedium)
            Text(
                "تا وقتی تأیید نکنی، هیچ موردی وارد جمع پیامکی نمی‌شود.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pending.isEmpty()) {
            item { EmptyCard("موردی برای بررسی وجود ندارد.") }
        } else {
            items(pending, key = FinanceEntry::id) { item ->
                PendingSmsCard(item, viewModel::approveSms, viewModel::ignoreSms)
            }
        }
    }
}

@Composable
private fun PendingSmsCard(
    entry: FinanceEntry,
    onApprove: (Long) -> Unit,
    onIgnore: (Long) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.bank ?: "بانک", fontWeight = FontWeight.Bold)
                Text("اطمینان ${entry.confidence}٪", color = MaterialTheme.colorScheme.primary)
            }
            Text(
                (if (entry.type == EntryType.INCOME) "درآمد: " else "خرج: ") + formatMoney(entry.amount),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                listOf(entry.account, formatJalali(entry.occurredAt))
                    .filter(String::isNotBlank).joinToString(" • "),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApprove(entry.id) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تأیید")
                }
                OutlinedButton(onClick = { onIgnore(entry.id) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Block, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("نادیده بگیر")
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AddEntryDialog(
    onDismiss: () -> Unit,
    onSave: (EntryType, Long, String, String, String) -> Unit
) {
    var type by rememberSaveable { mutableStateOf(EntryType.EXPENSE) }
    var amount by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    val parsedAmount = amount.toAmountOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ثبت تراکنش دستی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == EntryType.INCOME,
                        onClick = { type = EntryType.INCOME },
                        label = { Text("درآمد") }
                    )
                    FilterChip(
                        selected = type == EntryType.EXPENSE,
                        onClick = { type = EntryType.EXPENSE },
                        label = { Text("خرج") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("مبلغ به تومان") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("شرح") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی؛ مثل خوراک یا حقوق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = { Text("حساب؛ مثل بلوبانک یا نقدی") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = parsedAmount != null && parsedAmount > 0,
                onClick = { onSave(type, parsedAmount!!, note, account, category) }
            ) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun AddInstallmentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long, Long, Int, Int) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var lender by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    var total by rememberSaveable { mutableStateOf("") }
    var each by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableStateOf("") }
    val totalValue = total.toAmountOrNull() ?: 0L
    val eachValue = each.toAmountOrNull() ?: 0L
    val countValue = count.toAmountOrNull()?.toInt() ?: 0
    val dayValue = dueDay.toAmountOrNull()?.toInt() ?: 0
    val valid = title.isNotBlank() && eachValue > 0 && countValue > 0 && dayValue in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قسط جدید") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FormField(title, { title = it }, "نام قسط؛ مثل وام بانک") }
                item { FormField(lender, { lender = it }, "بانک یا طلبکار") }
                item { FormField(account, { account = it }, "حساب پرداخت") }
                item { NumberField(total, { total = it }, "مبلغ کل به تومان") }
                item { NumberField(each, { each = it }, "مبلغ هر قسط به تومان") }
                item { NumberField(count, { count = it }, "تعداد اقساط") }
                item { NumberField(dueDay, { dueDay = it }, "روز سررسید ماه؛ ۱ تا ۳۱") }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(title, lender, account, totalValue, eachValue, countValue, dayValue)
                }
            ) { Text("ثبت قسط") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun FormField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
