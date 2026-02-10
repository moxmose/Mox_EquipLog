package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MediaRepository(
    private val mediaDao: MediaDao,
    private val categoryDao: CategoryDao,
    private val appColorDao: AppColorDao,
    private val defaultColors: Array<String>,
    private val defaultCategories: Array<String>
) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allMedia: Flow<List<Media>> = mediaDao.getAllMedia()
    val allColors: Flow<List<AppColor>> = appColorDao.getAllColors()

    fun getMediaByCategory(category: String): Flow<List<Media>> = mediaDao.getMediaByCategory(category)

    suspend fun initializeAppData() {
        // 1. Inizializza Colori da Array di Risorse se il DB è vuoto
        if (appColorDao.getAllColors().first().isEmpty()) {
            val colorsToInsert = defaultColors.mapIndexed { index, colorString ->
                val (hex, name) = colorString.split(";")
                AppColor(hexValue = hex, name = name, isDefault = true, displayOrder = index, hidden = false)
            }
            appColorDao.insertAllColors(colorsToInsert)
        }

        // 2. Inizializza Categorie da Array di Risorse se il DB è vuoto
        if (categoryDao.getAllCategories().first().isEmpty()) {
            val categoriesToInsert = defaultCategories.map { categoryString ->
                val (id, name, color) = categoryString.split(";")
                Category(id = id, name = name, color = color)
            }
            categoryDao.insertAllCategories(categoriesToInsert)
        }

        // 3. Inizializza Icone (logica esistente)
        categoryDao.getAllCategories().first().forEach { category ->
            initializeIconsForCategory(category.id)
        }
    }

    private suspend fun initializeIconsForCategory(categoryId: String) {
        val currentMedia = mediaDao.getMediaByCategory(categoryId).first()
        val existingUris = currentMedia.map { it.uri }.toSet()
        var maxOrder = mediaDao.getMaxOrder(categoryId) ?: -1

        val iconsToInsert = mutableListOf<Media>()

        // Assicura che "none" sia presente e sia il primo
        val noneUri = "icon:none"
        if (!existingUris.contains(noneUri)) {
            iconsToInsert.add(Media(uri = noneUri, category = categoryId, mediaType = "ICON", displayOrder = 0, hidden = false))
            maxOrder++
        }

        // Aggiungi le icone mancanti
        val icons = EquipmentIconProvider.getIconsForCategory(categoryId)
        icons.keys.forEach { iconId ->
            val uri = "icon:$iconId"
            if (!existingUris.contains(uri)) {
                iconsToInsert.add(Media(uri = uri, category = categoryId, mediaType = "ICON", displayOrder = ++maxOrder, hidden = false))
            }
        }
        mediaDao.insertAllMedia(iconsToInsert)
    }

    suspend fun addMedia(mediaIdentifier: MediaIdentifier, category: String) {
        val maxOrder = mediaDao.getMaxOrder(category) ?: -1
        val media = when (mediaIdentifier) {
            is MediaIdentifier.Icon -> Media(uri = "icon:${mediaIdentifier.name}", category = category, mediaType = "ICON", displayOrder = maxOrder + 1, hidden = false)
            is MediaIdentifier.Photo -> Media(uri = mediaIdentifier.uri, category = category, mediaType = "IMAGE", displayOrder = maxOrder + 1, hidden = false)
        }
        mediaDao.insertMedia(media)
    }

    suspend fun updateMediaOrder(mediaList: List<Media>) {
        mediaDao.updateAllMedia(mediaList)
    }

    suspend fun removeMedia(uri: String, category: String) {
        val media = mediaDao.getMediaByUriAndCategory(uri, category)
        if (media != null && media.mediaType == "IMAGE") {
            mediaDao.deleteMedia(media)
        }
    }

    suspend fun toggleMediaVisibility(uri: String, category: String) {
        mediaDao.toggleHidden(uri, category)
    }

    suspend fun setCategoryDefault(categoryId: String, mediaIdentifier: MediaIdentifier?) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null) {
            val updatedCategory = when (mediaIdentifier) {
                is MediaIdentifier.Icon -> category.copy(defaultIconIdentifier = mediaIdentifier.name, defaultPhotoUri = null)
                is MediaIdentifier.Photo -> category.copy(defaultIconIdentifier = null, defaultPhotoUri = mediaIdentifier.uri)
                null -> category.copy(defaultIconIdentifier = null, defaultPhotoUri = null) // Reset
            }
            categoryDao.insertCategory(updatedCategory)
        }
    }

    suspend fun updateCategoryColor(categoryId: String, colorHex: String) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null) {
            categoryDao.insertCategory(category.copy(color = colorHex))
        }
    }

    suspend fun addColor(hex: String, name: String) {
        val maxOrder = appColorDao.getMaxOrder() ?: -1
        appColorDao.insertColor(AppColor(hexValue = hex, name = name, isDefault = false, displayOrder = maxOrder + 1, hidden = false))
    }

    suspend fun updateColor(color: AppColor) {
        appColorDao.updateColor(color)
    }

    suspend fun updateColorsOrder(colors: List<AppColor>) {
        appColorDao.updateAllColors(colors)
    }

    suspend fun toggleColorVisibility(id: Long) {
        appColorDao.toggleHidden(id)
    }

    suspend fun deleteColor(color: AppColor) {
        if (!color.isDefault) {
            appColorDao.deleteColor(color)
        }
    }
}
