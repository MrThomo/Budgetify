package com.budgetify.app.ui.addexpense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.budgetify.app.R
import com.budgetify.app.data.repository.SaveOutcome
import com.budgetify.app.ui.common.categoryLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel,
    onBack: () -> Unit,
    onSaved: (SaveOutcome) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.onReceiptPicked(uri)
    }

    val selectedName = categories.firstOrNull { it.id == viewModel.selectedCategoryId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_expense)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.amountText,
                onValueChange = viewModel::onAmountChange,
                label = { Text(stringResource(R.string.amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            if (categories.isEmpty()) {
                Text(stringResource(R.string.no_categories), color = MaterialTheme.colorScheme.error)
            } else {
                Box {
                    OutlinedButton(onClick = { categoryMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (selectedName != null) categoryLabel(selectedName)
                            else stringResource(R.string.category)
                        )
                    }
                    DropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(categoryLabel(category.name)) },
                                onClick = {
                                    viewModel.onCategorySelected(category.id)
                                    categoryMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.note_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource(R.string.date)}: ${viewModel.date}")
            }

            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (viewModel.receiptUri == null) stringResource(R.string.attach_receipt)
                    else stringResource(R.string.receipt_attached)
                )
            }
            viewModel.receiptUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = stringResource(R.string.receipt_attached),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            viewModel.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = !viewModel.isSaving && categories.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = viewModel.dateMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onDateMillisSelected(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
