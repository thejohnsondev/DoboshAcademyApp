package com.johnsondev.doboshacademyapp.data.response

import com.johnsondev.doboshacademyapp.data.dto.MovieDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieResponse(
    val page: Long,
    val results: List<MovieDto>,

    @SerialName("total_pages")
    val totalPages: Long,

    @SerialName("total_results")
    val totalResults: Long,
)