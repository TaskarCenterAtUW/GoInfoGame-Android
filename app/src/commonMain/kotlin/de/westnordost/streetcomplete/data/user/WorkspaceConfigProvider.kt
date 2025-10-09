package de.westnordost.streetcomplete.data.user

interface WorkspaceConfigProvider {
    val osmBaseUrl : String
    val workspaceToken: String?
    val workspaceId : Int?
}
