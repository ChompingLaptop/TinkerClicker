/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.copy.actions

import android.app.Application

import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.bindings.toActionDetails
import com.buzbuz.smartautoclicker.overlays.base.CopyViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [ActionCopyDialog].
 *
 * @param application the Android application.
 */
class ActionCopyModel(application: Application) : CopyViewModel<Action>(application) {

    /**
     * List of displayed action items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val actionList: Flow<List<ActionCopyItem>?> =
        combine(repository.getAllActions(), itemsFromCurrentContainer, searchQuery) { dbActions, eventActions, query ->
            eventActions ?: return@combine null
            if (query.isNullOrEmpty()) getAllItems(dbActions, eventActions) else getSearchedItems(dbActions, query)
        }

    /**
     * Get all items with the headers.
     * @param dbActions all actions in the database.
     * @param eventActions all actions in the current event.
     * @return the complete list of action items.
     */
    private fun getAllItems(dbActions: List<Action>, eventActions: List<Action>): List<ActionCopyItem> {
        val allItems = mutableListOf<ActionCopyItem>()

        // First, add the actions from the current event
        val eventItems = eventActions
            .sortedBy { it.name }
            .map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
            .distinct()
        if (eventItems.isNotEmpty()) allItems.add(ActionCopyItem.HeaderItem(R.string.dialog_action_copy_header_event))
        allItems.addAll(eventItems)

        // Then, add all other actions. Remove the one already in this event.
        val actions = dbActions
            .map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
            .toMutableList()
            .apply {
                removeIf { allItem ->
                    eventItems.find {
                        allItem.actionDetails.action.id == it.actionDetails.action.id || allItem == it
                    } != null
                }
            }
            .distinct()
        if (actions.isNotEmpty()) allItems.add(ActionCopyItem.HeaderItem(R.string.dialog_action_copy_header_all))
        allItems.addAll(actions)

        return allItems
    }

    /**
     * Get the result of the search query.
     * @param dbActions all actions in the database.
     * @param query the current search query.
     */
    private fun getSearchedItems(dbActions: List<Action>, query: String): List<ActionCopyItem> = dbActions
        .filter { action ->
            action.name!!.contains(query, true)
        }
        .map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
        .distinct()

    /**
     * Get a new action based on the provided one.
     * @param action the acton to copy.
     */
    fun getNewActionForCopy(action: Action): Action =
        when (action) {
            is Action.Click -> action.copy(id = 0, name = "" + action.name)
            is Action.Swipe -> action.copy(id = 0, name = "" + action.name)
            is Action.Pause -> action.copy(id = 0, name = "" + action.name)
            is Action.Intent -> action.copy(id = 0, name = "" + action.name)
        }

    /** Types of items in the action copy list. */
    sealed class ActionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(@StringRes val title: Int) : ActionCopyItem()

        /**
         * Action item.
         * @param actionDetails the details for the action.
         */
        data class ActionItem (val actionDetails: ActionDetails) : ActionCopyItem()
    }
}
