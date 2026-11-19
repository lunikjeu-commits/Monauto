package fr.monauto.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val Navy = Color(0xFF101A2B)
private val Panel = Color(0xFF1B2940)
private val Blue = Color(0xFF4EA1FF)
private val Muted = Color(0xFF9AAAC0)
private val Background = Color(0xFF0B1220)

data class Vehicle(
    val brand: String = "",
    val model: String = "",
    val engine: String = "",
    val year: String = "",
    val mileage: String = "",
    val purchasePrice: String = "",
    val purchaseDate: String = ""
)

data class ServiceEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val date: String,
    val mileage: String,
    val parts: String,
    val labor: String,
    val notes: String
) {
    val total: Double get() = (parts.toDoubleOrNull() ?: 0.0) + (labor.toDoubleOrNull() ?: 0.0)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MonAutoApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonAutoApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("mon_auto_data", Context.MODE_PRIVATE) }
    var vehicle by remember { mutableStateOf(loadVehicle(prefs)) }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    var tab by remember { mutableStateOf(0) }
    var showVehicle by remember { mutableStateOf(false) }
    var showEntry by remember { mutableStateOf(false) }

    fun saveVehicle(v: Vehicle) { vehicle = v; prefs.edit().putString("vehicle", vehicleJson(v).toString()).apply() }
    fun saveEntries(list: List<ServiceEntry>) { entries = list; prefs.edit().putString("entries", entriesJson(list).toString()).apply() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Blue, secondary = Color(0xFF48D6B5),
            background = Background, surface = Panel,
            onBackground = Color.White, onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("MON AUTO", fontWeight = FontWeight.Black)
                            Text("Carnet d'entretien automobile", color = Muted, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Navy, contentColor = Color.White) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Accueil") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Historique") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("Véhicule") })
                }
            },
            floatingActionButton = {
                if (tab != 2) FloatingActionButton(onClick = { showEntry = true }, containerColor = Blue, contentColor = Navy) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter une intervention")
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(vehicle, entries, Modifier.padding(padding), onAdd = { showEntry = true }, onVehicle = { showVehicle = true })
                1 -> HistoryScreen(entries, Modifier.padding(padding), onDelete = { id -> saveEntries(entries.filterNot { it.id == id }) })
                else -> VehicleScreen(vehicle, entries, Modifier.padding(padding), onEdit = { showVehicle = true })
            }
        }

        if (showVehicle) {
            VehicleDialog(vehicle, onDismiss = { showVehicle = false }, onSave = {
                saveVehicle(it); showVehicle = false
            })
        }
        if (showEntry) {
            EntryDialog(onDismiss = { showEntry = false }, onSave = {
                saveEntries(listOf(it) + entries); showEntry = false
            })
        }
    }
}

@Composable
fun Dashboard(v: Vehicle, entries: List<ServiceEntry>, modifier: Modifier, onAdd: () -> Unit, onVehicle: () -> Unit) {
    val expenses = entries.sumOf { it.total }
    val purchase = v.purchasePrice.toDoubleOrNull() ?: 0.0
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().clickable { onVehicle() }) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, null, tint = Blue, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(if (v.brand.isBlank() && v.model.isBlank()) "Configure ton véhicule" else "${v.brand} ${v.model}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(listOf(v.engine, v.year, if (v.mileage.isNotBlank()) "${v.mileage} km" else "").filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Ajoute les informations de ton véhicule" }, color = Muted)
                    Text("Modifier les informations  ›", color = Blue, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("ENTRETIEN & RÉPARATIONS", money(expenses), Modifier.weight(1f))
                StatCard("INTERVENTIONS", entries.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("PRIX D'ACHAT", money(purchase), Modifier.weight(1f))
                StatCard("COÛT TOTAL", money(purchase + expenses), Modifier.weight(1f), accent = true)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Dernières interventions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onAdd) { Text("Ajouter +") }
            }
        }
        if (entries.isEmpty()) item { EmptyState("Aucune intervention enregistrée", "Ajoute ta première vidange ou réparation.") }
        items(entries.take(4)) { EntryCard(it) }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = if (accent) Color(0xFF173A55) else Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text(value, color = if (accent) Color(0xFF65D9C1) else Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryScreen(entries: List<ServiceEntry>, modifier: Modifier, onDelete: (String) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Carnet d'entretien", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 12.dp))
        if (entries.isEmpty()) EmptyState("Ton historique est vide", "Les opérations enregistrées apparaîtront ici.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(entries, key = { it.id }) { entry ->
                Column {
                    EntryCard(entry)
                    TextButton(onClick = { onDelete(entry.id) }) { Text("Supprimer cette intervention", color = Color(0xFFFF8A8A)) }
                }
            }
        }
    }
}

@Composable
fun EntryCard(e: ServiceEntry) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(e.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(money(e.total), color = Color(0xFF65D9C1), fontWeight = FontWeight.Bold)
            }
            Text("${e.category}  •  ${e.date}", color = Blue, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
            if (e.mileage.isNotBlank()) Text("${e.mileage} km", color = Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Pièces : ${money(e.parts.toDoubleOrNull() ?: 0.0)}", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Main-d'œuvre : ${money(e.labor.toDoubleOrNull() ?: 0.0)}", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            if (e.notes.isNotBlank()) Text(e.notes, color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun VehicleScreen(v: Vehicle, entries: List<ServiceEntry>, modifier: Modifier, onEdit: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mon véhicule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VehicleLine("Marque", v.brand)
                VehicleLine("Modèle", v.model)
                VehicleLine("Motorisation", v.engine)
                VehicleLine("Année", v.year)
                VehicleLine("Kilométrage", if (v.mileage.isBlank()) "Non renseigné" else "${v.mileage} km")
                VehicleLine("Date d'achat", v.purchaseDate)
                VehicleLine("Prix d'achat", money(v.purchasePrice.toDoubleOrNull() ?: 0.0))
                HorizontalDivider(color = Muted.copy(alpha = .25f))
                VehicleLine("Total entretien", money(entries.sumOf { it.total }))
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Modifier mon véhicule") }
            }
        }
    }
}

@Composable
fun VehicleLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted)
        Text(value.ifBlank { "Non renseigné" }, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, null, tint = Blue, modifier = Modifier.size(32.dp))
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDialog(v: Vehicle, onDismiss: () -> Unit, onSave: (Vehicle) -> Unit) {
    var brand by remember { mutableStateOf(v.brand) }
    var model by remember { mutableStateOf(v.model) }
    var engine by remember { mutableStateOf(v.engine) }
    var year by remember { mutableStateOf(v.year) }
    var mileage by remember { mutableStateOf(v.mileage) }
    var price by remember { mutableStateOf(v.purchasePrice) }
    var date by remember { mutableStateOf(v.purchaseDate) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Informations du véhicule") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 440.dp)) {
            item { FormField("Marque", brand) { brand = it } }
            item { FormField("Modèle", model) { model = it } }
            item { FormField("Motorisation (ex. 1.4 HDi)", engine) { engine = it } }
            item { FormField("Année", year, KeyboardType.Number) { year = it } }
            item { FormField("Kilométrage actuel", mileage, KeyboardType.Number) { mileage = it } }
            item { FormField("Prix d'achat (€)", price, KeyboardType.Decimal) { price = it } }
            item { FormField("Date d'achat (JJ/MM/AAAA)", date) { date = it } }
        }
    }, confirmButton = { TextButton(onClick = { onSave(Vehicle(brand, model, engine, year, mileage, price, date)) }) { Text("Enregistrer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDialog(onDismiss: () -> Unit, onSave: (ServiceEntry) -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Entretien") }
    var date by remember { mutableStateOf("") }
    var mileage by remember { mutableStateOf("") }
    var parts by remember { mutableStateOf("0") }
    var labor by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    val categories = listOf("Entretien", "Vidange", "Freinage", "Moteur", "Transmission", "Pneumatiques", "Électricité", "Carrosserie", "Autre")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nouvelle intervention") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 490.dp)) {
            item { FormField("Intervention (ex. vidange moteur)", title) { title = it } }
            item {
                Text("Catégorie", color = Muted, style = MaterialTheme.typography.labelSmall)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = category, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) }
                    }
                }
            }
            item { FormField("Date (JJ/MM/AAAA)", date) { date = it } }
            item { FormField("Kilométrage", mileage, KeyboardType.Number) { mileage = it } }
            item { FormField("Coût des pièces (€)", parts, KeyboardType.Decimal) { parts = it } }
            item { FormField("Main-d'œuvre (€)", labor, KeyboardType.Decimal) { labor = it } }
            item { FormField("Notes / pièces remplacées", notes) { notes = it } }
        }
    }, confirmButton = {
        TextButton(onClick = { if (title.isNotBlank()) onSave(ServiceEntry(title = title, category = category, date = date, mileage = mileage, parts = parts, labor = labor, notes = notes)) }, enabled = title.isNotBlank()) { Text("Enregistrer") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } })
}

@Composable
fun FormField(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale.FRANCE).format(value)

fun vehicleJson(v: Vehicle) = JSONObject().apply {
    put("brand", v.brand); put("model", v.model); put("engine", v.engine); put("year", v.year)
    put("mileage", v.mileage); put("purchasePrice", v.purchasePrice); put("purchaseDate", v.purchaseDate)
}
fun loadVehicle(prefs: android.content.SharedPreferences): Vehicle = try {
    val o = JSONObject(prefs.getString("vehicle", "{}") ?: "{}")
    Vehicle(o.optString("brand"), o.optString("model"), o.optString("engine"), o.optString("year"), o.optString("mileage"), o.optString("purchasePrice"), o.optString("purchaseDate"))
} catch (_: Exception) { Vehicle() }

fun entriesJson(entries: List<ServiceEntry>) = JSONArray().apply {
    entries.forEach { e -> put(JSONObject().apply {
        put("id", e.id); put("title", e.title); put("category", e.category); put("date", e.date)
        put("mileage", e.mileage); put("parts", e.parts); put("labor", e.labor); put("notes", e.notes)
    }) }
}
fun loadEntries(prefs: android.content.SharedPreferences): List<ServiceEntry> = try {
    val a = JSONArray(prefs.getString("entries", "[]") ?: "[]")
    (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        ServiceEntry(o.optString("id"), o.optString("title"), o.optString("category"), o.optString("date"), o.optString("mileage"), o.optString("parts"), o.optString("labor"), o.optString("notes"))
    }
} catch (_: Exception) { emptyList() }
