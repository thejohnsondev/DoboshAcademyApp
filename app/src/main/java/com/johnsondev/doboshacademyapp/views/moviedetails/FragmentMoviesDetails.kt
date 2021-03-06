package com.johnsondev.doboshacademyapp.views.moviedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.transition.MaterialContainerTransform
import com.johnsondev.doboshacademyapp.R
import com.johnsondev.doboshacademyapp.adapters.ActorsAdapter
import com.johnsondev.doboshacademyapp.data.models.Movie
import com.johnsondev.doboshacademyapp.utilities.Constants.MOVIE_ID
import com.johnsondev.doboshacademyapp.utilities.Constants.MOVIE_KEY
import com.johnsondev.doboshacademyapp.utilities.InternetConnectionManager
import com.johnsondev.doboshacademyapp.utilities.themeColor
import com.johnsondev.doboshacademyapp.viewmodel.MovieDetailsViewModel
import kotlinx.coroutines.*
import java.util.*

class FragmentMoviesDetails : Fragment() {

    private var currentMovie: Movie? = null

    private var tvTitle: TextView? = null
    private var tvAge: TextView? = null
    private var tvGenres: TextView? = null
    private var tvReviews: TextView? = null
    private var movieRating: RatingBar? = null
    private var tvDescription: TextView? = null
    private var tvStoryLine: TextView? = null
    private var tvCast: TextView? = null
    private var headImage: ImageView? = null
    private var rvActors: RecyclerView? = null
    private var adapter: ActorsAdapter? = null
    private var addToCalendarBtn: Button? = null
    private var backBtn: TextView? = null

    private var date: Calendar? = null

    private lateinit var detailsViewModel: MovieDetailsViewModel
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var checkInternetConnection: InternetConnectionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_movies_details, container, false)
        date = Calendar.getInstance()

        initViews(view)
        initListeners()

        if (currentMovie?.id!! == 0) {
            tvReviews?.isVisible = false
            movieRating?.isVisible = false
            tvDescription?.isVisible = false
            tvStoryLine?.isVisible = false
            tvCast?.isVisible = false
            addToCalendarBtn?.isVisible = false
        }

        currentMovie?.let { movie ->
            val movieReviews: String =
                view.context.getString(R.string.reviews, movie.numberOfRatings)
            val movieAge: String = view.context.getString(R.string.plus, movie.minimumAge)
            tvTitle?.text = movie.title
            tvAge?.text = movieAge
            tvGenres?.text = movie.genres?.joinToString { it.name }
            tvReviews?.text = movieReviews
            movieRating?.progress = (movie.ratings * 2).toInt()
            tvDescription?.text = movie.overview

            detailsViewModel.getActorsForCurrentMovie()

            detailsViewModel.actorsList.observe(viewLifecycleOwner) {
                adapter?.setActors(it)
            }

            headImage?.load(movie.poster) {
                crossfade(true)
                placeholder(R.drawable.movie_placeholder)
                fallback(R.drawable.movie_placeholder)
                error(R.drawable.movie_placeholder)
            }
        }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        detailsViewModel = ViewModelProvider(this)[MovieDetailsViewModel::class.java]
        val movieId = arguments?.getInt(MOVIE_ID)

        currentMovie = if (movieId != 0) {
            detailsViewModel.getMovieById(movieId!!)
        } else {
            arguments?.getParcelable(MOVIE_KEY)
        }

        checkInternetConnection = InternetConnectionManager(requireContext())

        if (checkInternetConnection.isNetworkAvailable()) {
            scope.launch {
                if (currentMovie?.id!! != 0) {
                    detailsViewModel.loadActorsForMovieById(currentMovie?.id!!)
                }
            }
        } else {
            Toast.makeText(context, getString(R.string.unable_load_cast), Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun initViews(view: View) {

        tvTitle = view.findViewById(R.id.tv_title)
        tvAge = view.findViewById(R.id.tv_age)
        tvGenres = view.findViewById(R.id.movie_genres)
        tvReviews = view.findViewById(R.id.tv_reviews)
        movieRating = view.findViewById(R.id.movie_rating_bar)
        tvDescription = view.findViewById(R.id.tv_description)
        tvStoryLine = view.findViewById(R.id.tv_story_line)
        tvCast = view.findViewById(R.id.tv_cast)
        headImage = view.findViewById(R.id.head_image)
        addToCalendarBtn = view.findViewById(R.id.add_to_calendar_btn)
        backBtn = view.findViewById(R.id.back_btn)

        adapter = ActorsAdapter(requireContext())
        rvActors = view.findViewById(R.id.rv_actors)
        rvActors?.adapter = adapter
        rvActors?.setHasFixedSize(true)

    }

    private fun initListeners() {
        addToCalendarBtn?.setOnClickListener {
            detailsViewModel.callDatePicker(requireContext(), date!!, currentMovie!!)
        }

        backBtn?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

