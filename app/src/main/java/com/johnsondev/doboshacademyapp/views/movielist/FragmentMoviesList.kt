package com.johnsondev.doboshacademyapp.views.movielist

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.*
import com.google.android.material.button.MaterialButtonToggleGroup
import com.johnsondev.doboshacademyapp.R
import com.johnsondev.doboshacademyapp.adapters.MoviesAdapter
import com.johnsondev.doboshacademyapp.adapters.OnRecyclerItemClicked
import com.johnsondev.doboshacademyapp.data.models.Movie
import com.johnsondev.doboshacademyapp.data.services.MovieDbUpdateWorker
import com.johnsondev.doboshacademyapp.utilities.InternetConnectionManager
import com.johnsondev.doboshacademyapp.utilities.Constants
import com.johnsondev.doboshacademyapp.utilities.Constants.HORIZONTAL_SPAN_COUNT
import com.johnsondev.doboshacademyapp.utilities.Constants.MOVIE_KEY
import com.johnsondev.doboshacademyapp.utilities.Constants.PERIODIC_UPDATE_WORK
import com.johnsondev.doboshacademyapp.utilities.Constants.VERTICAL_SPAN_COUNT
import com.johnsondev.doboshacademyapp.utilities.getUpdateTime
import com.johnsondev.doboshacademyapp.utilities.saveUpdateTime
import com.johnsondev.doboshacademyapp.views.moviedetails.FragmentMoviesDetails
import com.johnsondev.doboshacademyapp.viewmodel.MoviesListViewModel
import com.johnsondev.doboshacademyapp.viewmodel.MovieViewModelFactory
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class FragmentMoviesList : Fragment() {

    private lateinit var rvMovie: RecyclerView
    private lateinit var adapter: MoviesAdapter
    private lateinit var swipeToRefresh: SwipeRefreshLayout
    private lateinit var typeOfMoviesList: MaterialButtonToggleGroup
    private lateinit var listViewModel: MoviesListViewModel
    private lateinit var tvLastUpdateTime: TextView
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var checkInternetConnection: InternetConnectionManager
    private var isConnectionErrorFromBundle: Boolean? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_movies_list, container, false)

        listViewModel = ViewModelProvider(
            this,
            MovieViewModelFactory(activity?.application!!)
        )[MoviesListViewModel::class.java]

        initViews(view)
        initWorkManager()
        initListenersAndObservers(view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
    }


    private fun initViews(view: View) {

        tvLastUpdateTime = view.findViewById(R.id.last_update_tv)
        typeOfMoviesList = view.findViewById(R.id.toggle_group)
        swipeToRefresh = view.findViewById(R.id.swipe_layout)
        typeOfMoviesList.check(R.id.btn_popular)

        rvMovie = view.findViewById(R.id.movie_list_rv)
        rvMovie.layoutManager = GridLayoutManager(view.context, calculateSpanCount())
        adapter = MoviesAdapter(view.context, clickListener)
        rvMovie.adapter = adapter

        isConnectionErrorFromBundle = arguments?.getBoolean(Constants.CONNECTION_ERROR_ARG) == true
        checkInternetConnection = InternetConnectionManager(requireContext())

    }

    private fun initWorkManager() {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateWorkRequest =
            PeriodicWorkRequest.Builder(MovieDbUpdateWorker::class.java, 8, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            PERIODIC_UPDATE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )

//        it`s for tests
//        WorkManager.getInstance(requireContext()).enqueue(updateWorkRequest)

    }

    private fun initListenersAndObservers(view: View) {

        if (isConnectionErrorFromBundle == true && !listViewModel.isInternetConnectionAvailable()) {
            Toast.makeText(
                context,
                getString(R.string.internet_connection_error),
                Toast.LENGTH_SHORT
            ).show()
        }

        typeOfMoviesList.addOnButtonCheckedListener { _, checkedId, _ ->
            listViewModel.changeMoviesList(checkedId)
        }

        swipeToRefresh.setOnRefreshListener {
            if (!checkInternetConnection.isNetworkAvailable()) {
                Toast.makeText(
                    context,
                    getString(R.string.internet_connection_error),
                    Toast.LENGTH_SHORT
                ).show()
                swipeToRefresh.isRefreshing = false
            } else {
                scope.launch {
                   listViewModel.loadMoviesFromNet().apply {
                        swipeToRefresh.isRefreshing = false
                    }
                }
                saveUpdateTime(requireContext())
                tvLastUpdateTime.text =
                    view.context.getString(R.string.last_update, getUpdateTime(requireContext()))
            }
        }

        listViewModel.getPopularMovies()
        listViewModel.getTopRatedMovies()
        listViewModel.getUpcomingMovies()


        listViewModel.popularMoviesList.observe(viewLifecycleOwner) {
            adapter.setMovies(it)
        }

        listViewModel.getAnotherMovieList().observe(viewLifecycleOwner) { movie ->
            adapter.setMovies(movie)
        }

        listViewModel.getLastUpdateTime(requireContext()).observe(viewLifecycleOwner) { time ->
            tvLastUpdateTime.text = view.context.getString(R.string.last_update, time)
        }
    }

    private fun calculateSpanCount(): Int {
        return if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            VERTICAL_SPAN_COUNT else HORIZONTAL_SPAN_COUNT
    }

    private fun doOnClick(movie: Movie, view: View) {

//        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
//            duration = resources.getInteger(R.integer.shared_element_transition_duration).toLong()
//        }
//        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
//            duration = resources.getInteger(R.integer.shared_element_transition_duration).toLong()
//        }

//        exitTransition = MaterialElevationScale(false).apply {
//            duration = resources.getInteger(R.integer.shared_element_transition_duration).toLong()
//        }
//
//        reenterTransition = MaterialElevationScale(true).apply {
//            duration = resources.getInteger(R.integer.shared_element_transition_duration).toLong()
//        }



        val bundleWithMovie = Bundle()
        bundleWithMovie.putParcelable(MOVIE_KEY, movie)

        val fragmentMoviesDetails = FragmentMoviesDetails()
        fragmentMoviesDetails.arguments = bundleWithMovie

        rvMovie.let {
            parentFragmentManager.beginTransaction().apply {
                addSharedElement(view,getString(R.string.transition_name))
                addToBackStack(null)
                replace(R.id.main_container, fragmentMoviesDetails)
                commit()
            }
        }
    }

    private val clickListener = object : OnRecyclerItemClicked {
        override fun onClick(movie: Movie, view: View) {
            doOnClick(movie, view)
        }
    }

}

