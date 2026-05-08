// features/image_to_pdf/viewmodel/PdfBuilderViewModel.kt
package com.example.imagetopdf.features.image_to_pdf.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imagetopdf.features.image_to_pdf.data.ImageRepository
import com.example.imagetopdf.features.image_to_pdf.domain.ConvertResult
import com.example.imagetopdf.features.image_to_pdf.domain.PdfSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PdfBuilderViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PdfBuilderState())
    val state: StateFlow<PdfBuilderState> = _state.asStateFlow()

    data class PdfBuilderState(
        val selectedUris: List<Uri> = emptyList(),
        val convertResult: ConvertResult = ConvertResult.Idle,
        val pdfName: String = "document_${System.currentTimeMillis()}",
        val selectedSize: String = "A4",
        val selectedQuality: String = "High",
        val progress: Float = 0f
    )

    fun addImages(uris: List<Uri>) {
        _state.update { current ->
            current.copy(
                selectedUris = (current.selectedUris + uris).distinct(),
                convertResult = ConvertResult.Idle
            )
        }
    }

    fun removeImage(uri: Uri) {
        _state.update { current ->
            val updated = current.selectedUris - uri
            current.copy(
                selectedUris = updated,
                convertResult = if (updated.isEmpty()) ConvertResult.Idle else current.convertResult
            )
        }
    }

    fun updatePdfName(name: String) {
        _state.update { it.copy(pdfName = name) }
    }

    fun updatePageSize(size: String) {
        _state.update { it.copy(selectedSize = size) }
    }

    fun updateQuality(quality: String) {
        _state.update { it.copy(selectedQuality = quality) }
    }

    fun convertToPdf() {
        viewModelScope.launch {
            val current = _state.value
            if (current.selectedUris.isEmpty()) return@launch

            _state.update { it.copy(convertResult = ConvertResult.Converting(0f), progress = 0f) }

            // Simulate progress updates
            val progressJob = launch {
                var progress = 0f
                while (progress < 0.9f) {
                    kotlinx.coroutines.delay(100)
                    progress += 0.05f
                    _state.update { it.copy(progress = progress) }
                }
            }

            val settings = PdfSettings(
                pageSize = current.selectedSize,
                quality = current.selectedQuality,
                fileName = current.pdfName.ifBlank { "document" }
            )

            val result = imageRepository.convertImagesToPdf(current.selectedUris, settings)
            progressJob.cancel()

            _state.update { it.copy(convertResult = result, progress = 1f) }
        }
    }

    fun reset() {
        _state.update { PdfBuilderState() }
    }
}