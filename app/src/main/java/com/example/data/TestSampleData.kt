package com.example.data

import com.example.data.local.entity.ActorEntity
import com.example.data.local.entity.LinkEntity
import com.example.data.local.entity.StudioEntity
import com.example.data.repository.VaultRepository
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Temporary Test Sample Data File
 * You can delete this file at any time when testing is complete.
 */
object TestSampleData {

    private fun parseDateToMillis(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("d MMM yyyy", Locale.US)
            format.parse(dateStr.trim())?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    data class RawTestItem(
        val title: String,
        val coverUrl: String,
        val hdUrl: String = "",
        val fourKUrl: String = "",
        val hdMagnet: String = "",
        val fourKMagnet: String = "",
        val actors: List<String> = emptyList(),
        val studio: String = "",
        val dateString: String = ""
    )

    val items = listOf(
        RawTestItem(
            title = "Will Scrub For Cock",
            coverUrl = "https://stashdb.org/images/72cd5131-96ae-40ca-a497-3da1ccf0d24e",
            hdUrl = "",
            fourKUrl = "",
            hdMagnet = "magnet:?xt=urn:btih:28ab4f299ef54beabc54bdb216e7423c68d224fc&dn=BrazzersExxtra",
            fourKMagnet = "magnet:?xt=urn:btih:7d5cdd299e4cdca04ced774f307ce6ac5d22c981&dn=BrazzersExxtra",
            actors = listOf("Raissa Bellini"),
            studio = "Brazzers Exxtra",
            dateString = "2 Jul 2026"
        ),
        RawTestItem(
            title = "Flexible Dance Instructor Worships His Cock",
            coverUrl = "https://stashdb.org/images/23c4deed-576d-4b16-916f-a7b3666997fe",
            hdUrl = "",
            fourKUrl = "",
            hdMagnet = "magnet:?xt=urn:btih:7ccd2218e04a722d9aa4c6c4614d2de288de3d17&dn=Milfy",
            fourKMagnet = "magnet:?xt=urn:btih:42c28c39b9c466003467e946bde7b0476df146fe&dn=Milfy",
            actors = listOf("Raissa Bellini"),
            studio = "Milfy",
            dateString = "27 May 2026"
        ),
        RawTestItem(
            title = "The Big Booty Maid Incident",
            coverUrl = "https://stashdb.org/images/28cb664a-b915-4712-8d6d-e1e320784f6c",
            hdUrl = "",
            fourKUrl = "",
            hdMagnet = "magnet:?xt=urn:btih:da18c271148f0d212c3b2e614822399996dc8c00&dn=AssParade",
            fourKMagnet = "magnet:?xt=urn:btih:3ad40eee25f45c60919c94951a2ecd54956ad258&dn=AssParade",
            actors = listOf("Roxanne Roselle"),
            studio = "Ass Parade",
            dateString = "7 Sept 2026"
        ),
        RawTestItem(
            title = "Catching Her Wife With The Sitter",
            coverUrl = "https://stashdb.org/images/d5c7409c-e65f-463d-9b07-c5a20124f9b5",
            hdUrl = "",
            fourKUrl = "",
            hdMagnet = "magnet:?xt=urn:btih:96f94deddf3f41b39dc892d12b70eeb31cc9673b&dn=AdultTime",
            fourKMagnet = "magnet:?xt=urn:btih:3eebb9fbeefe30129bd3952e50eca86aa904a902&dn=AdultTime",
            actors = listOf("August Skye", "Sarah Arabic", "Coco Lovelock"),
            studio = "Lez Be Bad",
            dateString = "28 Aug 2026"
        ),
        RawTestItem(
            title = "Plaything Gets Dominated By Her Sugar Daddy",
            coverUrl = "https://stashdb.org/images/96ce62e4-3199-45c4-842a-7ec027e6aa18",
            hdUrl = "",
            fourKUrl = "",
            hdMagnet = "magnet:?xt=urn:btih:3c731bb06c329b8a076675c9334b509edbde9d90&dn=Blacked",
            fourKMagnet = "magnet:?xt=urn:btih:fc619857eb5bd64566f414dc8ce168ad65509c0d&dn=Blacked",
            actors = listOf("Melztube"),
            studio = "Blacked",
            dateString = "28 Apr 2026"
        )
    )

    suspend fun seed(repository: VaultRepository) {
        val studioMap = mutableMapOf<String, String>()
        val actorMap = mutableMapOf<String, String>()

        // 1. Insert Studios
        items.map { it.studio.trim() }.filter { it.isNotEmpty() }.distinct().forEach { studioName ->
            val existingId = studioMap[studioName] ?: UUID.randomUUID().toString()
            studioMap[studioName] = existingId
            repository.insertStudio(
                StudioEntity(
                    id = existingId,
                    name = studioName
                )
            )
        }

        // 2. Insert Actors
        items.flatMap { it.actors }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { actorName ->
            val existingId = actorMap[actorName] ?: UUID.randomUUID().toString()
            actorMap[actorName] = existingId
            repository.insertActor(
                ActorEntity(
                    id = existingId,
                    name = actorName
                )
            )
        }

        // 3. Insert Link Entities
        items.forEachIndexed { index, raw ->
            val linkId = "test_item_${index + 1}"
            val studioId = studioMap[raw.studio.trim()]
            val actorIds = raw.actors.mapNotNull { actorMap[it.trim()] }
            val dateMillis = parseDateToMillis(raw.dateString)

            repository.insertLink(
                LinkEntity(
                    id = linkId,
                    title = raw.title,
                    coverImage = raw.coverUrl,
                    urlHD = raw.hdUrl.ifBlank { null },
                    url4K = raw.fourKUrl.ifBlank { null },
                    magnet = raw.hdMagnet.ifBlank { null },
                    magnet4K = raw.fourKMagnet.ifBlank { null },
                    studioIds = if (studioId != null) listOf(studioId) else emptyList(),
                    actorIds = actorIds,
                    assignedDate = dateMillis,
                    createdAt = System.currentTimeMillis() - (index * 60000L)
                )
            )
        }
    }
}
