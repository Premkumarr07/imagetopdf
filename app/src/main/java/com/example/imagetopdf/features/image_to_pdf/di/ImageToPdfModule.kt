// features/image_to_pdf/di/ImageToPdfModule.kt
package com.example.imagetopdf.features.image_to_pdf.di

import com.example.imagetopdf.features.image_to_pdf.data.ImageRepositoryImpl
import com.example.imagetopdf.features.image_to_pdf.data.ImageProcessorImpl
import com.example.imagetopdf.features.image_to_pdf.data.ImageRepository
import com.example.imagetopdf.features.image_to_pdf.data.PdfBuilderImpl
import com.example.imagetopdf.features.image_to_pdf.domain.ImageProcessor
import com.example.imagetopdf.features.image_to_pdf.domain.PdfBuilder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class ImageToPdfModule {

    @Binds
    abstract fun bindImageRepository(
        impl: ImageRepositoryImpl
    ): ImageRepository

    @Binds
    abstract fun bindPdfBuilder(
        impl: PdfBuilderImpl
    ): PdfBuilder

    @Binds
    abstract fun bindImageProcessor(
        impl: ImageProcessorImpl
    ): ImageProcessor
}