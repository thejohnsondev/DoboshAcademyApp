package com.johnsondev.doboshacademyapp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "actors")
data class ActorEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val picture: String?
)