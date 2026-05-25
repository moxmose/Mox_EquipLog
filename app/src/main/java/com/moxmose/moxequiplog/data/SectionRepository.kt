package com.moxmose.moxequiplog.data

import com.moxmose.moxequiplog.data.local.Section
import com.moxmose.moxequiplog.data.local.SectionDao
import kotlinx.coroutines.flow.Flow

class SectionRepository(private val sectionDao: SectionDao) {
    val allSections: Flow<List<Section>> = sectionDao.getAllSections()

    suspend fun insertSection(section: Section): Long = sectionDao.insertSection(section)
    suspend fun updateSection(section: Section) = sectionDao.updateSection(section)
    suspend fun updateSectionList(sectionList: List<Section>) = sectionDao.updateSectionList(sectionList)
    suspend fun deleteSection(section: Section) = sectionDao.deleteSection(section)
    suspend fun getSectionById(id: Int): Section? = sectionDao.getSectionById(id)
    suspend fun getSectionsCount(): Int = sectionDao.getSectionsCount()
}
