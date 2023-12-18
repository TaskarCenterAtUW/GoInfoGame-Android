package com.tcatuw.goinfo.testutils

import com.tcatuw.goinfo.data.osm.edits.ElementEdit
import com.tcatuw.goinfo.data.osm.edits.ElementEditAction
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapChanges
import com.tcatuw.goinfo.data.osm.edits.update_tags.StringMapEntryAdd
import com.tcatuw.goinfo.data.osm.edits.update_tags.UpdateElementTagsAction
import com.tcatuw.goinfo.data.osm.geometry.ElementGeometry
import com.tcatuw.goinfo.data.osm.geometry.ElementPointGeometry
import com.tcatuw.goinfo.data.osm.mapdata.BoundingBox
import com.tcatuw.goinfo.data.osm.mapdata.ElementType
import com.tcatuw.goinfo.data.osm.mapdata.LatLon
import com.tcatuw.goinfo.data.osm.mapdata.Node
import com.tcatuw.goinfo.data.osm.mapdata.Relation
import com.tcatuw.goinfo.data.osm.mapdata.RelationMember
import com.tcatuw.goinfo.data.osm.mapdata.Way
import com.tcatuw.goinfo.data.osm.osmquests.OsmElementQuestType
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuest
import com.tcatuw.goinfo.data.osm.osmquests.OsmQuestHidden
import com.tcatuw.goinfo.data.osmnotes.Note
import com.tcatuw.goinfo.data.osmnotes.NoteComment
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEdit
import com.tcatuw.goinfo.data.osmnotes.edits.NoteEditAction
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuest
import com.tcatuw.goinfo.data.osmnotes.notequests.OsmNoteQuestHidden
import com.tcatuw.goinfo.data.osmtracks.Trackpoint
import com.tcatuw.goinfo.data.quest.OsmQuestKey
import com.tcatuw.goinfo.data.quest.TestQuestTypeA
import com.tcatuw.goinfo.data.user.User
import com.tcatuw.goinfo.util.ktx.nowAsEpochMilliseconds

fun p(lat: Double = 0.0, lon: Double = 0.0) = LatLon(lat, lon)

fun node(
    id: Long = 1,
    pos: LatLon = p(),
    tags: Map<String, String> = emptyMap(),
    version: Int = 1,
    timestamp: Long? = null
) = Node(id, pos, tags, version, timestamp ?: nowAsEpochMilliseconds())

fun way(
    id: Long = 1,
    nodes: List<Long> = listOf(),
    tags: Map<String, String> = emptyMap(),
    version: Int = 1,
    timestamp: Long? = null
) = Way(id, nodes, tags, version, timestamp ?: nowAsEpochMilliseconds())

fun rel(
    id: Long = 1,
    members: List<RelationMember> = listOf(),
    tags: Map<String, String> = emptyMap(),
    version: Int = 1,
    timestamp: Long? = null
) = Relation(id, members.toMutableList(), tags, version, timestamp ?: nowAsEpochMilliseconds())

fun member(
    type: ElementType = ElementType.NODE,
    ref: Long = 1,
    role: String = ""
) = RelationMember(type, ref, role)

fun bbox(latMin: Double = 0.0, lonMin: Double = 0.0, latMax: Double = 1.0, lonMax: Double = 1.0) =
    BoundingBox(latMin, lonMin, latMax, lonMax)

fun waysAsMembers(wayIds: List<Long>, role: String = ""): List<RelationMember> =
    wayIds.map { id -> member(ElementType.WAY, id, role) }.toMutableList()

fun pGeom(lat: Double = 0.0, lon: Double = 0.0) = ElementPointGeometry(p(lat, lon))

fun note(
    id: Long = 1,
    position: LatLon = p(0.0, 0.0),
    timestamp: Long = 0,
    comments: List<NoteComment> = listOf(comment("test", NoteComment.Action.OPENED))
) = Note(position, id, timestamp, null, Note.Status.OPEN, comments)

fun comment(
    text: String,
    action: NoteComment.Action = NoteComment.Action.COMMENTED,
    timestamp: Long = 0,
    user: User? = null
) = NoteComment(timestamp, action, text, user)

fun noteEdit(
    id: Long = 1,
    noteId: Long = 5,
    action: NoteEditAction = NoteEditAction.COMMENT,
    text: String = "test123",
    timestamp: Long = 123L,
    imagePaths: List<String> = emptyList(),
    pos: LatLon = p(1.0, 1.0),
    isSynced: Boolean = false,
    track: List<Trackpoint> = emptyList(),
) = NoteEdit(
    id,
    noteId,
    pos,
    action,
    text,
    imagePaths,
    timestamp,
    isSynced,
    imagePaths.isNotEmpty(),
    track
)

fun edit(
    id: Long = 1L,
    geometry: ElementGeometry = pGeom(),
    timestamp: Long = 123L,
    action: ElementEditAction = UpdateElementTagsAction(node(), StringMapChanges(setOf(StringMapEntryAdd("hey", "ho")))),
    isSynced: Boolean = false
) = ElementEdit(
    id,
    QUEST_TYPE,
    geometry,
    "survey",
    timestamp,
    isSynced,
    action
)

fun questHidden(
    elementType: ElementType = ElementType.NODE,
    elementId: Long = 1L,
    questType: OsmElementQuestType<*> = QUEST_TYPE,
    pos: LatLon = p(),
    timestamp: Long = 123L
) = OsmQuestHidden(elementType, elementId, questType, pos, timestamp)

fun noteQuestHidden(
    note: Note = note(),
    timestamp: Long = 123L
) = OsmNoteQuestHidden(note, timestamp)

fun osmQuest(
    questType: OsmElementQuestType<*> = QUEST_TYPE,
    elementType: ElementType = ElementType.NODE,
    elementId: Long = 1L,
    geometry: ElementGeometry = pGeom()
) =
    OsmQuest(questType, elementType, elementId, geometry)

fun osmNoteQuest(
    id: Long = 1L,
    pos: LatLon = p()
) = OsmNoteQuest(id, pos)

fun osmQuestKey(
    elementType: ElementType = ElementType.NODE,
    elementId: Long = 1L,
    questTypeName: String = QUEST_TYPE.name
) = OsmQuestKey(elementType, elementId, questTypeName)

val QUEST_TYPE = TestQuestTypeA()
