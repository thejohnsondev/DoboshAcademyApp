package com.johnsondev.doboshacademyapp.views.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.johnsondev.doboshacademyapp.App
import com.johnsondev.doboshacademyapp.R
import com.johnsondev.doboshacademyapp.data.repositories.MoviesRepository
import com.johnsondev.doboshacademyapp.utilities.InternetConnectionManager
import com.johnsondev.doboshacademyapp.utilities.Constants
import com.johnsondev.doboshacademyapp.utilities.Constants.MOVIE_ID
import kotlinx.coroutines.*

class SplashScreenActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var checkInternetConnection: InternetConnectionManager

    private val moviesDatabase = App.getInstance().getMovieDatabase()

    private var movieId: Int? = null


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainActivityIntent = Intent(this, MainActivity::class.java)

        checkInternetConnection = InternetConnectionManager(this)
        scope.launch {
            if (moviesDatabase.movieDao().getAllMovies().isNotEmpty()) {

                scope.launch {
                    MoviesRepository.loadPopularMoviesFromDb()
                    MoviesRepository.loadTopRatedMoviesFromDb()
                    MoviesRepository.loadUpcomingMoviesFromDb()
                }.join()

                if (intent != null) {
                    handleIntent(intent)
                    mainActivityIntent.putExtra(MOVIE_ID, movieId)
                    startActivity(mainActivityIntent)
                    finish()
                }else{
                    startActivity(mainActivityIntent)
                    finish()
                }

            } else {
                if (checkInternetConnection.isNetworkAvailable()) {
                    scope.launch {
                        MoviesRepository.loadMoviesFromNet()
                    }.join()
                    openNextActivity(mainActivityIntent)
                } else {
                    openNextActivity(mainActivityIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                movieId = intent.data?.lastPathSegment?.toIntOrNull()
            }
        }
    }

    private fun openNextActivity(intent: Intent){
        intent.putExtra(Constants.CONNECTION_ERROR_EXTRA, true)
        startActivity(intent)
        finish()
    }



}