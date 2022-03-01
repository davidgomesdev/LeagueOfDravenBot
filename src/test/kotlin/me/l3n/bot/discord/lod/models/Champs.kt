package me.l3n.bot.discord.lod.models

import me.l3n.bot.discord.lod.model.Champion
import me.l3n.bot.discord.lod.model.Role
import me.l3n.bot.discord.lod.model.Rotation


object Champs {
    object NoIcon {
        val draven = Champion("Draven", 1, "Draven", null)
        val nami = Champion("Tsunami", 2, "Nami", null)
        val lulu = Champion("Lullaby", 3, "Lulu", null)
        val syndra = Champion("Syndra", 4, "Syndra", null)
        val nunu = Champion("Snowball", 5, "Nunu & Willump", null)
        val knox = Champion("Knox", 6, "Knox", null)

        val all = listOf(draven, nami, lulu, syndra, nunu, knox)
    }

    object WithIcon {
        val draven = NoIcon.draven.copy(icon = listOf(1, 2, 3))
        val nami = NoIcon.nami.copy(icon = listOf(4, 2, 6))
        val lulu = NoIcon.lulu.copy(icon = listOf(9, 8, 4))
        val syndra = NoIcon.syndra.copy(icon = listOf(8, 8, 8))
        val nunu = NoIcon.nunu.copy(icon = listOf(5, 65, 4))
        val knox = NoIcon.knox.copy(icon = listOf(100, 2, 0))

        val all = listOf(NoIcon.draven, NoIcon.nami, NoIcon.lulu, NoIcon.syndra, NoIcon.nunu, NoIcon.knox)
    }

    val noIconsList = mapOf(
        NoIcon.draven.id to NoIcon.draven,
        NoIcon.nami.id to NoIcon.nami,
        NoIcon.lulu.id to NoIcon.lulu,
        NoIcon.syndra.id to NoIcon.syndra,
        NoIcon.nunu.id to NoIcon.nunu,
        NoIcon.knox.id to NoIcon.knox,
    )
    val withIconsList = mapOf(
        WithIcon.draven.id to WithIcon.draven,
        WithIcon.nami.id to WithIcon.nami,
        WithIcon.lulu.id to WithIcon.lulu,
        WithIcon.syndra.id to WithIcon.syndra,
        WithIcon.nunu.id to WithIcon.nunu,
        WithIcon.knox.id to WithIcon.knox,
    )

    val currentRotationIds = listOf(NoIcon.draven.key, NoIcon.lulu.key)
    val currentRotation = mapOf(
        NoIcon.draven.id to NoIcon.draven,
        NoIcon.lulu.id to NoIcon.lulu,
    )
    val currentRotationWithIcons = mapOf(
        WithIcon.draven.id to WithIcon.draven,
        WithIcon.lulu.id to WithIcon.lulu,
        WithIcon.knox.id to WithIcon.knox,
    )

    val allRolesFilled = mapOf(
        NoIcon.draven.id to Role.Bot,
        NoIcon.nami.id to Role.Support,
        NoIcon.lulu.id to Role.Top,
        NoIcon.syndra.id to Role.Mid,
        NoIcon.nunu.id to Role.Jungle,
        NoIcon.knox.id to Role.Unknown,
    )
    val rolesFilledWithoutUnknown = mapOf(
        NoIcon.draven.id to Role.Bot,
        NoIcon.nami.id to Role.Support,
        NoIcon.lulu.id to Role.Top,
        NoIcon.syndra.id to Role.Mid,
        NoIcon.nunu.id to Role.Jungle,
        NoIcon.knox.id to Role.Support,
    )
    val someRolesFilled = mapOf(
        NoIcon.draven.id to Role.Bot,
        NoIcon.nami.id to Role.Support,
        NoIcon.lulu.id to Role.Bot,
        NoIcon.syndra.id to Role.Mid,
        NoIcon.nunu.id to Role.Support,
        NoIcon.knox.id to Role.Unknown,
    )
    val someRolesFilledWithoutUnknown = mapOf(
        NoIcon.draven.id to Role.Bot,
        NoIcon.nami.id to Role.Support,
        NoIcon.lulu.id to Role.Bot,
        NoIcon.syndra.id to Role.Mid,
        NoIcon.nunu.id to Role.Support,
        NoIcon.knox.id to Role.Mid,
    )

    val roledDTO = mapOf(
        Role.Top to listOf(WithIcon.lulu, WithIcon.knox),
        Role.Jungle to listOf(),
        Role.Mid to listOf(WithIcon.syndra),
        Role.Bot to listOf(WithIcon.draven),
        Role.Support to listOf(WithIcon.lulu),
    )
}

object Rotations {
    val withoutLowLevel = Rotation(Champs.NoIcon.all, Champs.roledDTO, null)
}
