/*
 * Copyright (C) 2023 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief

import android.app.Application
import android.graphics.Point
import android.graphics.PointF

import androidx.core.graphics.toPointF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.domain.DumbEditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.DumbActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.toDumbActionDetails

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DumbScenarioBriefViewModel(application: Application): AndroidViewModel(application) {

    private val dumbEditionRepository = DumbEditionRepository.getInstance(application)

    val visualizedActions: Flow<List<DumbActionDetails>> = dumbEditionRepository.editedDumbScenario
        .map { scenario ->
            scenario?.dumbActions?.map { dumbAction ->
                dumbAction.toDumbActionDetails(
                    context = application,
                    withPositions = false,
                )
            }
        }
        .filterNotNull()

    private val actionListSnapIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val focusedActionDetails: Flow<FocusedActionDetails> = dumbEditionRepository.editedDumbScenario
        .combine(actionListSnapIndex) { dumbScenario, index ->
            if (dumbScenario == null || index < 0)
                return@combine null

            if (index >= dumbScenario.dumbActions.size) FocusedActionDetails(isEmpty = true)
            else dumbScenario.toFocusedActionDetails(index)
        }.filterNotNull()

    fun onNewActionListSnapIndex(index: Int) {
        actionListSnapIndex.value = index
    }

    fun createNewDumbClick(position: Point): DumbAction.DumbClick =
        dumbEditionRepository.dumbActionBuilder.createNewDumbClick(getApplication(), position)

    fun createNewDumbSwipe(from: Point, to: Point): DumbAction.DumbSwipe =
        dumbEditionRepository.dumbActionBuilder.createNewDumbSwipe(getApplication(), from, to)

    fun createNewDumbPause(): DumbAction.DumbPause =
        dumbEditionRepository.dumbActionBuilder.createNewDumbPause(getApplication())

    fun addNewDumbAction(dumbAction: DumbAction, index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.addNewDumbAction(
                dumbAction = dumbAction,
                insertionIndex = index,
            )
        }
    }

    fun updateDumbAction(dumbAction: DumbAction) {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.updateDumbAction(dumbAction)
        }
    }

    fun deleteDumbAction(dumbAction: DumbAction) {
        viewModelScope.launch(Dispatchers.IO) {
            dumbEditionRepository.deleteDumbAction(dumbAction)
        }
    }
}

private fun DumbScenario.toFocusedActionDetails(focusIndex: Int): FocusedActionDetails {
    val description = when (val dumbAction = dumbActions[focusIndex]) {
        is DumbAction.DumbClick -> DumbActionDescription.Click(
            position = dumbAction.position.toPointF(),
            pressDurationMs = dumbAction.pressDurationMs,
        )

        is DumbAction.DumbSwipe -> DumbActionDescription.Swipe(
            from = dumbAction.fromPosition.toPointF(),
            to = dumbAction.toPosition.toPointF(),
            swipeDurationMs = dumbAction.swipeDurationMs,
        )

        is DumbAction.DumbPause -> DumbActionDescription.Pause(
            pauseDurationMs = dumbAction.pauseDurationMs,
        )
    }

    return FocusedActionDetails(focusIndex, dumbActions.size, description)
}

data class FocusedActionDetails(
    val actionIndex: Int = 0,
    val actionCount: Int = 0,
    val actionDescription: DumbActionDescription? = null,
    val isEmpty: Boolean = false,
)

sealed class DumbActionDescription {
    data class Click(
        val position: PointF,
        val pressDurationMs: Long,
    ) : DumbActionDescription()

    data class Swipe(
        val from: PointF,
        val to: PointF,
        val swipeDurationMs: Long,
    ) : DumbActionDescription()

    data class Pause(
        val pauseDurationMs: Long,
    ) : DumbActionDescription()
}