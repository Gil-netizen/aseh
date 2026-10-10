package io.github.gilnetizen.aseh

import io.github.gilnetizen.aseh.core.model.WorkspaceKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordAddress
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceRecordKind
import io.github.gilnetizen.aseh.domain.workspace.WorkspaceSnapshot

/** A device-local search hit that opens the Build workspace which owns the record. */
internal data class WorkspaceSearchEntry(
  val id: String,
  val title: String,
  val detail: String,
  val category: String,
  val workspaceKind: WorkspaceKind,
  val address: WorkspaceRecordAddress,
)

/**
 * Lexical search over user-owned workspace state. The index is rebuilt from the in-memory snapshot
 * so no workspace text is copied to a second store or sent off device.
 */
internal fun searchWorkspace(
  snapshot: WorkspaceSnapshot,
  query: String,
  limit: Int = 30,
): List<WorkspaceSearchEntry> {
  val terms = query.trim()
    .lowercase()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
  if (terms.isEmpty() || limit <= 0) return emptyList()

  fun matches(vararg fields: String): Boolean {
    val searchable = fields.joinToString(" ").lowercase()
    return terms.all(searchable::contains)
  }

  return buildList {
    snapshot.selfWorkspaces.forEach { workspace ->
      if (matches(workspace.label, "self personal practice workspace")) {
        add(
          WorkspaceSearchEntry(
            id = "self:${workspace.id.value}",
            title = workspace.label,
            detail = "Personal practice workspace",
            category = "Self",
            workspaceKind = WorkspaceKind.SELF,
            address = WorkspaceRecordAddress(workspace.id, WorkspaceRecordKind.WORKSPACE),
          ),
        )
      }
      workspace.practiceAdoptions.forEach { practice ->
        if (matches(workspace.label, practice.label, practice.status.name, practice.reviewOn?.toString().orEmpty())) {
          add(
            WorkspaceSearchEntry(
              id = "self:${workspace.id.value}:practice:${practice.id.value}",
              title = practice.label,
              detail = "${workspace.label} · ${practice.status.name.lowercase().replace('_', ' ')}",
              category = "Personal practice",
              workspaceKind = WorkspaceKind.SELF,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.PERSONAL_PRACTICE,
                practice.id,
              ),
            ),
          )
        }
      }
    }

    snapshot.households.forEach { workspace ->
      if (matches(workspace.label, "household workspace")) {
        add(
          WorkspaceSearchEntry(
            id = "household:${workspace.id.value}",
            title = workspace.label,
            detail = "Household workspace",
            category = "Household",
            workspaceKind = WorkspaceKind.HOUSEHOLD,
            address = WorkspaceRecordAddress(workspace.id, WorkspaceRecordKind.WORKSPACE),
          ),
        )
      }
      workspace.responsibilities.forEach { responsibility ->
        if (
          matches(
            workspace.label,
            responsibility.label,
            responsibility.status.name,
            responsibility.dueOn.toString(),
            responsibility.assignedTo?.value.orEmpty(),
          )
        ) {
          add(
            WorkspaceSearchEntry(
              id = "household:${workspace.id.value}:responsibility:${responsibility.id.value}",
              title = responsibility.label,
              detail = "Due ${responsibility.dueOn} · ${workspace.label}",
              category = "Household responsibility",
              workspaceKind = WorkspaceKind.HOUSEHOLD,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.HOUSEHOLD_RESPONSIBILITY,
                responsibility.id,
              ),
            ),
          )
        }
      }
      workspace.calendarItems.forEach { item ->
        if (matches(workspace.label, item.label, item.kind.name, item.status.name, item.startsOn.toString())) {
          add(
            WorkspaceSearchEntry(
              id = "household:${workspace.id.value}:calendar:${item.id.value}",
              title = item.label,
              detail = "${item.startsOn} · ${workspace.label}",
              category = "Household calendar",
              workspaceKind = WorkspaceKind.HOUSEHOLD,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.HOUSEHOLD_CALENDAR_ITEM,
                item.id,
              ),
            ),
          )
        }
      }
      workspace.preparationKits.forEach { kit ->
        if (matches(workspace.label, kit.label, kit.status.name, kit.targetOn.toString())) {
          add(
            WorkspaceSearchEntry(
              id = "household:${workspace.id.value}:kit:${kit.id.value}",
              title = kit.label,
              detail = "Target ${kit.targetOn} · ${workspace.label}",
              category = "Preparation kit",
              workspaceKind = WorkspaceKind.HOUSEHOLD,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.HOUSEHOLD_PREPARATION_KIT,
                kit.id,
              ),
            ),
          )
        }
        kit.tasks.forEach { task ->
          if (matches(workspace.label, kit.label, task.label, task.status.name, task.dueOn.toString())) {
            add(
              WorkspaceSearchEntry(
                id = "household:${workspace.id.value}:kit:${kit.id.value}:task:${task.id.value}",
                title = task.label,
                detail = "${kit.label} · due ${task.dueOn}",
                category = "Preparation task",
                workspaceKind = WorkspaceKind.HOUSEHOLD,
                address = WorkspaceRecordAddress(
                  workspace.id,
                  WorkspaceRecordKind.PREPARATION_KIT_TASK,
                  task.id,
                  kit.id,
                ),
              ),
            )
          }
        }
      }
    }

    snapshot.qahalWorkspaces.forEach { workspace ->
      if (matches(workspace.label, "qahal community workspace")) {
        add(
          WorkspaceSearchEntry(
            id = "qahal:${workspace.id.value}",
            title = workspace.label,
            detail = "Qahal workspace",
            category = "Qahal",
            workspaceKind = WorkspaceKind.QAHAL,
            address = WorkspaceRecordAddress(workspace.id, WorkspaceRecordKind.WORKSPACE),
          ),
        )
      }
      workspace.decisions.forEach { decision ->
        if (
          matches(
            workspace.label,
            decision.label,
            decision.publicSummary,
            decision.authorityScope,
            decision.classification.name,
            decision.status.name,
            decision.dissentSummary.orEmpty(),
          )
        ) {
          add(
            WorkspaceSearchEntry(
              id = "qahal:${workspace.id.value}:decision:${decision.id.value}",
              title = decision.label,
              detail = "${decision.publicSummary} · ${workspace.label}",
              category = "Qahal decision",
              workspaceKind = WorkspaceKind.QAHAL,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.QAHAL_DECISION,
                decision.id,
              ),
            ),
          )
        }
      }
      workspace.volunteerRotations.forEach { rotation ->
        if (matches(workspace.label, rotation.label, rotation.publicRole.value, rotation.status.name)) {
          add(
            WorkspaceSearchEntry(
              id = "qahal:${workspace.id.value}:rotation:${rotation.id.value}",
              title = rotation.label,
              detail = "Role ${rotation.publicRole.value} · ${workspace.label}",
              category = "Volunteer rotation",
              workspaceKind = WorkspaceKind.QAHAL,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.VOLUNTEER_ROTATION,
                rotation.id,
              ),
            ),
          )
        }
        rotation.slots.forEach { slot ->
          if (
            matches(
              workspace.label,
              rotation.label,
              slot.status.name,
              slot.serviceOn.toString(),
              slot.assignedTo?.value.orEmpty(),
            )
          ) {
            add(
              WorkspaceSearchEntry(
                id = "qahal:${workspace.id.value}:rotation:${rotation.id.value}:slot:${slot.id.value}",
                title = "${rotation.label} · ${slot.serviceOn}",
                detail = slot.assignedTo?.value ?: "Unassigned volunteer slot",
                category = "Volunteer slot",
                workspaceKind = WorkspaceKind.QAHAL,
                address = WorkspaceRecordAddress(
                  workspace.id,
                  WorkspaceRecordKind.VOLUNTEER_SLOT,
                  slot.id,
                  rotation.id,
                ),
              ),
            )
          }
        }
      }
      workspace.inventory.forEach { item ->
        if (matches(workspace.label, item.label, item.status.name, item.nextCheckOn?.toString().orEmpty())) {
          add(
            WorkspaceSearchEntry(
              id = "qahal:${workspace.id.value}:inventory:${item.id.value}",
              title = item.label,
              detail = "${item.quantityOnHand} on hand · minimum ${item.minimumDesired}",
              category = "Inventory",
              workspaceKind = WorkspaceKind.QAHAL,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.INVENTORY_ITEM,
                item.id,
              ),
            ),
          )
        }
      }
      workspace.financialControls.forEach { checklist ->
        if (matches(workspace.label, checklist.label, checklist.status.name, checklist.reviewOn?.toString().orEmpty())) {
          add(
            WorkspaceSearchEntry(
              id = "qahal:${workspace.id.value}:financial:${checklist.id.value}",
              title = checklist.label,
              detail = "Procedure checklist · ${workspace.label}",
              category = "Financial control",
              workspaceKind = WorkspaceKind.QAHAL,
              address = WorkspaceRecordAddress(
                workspace.id,
                WorkspaceRecordKind.FINANCIAL_CHECKLIST,
                checklist.id,
              ),
            ),
          )
        }
        checklist.controls.forEach { control ->
          if (
            matches(
              workspace.label,
              checklist.label,
              control.kind.name,
              control.status.name,
              control.dueOn?.toString().orEmpty(),
              control.responsibleRole?.value.orEmpty(),
            )
          ) {
            add(
              WorkspaceSearchEntry(
                id = "qahal:${workspace.id.value}:financial:${checklist.id.value}:control:${control.id.value}",
                title = control.kind.name.lowercase().replace('_', ' '),
                detail = "${checklist.label} · ${control.status.name.lowercase().replace('_', ' ')}",
                category = "Financial control item",
                workspaceKind = WorkspaceKind.QAHAL,
                address = WorkspaceRecordAddress(
                  workspace.id,
                  WorkspaceRecordKind.FINANCIAL_CONTROL,
                  control.id,
                  checklist.id,
                ),
              ),
            )
          }
        }
      }
    }
  }.take(limit)
}
