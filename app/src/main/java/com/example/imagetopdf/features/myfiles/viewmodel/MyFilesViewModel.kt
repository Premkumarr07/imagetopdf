package com.example.imagetopdf.features.myfiles.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.features.home.model.PdfFileModel
import com.example.imagetopdf.features.home.repository.PdfFileRepository
import com.example.imagetopdf.features.myfiles.ui.SortMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class MyFilesViewModel : ViewModel() {

    private val _allFiles    = MutableStateFlow<List<PdfFileModel>>(emptyList())
    private val _sortMode    = MutableStateFlow(SortMode.DATE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading   = MutableStateFlow(false)
    private val _pdfFiles    = MutableStateFlow<List<PdfFileModel>>(emptyList())

    val sortMode:    StateFlow<SortMode>         = _sortMode
    val searchQuery: StateFlow<String>           = _searchQuery
    val isLoading:   StateFlow<Boolean>          = _isLoading
    val pdfFiles:    StateFlow<List<PdfFileModel>> = _pdfFiles

    fun addPdfFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)

                val fileName = "PDF_${System.currentTimeMillis()}.pdf"

                val dir = File(
                    context.getExternalFilesDir(null),
                    "ImageToPDF"
                )

                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, fileName)

                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                loadFiles()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _allFiles.value  = PdfFileRepository.loadPdfFiles()
            _isLoading.value = false
        }
    }

    fun onSortChange(mode: SortMode)      { _sortMode.value    = mode  }
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun deleteFile(file: PdfFileModel) {
        viewModelScope.launch(Dispatchers.IO) {
            File(file.path).delete()
            loadFiles()
        }
    }
}