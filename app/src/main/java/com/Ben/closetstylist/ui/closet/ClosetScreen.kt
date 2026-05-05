package com.Ben.closetstylist.ui.closet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.Ben.closetstylist.R
import com.Ben.closetstylist.data.ClothingCategory
import com.Ben.closetstylist.data.ClothingItem
import com.Ben.closetstylist.ui.common.SkeletonItemGrid

@Composable
fun ClosetScreen(viewModel: ClosetViewModel, onAddItem: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.closet_add_item))
            }
        },
    ) { innerPadding ->
        when {
            !uiState.isLoaded -> SkeletonItemGrid(modifier = Modifier.padding(innerPadding).fillMaxSize())
            uiState.items.isEmpty() -> EmptyCloset(onAddItem = onAddItem, modifier = Modifier.padding(innerPadding))
            else -> Box(modifier = Modifier.padding(innerPadding)) {
                ItemGrid(items = uiState.items, modifier = Modifier.fillMaxSize())
                FilterRow(activeFilter = uiState.activeFilter, onFilterSelected = viewModel::setFilter)
            }
        }
    }
}

@Composable
private fun FilterRow(
    activeFilter: ClothingCategory?,
    onFilterSelected: (ClothingCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = activeFilter == null,
                onClick = { onFilterSelected(null) },
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(ClothingCategory.entries.toList()) { category ->
            FilterChip(
                selected = activeFilter == category,
                onClick = { onFilterSelected(if (activeFilter == category) null else category) },
                label = { Text(category.displayName()) },
            )
        }
    }
}

@Composable
private fun ItemGrid(
    items: List<ClothingItem>,
    modifier: Modifier = Modifier,
) {
    val filterRowHeight = 56.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = filterRowHeight,
            bottom = 88.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items, key = { it.id }) { item ->
            ClothingItemCard(item = item)
        }
    }
}

@Composable
private fun EmptyCloset(onAddItem: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_empty_closet),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.closet_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.closet_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAddItem) {
            Text(stringResource(R.string.closet_add_first_item))
        }
    }
}

@Composable
private fun ClothingItemCard(item: ClothingItem) {
    Card(modifier = Modifier.aspectRatio(0.75f)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imagePath,
                contentDescription = item.description.ifEmpty { item.category.displayName() },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = item.category.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
