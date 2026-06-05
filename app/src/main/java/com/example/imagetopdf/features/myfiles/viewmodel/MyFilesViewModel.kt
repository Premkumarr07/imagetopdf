package com.example.imagetopdf.features.myfiles.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.features.home.model.PdfFileModel
import com.example.imagetopdf.features.home.repository.PdfFileRepository
import com.example.imagetopdf.features.myfiles.ui.SortMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MyFilesViewModel @Inject constructor(
    private val pdfFileRepository: PdfFileRepository
) : ViewModel() {

    private val _allFiles = MutableStateFlow<List<PdfFileModel>>(emptyList())
    private val _pdfFiles = MutableStateFlow<List<PdfFileModel>>(emptyList())

    private val _sortMode = MutableStateFlow(SortMode.DATE_DESC)
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)

    val pdfFiles: StateFlow<List<PdfFileModel>> = _pdfFiles.asStateFlow()
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true

            val files = pdfFileRepository.loadPdfFiles()

            _allFiles.value = files
            applyFilters()

            _isLoading.value = false
        }
    }
    fun addPdfFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val resolver = context.contentResolver

                val originalName =
                    resolver.query(uri, null, null, null, null)?.use { cursor ->
                        val index = cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )

                        if (cursor.moveToFirst() && index >= 0) {
                            cursor.getString(index)
                        } else {
                            null
                        }
                    } ?: "Imported_${System.currentTimeMillis()}.pdf"

                val bytes =
                    resolver.openInputStream(uri)?.use {
                        it.readBytes()
                    } ?: return@launch

                pdfFileRepository.savePdfToAppFolder(
                    bytes = bytes,
                    fileName = originalName.removeSuffix(".pdf")
                )

                loadFiles()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun onSortChange(mode: SortMode) {
        _sortMode.value = mode
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun deleteFile(file: PdfFileModel) {
        viewModelScope.launch(Dispatchers.IO) {
            pdfFileRepository.deleteFile(file.path)
            loadFiles()
        }
    }

    private fun applyFilters() {
        var files = _allFiles.value

        if (_searchQuery.value.isNotBlank()) {
            files = files.filter {
                it.name.contains(
                    _searchQuery.value,
                    ignoreCase = true
                )
            }
        }

        files = when (_sortMode.value) {
            SortMode.DATE_DESC -> files.sortedByDescending { it.lastModified }
            SortMode.DATE_ASC -> files.sortedBy { it.lastModified }
            SortMode.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortMode.SIZE_DESC -> files.sortedByDescending { it.sizeBytes }
        }

        _pdfFiles.value = files
    }

}
