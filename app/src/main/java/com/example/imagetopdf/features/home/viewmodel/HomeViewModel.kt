package com.example.imagetopdf.features.home.viewmodel

import PdfFileRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.features.home.model.PdfFileModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val pdfFiles: List<PdfFileModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel @Inject constructor(
    private val pdfFileRepository: PdfFileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFiles()
    }

    fun loadFiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val files = pdfFileRepository.loadPdfFiles()
                _uiState.value = _uiState.value.copy(
                    pdfFiles = files,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load files"
                )
            }
        }
    }

    fun deleteFile(file: PdfFileModel) {
        viewModelScope.launch {
            pdfFileRepository.deleteFile(file.path)
            loadFiles() // Refresh list
        }
    }

    fun shareFile(file: PdfFileModel) {
        file.uri?.let { uri ->
            pdfFileRepository.sharePdf(uri)
        }
    }

    fun openFile(file: PdfFileModel) {
        file.uri?.let { uri ->
            pdfFileRepository.openPdf(uri)
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}