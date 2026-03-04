package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.*
import com.moxmose.moxequiplog.ui.options.EquipmentIconProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ImageRepository(
    private val imageDao: ImageDao,
    private val categoryDao: CategoryDao,
    private val appColorDao: AppColorDao,
    private val appPreferenceDao: AppPreferenceDao,
    private val defaultColors: Array<String>,
    private val defaultCategories: Array<String>
) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val allImages: Flow<List<Image>> = imageDao.getAllImages()
    val allColors: Flow<List<AppColor>> = appColorDao.getAllColors()

    fun getImagesByCategory(category: String): Flow<List<Image>> = imageDao.getImagesByCategory(category)

    fun getCategoryColor(categoryId: String): Flow<String?> = 
        appPreferenceDao.getPreferenceFlow("default_color_$categoryId")

    fun getCategoryDefaultIcon(categoryId: String): Flow<String?> = 
        appPreferenceDao.getPreferenceFlow("default_icon_$categoryId")

    fun getCategoryDefaultPhoto(categoryId: String): Flow<String?> = 
        appPreferenceDao.getPreferenceFlow("default_photo_$categoryId")

    suspend fun initializeAppData() {
        // 1. Inizializza Colori
        if (appColorDao.getAllColors().first().isEmpty()) {
            val colorsToInsert = defaultColors.mapIndexed { index, colorString ->
                val (hex, name) = colorString.split(";")
                AppColor(hexValue = hex, name = name, isDefault = true, displayOrder = index, hidden = false)
            }
            appColorDao.insertAllColors(colorsToInsert)
        }

        // 2. Inizializza Categorie e relative preferenze
        if (categoryDao.getAllCategories().first().isEmpty()) {
            val categoriesToInsert = defaultCategories.map { categoryString ->
                val (id, name, color) = categoryString.split(";")
                // Salva il colore iniziale come preferenza di default
                appPreferenceDao.insertPreference(AppPreference("default_color_$id", color))
                Category(id = id, name = name)
            }
            categoryDao.insertAllCategories(categoriesToInsert)
        }

        // 3. Inizializza Icone
        categoryDao.getAllCategories().first().forEach { category ->
            initializeIconsForCategory(category.id)
        }
    }

    private suspend fun initializeIconsForCategory(categoryId: String) {
        val currentImages = imageDao.getImagesByCategory(categoryId).first()
        val existingUris = currentImages.map { it.uri }.toSet()
        var maxOrder = imageDao.getMaxOrder(categoryId) ?: -1

        val iconsToInsert = mutableListOf<Image>()
        val noneUri = "icon:none"
        if (!existingUris.contains(noneUri)) {
            iconsToInsert.add(Image(uri = noneUri, category = categoryId, imageType = "ICON", displayOrder = 0, hidden = false))
            maxOrder++
        }

        val icons = EquipmentIconProvider.getIconsForCategory(categoryId)
        icons.keys.forEach { iconId ->
            val uri = "icon:$iconId"
            if (!existingUris.contains(uri)) {
                iconsToInsert.add(Image(uri = uri, category = categoryId, imageType = "ICON", displayOrder = ++maxOrder, hidden = false))
            }
        }
        imageDao.insertAllImages(iconsToInsert)
    }

    suspend fun addImage(imageIdentifier: ImageIdentifier, category: String) {
        val maxOrder = imageDao.getMaxOrder(category) ?: -1
        val image = when (imageIdentifier) {
            is ImageIdentifier.Icon -> Image(uri = "icon:${imageIdentifier.name}", category = category, imageType = "ICON", displayOrder = maxOrder + 1, hidden = false)
            is ImageIdentifier.Photo -> Image(uri = imageIdentifier.uri, category = category, imageType = "IMAGE", displayOrder = maxOrder + 1, hidden = false)
        }
        imageDao.insertImage(image)
    }

    suspend fun updateImageOrder(imageList: List<Image>) {
        imageDao.updateAllImages(imageList)
    }

    suspend fun removeImage(image: Image) {
        if (image.imageType == "IMAGE") {
            imageDao.deleteImage(image)
        }
    }

    suspend fun toggleImageVisibility(image: Image) {
        imageDao.toggleHidden(image.uri, image.category)
    }

    suspend fun setCategoryDefault(categoryId: String, imageIdentifier: ImageIdentifier?) {
        when (imageIdentifier) {
            is ImageIdentifier.Icon -> {
                appPreferenceDao.insertPreference(AppPreference("default_icon_$categoryId", imageIdentifier.name))
                appPreferenceDao.deletePreference("default_photo_$categoryId")
            }
            is ImageIdentifier.Photo -> {
                appPreferenceDao.deletePreference("default_icon_$categoryId")
                appPreferenceDao.insertPreference(AppPreference("default_photo_$categoryId", imageIdentifier.uri))
            }
            null -> {
                appPreferenceDao.deletePreference("default_icon_$categoryId")
                appPreferenceDao.deletePreference("default_photo_$categoryId")
            }
        }
    }

    suspend fun updateCategoryColor(categoryId: String, colorHex: String) {
        appPreferenceDao.insertPreference(AppPreference("default_color_$categoryId", colorHex))
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
