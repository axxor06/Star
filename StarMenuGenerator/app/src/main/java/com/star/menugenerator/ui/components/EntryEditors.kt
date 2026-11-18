package com.star.menugenerator.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.star.menugenerator.domain.Entry
import com.star.menugenerator.domain.EntryType
import com.star.menugenerator.domain.Section
import com.star.menugenerator.domain.blankSection
import com.star.menugenerator.domain.entryTotal
import com.star.menugenerator.domain.newId
import com.star.menugenerator.domain.swapped
import com.star.menugenerator.ui.theme.StarDeep
import com.star.menugenerator.ui.theme.StarPurple

private val Words = KeyboardCapitalization.Words

private fun badgeFor(type: EntryType) = when (type) {
    EntryType.INDIVIDUAL -> "Individual item"
    EntryType.PACKAGE -> "Package / Sadhya"
    EntryType.BUFFET -> "Buffet"
}

/** Card around one entry with type badge and move / duplicate / delete actions. */
@Composable
fun EntryShell(
    entry: Entry, first: Boolean, last: Boolean,
    onMove: (Int) -> Unit, onDup: () -> Unit, onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) {
        ConfirmDialog(
            title = "Delete this entry?",
            message = (if (entry.name.trim().isNotEmpty()) "“${entry.name.trim()}”" else "This entry") + " will be removed from the menu.",
            onConfirm = onDelete, onDismiss = { confirm = false },
        )
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TypeBadge(badgeFor(entry.type))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onMove(-1) }, enabled = !first, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onMove(1) }, enabled = !last, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDup, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate entry", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { confirm = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
            content()
        }
    }
}

@Composable
fun IndividualEditor(entry: Entry, errs: Map<String, String>, onChange: (Entry) -> Unit) {
    var more by remember(entry.id) {
        mutableStateOf(entry.unit.isNotEmpty() || entry.description.isNotEmpty() || entry.note.isNotEmpty())
    }
    fun er(f: String): String? = errs["${entry.id}:$f"]
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StarField("Item name", entry.name, { onChange(entry.copy(name = it)) }, error = er("name"), placeholder = "e.g. Dosa", capitalization = Words)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StarField(
                "Quantity", entry.quantity, { onChange(entry.copy(quantity = it)) },
                modifier = Modifier.weight(1f), error = er("quantity"), placeholder = "e.g. 100", keyboard = KeyboardType.Decimal,
            )
            StarField(
                "Price per unit (₹)", entry.unitPrice, { onChange(entry.copy(unitPrice = it)) },
                modifier = Modifier.weight(1f), error = er("unitPrice"), placeholder = "e.g. 5", keyboard = KeyboardType.Decimal,
            )
        }
        TotalLine(entryTotal(entry), "Total:")
        TextButton(onClick = { more = !more }) {
            Icon(if (more) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("More details")
        }
        if (more) {
            StarField("Unit (optional)", entry.unit, { onChange(entry.copy(unit = it)) }, error = er("unit"), hint = "Shown after the quantity, e.g. ltr, kg, pieces")
            StarField("Description (optional)", entry.description, { onChange(entry.copy(description = it)) }, error = er("description"))
            StarField("Note (optional)", entry.note, { onChange(entry.copy(note = it)) }, error = er("note"))
        }
    }
}

@Composable
fun PackageEditor(entry: Entry, errs: Map<String, String>, onChange: (Entry) -> Unit) {
    fun er(f: String): String? = errs["${entry.id}:$f"]
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StarField("Package name", entry.name, { onChange(entry.copy(name = it)) }, error = er("name"), placeholder = "e.g. Chicken Biriyani or Onam Sadhya", capitalization = Words)
        StarField(
            "Included items", entry.itemsText, { onChange(entry.copy(itemsText = it)) },
            error = er("items"), hint = "Separate items with commas (or paste one per line)",
            placeholder = "Salad, Chicken Biriyani, Chilli Chicken, Ice Cream",
            singleLine = false, minLines = 3, capitalization = Words,
        )
        ItemChips(entry.itemsText) { onChange(entry.copy(itemsText = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StarField(
                "Price per plate (₹)", entry.pricePerPlate, { onChange(entry.copy(pricePerPlate = it)) },
                modifier = Modifier.weight(1f), error = er("pricePerPlate"), placeholder = "e.g. 180", keyboard = KeyboardType.Decimal,
            )
            StarField(
                "Number of plates", entry.plateCount, { onChange(entry.copy(plateCount = it)) },
                modifier = Modifier.weight(1f), error = er("plateCount"), hint = "Optional",
                placeholder = "e.g. 100", keyboard = KeyboardType.Number,
            )
        }
        TotalLine(entryTotal(entry), "Package total:")
    }
}

@Composable
fun BuffetEditor(entry: Entry, errs: Map<String, String>, onChange: (Entry) -> Unit) {
    fun er(id: String, f: String): String? = errs["$id:$f"]
    fun setSections(s: List<Section>) = onChange(entry.copy(sections = s))
    fun upd(id: String, patch: (Section) -> Section) = setSections(entry.sections.map { if (it.id == id) patch(it) else it })
    var deleteIndex by remember { mutableStateOf(-1) }
    if (deleteIndex >= 0) {
        val s = entry.sections.getOrNull(deleteIndex)
        ConfirmDialog(
            title = "Delete this section?",
            message = "“${s?.name?.ifEmpty { "Untitled" } ?: "Untitled"}” and its items will be removed.",
            onConfirm = { setSections(entry.sections.filterIndexed { x, _ -> x != deleteIndex }) },
            onDismiss = { deleteIndex = -1 },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StarField("Buffet name", entry.name, { onChange(entry.copy(name = it)) }, error = er(entry.id, "name"), placeholder = "e.g. Grand Dinner Buffet", capitalization = Words)
        entry.sections.forEachIndexed { i, s ->
            Surface(
                shape = MaterialTheme.shapes.medium, color = Color.White,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Section ${i + 1}", fontWeight = FontWeight.SemiBold, color = StarDeep, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { setSections(entry.sections.swapped(i, -1)) }, enabled = i > 0, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Move section up", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { setSections(entry.sections.swapped(i, 1)) }, enabled = i < entry.sections.lastIndex, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Move section down", modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = {
                                val c = entry.sections.toMutableList()
                                c.add(i + 1, s.copy(id = newId()))
                                setSections(c)
                            },
                            modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate section", modifier = Modifier.size(18.dp)) }
                        IconButton(
                            onClick = {
                                if (s.itemsText.trim().isEmpty()) setSections(entry.sections.filterIndexed { x, _ -> x != i }) else deleteIndex = i
                            },
                            modifier = Modifier.size(32.dp),
                        ) { Icon(Icons.Filled.Delete, contentDescription = "Delete section", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    }
                    StarField("Section name", s.name, { v -> upd(s.id) { it.copy(name = v) } }, error = er(s.id, "name"), placeholder = "e.g. Welcome Drinks", capitalization = Words)
                    StarField(
                        "Items", s.itemsText, { v -> upd(s.id) { it.copy(itemsText = v) } },
                        error = er(s.id, "items"), hint = "Separate items with commas",
                        placeholder = "Fresh Lime, Mojito, Watermelon Juice", singleLine = false, minLines = 2, capitalization = Words,
                    )
                    ItemChips(s.itemsText) { t -> upd(s.id) { it.copy(itemsText = t) } }
                }
            }
        }
        TextButton(onClick = { setSections(entry.sections + blankSection()) }) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add section", color = StarPurple)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StarField(
                "Buffet price per plate (₹)", entry.pricePerPlate, { onChange(entry.copy(pricePerPlate = it)) },
                modifier = Modifier.weight(1f), error = er(entry.id, "pricePerPlate"), placeholder = "e.g. 450", keyboard = KeyboardType.Decimal,
            )
            StarField(
                "Number of plates", entry.plateCount, { onChange(entry.copy(plateCount = it)) },
                modifier = Modifier.weight(1f), error = er(entry.id, "plateCount"), hint = "Optional",
                placeholder = "e.g. 100", keyboard = KeyboardType.Number,
            )
        }
        TotalLine(entryTotal(entry), "Buffet total:")
    }
}

@Composable
fun EntryEditor(entry: Entry, errs: Map<String, String>, onChange: (Entry) -> Unit) {
    when (entry.type) {
        EntryType.INDIVIDUAL -> IndividualEditor(entry, errs, onChange)
        EntryType.PACKAGE -> PackageEditor(entry, errs, onChange)
        EntryType.BUFFET -> BuffetEditor(entry, errs, onChange)
    }
}
