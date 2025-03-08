package com.jonathanfoucher.movieapi.controllers;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jonathanfoucher.movieapi.data.dto.MovieDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(MovieController.class)
class MovieControllerTest {
    private MockMvc mockMvc;
    @Autowired
    private MovieController movieController;

    private static final String MOVIES_WITH_ID_PATH = "/movies/{movie_id}";
    private static final String MOVIES_PATH = "/movies";

    private static final Long ID = 15L;
    private static final String TITLE = "Title";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2020, 1, 1);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger log = (Logger) LoggerFactory.getLogger(MovieController.class);
    private static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    void initEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(movieController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        listAppender.list.clear();
        listAppender.start();
        log.addAppender(listAppender);
    }

    @AfterEach
    void reset() {
        log.detachAppender(listAppender);
        listAppender.stop();
    }

    @Test
    void getMovie() throws Exception {
        // GIVEN
        MovieDto movie = initMovie();

        // WHEN / THEN
        mockMvc.perform(get(MOVIES_WITH_ID_PATH, ID)
                        .header("Accept", APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(movie)));

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertNotNull(logsList.getFirst());
        assertEquals(Level.INFO, logsList.getFirst().getLevel());
        assertEquals("Received request to get movie 15 with headers [Accept:\"application/json\"]", logsList.getFirst().getFormattedMessage());
    }

    @Test
    void saveMovie() throws Exception {
        // GIVEN
        MovieDto movieDto = initMovie();

        // WHEN / THEN
        mockMvc.perform(post(MOVIES_PATH)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(movieDto))
                )
                .andExpect(status().isOk());

        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals(1, logsList.size());
        assertNotNull(logsList.getFirst());
        assertEquals(Level.INFO, logsList.getFirst().getLevel());
        assertEquals("Received request to save movie: { id=15, title=\"Title\", release_date=2020-01-01 } with headers [Content-Type:\"application/json\", Content-Length:\"51\"]", logsList.getFirst().getFormattedMessage());
    }

    private MovieDto initMovie() {
        MovieDto movie = new MovieDto();
        movie.setId(ID);
        movie.setTitle(TITLE);
        movie.setReleaseDate(RELEASE_DATE);
        return movie;
    }
}
