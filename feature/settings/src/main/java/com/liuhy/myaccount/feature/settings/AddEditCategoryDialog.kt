package com.liuhy.myaccount.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.liuhy.myaccount.core.common.getIconVector
import com.liuhy.myaccount.core.common.parseColorHex
import com.liuhy.myaccount.core.data.model.Category
import com.liuhy.myaccount.core.data.model.TransactionType

/**
 * 添加/编辑分类对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategoryDialog(
    isEdit: Boolean,
    initialCategory: Category?,
    parentCategories: List<Category>,
    onCategorySaved: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(initialCategory?.iconName ?: "add") }
    var selectedColor by remember { mutableStateOf(initialCategory?.colorHex ?: "#2196F3") }
    var selectedType by remember { mutableStateOf(initialCategory?.type ?: TransactionType.EXPENSE) }
    var selectedParentId by remember { mutableStateOf<Long?>(initialCategory?.parentId) }
    
    var showIconPicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showParentDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) {
                    if (selectedParentId == null) "编辑一级分类" else "编辑二级分类"
                } else {
                    if (selectedParentId == null) "添加一级分类" else "添加二级分类"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 分类名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 图标选择（仅一级分类需要）
                if (selectedParentId == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("图标", modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { showIconPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconVector(selectedIcon),
                                contentDescription = null,
                                tint = parseColorHex(selectedColor),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 颜色选择
                Text("颜色", modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf("#F44336", "#E91E63", "#9C27B0", "#2196F3", "#00BCD4", "#4CAF50", "#FF9800", "#FF5722", "#795548", "#607D8B")
                    colors.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(parseColorHex(colorHex), CircleShape)
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == colorHex) {
                                Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 收支类型（只有一级分类可选）
                if (selectedParentId == null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedType == TransactionType.EXPENSE) "支出" else "收入",
                            onValueChange = { },
                            label = { Text("收支类型") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showTypeDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("支出") },
                                onClick = {
                                    selectedType = TransactionType.EXPENSE
                                    showTypeDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("收入") },
                                onClick = {
                                    selectedType = TransactionType.INCOME
                                    showTypeDropdown = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 父分类选择（添加二级分类时）
                if (!isEdit || initialCategory?.parentId != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val parentName = parentCategories.find { it.id == selectedParentId }?.name ?: "无（一级分类）"
                        OutlinedTextField(
                            value = parentName,
                            onValueChange = { },
                            label = { Text("父分类") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showParentDropdown = true }
                        )
                        DropdownMenu(
                            expanded = showParentDropdown,
                            onDismissRequest = { showParentDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("无（一级分类）") },
                                onClick = {
                                    selectedParentId = null
                                    showParentDropdown = false
                                }
                            )
                            parentCategories.forEach { parent ->
                                DropdownMenuItem(
                                    text = { Text(parent.name) },
                                    onClick = {
                                        selectedParentId = parent.id
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val category = Category(
                        id = initialCategory?.id ?: 0,
                        name = name,
                        iconName = selectedIcon,
                        colorHex = selectedColor,
                        type = selectedType,
                        parentId = selectedParentId,
                        isDefault = initialCategory?.isDefault ?: false,
                        sortOrder = initialCategory?.sortOrder ?: 0
                    )
                    onCategorySaved(category)
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 图标选择器
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = selectedIcon,
            onIconSelected = { selectedIcon = it },
            onDismiss = { showIconPicker = false }
        )
    }
}
