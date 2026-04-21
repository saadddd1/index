package com.example.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ledger.data.AppDatabase
import com.example.ledger.data.AutoBill
import com.example.ledger.data.Item
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemViewModel(private val db: AppDatabase) : ViewModel() {
    private val itemDao = db.itemDao()
    private val autoBillDao = db.autoBillDao()

    val items: StateFlow<List<Item>> = itemDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingBills: StateFlow<List<AutoBill>> = autoBillDao.getPendingBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBills: StateFlow<List<AutoBill>> = autoBillDao.getAllAutoBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(name: String, price: Double, dateMillis: Long, residualValue: Double) {
        viewModelScope.launch {
            itemDao.insertItem(Item(
                name = name, 
                price = price, 
                purchaseDateMillis = dateMillis,
                residualValue = residualValue
            ))
        }
    }

    fun sellItem(item: Item, soldPrice: Double, soldDateMillis: Long) {
        viewModelScope.launch {
            itemDao.updateItem(item.copy(
                isSold = true,
                residualValue = soldPrice,
                soldDateMillis = soldDateMillis
            ))
        }
    }

    fun updateItemDetails(item: Item, newName: String, newPrice: Double, newDateMillis: Long) {
        viewModelScope.launch {
            itemDao.updateItem(item.copy(
                name = newName,
                price = newPrice,
                purchaseDateMillis = newDateMillis
            ))
        }
    }

    fun deleteItem(id: Int) {
        viewModelScope.launch {
            itemDao.deleteItem(id)
        }
    }

    fun dismissAutoBill(bill: AutoBill) {
        viewModelScope.launch {
            autoBillDao.deleteAutoBill(bill.id)
        }
    }

    fun convertBillToItem(bill: AutoBill, itemName: String, residual: Double) {
        viewModelScope.launch {
            autoBillDao.updateAutoBill(bill.copy(isProcessed = true))
            itemDao.insertItem(Item(
                name = itemName,
                price = bill.amount,
                purchaseDateMillis = bill.timestampMillis,
                residualValue = residual
            ))
        }
    }
}

class ItemViewModelFactory(private val db: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ItemViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
