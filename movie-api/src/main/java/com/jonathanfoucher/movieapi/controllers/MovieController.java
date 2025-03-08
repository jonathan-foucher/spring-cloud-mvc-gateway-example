package com.jonathanfoucher.movieapi.controllers;

import com.jonathanfoucher.movieapi.data.dto.MovieDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/movies")
public class MovieController {
    Logger log = LoggerFactory.getLogger(MovieController.class);

    @GetMapping("/{movie_id}")
    public MovieDto getMovie(@PathVariable("movie_id") Long movieId,
                             @RequestHeader HttpHeaders headers
    ) {
        log.info("Received request to get movie {} with headers {}", movieId, headers);
        return initMovie(movieId);
    }

    @PostMapping
    public void saveMovie(@RequestBody MovieDto movie,
                          @RequestHeader HttpHeaders headers
    ) {
        log.info("Received request to save movie: {} with headers {}", movie, headers);
    }

    private MovieDto initMovie(Long id) {
        MovieDto movie = new MovieDto();
        movie.setId(id);
        movie.setTitle("Title");
        movie.setReleaseDate(LocalDate.of(2020, 1, 1));
        return movie;
    }
}
