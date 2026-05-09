package dev.mariinkys.kantan.ui.debug

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mariinkys.kantan.data.local.dao.KanjiDao
import dev.mariinkys.kantan.data.local.dao.TermDao
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    val termDao: TermDao,
    val kanjiDao: KanjiDao
) : ViewModel()