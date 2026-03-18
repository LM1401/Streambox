package com.example.streambox

object MockMovieData {
    fun getMockMovies(): List<TmdbMovie> {
        return listOf(
            TmdbMovie(
                id = 1,
                title = "Deadpool & Wolverine",
                posterPath = "/8cdWjNlecmHG8CJUBfTL2s32.jpg",
                backdropPath = "/yDHYQX0SvnYvYWLhoenpB9SvepX.jpg",
                overview = "A listless Wade Wilson toils away in civilian life. His days as a morally flexible mercenary, Deadpool, behind him.",
                voteAverage = 7.8,
                releaseDate = "2024-07-24"
            ),
            TmdbMovie(
                id = 2,
                title = "Inside Out 2",
                posterPath = "/vpnxB9goS966MGv77it9696st9.jpg",
                backdropPath = "/stKG98o9S966MGv77it9696st9.jpg",
                overview = "Teenager Riley's mind headquarters is undergoing a sudden demolition to make room for something entirely unexpected: new Emotions!",
                voteAverage = 8.1,
                releaseDate = "2024-06-12"
            ),
            TmdbMovie(
                id = 3,
                title = "Kingdom of Planet of the Apes",
                posterPath = "/gKkl37BQu6f8tWc99Bjql2hq.jpg",
                backdropPath = "/fqv8v6hBQu6f8tWc99Bjql2hq.jpg",
                overview = "Several generations in the future following Caesar's reign, apes are now the dominant species.",
                voteAverage = 7.2,
                releaseDate = "2024-05-08"
            ),
            TmdbMovie(
                id = 4,
                title = "Bad Boys: Ride or Die",
                posterPath = "/oM9v966MGv77it9696st9.jpg",
                backdropPath = "/pM9v966MGv77it9696st9.jpg",
                overview = "After their late captain is framed, Lowrey and Burnett try to clear his name.",
                voteAverage = 7.0,
                releaseDate = "2024-06-05"
            ),
            TmdbMovie(
                id = 5,
                title = "Despicable Me 4",
                posterPath = "/w9v966MGv77it9696st9.jpg",
                backdropPath = "/x9v966MGv77it9696st9.jpg",
                overview = "Gru and Lucy and their girls—Margo, Edith and Agnes—welcome a new member to the Gru family, Gru Jr.",
                voteAverage = 7.3,
                releaseDate = "2024-06-20"
            )
        )
    }
}
